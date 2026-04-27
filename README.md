# AI Chat Disposition Engine - Backend focused

> Async Spring Boot service that generates structured summaries of closed Converse chat sessions using Claude (AWS Bedrock), persists them to MySQL, caches in Redis, and delivers in real time to the agent's Converse view and the CRM lead timeline.

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)]()
[![jOOQ](https://img.shields.io/badge/jOOQ-3.19-blue)]()
[![AWS Bedrock](https://img.shields.io/badge/AWS-Bedrock-yellow)]()

---

## Context

**Converse** is a B2B omnichannel conversation platform used by businesses in healthcare, BFSI, ed-tech, real estate, quick-commerce, and higher education worldwide. When a **lead** (the end customer) messages a business via WhatsApp, SMS, or the chat widget, the message is handled by an **agent** (an employee of the business) through Converse.

A chat **session** is the window of continuous conversation between the lead and the agent. Sessions end either when the agent manually closes them, or when an inactivity timeout elapses from the lead's last inbound message.

**The problem:** historically, agents had to manually write notes summarising each closed session — what the lead wanted, what the agent did, what's pending. This is tedious and inconsistent, and when a senior agent or QA analyst later opens the lead's profile they have no quick way to understand past conversations without reading every transcript.

**This service** automates that step. It listens for session close, generates a structured disposition summary via Claude, and posts it to two places:

1. The **lead's CRM timeline** — a permanent activity record searchable by anyone on the team.
2. The **Converse conversation view** — rendered as a card pinned to the bottom of the closed session, delivered via WebSocket so the closing agent sees it appear in real time.

---

## Architecture at a glance

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Converse platform                                │
│                                                                          │
│   Agent closes session                                                   │
│   OR auto-expiry timer fires                                             │
│           │                                                              │
│           ▼                                                              │
│   SessionService transitions state → CLOSED                              │
│           │                                                              │
│           │  Spring ApplicationEvent (in-process)                        │
│           ▼                                                              │
│   ┌───────────────────────┐                                              │
│   │ DispositionGenerator  │  @Async — does not block agent's HTTP resp.  │
│   └───────────┬───────────┘                                              │
│               │                                                          │
│   ┌───────────┴────────────┐                                             │
│   ▼                        ▼                                             │
│ [Elasticsearch]      [MySQL / jOOQ]                                      │
│ Full transcript      Lead, agent, session metadata                       │
│   │                        │                                             │
│   └───────────┬────────────┘                                             │
│               ▼                                                          │
│   ┌───────────────────────┐                                              │
│   │  AWS Bedrock (Claude) │  Structured JSON summary                     │
│   └───────────┬───────────┘                                              │
│               ▼                                                          │
│   ┌───────────────────────┐                                              │
│   │  DispositionAssembler │  Validate + build card                       │
│   └───────────┬───────────┘                                              │
│               │                                                          │
│   ┌───────────┼───────────┐──────────────────────────────┐               │
│   ▼           ▼           ▼                              ▼               │
│ [MySQL]    [Redis]     [WebSocket]               [CRM API]           │
│ Persist    24h cache   Push to agent             Post activity on        │
│ forever    warm reads  Converse view live        lead timeline           │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Why this tech stack

| Component | Used for | Justification |
|---|---|---|
| **Spring Boot `@Async`** | Fire disposition generation on session close without blocking the agent's HTTP response | Producer and consumer live in the same service. A broker would be over-engineering; an in-process event is simpler, easier to debug, and has no infrastructure cost. |
| **Elasticsearch** | Fetch the full message transcript for a session | Converse already stores messages in ES for agent-facing search. We reuse the index. |
| **MySQL + jOOQ + Flyway** | Persist disposition records + idempotency + metadata reads | jOOQ gives type-safe SQL without JPA's magic. Flyway keeps schema changes versioned and reviewable in PRs. The `UNIQUE(session_id)` constraint is the idempotency guard. |
| **Redis** | 24h cache backing real-time reads of recently-generated dispositions | Agents reopen conversations within minutes of closing; supervisors view the same lead right after. Redis keeps these reads sub-millisecond and offloads MySQL during session-close bursts (end of business day, EMI cycles for BFSI tenants). |
| **AWS Bedrock (Claude)** | Generate structured JSON summary | Claude handles multilingual transcripts (Hindi/Hinglish/Tamil common in Indian BFSI) and produces reliable structured output when instructed with strict JSON schemas and few-shot examples. |
| **WebSocket** | Push the disposition card to the closing agent's Converse view in real time | Agents see the card appear within seconds of clicking "End Session" — no polling, no refresh. |
| **Resilience4j** | Retry + circuit breaker on Bedrock and CRM API calls | Bedrock occasionally throttles; CRM API is a separate service with its own availability. |

**Why not Kafka?** The session-close producer and the disposition-generation consumer are the same Spring Boot application. A Kafka topic between them would add operational overhead (ZK/KRaft cluster, consumer group management, DLT handling) for zero functional benefit. An in-process `ApplicationEventPublisher` combined with `@Async` handler gives the same async decoupling without the infrastructure. If a future requirement introduces a second downstream consumer (analytics pipeline, billing audit) that justification changes — at that point Kafka earns its place.

---

## Disposition card format

Posted to both the CRM lead timeline and the Converse conversation view:

```
Chat disposition — Session 19899288 — 00:15 22/04/2026

Lead:            Rahul Sharma
Agent:           Priya Menon
Session:         19899288
Initiated at:    22/04/2026 00:00
Lead phone:      +91 98XXXXX412
Channel:         WhatsApp
Business phone:  +91 9220XXXXXX
Summary:         Lead requested rescheduling of a clinic appointment to 25th April
                 afternoon. Agent confirmed a new slot at 3:00 PM IST and advised
                 that a confirmation SMS would follow. Fully resolved.
```

The `Summary` field is a 2–4 sentence narrative produced by Claude. Structured fields (`lead_intent`, `agent_action`, `sentiment`, `unresolved`, `key_entities`) are stored in the database and exposed via the API for filtering and analytics, but are not shown on the default card surface.

---

## Running locally

**Prerequisites:** Java 17, Docker, AWS credentials with Bedrock access.

```bash
# Start MySQL, Redis, Elasticsearch via docker-compose
docker-compose up -d

# Run Flyway migrations
./gradlew flywayMigrate

# Generate jOOQ classes from the migrated schema
./gradlew generateJooq

# Build and run
./gradlew bootRun
```

Environment variables:

```bash
export AWS_REGION=ap-south-1
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export BEDROCK_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
export DB_HOST=localhost
export REDIS_HOST=localhost
export ES_URI=http://localhost:9200
```

---

## Triggering a disposition

Session close is signaled via an internal REST endpoint (in production this would be called by the Converse session service when it transitions state to `CLOSED`):

```bash
curl -X POST http://localhost:8080/internal/sessions/19899288/close \
  -H "Content-Type: application/json" \
  -d '{"closeReason": "AGENT_CLOSED"}'
```

The endpoint returns immediately (202 Accepted); disposition generation happens async.

Reading a disposition:

```bash
curl http://localhost:8080/api/dispositions/by-session/19899288
```

Served from Redis if cached, falls back to MySQL otherwise.

---

## Observability

Metrics exposed at `/actuator/prometheus`:

- `disposition_latency_ms` — histogram, session-close → both deliveries confirmed. p95 target: < 10s.
- `disposition_bedrock_error_rate` — counter per tenant. Alert when > 2% over 10 min.
- `disposition_fallback_rate` — % of dispositions using rule-based fallback. Alert when > 5%.
- `disposition_cache_hit_ratio` — Redis hit rate on read path.

---

## See also

- [`DESIGN.md`](./DESIGN.md) — detailed technical design document covering Claude prompt engineering, failure modes, schema contracts, and scaling considerations.

---

## License

MIT
