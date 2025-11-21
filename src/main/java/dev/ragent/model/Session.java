package dev.ragent.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import dev.ragent.util.Constants;

/**
 * Model representing a chat session
 */
public class Session {
    private final String id;
    private String name;
    private String model;
    private boolean useQueryTransformation;
    private double temperature;
    private int topK;
    private final LocalDateTime createdAt;

    /**
     * Create a new session with generated UUID
     */
    public Session(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.model = Constants.DEFAULT_MODEL;
        this.useQueryTransformation = true; // Default enabled
        this.temperature = 1.0; // Default temperature
        this.topK = 5; // Default top K results
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Load existing session from database
     */
    public Session(String id, String name, String model, boolean useQueryTransformation, double temperature, int topK,
            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.model = model;
        this.useQueryTransformation = useQueryTransformation;
        this.temperature = temperature;
        this.topK = topK;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isUseQueryTransformation() {
        return useQueryTransformation;
    }

    public void setUseQueryTransformation(boolean useQueryTransformation) {
        this.useQueryTransformation = useQueryTransformation;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
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
