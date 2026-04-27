package com.converse.disposition.service;

import com.converse.disposition.client.BedrockClaudeClient;
import com.converse.disposition.client.CrmActivityClient;
import com.converse.disposition.exception.DispositionValidationException;
import com.converse.disposition.model.DispositionCard;
import com.converse.disposition.model.DispositionStatus;
import com.converse.disposition.model.DispositionSummary;
import com.converse.disposition.model.SessionClosedEvent;
import com.converse.disposition.model.TranscriptMessage;
import com.converse.disposition.repository.DispositionRepository;
import com.converse.disposition.websocket.DispositionWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class DispositionGenerator {

    private final DispositionRepository dispositionRepository;
    private final TranscriptFetcher transcriptFetcher;
    private final BedrockClaudeClient bedrockClient;
    private final DispositionValidator validator;
    private final DispositionAssembler assembler;
    private final DispositionCacheService cacheService;
    private final DispositionWebSocketHandler webSocketHandler;
    private final CrmActivityClient crmClient;
    private final DispositionMetrics metrics;

    public DispositionGenerator(DispositionRepository dispositionRepository,
                                 TranscriptFetcher transcriptFetcher,
                                 BedrockClaudeClient bedrockClient,
                                 DispositionValidator validator,
                                 DispositionAssembler assembler,
                                 DispositionCacheService cacheService,
                                 DispositionWebSocketHandler webSocketHandler,
                                 CrmActivityClient crmClient,
                                 DispositionMetrics metrics) {
        this.dispositionRepository = dispositionRepository;
        this.transcriptFetcher     = transcriptFetcher;
        this.bedrockClient         = bedrockClient;
        this.validator             = validator;
        this.assembler             = assembler;
        this.cacheService          = cacheService;
        this.webSocketHandler      = webSocketHandler;
        this.crmClient             = crmClient;
        this.metrics               = metrics;
    }

    @Async("dispositionExecutor")
    @EventListener
    public void onSessionClosed(SessionClosedEvent event) {
        log.info("Received SessionClosedEvent for sessionId={}", event.sessionId());
        Instant start = Instant.now();

        try {
            dispositionRepository.insertPending(event);
        } catch (DuplicateKeyException e) {
            log.info("Disposition already exists for sessionId={} — skipping", event.sessionId());
            return;
        }

        try {
            generate(event, start);
        } catch (Exception e) {
            log.error("Fatal error during disposition generation for sessionId={}", event.sessionId(), e);
            dispositionRepository.markFailed(event.sessionId(), e.getMessage());
            metrics.recordLatency(Duration.between(start, Instant.now()), "failed");
        }
    }

    private void generate(SessionClosedEvent event, Instant start) {
        dispositionRepository.updateStatus(event.sessionId(), DispositionStatus.GENERATING);

        List<TranscriptMessage> transcript = transcriptFetcher.fetch(event.sessionId());

        DispositionCard card;
        boolean fallback = false;

        try {
            DispositionSummary summary = bedrockClient.summarize(event, transcript);
            summary = validator.validateOrRepair(summary, event, transcript);

            dispositionRepository.markCompleted(event.sessionId(), summary);
            card = assembler.assemble(event, summary, false);
            metrics.recordLatency(Duration.between(start, Instant.now()), "completed");
        } catch (Exception e) {
            log.warn("Falling back to rule-based summary for sessionId={}: {}", event.sessionId(), e.getMessage());
            fallback = true;
            card = assembler.assembleFallback(event, e.getMessage());
            dispositionRepository.markFallback(event.sessionId(), card.summary(), e.getMessage());
            metrics.recordLatency(Duration.between(start, Instant.now()), "fallback");
        }

        cacheService.put(card);

        final DispositionCard finalCard = card;
        CompletableFuture.runAsync(() -> deliverToConverse(event, finalCard));
        CompletableFuture.runAsync(() -> deliverToCrm(event, finalCard));
    }

    private void deliverToConverse(SessionClosedEvent event, DispositionCard card) {
        try {
            boolean pushed = webSocketHandler.pushToAgent(event.agentId(), card);
            if (pushed) {
                dispositionRepository.markConverseDelivered(event.sessionId());
            }
        } catch (Exception e) {
            log.warn("WebSocket delivery failed for sessionId={}", event.sessionId(), e);
            metrics.recordDeliveryFailure("websocket");
        }
    }

    private void deliverToCrm(SessionClosedEvent event, DispositionCard card) {
        try {
            crmClient.postDisposition(card);
            dispositionRepository.markCrmDelivered(event.sessionId());
        } catch (Exception e) {
            log.warn("CRM delivery failed for sessionId={}", event.sessionId(), e);
            metrics.recordDeliveryFailure("crm");
        }
    }
}
