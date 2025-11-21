package dev.ragent.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Singleton service for managing API key configuration for multiple providers
 */
public class APIKeyService {

    private static final Logger logger = LogManager.getLogger(APIKeyService.class);
    private static final String OPENAI_ENV_KEY = "OPENAI_API_KEY";
    private static final String GROQ_ENV_KEY = "GROQ_API_KEY";
    private static final String GEMINI_ENV_KEY = "GEMINI_API_KEY";
    private static APIKeyService instance;

    private String openaiApiKey;
    private String groqApiKey;
    private String geminiApiKey;

    /**
     * Private constructor to prevent instantiation
     */
    private APIKeyService() {
    }

    /**
     * Get the singleton instance
     * 
     * @return the singleton instance
     */
    public static synchronized APIKeyService getInstance() {
        if (instance == null) {
            instance = new APIKeyService();
        }
        return instance;
    }

    /**
     * Load API keys from .env file
     * 
     * @return true if at least one API key was successfully loaded, false otherwise
     */
    public boolean loadApiKey() {
        // Try to load from .env file
        openaiApiKey = loadFromEnv(OPENAI_ENV_KEY);
        groqApiKey = loadFromEnv(GROQ_ENV_KEY);
        geminiApiKey = loadFromEnv(GEMINI_ENV_KEY);

        boolean hasOpenAI = openaiApiKey != null && !openaiApiKey.trim().isEmpty();
        boolean hasGroq = groqApiKey != null && !groqApiKey.trim().isEmpty();
        boolean hasGemini = geminiApiKey != null && !geminiApiKey.trim().isEmpty();

        if (!hasOpenAI && !hasGroq && !hasGemini) {
            logger.error("No API keys found in .env file");
            return false;
        } else {
            if (hasOpenAI)
                logger.info("OpenAI API key loaded from .env file");
            if (hasGroq)
                logger.info("Groq API key loaded from .env file");
            if (hasGemini)
                logger.info("Gemini API key loaded from .env file");
            return true;
        }
    }

    /**
     * Load API key from .env file
     * 
     * @param envKey the environment variable key
     * @return API key or null if not found
     */
    private String loadFromEnv(String envKey) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            String key = dotenv.get(envKey);
            if (key != null && !key.trim().isEmpty()) {
                return key;
            }
        } catch (Exception e) {
            logger.warn("Error loading .env file", e);
        }
        return null;
    }

    /**
     * Get the API key for a specific provider
     * 
     * @param provider the provider name ("openai", "groq", or "gemini")
     * @return API key or null if not loaded
     */
    public String getApiKey(String provider) {
        if ("groq".equalsIgnoreCase(provider)) {
            return groqApiKey;
        } else if ("gemini".equalsIgnoreCase(provider)) {
            return geminiApiKey;
        }
        return openaiApiKey; // default to openai
    }

    /**
     * Get the OpenAI API key (for backward compatibility)
     * 
     * @return OpenAI API key or null if not loaded
     */
    public String getApiKey() {
        return openaiApiKey;
    }

    /**
     * Check if API key is available for a specific provider
     * 
     * @param provider the provider name ("openai" or "groq")
     * @return true if API key is available, false otherwise
     */
    public boolean hasApiKey(String provider) {
        String key = getApiKey(provider);
        return key != null && !key.trim().isEmpty();
    }

    /**
     * Check if OpenAI API key is available (for backward compatibility)
     * 
     * @return true if API key is available, false otherwise
     */
    public boolean hasApiKey() {
        return openaiApiKey != null && !openaiApiKey.trim().isEmpty();
    }

    /**
     * Set API key manually for a specific provider
     * 
     * @param provider the provider name ("openai", "groq", or "gemini")
     * @param apiKey   the API key to set
     */
    public void setApiKey(String provider, String apiKey) {
        if ("groq".equalsIgnoreCase(provider)) {
            this.groqApiKey = apiKey;
        } else if ("gemini".equalsIgnoreCase(provider)) {
            this.geminiApiKey = apiKey;
        } else {
            this.openaiApiKey = apiKey;
        }
        logger.info("API key manually set for provider: {}", provider);
    }

    /**
     * Set OpenAI API key manually (for backward compatibility)
     * 
     * @param apiKey the API key to set
     */
    public void setApiKey(String apiKey) {
        this.openaiApiKey = apiKey;
        logger.info("API key set manually");
    }

    /**
     * Clear all API keys
     */
    public void clearApiKey() {
        this.openaiApiKey = null;
        this.groqApiKey = null;
        this.geminiApiKey = null;
        logger.info("API keys cleared");
    }

    /**
     * Validate the OpenAI API key by making a test request
     * 
     * @return true if the API key is valid, false otherwise
     */
    public boolean validateApiKey() {
        if (!hasApiKey()) {
            return false;
        }

        try {
            // Make a minimal test request to validate the key
            dev.langchain4j.model.openai.OpenAiChatModel testModel = dev.langchain4j.model.openai.OpenAiChatModel
                    .builder()
                    .apiKey(openaiApiKey)
                    .modelName("gpt-4o-mini")
                    .maxTokens(1)
                    .build();

            // Try to generate a minimal response
            testModel.chat("test");
            logger.info("API key validation successful");
            return true;
        } catch (Exception e) {
            logger.error("API key validation failed", e);
            return false;
        }
    }

    /**
     * Validate a specific API key without setting it
     * 
     * @param keyToValidate the API key to validate
     * @return true if the API key is valid, false otherwise
     */
    public boolean validateApiKey(String keyToValidate) {
        if (keyToValidate == null || keyToValidate.trim().isEmpty()) {
            return false;
        }

        try {
            OpenAiChatModel testModel = OpenAiChatModel
                    .builder()
                    .apiKey(keyToValidate)
                    .modelName("gpt-4o-mini")
                    .maxTokens(1)
                    .build();

            testModel.chat("test");
            logger.info("API key validation successful");
            return true;
        } catch (Exception e) {
            logger.error("API key validation failed", e);
            return false;
        }
    }
}
