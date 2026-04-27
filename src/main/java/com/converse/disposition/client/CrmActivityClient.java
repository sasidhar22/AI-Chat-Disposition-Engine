package com.converse.disposition.client;

import com.converse.disposition.exception.CrmDeliveryException;
import com.converse.disposition.model.DispositionCard;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CrmActivityClient {

    private final RestClient restClient;

    @Value("${disposition.crm.activity-type:CHAT_DISPOSITION}")
    private String activityType;

    public CrmActivityClient(
            @Value("${disposition.crm.base-url:http://localhost:9090}") String baseUrl,
            @Value("${disposition.crm.timeout-ms:5000}") int timeoutMs) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Retry(name = "crmApi")
    public void postDisposition(DispositionCard card) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId",       card.sessionId());
        payload.put("channel",         card.channel() != null ? card.channel().name() : null);
        payload.put("businessPhone",   card.businessPhone());
        payload.put("leadName",        card.leadName());
        payload.put("leadPhone",       card.leadPhone());
        payload.put("agentName",       card.agentName());
        payload.put("sessionStartedAt", card.sessionStartedAt());
        payload.put("sessionEndedAt",  card.sessionEndedAt());
        payload.put("summary",         card.summary() != null ? card.summary().summary() : null);
        payload.put("sentiment",       card.summary() != null ? card.summary().sentiment() : null);
        payload.put("unresolved",      card.summary() != null ? card.summary().unresolved() : null);
        payload.put("fallback",        card.fallback());

        Map<String, Object> body = new HashMap<>();
        body.put("leadId",       card.leadId());
        body.put("activityType", activityType);
        body.put("sessionId",    card.sessionId());
        body.put("occurredAt",   card.sessionEndedAt());
        body.put("payload",      payload);

        try {
            restClient.post()
                    .uri("/activities")
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        log.error("CRM API returned 4xx for sessionId={}: status={}",
                                card.sessionId(), response.getStatusCode());
                        throw new CrmDeliveryException("CRM API 4xx: " + response.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("CRM API returned 5xx for sessionId={}: status={}",
                                card.sessionId(), response.getStatusCode());
                        throw new CrmDeliveryException("CRM API 5xx: " + response.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (CrmDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmDeliveryException("CRM API request failed for sessionId=" + card.sessionId(), e);
        }
    }
}
