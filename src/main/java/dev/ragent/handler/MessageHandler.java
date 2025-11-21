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
     * Create a regeneration callback for an AI message entry
     */
    private Runnable createRegenerationCallback(ChatMessageEntry aiMessageEntry) {
        return () -> regenerateMessage(aiMessageEntry);
    }

    /**
     * Regenerate the response for an AI message
     */
    public void regenerateMessage(ChatMessageEntry aiMessageEntry) {
        ChatMessage aiMessage = aiMessageEntry.getMessage();

        // Find the user message that preceded this AI message
        int aiMessageIndex = chatContainer.getChildren().indexOf(aiMessageEntry);
        if (aiMessageIndex <= 0) {
            logger.error("Cannot regenerate: AI message not found in chat container");
            return;
        }

        // Get the user message entry (should be immediately before the AI message)
        ChatMessageEntry userMessageEntry = null;
        for (int i = aiMessageIndex - 1; i >= 0; i--) {
            if (chatContainer.getChildren().get(i) instanceof ChatMessageEntry entry) {
                if (entry.getMessage().isUser()) {
                    userMessageEntry = entry;
                    break;
                }
            }
        }

        if (userMessageEntry == null) {
            logger.error("Cannot regenerate: No user message found before AI message");
            AlertHelper.showError("Error", "Cannot find the original user message to regenerate the response.");
            return;
        }

        ChatMessage userMessage = userMessageEntry.getMessage();
        Session currentSession = sessionStateHandler.getCurrentSession();
        if (currentSession == null) {
            logger.error("Cannot regenerate: No session selected");
            return;
        }

        // Delete all messages from database after (and including) the AI message's
        // timestamp
        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService != null) {
            databaseService.deleteMessagesAfter(currentSession.getId(), aiMessage.getTimestamp());
            logger.info("Deleted messages after timestamp: {}", aiMessage.getTimestamp());
        }

        // Remove all UI messages after (and including) the AI message
        int indexToRemoveFrom = aiMessageIndex;
        chatContainer.getChildren().remove(indexToRemoveFrom, chatContainer.getChildren().size());

        // Regenerate the response
        generateResponse(userMessage.getContent());
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

        // Generate the response
        generateResponse(userMessage);
    }

    /**
     * Generate an AI response for the given user message
     */
    private void generateResponse(String userMessage) {
        RAGService ragService = sessionStateHandler.getRagService();
        if (ragService == null) {
            logger.error("Cannot generate response: RAG service not initialized");
            return;
        }

        Session currentSession = sessionStateHandler.getCurrentSession();
        if (currentSession == null) {
            logger.error("Cannot generate response: No session selected");
            return;
        }

        // Create placeholder for AI response
        ChatMessage aiChatMessage = new ChatMessage("...", false);
        ChatMessageEntry aiMessageBox = new ChatMessageEntry(aiChatMessage);
        chatContainer.getChildren().add(aiMessageBox);

        // Set the regeneration callback after the message box is created
        aiMessageBox.setOnRegenerateCallback(createRegenerationCallback(aiMessageBox));
        logger.debug("Set regeneration callback for new AI message: {}", aiChatMessage.getId());

        // Disable all controls and message buttons while processing
        toggleAllControlsCallback.run();
        setAllMessageButtonsEnabled(false);
        statusLabel.setText("Generating response...");

        // Query RAG with streaming in background
        String finalUserMessage = userMessage;
        new Thread(() -> {
            try {
                ragService.queryStreaming(finalUserMessage, new RAGService.StreamingCallback() {
                    private final StringBuilder responseBuilder = new StringBuilder();
                    private List<String> sources = new ArrayList<>();

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

                            // Re-enable all controls and message buttons
                            toggleAllControlsCallback.run();
                            setAllMessageButtonsEnabled(true);
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

                            // Re-enable all controls and message buttons
                            toggleAllControlsCallback.run();
                            setAllMessageButtonsEnabled(true);
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

                    // Re-enable all controls and message buttons
                    toggleAllControlsCallback.run();
                    setAllMessageButtonsEnabled(true);
                    statusLabel.setText("Error getting response");
                });
            }
        }).start();
    }

    /**
     * Enable or disable buttons on all message entries
     */
    private void setAllMessageButtonsEnabled(boolean enabled) {
        for (javafx.scene.Node node : chatContainer.getChildren()) {
            if (node instanceof ChatMessageEntry messageEntry) {
                messageEntry.setButtonsEnabled(enabled);
            }
        }
    }
}
