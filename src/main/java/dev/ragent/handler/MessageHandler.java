package dev.ragent.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.ragent.model.ChatMessage;
import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.service.RAGService;
import dev.ragent.util.Constants;
import dev.ragent.view.AlertHelper;
import dev.ragent.view.ChatAreaMessage;
import dev.ragent.view.ChatMessageEntry;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Handles message sending and response streaming.
 */
public class MessageHandler {

    private static final Logger logger = LogManager.getLogger(MessageHandler.class);

    private final VBox chatContainer;
    private final TextArea messageInput;
    private final Label statusLabel;
    private final SessionStateHandler sessionStateHandler;
    private final Runnable toggleAllControlsCallback;

    public MessageHandler(
            VBox chatContainer,
            TextArea messageInput,
            Label statusLabel,
            SessionStateHandler sessionStateHandler,
            Runnable toggleAllControlsCallback) {
        this.chatContainer = chatContainer;
        this.messageInput = messageInput;
        this.statusLabel = statusLabel;
        this.sessionStateHandler = sessionStateHandler;
        this.toggleAllControlsCallback = toggleAllControlsCallback;
    }

    /**
     * Handle sending a message.
     */
    public void handleSendMessage() {
        String userMessage = messageInput.getText().trim();

        if (userMessage.isEmpty()) {
            return;
        }

        // Check query length
        if (userMessage.length() > Constants.MAX_QUERY_LENGTH) {
            AlertHelper.showWarning("Query Too Long", "Your query exceeds the maximum length",
                    String.format("Please limit your query to %d characters. Current length: %d characters.",
                            Constants.MAX_QUERY_LENGTH, userMessage.length()));
            return;
        }

        RAGService ragService = sessionStateHandler.getRagService();
        if (ragService == null) {
            logger.warn("Cannot send message: RAG service not initialized (API key may be missing)");
            return;
        }

        Session currentSession = sessionStateHandler.getCurrentSession();
        if (currentSession == null) {
            logger.error("Cannot send message: No session selected");
            return;
        }

        logger.info("========== Sending Message ==========");
        logger.info("Session: id={}, name='{}'", currentSession.getId(), currentSession.getName());
        logger.info("Model: {}, Temperature: {}, Top K: {}", currentSession.getModel(), currentSession.getTemperature(),
                currentSession.getTopK());
        logger.info("Message length: {} characters", userMessage.length());
        logger.debug("Message content: {}", userMessage);

        // Clear input
        messageInput.clear();

        // Remove empty state message if present
        chatContainer.getChildren().removeIf(node -> node instanceof ChatAreaMessage);

        // Add user message to chat
        ChatMessage userChatMessage = new ChatMessage(userMessage, true);
        ChatMessageEntry userMessageBox = new ChatMessageEntry(userChatMessage);
        chatContainer.getChildren().add(userMessageBox);

        // Save user message to database
        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService != null) {
            databaseService.saveChatMessage(currentSession.getId(), userChatMessage);
        }

        // Create placeholder for AI response
        ChatMessage aiChatMessage = new ChatMessage("...", false);
        ChatMessageEntry aiMessageBox = new ChatMessageEntry(aiChatMessage);
        chatContainer.getChildren().add(aiMessageBox);

        // Disable all controls while processing
        toggleAllControlsCallback.run();
        statusLabel.setText("Generating response...");

        // Query RAG with streaming in background
        String finalUserMessage = userMessage;
        new Thread(() -> {
            try {
                ragService.queryStreaming(finalUserMessage, new RAGService.StreamingCallback() {
                    private final StringBuilder responseBuilder = new StringBuilder();
                    private List<String> sources = new ArrayList<>();
                    private int segmentCount = 0;

                    @Override
                    public void onProgress(String progressMessage) {
                        Platform.runLater(() -> {
                            // Show progress in the reference label while processing
                            aiMessageBox.setTopLabel(progressMessage);
                        });
                    }

                    @Override
                    public void onStart(List<String> sourceDocs, int segments) {
                        sources = sourceDocs;
                        segmentCount = segments;
                    }

                    @Override
                    public void onNext(String token) {
                        responseBuilder.append(token);
                        Platform.runLater(() -> {
                            aiMessageBox.updateText(responseBuilder.toString());
                        });
                    }

                    @Override
                    public void onComplete(String fullResponse) {
                        Platform.runLater(() -> {
                            // Update final message
                            aiMessageBox.updateText(fullResponse);

                            // Set sources if available
                            String sourcesText = null;
                            if (!sources.isEmpty()) {
                                sourcesText = String.join(", ", sources);
                                aiMessageBox.setTopLabel("Referenced from: " + sourcesText);
                            }

                            // Create final AI message with sources and save to database
                            ChatMessage finalAiMessage = new ChatMessage(fullResponse, false, sourcesText);
                            DatabaseService databaseService = DatabaseService.getInstance();
                            if (databaseService != null) {
                                databaseService.saveChatMessage(currentSession.getId(), finalAiMessage);
                            }

                            // Re-enable all controls
                            toggleAllControlsCallback.run();
                            statusLabel.setText("Ready");
                            messageInput.requestFocus();
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        logger.error("========== Error Getting Response ==========");
                        logger.error("Session: {}", currentSession.getName());
                        logger.error("Error type: {}", error.getClass().getSimpleName());
                        logger.error("Error message: {}", error.getMessage(), error);
                        Platform.runLater(() -> {
                            // Remove placeholder AI message
                            chatContainer.getChildren().remove(aiMessageBox);

                            AlertHelper.showError("Error", "Failed to get response: " + error.getMessage());

                            // Re-enable all controls
                            toggleAllControlsCallback.run();
                            statusLabel.setText("Error occurred");
                            messageInput.requestFocus();
                        });
                    }
                });
            } catch (Exception e) {
                logger.error("========== Error Initiating Streaming Query ==========");
                logger.error("Session: {}", currentSession.getName());
                logger.error("Exception type: {}", e.getClass().getSimpleName());
                logger.error("Exception details", e);
                Platform.runLater(() -> {
                    // Remove placeholder AI message
                    chatContainer.getChildren().remove(aiMessageBox);

                    AlertHelper.showError("Error", "Failed to get response", e.getMessage());

                    // Re-enable all controls
                    toggleAllControlsCallback.run();
                    statusLabel.setText("Error getting response");
                });
            }
        }).start();
    }
}
