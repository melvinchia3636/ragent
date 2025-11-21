package dev.ragent.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a chat message in the session
 */
public class ChatMessage {
    private final String id;
    private final String content;
    private final boolean isUser;
    private final LocalDateTime timestamp;
    private final String sources; // Optional sources for AI messages
    private List<ContextReference> contextReferences; // Context chunks used for AI messages
    private List<String> queryVariations; // Query variations used for retrieval

    public ChatMessage(String content, boolean isUser) {
        this(UUID.randomUUID().toString(), content, isUser, LocalDateTime.now(), null);
    }

    public ChatMessage(String content, boolean isUser, String sources) {
        this(UUID.randomUUID().toString(), content, isUser, LocalDateTime.now(), sources);
    }

    public ChatMessage(String id, String content, boolean isUser, LocalDateTime timestamp, String sources) {
        this.id = id;
        this.content = content;
        this.isUser = isUser;
        this.timestamp = timestamp;
        this.sources = sources;
        this.contextReferences = new ArrayList<>();
        this.queryVariations = new ArrayList<>();
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

    public List<ContextReference> getContextReferences() {
        return contextReferences;
    }

    public void setContextReferences(List<ContextReference> contextReferences) {
        this.contextReferences = contextReferences != null ? contextReferences : new ArrayList<>();
    }

    public List<String> getQueryVariations() {
        return queryVariations;
    }

    public void setQueryVariations(List<String> queryVariations) {
        this.queryVariations = queryVariations != null ? queryVariations : new ArrayList<>();
    }

    public boolean hasContextData() {
        return (contextReferences != null && !contextReferences.isEmpty()) ||
                (queryVariations != null && !queryVariations.isEmpty());
    }
}
