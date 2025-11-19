package dev.assignment.handler;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.assignment.model.ChatMessage;
import dev.assignment.model.Session;
import dev.assignment.service.DatabaseService;
import dev.assignment.service.RAGService;
import dev.assignment.view.AlertHelper;
import dev.assignment.view.ChatAreaMessage;
import dev.assignment.view.ChatMessageEntry;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Handles chat history operations including loading and clearing.
 */
public class ChatHistoryHandler {

    private static final Logger logger = LogManager.getLogger(ChatHistoryHandler.class);

    private final VBox chatContainer;
    private final Label statusLabel;
    private final SessionStateHandler sessionStateHandler;

    public ChatHistoryHandler(
            VBox chatContainer,
            Label statusLabel,
            SessionStateHandler sessionStateHandler) {
        this.chatContainer = chatContainer;
        this.statusLabel = statusLabel;
        this.sessionStateHandler = sessionStateHandler;
    }

    /**
     * Load chat history from database.
     */
    public void loadChatHistory() {
        Session currentSession = sessionStateHandler.getCurrentSession();
        if (currentSession == null) {
            logger.warn("Cannot load chat history: No session selected");
            return;
        }

        logger.info("========== Loading Chat History ==========");
        logger.info("Session: id={}, name='{}'", currentSession.getId(), currentSession.getName());

        ChatAreaMessage loadingMessage = new ChatAreaMessage("Loading chat history...");
        chatContainer.getChildren().add(loadingMessage);
        statusLabel.setText("Loading chat history...");
        sessionStateHandler.setInputControlsDisabled(true);
        new Thread(() -> {
            DatabaseService databaseService = DatabaseService.getInstance();
            if (databaseService == null) {
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(loadingMessage);
                    ChatAreaMessage errorMessage = new ChatAreaMessage(
                            "Database unavailable\n\nCannot load chat history.");
                    chatContainer.getChildren().add(errorMessage);
                    sessionStateHandler.setInputControlsDisabled(false);
                    statusLabel.setText("Ready");
                });
                return;
            }

            List<ChatMessage> history = databaseService.getChatHistory(currentSession.getId());

            Platform.runLater(() -> {
                chatContainer.getChildren().remove(loadingMessage);

                if (history.isEmpty()) {
                    ChatAreaMessage emptyMessage = new ChatAreaMessage(
                            "Start a conversation!\n\n" +
                                    "Send a message to chat with your knowledgebase.");
                    chatContainer.getChildren().add(emptyMessage);
                    logger.info("No chat history found for session: {}", currentSession.getName());
                } else {
                    for (ChatMessage message : history) {
                        ChatMessageEntry messageBox = new ChatMessageEntry(message);
                        chatContainer.getChildren().add(messageBox);
                    }
                    logger.info("Successfully loaded {} messages for session: {}",
                            history.size(), currentSession.getName());
                }

                statusLabel.setText("Ready");
                sessionStateHandler.setInputControlsDisabled(false);
            });
        }).start();
    }

    /**
     * Handle clearing the session history.
     */
    public void handleClearSession() {
        Session currentSession = sessionStateHandler.getCurrentSession();

        logger.info("========== Clear Session Request ==========");
        logger.info("Session: id={}, name='{}'", currentSession.getId(), currentSession.getName());

        boolean confirmed = AlertHelper.showConfirm(
                "Clear Session",
                "Are you sure you want to clear this session?",
                "This will delete all chat history for this session. This action cannot be undone.");

        if (confirmed) {
            DatabaseService databaseService = DatabaseService.getInstance();
            if (databaseService == null) {
                AlertHelper.showError(
                        "Database Error",
                        "Cannot Clear Session",
                        "The database is unavailable.");
                return;
            }

            databaseService.clearChatHistory(currentSession.getId());
            RAGService ragService = sessionStateHandler.getRagService();
            if (ragService != null) {
                ragService.clearHistory();
            }

            chatContainer.getChildren().removeIf(node -> node instanceof ChatMessageEntry);

            ChatAreaMessage emptyMessage = new ChatAreaMessage(
                    "Start a conversation!\n\n" +
                            "Send a message to chat with your knowledgebase.");
            chatContainer.getChildren().add(emptyMessage);

            logger.info("Session successfully cleared: id={}, name='{}'",
                    currentSession.getId(), currentSession.getName());
            statusLabel.setText("Session cleared");
        } else {
            logger.info("Session clear cancelled by user");
        }
    }

    /**
     * Clear the chat container.
     */
    public void clearChatContainer() {
        chatContainer.getChildren().clear();
    }
}
