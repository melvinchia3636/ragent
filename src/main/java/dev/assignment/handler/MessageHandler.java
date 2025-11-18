package dev.assignment.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.assignment.model.ChatMessage;
import dev.assignment.model.Session;
import dev.assignment.service.DatabaseService;
import dev.assignment.service.RAGService;
import dev.assignment.util.Constants;
import dev.assignment.view.AlertHelper;
import dev.assignment.view.ChatAreaMessage;
import dev.assignment.view.ChatMessageEntry;
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
            logger.warn("Attempted to send message without RAG service");
            return;
        }

        Session currentSession = sessionStateHandler.getCurrentSession();
        if (currentSession == null) {
            logger.warn("Attempted to send message without session");
            return;
        }

        logger.info("Sending message: {}", userMessage);

        // Clear input
        messageInput.clear();

        // Remove empty state message if present
        chatContainer.getChildren().removeIf(node -> node instanceof ChatAreaMessage);

        // Add user message to chat
        ChatMessage userChatMessage = new ChatMessage(userMessage, true);
        ChatMessageEntry userMessageBox = new ChatMessageEntry(userChatMessage);
        chatContainer.getChildren().add(userMessageBox);

        // Save user message to database
        DatabaseService.getInstance().saveChatMessage(currentSession.getId(), userChatMessage);

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
                    private java.util.List<String> sources = new java.util.ArrayList<>();

                    @Override
                    public void onStart(java.util.List<String> sourceDocs) {
                        sources = sourceDocs;
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
                                aiMessageBox.setSources(sourcesText);
                            }

                            // Create final AI message with sources and save to database
                            ChatMessage finalAiMessage = new ChatMessage(fullResponse, false, sourcesText);
                            DatabaseService.getInstance().saveChatMessage(currentSession.getId(), finalAiMessage);

                            // Re-enable all controls
                            toggleAllControlsCallback.run();
                            statusLabel.setText("Ready");
                            messageInput.requestFocus();
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        logger.error("Error getting response", error);
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
                logger.error("Error initiating streaming query", e);
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
