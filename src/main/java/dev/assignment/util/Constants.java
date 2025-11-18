package dev.assignment.util;

/**
 * Application constants
 */
public class Constants {

    /**
     * Available OpenAI models for chat
     */
    public static final String[] AVAILABLE_MODELS = {
            "gpt-4o-mini",
            "gpt-4o",
            "gpt-4.1",
            "gpt-4.1-mini",
            "gpt-4.1-nano",
            "gpt-5",
            "gpt-5-mini",
            "gpt-5-nano"
    };

    /**
     * Default model
     */
    public static final String DEFAULT_MODEL = "gpt-4o-mini";

    /**
     * Maximum query length in characters
     */
    public static final int MAX_QUERY_LENGTH = 4000;

    /**
     * Maximum number of documents per knowledge base
     */
    public static final int MAX_DOCUMENTS_PER_SESSION = 1000;

    /**
     * Maximum document file size in bytes (50MB)
     */
    public static final long MAX_DOCUMENT_SIZE_BYTES = 50 * 1024 * 1024;

    private Constants() {
        // Prevent instantiation
    }
}
