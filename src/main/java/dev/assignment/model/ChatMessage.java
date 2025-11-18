package dev.assignment.model;

import java.time.LocalDateTime;

/**
 * Represents a chat message in the session
 */
public class ChatMessage {
    private final String id;
    private final String content;
    private final boolean isUser;
    private final LocalDateTime timestamp;
    private final String sources; // Optional sources for AI messages

    public ChatMessage(String content, boolean isUser) {
        this(java.util.UUID.randomUUID().toString(), content, isUser, LocalDateTime.now(), null);
    }

    public ChatMessage(String content, boolean isUser, String sources) {
        this(java.util.UUID.randomUUID().toString(), content, isUser, LocalDateTime.now(), sources);
    }

    public ChatMessage(String id, String content, boolean isUser, LocalDateTime timestamp, String sources) {
        this.id = id;
        this.content = content;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.sources = sources;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSources() {
        return sources;
    }

    public boolean hasSources() {
        return sources != null && !sources.trim().isEmpty();
    }
}
