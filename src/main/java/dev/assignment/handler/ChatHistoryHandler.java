package dev.assignment.handler;

import java.util.List;

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
            return;
        }

        logger.info("Loading chat history for session: {}", currentSession.getName());

        ChatAreaMessage loadingMessage = new ChatAreaMessage("Loading chat history...");
        chatContainer.getChildren().add(loadingMessage);
        statusLabel.setText("Loading chat history...");
        sessionStateHandler.setInputControlsDisabled(true);
        new Thread(() -> {
            List<ChatMessage> history = DatabaseService.getInstance().getChatHistory(currentSession.getId());

            Platform.runLater(() -> {
                chatContainer.getChildren().remove(loadingMessage);

                if (history.isEmpty()) {
                    ChatAreaMessage emptyMessage = new ChatAreaMessage(
                            "Start a conversation!\n\n" +
                                    "Send a message to chat with your knowledgebase.");
                    chatContainer.getChildren().add(emptyMessage);
                    logger.info("No chat history found - showing empty state message");
                } else {
                    for (ChatMessage message : history) {
                        ChatMessageEntry messageBox = new ChatMessageEntry(message);
                        chatContainer.getChildren().add(messageBox);
                    }
                    logger.info("Loaded {} messages from chat history", history.size());
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
        logger.info("Clearing session");

        if (currentSession == null) {
            logger.warn("No session selected for clearing session");
            return;
        }

        boolean confirmClear = AlertHelper.showConfirm(
                "Clear Session",
                "Clear all messages in this session?",
                "This action cannot be undone.");

        if (confirmClear) {
            DatabaseService.getInstance().clearChatHistory(currentSession.getId());

            RAGService ragService = sessionStateHandler.getRagService();
            if (ragService != null) {
                ragService.clearHistory();
            }

            chatContainer.getChildren().removeIf(node -> node instanceof ChatMessageEntry);

            ChatAreaMessage emptyMessage = new ChatAreaMessage(
                    "Start a conversation!\n\n" +
                            "Send a message to chat with your knowledgebase.");
            chatContainer.getChildren().add(emptyMessage);

            logger.info("Session cleared for session: {}", currentSession.getName());
            statusLabel.setText("Session cleared");
        }
    }

    /**
     * Clear the chat container.
     */
    public void clearChatContainer() {
        chatContainer.getChildren().clear();
    }
}
