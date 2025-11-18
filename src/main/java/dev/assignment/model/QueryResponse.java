package dev.assignment.model;

import java.util.List;

/**
 * Represents a response from the RAG system including sources
 */
public class QueryResponse {
    private final String response;
    private final List<String> sources;

    public QueryResponse(String response, List<String> sources) {
        this.response = response;
        this.sources = sources;
    }

    public String getResponse() {
        return response;
    }

    public List<String> getSources() {
        return sources;
    }

    public boolean hasSources() {
        return sources != null && !sources.isEmpty();
    }
}
