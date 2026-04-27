# AI Chat Disposition Engine — Design Document

## 1. Problem statement

Converse is a B2B omnichannel conversation platform used by businesses in healthcare, BFSI, ed-tech, real estate, quick-commerce, and higher education worldwide. A **chat session** is a continuous conversation window between a **lead** (the end customer) and an **agent** (an employee of the business) across WhatsApp, SMS, or the web chat widget.

When a session ends — either by the agent clicking "End Session" or by inactivity timeout from the lead's last inbound message — the agent is expected to write a note summarising what happened. In practice this is inconsistent: some agents write thorough notes, most write one-liners, many skip it entirely. Downstream consumers of the lead profile (senior agents picking up future conversations, QA analysts, sales reps) therefore have no reliable way to understand past interactions without reading full transcripts.

The disposition engine automates this step: on every session close, it generates a structured summary via Claude and posts it to the lead's CRM timeline and the Converse conversation view.

## 2. Scope

**In scope:**
- Listening for session-close events within the Converse service boundary
- Fetching the full transcript from Elasticsearch
- Generating a structured summary via Claude on AWS Bedrock
- Persisting the disposition record to MySQL
- Caching the generated card in Redis for 24 hours
- Real-time delivery to the agent's Converse view via WebSocket
- Posting the disposition as an activity on the CRM lead timeline

**Out of scope:**
- Real-time mid-session summarization (this fires only on session close)
- Lead sentiment tracking across multiple sessions (one session = one disposition)
- Routing decisions based on disposition content
- Custom per-tenant prompt templates (single system prompt with tenant-industry context injection)

## 3. Domain model

**Tenant** — a business using Converse (e.g. a hospital, a bank, a university). Each tenant has an `industry` classification used to frame the Claude prompt.

**Lead** — the end customer messaging the business. Belongs to a single tenant.

**Agent** — an employee of the tenant who handles conversations in Converse.

**Chat session** — a continuous conversation between one lead and one agent, bounded by session start and session end. Channel is fixed per session (WhatsApp, SMS, or web bot widget).

**Disposition** — the output of this service. One disposition per session.

## 4. Trigger semantics

Two code paths converge on the same event:

### 4.1 Agent-initiated close
Agent clicks "End Session" in the Converse UI. The Converse session service:
1. Transitions `chat_session.session_status` to `CLOSED`
2. Sets `session_ended_at` and `close_reason = AGENT_CLOSED`
3. Publishes a Spring `ApplicationEvent` (in-process)
4. Returns HTTP 200 to the agent's UI

The disposition generator subscribes to this event via `@EventListener @Async`, so step 4 does not wait for disposition generation.

### 4.2 Auto-expiry
A scheduled job runs every minute, scanning sessions where:
- `session_status = 'ACTIVE'`
- Last inbound message timestamp is older than the tenant-configured inactivity window (default 30 minutes, configurable per tenant)

For each match it transitions the session to `EXPIRED` with `close_reason = AUTO_EXPIRED` and publishes the same event.

### 4.3 Idempotency
The same session must never produce two dispositions. The guard is a `UNIQUE(session_id)` constraint on `chat_disposition`. The generator attempts to insert a `PENDING` row first; on duplicate key, the task aborts. This handles:
- Both close paths firing in the same minute (agent closes just as the expiry job runs)
- Event-listener retries within Spring
- Manual re-triggering via admin API

## 5. Event payload

```java
public record SessionClosedEvent(
    long sessionId,
    String tenantId,
    String leadId,
    String agentId,
    Channel channel,              // WHATSAPP | SMS | WEB_BOT
    String businessPhone,         // nullable when channel = WEB_BOT
    CloseReason closeReason,      // AGENT_CLOSED | AUTO_EXPIRED
    Instant sessionStartedAt,
    Instant sessionEndedAt,
    TenantIndustry tenantIndustry // BFSI | EDTECH | QUICK_COMMERCE | REAL_ESTATE | HEALTHCARE | HIGHER_EDUCATION | OTHER
) {}
```

`tenantIndustry` is denormalized onto the event at publish time — the disposition flow should not need a synchronous DB lookup just to pick the right prompt framing.

## 6. Database schema

### 6.1 `chat_disposition`

The source of truth for dispositions, owned by this service. See `V2__chat_disposition.sql` for the exact DDL.

Key columns:
- `session_id BIGINT UNIQUE` — the idempotency guard
- `status VARCHAR(24)` — state machine: `PENDING → GENERATING → COMPLETED | FAILED | FALLBACK`
- `summary TEXT` — Claude's 2-4 sentence narrative
- `lead_intent`, `agent_action`, `sentiment`, `unresolved` — structured fields from Claude
- `key_entities_json JSON` — extracted entities (order IDs, amounts, dates, etc.)
- `crm_delivered_at`, `converse_delivered_at` — delivery timestamps (nullable until delivered)

### 6.2 Read models

`chat_session`, `lead`, `agent` — these are **owned by Converse** in production. In this repo they are defined in `V1__converse_read_models.sql` so the service can run end-to-end locally. In production the service would connect to Converse's schema via a read replica or a shared schema.

## 7. Processing pipeline

### 7.1 Stage 1 — Ingestion
`SessionClosedEventHandler.onSessionClosed()`:
1. Insert `chat_disposition` row with `status = PENDING`. On `DuplicateKeyException`, abort (idempotency).
2. Submit task to `@Async` executor. Immediately return.

### 7.2 Stage 2 — Transcript fetch
`TranscriptFetcher.fetch(sessionId)`:
1. Query Elasticsearch index `converse-messages-*` filtered by `session_id`, sorted by `sent_at ASC`.
2. Apply transcript budget:
   - If total messages ≤ 150 and total chars ≤ 48,000: pass through
   - Otherwise: keep first 10 messages + last 50, insert `[... N messages omitted ...]` marker
3. Format each message as `{ROLE} [{relative_time}]: {content}` where ROLE is `LEAD` or `AGENT`

Relative time is `session_start → now` in `mm:ss` or `HH:mm:ss`. This strips timezone ambiguity and reduces token count.

### 7.3 Stage 3 — LLM call
`BedrockClaudeClient.summarize(context, transcript)`:
1. Build the Messages API request body (see §8 for prompt spec)
2. Call `bedrock-runtime.invokeModel` with the configured model id
3. Parse response, extract the assistant message content
4. Strip any markdown code fences Claude may have added despite the system prompt
5. Parse as `DispositionSummary` JSON via Jackson

Wrapped in `@Retry(name = "bedrock")` — up to 3 attempts with exponential backoff on throttling or network errors. Wrapped in `@CircuitBreaker(name = "bedrock")` — opens after 50% failure rate over 20 calls, stays open for 30s, then half-open.

### 7.4 Stage 4 — Validation + repair
`DispositionValidator.validate(summary)`:
- `summary` is non-null, 1–800 chars
- `sentiment` is a valid enum value
- `key_entities` is a list (possibly empty), each entity has `type` and `value`
- If validation fails: one retry with a "repair" prompt that includes the invalid response and asks Claude to fix the schema violation specifically

### 7.5 Stage 5 — Assembly + persist
`DispositionAssembler.assemble(event, summary)`:
1. Fetch lead + agent metadata from MySQL via jOOQ
2. Build `DispositionCard` with all metadata + summary fields
3. Update `chat_disposition` row: `status = COMPLETED`, all summary fields populated
4. Write serialized card to Redis: `disposition:{tenant_id}:{session_id}` with 24h TTL

### 7.6 Stage 6 — Parallel delivery
Two independent `CompletableFuture`s:

**WebSocket push** via `DispositionWebSocketHandler.pushToAgent(agentId, card)`
- Looks up the agent's active WebSocket session(s) in the session registry
- Sends the card as a `DISPOSITION_READY` message
- If the agent is offline: skip silently (card is already persisted and will be visible on reopen)
- On success: set `chat_disposition.converse_delivered_at`

**CRM Activity API** via `CrmActivityClient.postDisposition(card)`
- POST to `https://api.converse.example.com/activities` with body:
  ```json
  {
    "leadId": "...",
    "activityType": "CHAT_DISPOSITION",
    "sessionId": 19899288,
    "occurredAt": "2026-04-22T00:15:00Z",
    "payload": { ... full card ... }
  }
  ```
- Wrapped in `@Retry(name = "crmApi")`
- On success: set `chat_disposition.crm_delivered_at`

Delivery failures on either channel are logged but do not fail the other. A separate retry worker picks up rows where `status = COMPLETED` but either delivery timestamp is NULL older than 15 minutes.

## 8. Prompt engineering

### 8.1 System prompt (fixed)

```
You are a contact center assistant. A chat session between a customer (LEAD)
and a support agent (AGENT) has ended. Produce a concise disposition summary
for the agent's CRM record.

Context: This session is from a {INDUSTRY} business on channel {CHANNEL}.

Rules:
- Respond ONLY with valid JSON matching the schema below.
- No markdown, no code fences, no explanation.
- "summary": 2-4 sentences, plain English, written for a CRM activity log.
- "lead_intent": one sentence describing what the lead wanted.
- "agent_action": one sentence describing what the agent did or attempted.
- "sentiment": one of POSITIVE | NEUTRAL | FRUSTRATED | ANGRY | CONFUSED.
- "unresolved": null if fully resolved, otherwise one sentence on what remains open.
- "key_entities": list of {type, value} objects extracted from the conversation
  — order IDs, policy numbers, course names, amounts, dates, appointment refs,
  anything domain-relevant. Empty array if none.
- If the session has only 1-2 messages, still produce a summary — keep it brief.
- Do not include raw PII: mask phone numbers as [PHONE], account numbers as [ACCT].
- If the transcript is in Hindi, Hinglish, Tamil, or any other language, produce
  the summary in English.

Schema:
{
  "summary": "string",
  "lead_intent": "string",
  "agent_action": "string",
  "sentiment": "POSITIVE|NEUTRAL|FRUSTRATED|ANGRY|CONFUSED",
  "unresolved": "string | null",
  "key_entities": [{"type": "string", "value": "string"}]
}
```

### 8.2 Few-shot examples

Two examples appended to the system prompt covering the extremes of the tenant spectrum:

1. **Quick-commerce** session — short, transactional, entity-heavy (order IDs, refund amounts), FRUSTRATED sentiment, unresolved.
2. **Higher-education** session — longer, relationship-driven, no numeric entities, POSITIVE sentiment, fully resolved.

This pairing teaches the model to handle both domain vocabularies without hallucinating irrelevant terms from one industry into another.

### 8.3 User message

The formatted transcript produced by `TranscriptFetcher`, prefixed with a header:

```
[CHANNEL: WhatsApp]
[INDUSTRY: BFSI]
[SESSION DURATION: 15m 22s]

LEAD  [00:00]: Hi I want to know about my home loan EMI
AGENT [00:02]: Could you share your registered mobile number?
LEAD  [00:03]: 98XXXXX412
...
```

### 8.4 Thin-session handling

For sessions with 0–2 messages, Claude still receives the full prompt and produces a summary — typically a one-liner like *"Lead initiated contact but did not continue the conversation; no intent was expressed."* No special-casing in the application code; the prompt handles this gracefully because of the explicit instruction.

## 9. Failure modes

| Scenario | Handling |
|---|---|
| Claude returns malformed JSON | Retry once with repair prompt. If still malformed, set `status = FALLBACK`, build rule-based summary (first lead message as intent, last agent message as action, sentiment = NEUTRAL). Card still gets posted. |
| Bedrock throttled or timed out | Resilience4j retries 3× with backoff. On exhaustion, same rule-based fallback. |
| Elasticsearch unavailable | Circuit breaker opens. Status = FAILED, `summary = "Transcript unavailable at the time of session close."` Card posted anyway with metadata. |
| Session close event fires twice | Second insert hits UNIQUE constraint → `DuplicateKeyException` → task aborts silently. First insert's flow proceeds normally. |
| CRM Activity API down | Task logs and moves on. Retry worker picks up `chat_disposition` rows with `status = COMPLETED` and `crm_delivered_at IS NULL` older than 15 minutes. |
| WebSocket delivery: agent offline | Logged as info. Card is already in Redis + MySQL, so it appears when the agent reopens the conversation. |
| Very long transcript (thousands of messages) | Truncated at fetch time to head 10 + tail 50 with omission marker. |
| Non-English transcript | System prompt explicitly instructs Claude to produce the summary in English regardless of input language. Claude handles Hindi, Hinglish, Tamil natively. |

## 10. Observability

Metrics exposed via Micrometer + Prometheus (`/actuator/prometheus`):

- `disposition.latency.ms` — `Timer` tagged by `tenant_industry`, `outcome`. Measured from event receipt to both deliveries confirmed (or FAILED status). p95 target < 10s.
- `disposition.bedrock.calls` — `Counter` tagged by `outcome` (`success`, `retry`, `failed`, `fallback`).
- `disposition.bedrock.tokens` — `DistributionSummary` on request + response token counts, for cost tracking.
- `disposition.cache.hits` / `disposition.cache.misses` — Redis hit ratio on the read path.
- `disposition.delivery.failures` — `Counter` tagged by `target` (`crm`, `websocket`).

Alerts:
- Bedrock failure rate > 2% over 10 min — paging
- Fallback rate > 5% over 30 min — paging
- Any delivery failure backlog > 500 rows older than 1 hour — paging

## 11. Security and compliance

- **AWS credentials** for Bedrock are loaded via the default credential chain (IAM role in production, env vars in dev). Never committed.
- **PII masking**: system prompt instructs Claude to mask phone numbers and account numbers in the `summary` field. Structured `key_entities` may contain entity types like `order_id` but not raw personal identifiers. A post-processing regex pass on the `summary` string provides belt-and-suspenders masking for `\d{10,}` and `\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}` patterns.
- **DPDP Act compliance** (for Indian BFSI tenants): transcript content leaves the Converse boundary to reach AWS Bedrock. This is covered under the existing data processing agreement with AWS, which is part of Converse's master DPA with these tenants.
- **Tenant isolation**: every Redis key, Elasticsearch query, and jOOQ query includes `tenant_id`. Cross-tenant read is impossible at the query layer.

## 12. Scaling considerations

At scale, session volume can be high across many tenants simultaneously. The design accounts for this in several ways.

**Bedrock concurrency** is the real bottleneck. AWS imposes per-account concurrency limits on model invocations. Mitigations:
- The `@Async` executor has a bounded queue (500 slots); rejected tasks fall to a disk-backed retry queue via the existing Spring Retry template. No dispositions are lost; they are just delayed during extreme spikes.
- Per-tenant rate limiting via `Semaphore` keyed on tenant_id caps any single tenant's concurrent Bedrock calls, preventing one noisy tenant from starving the rest.

**Redis memory**: each cached card is small (~2KB). With a 24h TTL, the working set stays well within typical ElastiCache limits even at high session volumes.

**MySQL growth**: `chat_disposition` grows proportionally with session volume. Partitioning by `created_at` monthly keeps queries efficient; cold partitions can be archived to S3 over time.

## 13. What this project is NOT

- Not a real-time chatbot. Claude is called once per closed session, not per message.
- Not a routing system. Claude's `unresolved` field is informational; routing decisions remain Converse's responsibility.
- Not a replacement for the agent. The disposition card supplements the agent's CRM record; agents can still edit it if needed (though they rarely do in practice, once the auto-generated version is trusted).

## 14. Implementation plan

If implementing from scratch (e.g. with Claude Code), suggested sequencing:

1. **Scaffolding**: Gradle build, Flyway migrations, jOOQ generation, application.yml, docker-compose for local MySQL + Redis + Elasticsearch.
2. **Models**: `SessionClosedEvent`, `DispositionSummary`, `DispositionCard`, `TranscriptMessage`, enums (`Channel`, `CloseReason`, `Sentiment`, `TenantIndustry`, `DispositionStatus`).
3. **Repositories** (jOOQ): `DispositionRepository`, `LeadRepository`, `AgentRepository`, `SessionRepository`.
4. **Transcript fetcher**: Elasticsearch client + budgeting logic.
5. **Bedrock client**: Messages API integration + JSON parsing + error handling.
6. **Prompt builder**: system prompt + few-shot examples + user message formatter.
7. **Validator + assembler**: schema validation, rule-based fallback, card building.
8. **Redis cache**: key layout + serialization + TTL.
9. **Event handler**: `@Async @EventListener` wiring the pipeline together.
10. **WebSocket handler**: session registry + push delivery.
11. **CRM client**: HTTP client with Resilience4j.
12. **REST controllers**: `POST /internal/sessions/{id}/close` (trigger) + `GET /api/dispositions/by-session/{id}` (read).
13. **Retry worker**: scheduled sweep of undelivered dispositions.
14. **Observability**: Micrometer tags + Prometheus endpoint.
15. **Tests**: Testcontainers-based integration tests for each stage + an end-to-end happy-path test with a mocked Bedrock client.
