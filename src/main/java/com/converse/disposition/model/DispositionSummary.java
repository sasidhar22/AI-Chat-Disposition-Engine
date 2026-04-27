package com.converse.disposition.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DispositionSummary(
        @JsonProperty("summary") String summary,
        @JsonProperty("lead_intent") String leadIntent,
        @JsonProperty("agent_action") String agentAction,
        @JsonProperty("sentiment") Sentiment sentiment,
        @JsonProperty("unresolved") String unresolved,
        @JsonProperty("key_entities") List<KeyEntity> keyEntities
) {
    public record KeyEntity(
            @JsonProperty("type") String type,
            @JsonProperty("value") String value
    ) {}
}
