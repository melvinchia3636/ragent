package dev.assignment.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.scene.control.TextInputDialog;

/**
 * Singleton service for managing OpenAI API key configuration
 */
public class APIKeyService {

    private static final Logger logger = LogManager.getLogger(APIKeyService.class);
    private static final String ENV_KEY = "OPENAI_API_KEY";
    private static APIKeyService instance;

    private String apiKey;

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
     * Load API key from .env file or prompt user if not found
     * 
     * @return true if API key was successfully loaded, false otherwise
     */
    public boolean loadApiKey() {
        // Try to load from .env file
        apiKey = loadFromEnv();

        // If no API key found, prompt user
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.info("No API key found in .env, prompting user");
            return promptForApiKey();
        } else {
            logger.info("API key loaded from .env file");
            return true;
        }
    }

    /**
     * Load API key from .env file
     * 
     * @return API key or null if not found
     */
    private String loadFromEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            String key = dotenv.get(ENV_KEY);
            if (key != null && !key.trim().isEmpty()) {
                return key;
            }
        } catch (Exception e) {
            logger.warn("Error loading .env file", e);
        }
        return null;
    }

    /**
     * Prompt user for API key
     * 
     * @return true if user provided a key, false otherwise
     */
    private boolean promptForApiKey() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("OpenAI API Key");
        dialog.setHeaderText("Enter your OpenAI API Key");
        dialog.setContentText("API Key:");

        dialog.showAndWait().ifPresent(key -> {
            this.apiKey = key;
            logger.info("API key provided by user");
        });

        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("No API key provided");
            return false;
        }

        return true;
    }

    /**
     * Get the API key
     * 
     * @return API key or null if not loaded
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Check if API key is available
     * 
     * @return true if API key is available, false otherwise
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Set API key manually
     * 
     * @param apiKey the API key to set
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        logger.info("API key set manually");
    }

    /**
     * Clear the API key
     */
    public void clearApiKey() {
        this.apiKey = null;
        logger.info("API key cleared");
    }

    /**
     * Validate the API key by making a test request to OpenAI
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
                    .apiKey(apiKey)
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
            dev.langchain4j.model.openai.OpenAiChatModel testModel = dev.langchain4j.model.openai.OpenAiChatModel
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
