package com.converse.disposition.model;

import java.time.Instant;

public record TranscriptMessage(
        String messageId,
        long sessionId,
        Participant participant,
        String content,
        Instant sentAt
) {
    public enum Participant {
        LEAD,
        AGENT
    }
}
