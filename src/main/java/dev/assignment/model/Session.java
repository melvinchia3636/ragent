package dev.assignment.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Model representing a chat session
 */
public class Session {
    private final String id;
    private String name;
    private final LocalDateTime createdAt;

    /**
     * Create a new session with generated UUID
     */
    public Session(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Load existing session from database
     */
    public Session(String id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFormattedCreatedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, h.mma");
        return createdAt.format(formatter);
    }

    @Override
    public String toString() {
        return name;
    }
}
