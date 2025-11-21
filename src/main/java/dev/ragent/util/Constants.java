package dev.ragent.util;

/**
 * Application constants
 */
public class Constants {

    /**
     * Available OpenAI models for chat
     */
    public static final String[] AVAILABLE_MODELS = {
            "openai|gpt-4o-mini",
            "openai|gpt-4o",
            "openai|gpt-4.1",
            "openai|gpt-4.1-mini",
            "openai|gpt-4.1-nano",
            "openai|gpt-5",
            "openai|gpt-5-mini",
            "openai|gpt-5-nano",
            "gemini|gemini-2.5-flash-lite",
            "gemini|gemini-2.5-flash-lite-preview",
            "gemini|gemini-2.5-flash",
            "gemini|gemini-2.5-flash-preview",
            "gemini|gemini-2.5-pro",
            "groq|llama-3.1-8b-instant",
            "groq|llama-3.3-70b-versatile",
            "groq|openai/gpt-oss-120b",
            "groq|openai/gpt-oss-20b"
    };

    /**
     * Default model
     */
    public static final String DEFAULT_MODEL = "openai|gpt-4o-mini";

    /**
     * Extract provider from model string (e.g., "openai|gpt-4o" -> "openai")
     */
    public static String getProvider(String model) {
        if (model == null || !model.contains("|")) {
            return "openai"; // default to openai for backward compatibility
        }
        return model.split("\\|")[0];
    }

    /**
     * Extract model name from model string (e.g., "openai|gpt-4o" -> "gpt-4o")
     */
    public static String getModelName(String model) {
        if (model == null || !model.contains("|")) {
            return model; // return as-is for backward compatibility
        }
        return model.split("\\|")[1];
    }

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
