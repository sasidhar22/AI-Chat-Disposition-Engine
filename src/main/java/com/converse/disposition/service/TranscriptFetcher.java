package com.converse.disposition.service;

import com.converse.disposition.model.TranscriptMessage;
import com.converse.disposition.repository.MessageDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TranscriptFetcher {

    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${disposition.transcript.max-messages:150}")
    private int maxMessages;

    @Value("${disposition.transcript.head-keep:10}")
    private int headKeep;

    @Value("${disposition.transcript.tail-keep:50}")
    private int tailKeep;

    @Value("${disposition.transcript.max-input-chars:48000}")
    private int maxInputChars;

    public TranscriptFetcher(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<TranscriptMessage> fetch(long sessionId) {
        try {
            String queryJson = String.format("""
                    {
                      "bool": {
                        "filter": [{"term": {"session_id": %d}}]
                      }
                    }""", sessionId);

            StringQuery query = new StringQuery(queryJson);
            query.addSort(org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.ASC, "sent_at"));
            query.setMaxResults(1000);

            List<TranscriptMessage> messages = elasticsearchOperations
                    .search(query, MessageDocument.class)
                    .getSearchHits()
                    .stream()
                    .map(SearchHit::getContent)
                    .map(MessageDocument::toTranscriptMessage)
                    .toList();

            return applyBudget(messages);
        } catch (Exception e) {
            log.error("Failed to fetch transcript from Elasticsearch for sessionId={}", sessionId, e);
            return List.of();
        }
    }

    private List<TranscriptMessage> applyBudget(List<TranscriptMessage> messages) {
        int totalChars = messages.stream().mapToInt(m -> m.content().length()).sum();
        if (messages.size() <= maxMessages && totalChars <= maxInputChars) {
            return messages;
        }

        int omitted = messages.size() - headKeep - tailKeep;
        if (omitted <= 0) return messages;

        List<TranscriptMessage> result = new ArrayList<>();
        result.addAll(messages.subList(0, Math.min(headKeep, messages.size())));

        if (omitted > 0 && messages.size() > headKeep) {
            TranscriptMessage marker = new TranscriptMessage(
                    "__omitted__",
                    messages.get(headKeep).sessionId(),
                    TranscriptMessage.Participant.LEAD,
                    "[... " + omitted + " messages omitted ...]",
                    messages.get(headKeep).sentAt()
            );
            result.add(marker);
        }

        int tailStart = messages.size() - tailKeep;
        if (tailStart < headKeep) tailStart = headKeep;
        if (tailStart < messages.size()) {
            result.addAll(messages.subList(tailStart, messages.size()));
        }

        return result;
    }
}
