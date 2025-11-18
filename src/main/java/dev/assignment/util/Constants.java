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

    private Constants() {
        // Prevent instantiation
    }
}
