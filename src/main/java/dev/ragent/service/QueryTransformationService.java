package dev.ragent.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Service for query transformation and reformulation
 * Generates multiple variations of user queries to improve retrieval coverage
 */
public class QueryTransformationService {

    private static final Logger logger = LogManager.getLogger(QueryTransformationService.class);

    private final OpenAiChatModel chatModel;
    private static final int MAX_VARIATIONS = 1;

    public QueryTransformationService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Generate multiple query variations using LLM
     * Techniques:
     * 1. Rephrasing - Different ways to express the same question
     * 2. Decomposition - Breaking complex queries into sub-questions
     * 3. Keyword extraction - Identifying key terms for broader search
     * 
     * @param originalQuery The original user query
     * @return List of query variations including the original
     */
    public List<String> generateQueryVariations(String originalQuery) {
        List<String> variations = new ArrayList<>();
        variations.add(originalQuery); // Always include original

        try {
            String prompt = buildTransformationPrompt(originalQuery);

            ChatRequest request = ChatRequest.builder()
                    .messages(dev.langchain4j.data.message.UserMessage.from(prompt))
                    .build();

            ChatResponse response = chatModel.chat(request);
            String result = response.aiMessage().text();

            // Parse the LLM response to extract variations
            List<String> generatedVariations = parseVariations(result);
            variations.addAll(generatedVariations);

            logger.debug("Generated {} query variations for: '{}'", variations.size(), originalQuery);

        } catch (Exception e) {
            logger.warn("Failed to generate query variations, using original only", e);
        }

        return variations;
    }

    /**
     * Build the prompt for query transformation
     */
    private String buildTransformationPrompt(String query) {
        return String.format(
                "Given this user question, generate ONE semantically equivalent reformulation that uses " +
                        "different wording while preserving the exact same meaning and intent. " +
                        "The reformulation should help retrieve the same relevant information.\n\n" +
                        "Original question: %s\n\n" +
                        "Requirements:\n" +
                        "1. Preserve all key concepts and technical terms\n" +
                        "2. Only change phrasing and sentence structure\n" +
                        "3. Do NOT broaden or narrow the scope\n" +
                        "4. Do NOT add extra context or examples\n" +
                        "5. Keep it concise and focused\n\n" +
                        "Reformulation:",
                MAX_VARIATIONS, query, MAX_VARIATIONS);
    }

    /**
     * Parse the LLM response to extract query variations
     * Handles various response formats
     */
    private List<String> parseVariations(String response) {
        List<String> variations = new ArrayList<>();

        // Split by newlines and clean up
        String[] lines = response.split("\n");

        for (String line : lines) {
            String cleaned = line.trim();

            // Skip empty lines
            if (cleaned.isEmpty()) {
                continue;
            }

            // Remove common prefixes like "1.", "2.", "-", "*", etc.
            cleaned = cleaned.replaceFirst("^[\\d]+\\.\\s*", "");
            cleaned = cleaned.replaceFirst("^[-*•]\\s*", "");
            cleaned = cleaned.trim();

            // Skip if still empty or too short
            if (cleaned.isEmpty() || cleaned.length() < 5) {
                continue;
            }

            // Add variation
            variations.add(cleaned);

            // Stop if we have enough variations
            if (variations.size() >= MAX_VARIATIONS) {
                break;
            }
        }

        return variations;
    }

    /**
     * Generate a step-back query for more abstract/general retrieval
     * Useful for finding background information
     * 
     * @param originalQuery The specific user query
     * @return A more general/abstract version of the query
     */
    public String generateStepBackQuery(String originalQuery) {
        try {
            String prompt = String.format(
                    "Given this specific question: '%s'\n\n" +
                            "Generate a more general/abstract version that would help find background information or principles.\n"
                            +
                            "For example:\n" +
                            "- Specific: 'How do I implement binary search in Java?'\n" +
                            "- General: 'What are search algorithms and their implementations?'\n\n" +
                            "Only return the general question, nothing else.",
                    originalQuery);

            ChatRequest request = ChatRequest.builder()
                    .messages(dev.langchain4j.data.message.UserMessage.from(prompt))
                    .build();

            ChatResponse response = chatModel.chat(request);
            String stepBackQuery = response.aiMessage().text().trim();

            logger.debug("Generated step-back query: '{}' -> '{}'", originalQuery, stepBackQuery);
            return stepBackQuery;

        } catch (Exception e) {
            logger.warn("Failed to generate step-back query, using original", e);
            return originalQuery;
        }
    }

    /**
     * Decompose a complex query into simpler sub-queries
     * Useful for multi-part questions
     * 
     * @param complexQuery The complex user query
     * @return List of simpler sub-queries
     */
    public List<String> decomposeQuery(String complexQuery) {
        try {
            String prompt = String.format(
                    "Break down this complex question into 2-3 simpler sub-questions that together would answer the original:\n\n"
                            +
                            "Complex question: %s\n\n" +
                            "Requirements:\n" +
                            "1. Each sub-question should be self-contained\n" +
                            "2. Sub-questions should be simpler than the original\n" +
                            "3. Do not number them\n" +
                            "4. One sub-question per line\n\n" +
                            "Sub-questions:",
                    complexQuery);

            ChatRequest request = ChatRequest.builder()
                    .messages(dev.langchain4j.data.message.UserMessage.from(prompt))
                    .build();

            ChatResponse response = chatModel.chat(request);
            String result = response.aiMessage().text();

            List<String> subQueries = parseVariations(result);

            // If decomposition failed, return original
            if (subQueries.isEmpty()) {
                subQueries.add(complexQuery);
            }

            logger.debug("Decomposed query into {} sub-queries", subQueries.size());
            return subQueries;

        } catch (Exception e) {
            logger.warn("Failed to decompose query, using original", e);
            return Arrays.asList(complexQuery);
        }
    }
}
