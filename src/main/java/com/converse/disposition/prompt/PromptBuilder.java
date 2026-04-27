package com.converse.disposition.prompt;

import com.converse.disposition.model.SessionClosedEvent;
import com.converse.disposition.model.TranscriptMessage;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a contact center assistant. A chat session between a customer (LEAD)
            and a support agent (AGENT) has ended. Produce a concise disposition summary
            for the agent's CRM record.

            Context: This session is from a %s business on channel %s.

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

            Examples:

            Example 1 (QUICK_COMMERCE, WhatsApp, FRUSTRATED, unresolved):
            Input transcript:
            LEAD [00:00]: my order #ZPT-20481 hasnt arrived yet its been 2 hours
            AGENT [00:02]: I apologise for the delay. Let me check the status.
            LEAD [00:03]: please hurry I need this urgently
            AGENT [00:05]: I can see the order is delayed at the hub. I am escalating to the delivery team.
            LEAD [00:07]: this always happens. last time order #ZPT-18822 was also late
            AGENT [00:09]: I understand your frustration. A supervisor will call you within 30 minutes.
            LEAD [00:10]: fine but this is the last time i order from you

            Output:
            {"summary":"Lead contacted regarding two delayed orders, ZPT-20481 and ZPT-18822. Agent confirmed the current order is stuck at the hub and escalated to the delivery team. A supervisor callback was promised within 30 minutes. Lead expressed strong dissatisfaction.","lead_intent":"Lead wanted immediate delivery update and resolution for a delayed order.","agent_action":"Agent checked order status, confirmed hub delay, and escalated to the delivery team with a 30-minute supervisor callback commitment.","sentiment":"FRUSTRATED","unresolved":"Supervisor callback pending; lead has not received the order.","key_entities":[{"type":"order_id","value":"ZPT-20481"},{"type":"order_id","value":"ZPT-18822"}]}

            Example 2 (HIGHER_EDUCATION, web chat, POSITIVE, resolved):
            Input transcript:
            LEAD [00:00]: Hi I wanted to know about admission requirements for your MBA program
            AGENT [00:01]: Hello! Happy to help. Our MBA requires a bachelor's degree with minimum 55% aggregate and a valid CAT/GMAT score.
            LEAD [00:03]: I have 60% in B.Com and CAT 2024 score of 87 percentile. Am I eligible?
            AGENT [00:04]: Yes, you are eligible! The deadline for the current cycle is June 30th. Shall I send you the application link?
            LEAD [00:05]: Yes please
            AGENT [00:06]: Done! You should receive the link on your registered email shortly. Is there anything else I can help with?
            LEAD [00:07]: No that's all thank you!

            Output:
            {"summary":"Lead enquired about MBA admission eligibility. Agent confirmed eligibility based on a 60%% B.Com aggregate and 87 percentile CAT 2024 score, and shared the application link via email. Session fully resolved.","lead_intent":"Lead wanted to confirm eligibility for the MBA program and obtain the application link.","agent_action":"Agent confirmed eligibility, communicated the June 30th application deadline, and sent the application link to the lead's registered email.","sentiment":"POSITIVE","unresolved":null,"key_entities":[{"type":"program","value":"MBA"},{"type":"exam_score","value":"CAT 2024 — 87 percentile"},{"type":"deadline","value":"June 30th"}]}
            """;

    public String buildSystemPrompt(SessionClosedEvent event) {
        return String.format(SYSTEM_PROMPT_TEMPLATE,
                event.tenantIndustry().name().replace('_', ' '),
                event.channel().name());
    }

    public String buildUserMessage(SessionClosedEvent event, List<TranscriptMessage> transcript) {
        StringBuilder sb = new StringBuilder();
        sb.append("[CHANNEL: ").append(event.channel().name()).append("]\n");
        sb.append("[INDUSTRY: ").append(event.tenantIndustry().name()).append("]\n");

        if (!transcript.isEmpty()) {
            long durationSeconds = Duration.between(
                    event.sessionStartedAt(), event.sessionEndedAt()).getSeconds();
            sb.append("[SESSION DURATION: ").append(formatDuration(durationSeconds)).append("]\n");
        }
        sb.append("\n");

        Instant sessionStart = event.sessionStartedAt();
        for (TranscriptMessage msg : transcript) {
            long offsetSeconds = Duration.between(sessionStart, msg.sentAt()).getSeconds();
            if (offsetSeconds < 0) offsetSeconds = 0;
            String role = msg.participant().name();
            sb.append(String.format("%-5s [%s]: %s%n", role, formatOffset(offsetSeconds), msg.content()));
        }

        return sb.toString();
    }

    public String buildRepairPrompt(String originalJson, String validationError) {
        return String.format("""
                The following JSON response failed validation: %s

                Original response:
                %s

                Please fix the JSON to match the required schema exactly. Respond ONLY with valid JSON — no markdown, no explanation.
                """, validationError, originalJson);
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %dm %ds", h, m, s);
        return String.format("%dm %ds", m, s);
    }

    private String formatOffset(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}
