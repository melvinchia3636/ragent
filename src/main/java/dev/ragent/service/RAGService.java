package dev.ragent.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.ragent.util.Constants;

/**
 * Service for handling RAG (Retrieval Augmented Generation) operations
 */
public class RAGService {

    private static final Logger logger = LogManager.getLogger(RAGService.class);

    private final String modelName;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final OpenAiChatModel chatModel;
    private final OpenAiStreamingChatModel streamingChatModel;
    private final List<ChatMessage> sessionHistory;
    private final Map<String, Long> indexedFiles; // filename -> last modified timestamp
    private final DocumentIndexingService indexingService;
    private final RerankingService rerankingService;
    private final QueryTransformationService queryTransformationService;
    private final boolean useQueryTransformation;
    private final int topK;

    // Progress callback interface
    public interface ProgressCallback {
        void onProgress(String message, int current, int total);
    }

    private static final double MIN_SCORE = 0.5;
    private static final int MAX_RESULTS_BEFORE_RERANK = 20; // Retrieve more results for re-ranking

    private final String sessionId;

    public RAGService(String sessionId, String modelName, boolean useQueryTransformation, double temperature,
            int topK) {
        this.sessionId = sessionId;
        this.modelName = modelName;
        this.useQueryTransformation = useQueryTransformation;
        this.topK = topK;
        this.sessionHistory = new ArrayList<>();
        this.indexedFiles = new HashMap<>();

        // Extract provider and actual model name
        String provider = Constants.getProvider(modelName);
        String actualModelName = Constants.getModelName(modelName);
        String openAIAPIKey = APIKeyService.getInstance().getApiKey("openai");
        String apiKey = APIKeyService.getInstance().getApiKey(provider);

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openAIAPIKey)
                .modelName("text-embedding-3-small")
                .build();
        this.embeddingStore = new InMemoryEmbeddingStore<>();

        // Build chat model with provider-specific configuration
        if ("groq".equalsIgnoreCase(provider)) {
            this.chatModel = OpenAiChatModel.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .apiKey(apiKey)
                    .modelName(actualModelName)
                    .temperature(temperature)
                    .build();

            this.streamingChatModel = OpenAiStreamingChatModel.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .apiKey(apiKey)
                    .modelName(actualModelName)
                    .temperature(temperature)
                    .build();
        } else if ("gemini".equalsIgnoreCase(provider)) {
            this.chatModel = OpenAiChatModel.builder()
                    .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
                    .apiKey(apiKey)
                    .modelName(actualModelName)
                    .temperature(temperature)
                    .build();

            this.streamingChatModel = OpenAiStreamingChatModel.builder()
                    .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
                    .apiKey(apiKey)
                    .modelName(actualModelName)
                    .temperature(temperature)
                    .build();
        } else {
            this.chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(actualModelName)
                    .temperature(temperature)
                    .build();

            this.streamingChatModel = OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(actualModelName)
                    .temperature(temperature)
                    .build();
        }

        this.indexingService = new DocumentIndexingService(sessionId, embeddingModel, embeddingStore, indexedFiles);
        this.rerankingService = new RerankingService();
        this.queryTransformationService = new QueryTransformationService(chatModel);

        // Add system message
        sessionHistory.add(SystemMessage.from(
                "You are a helpful AI assistant. Use the provided context to answer questions accurately. " +
                        "If the context doesn't contain relevant information, say so politely. " +
                        "In your response, do not use any markdown formatting. Simple plain text is preferred."));

        // Load session history from database
        loadSessionHistory();

        EmbeddingCacheService.loadCache(sessionId, embeddingStore, indexedFiles);
    }

    /**
     * Load session history from database and restore it to the session
     */
    private void loadSessionHistory() {
        // Using fully qualified name to avoid confusion with
        // dev.langchain4j.data.message.ChatMessage
        List<dev.ragent.model.ChatMessage> dbMessages = DatabaseService.getInstance().getChatHistory(sessionId);

        for (dev.ragent.model.ChatMessage dbMessage : dbMessages) {
            if (dbMessage.isUser()) {
                sessionHistory.add(UserMessage.from(dbMessage.getContent()));
            } else {
                sessionHistory.add(AiMessage.from(dbMessage.getContent()));
            }
        }

        logger.debug("Loaded {} messages from database into session history", dbMessages.size());
    }

    /**
     * Index all documents from the knowledgebase (incremental) with progress
     * callback
     */
    public void indexKnowledgebase(ResourceService resourceService, ProgressCallback progressCallback)
            throws IOException {
        indexingService.indexKnowledgebase(resourceService,
                progressCallback != null ? (msg, curr, total) -> progressCallback.onProgress(msg, curr, total) : null);
    }

    /**
     * Index a single file when added to knowledgebase
     */
    public void indexSingleFile(File file) throws IOException {
        indexingService.indexSingleFile(file);
    }

    /**
     * Remove a file from the index when deleted from knowledgebase
     */
    public void removeFileFromIndexByName(String fileName) {
        indexingService.removeFileFromIndexByName(fileName);
    }

    /**
     * Callback interface for streaming responses
     */
    public interface StreamingCallback {
        void onProgress(String progressMessage);

        void onStart(List<String> sources, int segmentCount);

        void onNext(String token);

        void onComplete(String fullResponse, List<dev.ragent.model.ContextReference> contextReferences,
                List<String> queryVariations);

        void onError(Throwable error);
    }

    /**
     * Query the RAG system with a user message and stream the response via callback
     * Basically, our final prompt to the chat model is:
     * 
     * Relevant context:
     * [context from retrieved segments]
     * User question: [user message]
     * 
     * So, simply put, RAG is essentially just an automated way to build better
     * prompts for LLMs by retrieving relevant information from a knowledgebase.
     * 
     * For the context chaining part, we are utilizing recent session history
     * to facilitate follow-up questions that depend on prior context.
     * 
     * For example:
     * User: "How old is Melvin Chia?"
     * AI: "Melvin Chia is 19 years old."
     * User: "How about his brother?"
     * AI: "Melvin Chia's brother is 28 years old."
     */
    public void queryStreaming(String userMessage, StreamingCallback callback) {
        logger.info("========== Processing Streaming Query ==========");
        logger.info("User message: {}", userMessage);
        logger.info("Message length: {} characters", userMessage.length());
        logger.info("Query transformation enabled: {}", useQueryTransformation);

        try {
            // Step 1: Contextualization
            logger.info("Step 1/5: Analyzing query context");
            callback.onProgress("Analyzing query context...");
            String contextualizedQuery = buildContextualizedQuery(userMessage);
            logger.debug("Contextualized query: {}", contextualizedQuery);
            logger.debug("Session history size: {} messages", sessionHistory.size());

            // Step 2: Query transformation (if enabled)
            logger.info("Step 2/5: Query transformation");
            List<String> queryVariations;
            if (useQueryTransformation) {
                callback.onProgress("Generating query variations...");
                queryVariations = queryTransformationService.generateQueryVariations(contextualizedQuery);
                logger.info("Generated {} query variations (original + {} alternatives)",
                        queryVariations.size(), queryVariations.size() - 1);
                for (int i = 0; i < queryVariations.size(); i++) {
                    logger.debug("Variation {}: {}", i + 1, queryVariations.get(i));
                }
            } else {
                queryVariations = List.of(contextualizedQuery);
                logger.info("Query transformation disabled, using single query");
            }

            // Step 3: Multi-query retrieval
            logger.info("Step 3/5: Searching knowledgebase with {} variation(s)", queryVariations.size());
            callback.onProgress("Searching knowledgebase (" + queryVariations.size() + " variation"
                    + (queryVariations.size() > 1 ? "s" : "") + ")...");
            Set<EmbeddingMatch<TextSegment>> allMatches = new HashSet<>();

            for (int i = 0; i < queryVariations.size(); i++) {
                String queryVariation = queryVariations.get(i);
                logger.debug("Searching with variation {}: {}", i + 1, queryVariation);

                Embedding queryEmbedding = embeddingModel.embed(queryVariation).content();

                EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(MAX_RESULTS_BEFORE_RERANK)
                        .minScore(MIN_SCORE)
                        .build();

                EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
                int previousSize = allMatches.size();
                allMatches.addAll(searchResult.matches());
                logger.debug("Variation {} retrieved {} segments ({} new, {} duplicates)",
                        i + 1, searchResult.matches().size(),
                        allMatches.size() - previousSize,
                        searchResult.matches().size() - (allMatches.size() - previousSize));
            }

            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>(allMatches);
            logger.info("Multi-query retrieval complete: {} unique segments found", matches.size());

            // Step 4: Re-ranking
            logger.info("Step 4/5: Re-ranking {} segments", matches.size());
            callback.onProgress("Re-ranking results (" + matches.size() + " segments)...");

            // Re-rank using the contextualized query for consistency with retrieval
            List<EmbeddingMatch<TextSegment>> rerankedSegments = rerankingService.rerank(contextualizedQuery, matches);

            // Take top topK results after re-ranking
            int beforeLimit = rerankedSegments.size();
            rerankedSegments = rerankedSegments.stream()
                    .limit(topK)
                    .collect(Collectors.toList());
            logger.info("Re-ranking complete: keeping top {} of {} segments", rerankedSegments.size(), beforeLimit);

            // Extract unique source files and build context references
            Set<String> sourceFiles = new HashSet<>();
            List<dev.ragent.model.ContextReference> contextReferences = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : rerankedSegments) {
                TextSegment segment = match.embedded();
                String fileName = "Unknown";
                String filePath = "Unknown";

                if (segment.metadata() != null) {
                    if (segment.metadata().containsKey("fileName")) {
                        fileName = segment.metadata().getString("fileName");
                        sourceFiles.add(fileName);
                    }
                    if (segment.metadata().containsKey("filePath")) {
                        filePath = segment.metadata().getString("filePath");
                    } else {
                        filePath = fileName;
                    }
                }

                // Create context reference
                contextReferences.add(new dev.ragent.model.ContextReference(
                        filePath,
                        segment.text(),
                        match.score()));
            }
            logger.info("Sources: {} segments from {} files: {}",
                    rerankedSegments.size(), sourceFiles.size(), sourceFiles);

            // Step 5: Generating response
            logger.info("Step 5/5: Generating AI response");
            callback.onProgress("Generating response...");

            // Notify callback with sources and segment count
            callback.onStart(new ArrayList<>(sourceFiles), rerankedSegments.size());

            // Build context from relevant segments
            StringBuilder context = new StringBuilder();
            if (!rerankedSegments.isEmpty()) {
                context.append("Relevant context:\n\n");
                for (EmbeddingMatch<TextSegment> match : rerankedSegments) {
                    context.append(match.embedded().text()).append("\n\n");
                }
                logger.debug("Context built: {} characters from {} segments",
                        context.length(), rerankedSegments.size());
            } else {
                logger.warn("No relevant context found for query");
            }

            // Build the message with context for the current query
            String messageWithContext;
            if (context.length() > 0) {
                messageWithContext = context + "\nUser question: " + userMessage;
                logger.debug("Final message with context: {} characters", messageWithContext.length());
            } else {
                messageWithContext = userMessage;
                logger.debug("No context available, using original message");
            }

            // Add user message to session history (without RAG context)
            sessionHistory.add(UserMessage.from(userMessage));
            logger.debug("Added user message to session history (total: {})", sessionHistory.size());

            // Build chat request with session history + current RAG context
            List<ChatMessage> messagesForRequest = new ArrayList<>(sessionHistory);
            messagesForRequest.set(messagesForRequest.size() - 1, UserMessage.from(messageWithContext));
            logger.debug("Prepared {} messages for chat model", messagesForRequest.size());

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messagesForRequest)
                    .build();

            logger.info("Initiating streaming chat with model: {}", modelName);

            // Store for callback
            final List<dev.ragent.model.ContextReference> finalContextRefs = contextReferences;
            final List<String> finalQueryVariations = queryVariations;

            // Stream the response
            StringBuilder fullResponse = new StringBuilder();

            streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    fullResponse.append(partialResponse);
                    callback.onNext(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    String responseText = fullResponse.toString();
                    sessionHistory.add(AiMessage.from(responseText));
                    logger.info("Response complete: {} characters, session history now has {} messages",
                            responseText.length(), sessionHistory.size());
                    logger.info("========== Streaming Query Complete ==========");
                    callback.onComplete(responseText, finalContextRefs, finalQueryVariations);
                }

                @Override
                public void onError(Throwable error) {
                    logger.error("========== Streaming Query Error ==========");
                    logger.error("Error type: {}", error.getClass().getSimpleName());
                    logger.error("Error details", error);
                    callback.onError(error);
                }
            });

        } catch (Exception e) {
            logger.error("========== Exception During Streaming Query ==========");
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception details", e);
            callback.onError(e);
        }
    }

    /**
     * Clear session history (keeps system message)
     */
    public void clearHistory() {
        sessionHistory.clear();
        sessionHistory.add(SystemMessage.from(
                "You are a helpful AI assistant. Use the provided context to answer questions accurately. " +
                        "If the context doesn't contain relevant information, say so politely." +
                        " In your response, do not use any markdown formatting. Simple plain text is preferred."));
    }

    /**
     * Reload session history from database
     * This should be called after database messages are deleted to ensure
     * consistency
     */
    public void reloadSessionHistory() {
        clearHistory();
        loadSessionHistory();
        logger.info("Reloaded session history from database: {} messages", sessionHistory.size() - 1); // -1 for system
                                                                                                       // message
    }

    /**
     * Build a contextualized query by incorporating recent session history
     * This helps with follow-up questions like "How about that?" or "Tell me more"
     * 
     * @param userMessage The current user message
     * @return Contextualized query string for better embedding search
     */
    private String buildContextualizedQuery(String userMessage) {
        // If no session history beyond system message, return as-is
        if (sessionHistory.size() <= 1) {
            return userMessage;
        }

        // Build context from recent session (last 2 exchanges = 4 messages)
        // This helps resolve pronouns and implicit references
        StringBuilder contextBuilder = new StringBuilder();
        int startIdx = Math.max(1, sessionHistory.size() - 4); // Skip system message at index 0

        for (int i = startIdx; i < sessionHistory.size(); i++) {
            ChatMessage msg = sessionHistory.get(i);
            switch (msg) {
                case UserMessage userMsg ->
                    contextBuilder.append("User asked: ").append(userMsg.singleText()).append(" ");
                case AiMessage aiMsg -> {
                    // Include a brief snippet of AI response for context
                    String aiText = aiMsg.text();
                    String snippet = aiText.length() > 100 ? aiText.substring(0, 100) + "..." : aiText;
                    contextBuilder.append("Assistant answered: ").append(snippet).append(" ");
                }
                default -> {
                }
            }
        }

        // Append current question
        contextBuilder.append("Current question: ").append(userMessage);

        String contextualizedQuery = contextBuilder.toString();
        logger.debug("Contextualized query: {}", contextualizedQuery);

        return contextualizedQuery;
    }

}
