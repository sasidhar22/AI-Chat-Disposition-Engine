package com.converse.disposition.repository;

import com.converse.disposition.model.TranscriptMessage;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "converse-messages")
public class MessageDocument {

    @Id
    private String messageId;

    @Field(name = "session_id", type = FieldType.Long)
    private Long sessionId;

    @Field(name = "participant", type = FieldType.Keyword)
    private String participant;

    @Field(name = "content", type = FieldType.Text)
    private String content;

    @Field(name = "sent_at", type = FieldType.Date)
    private Instant sentAt;

    private MessageDocument() {}

    private MessageDocument(Builder b) {
        this.messageId   = b.messageId;
        this.sessionId   = b.sessionId;
        this.participant = b.participant;
        this.content     = b.content;
        this.sentAt      = b.sentAt;
    }

    public static Builder builder() { return new Builder(); }

    public String getMessageId()   { return messageId; }
    public Long   getSessionId()   { return sessionId; }
    public String getParticipant() { return participant; }
    public String getContent()     { return content; }
    public Instant getSentAt()     { return sentAt; }

    public void setMessageId(String messageId)     { this.messageId = messageId; }
    public void setSessionId(Long sessionId)       { this.sessionId = sessionId; }
    public void setParticipant(String participant) { this.participant = participant; }
    public void setContent(String content)         { this.content = content; }
    public void setSentAt(Instant sentAt)          { this.sentAt = sentAt; }

    public TranscriptMessage toTranscriptMessage() {
        return new TranscriptMessage(
                messageId,
                sessionId != null ? sessionId : 0L,
                TranscriptMessage.Participant.valueOf(participant),
                content,
                sentAt
        );
    }

    public static class Builder {
        private String messageId;
        private Long sessionId;
        private String participant;
        private String content;
        private Instant sentAt;

        public Builder messageId(String v)   { this.messageId = v; return this; }
        public Builder sessionId(Long v)     { this.sessionId = v; return this; }
        public Builder participant(String v) { this.participant = v; return this; }
        public Builder content(String v)     { this.content = v; return this; }
        public Builder sentAt(Instant v)     { this.sentAt = v; return this; }
        public MessageDocument build()       { return new MessageDocument(this); }
    }
}
