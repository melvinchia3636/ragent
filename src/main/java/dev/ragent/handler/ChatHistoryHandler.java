package dev.ragent.handler;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.girod.javafx.svgimage.SVGImage;

import dev.ragent.model.ChatMessage;
import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.service.RAGService;
import dev.ragent.util.Icon;
import dev.ragent.view.AlertHelper;
import dev.ragent.view.ChatAreaMessage;
import dev.ragent.view.ChatMessageEntry;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Handles chat history operations including loading and clearing.
 */
public class ChatHistoryHandler {

    private static final Logger logger = LogManager.getLogger(ChatHistoryHandler.class);

    private final VBox chatContainer;
    private final Label statusLabel;
    private final SessionStateHandler sessionStateHandler;
    private VBox placeholderContainer;

    public ChatHistoryHandler(
            VBox chatContainer,
            Label statusLabel,
            SessionStateHandler sessionStateHandler) {
        this.chatContainer = chatContainer;
        this.statusLabel = statusLabel;
        this.sessionStateHandler = sessionStateHandler;

        initializePlaceholder();

        // Show placeholder initially
        showPlaceholder();
    }

    /**
     * Initialize the placeholder container for when no session is selected.
     */
    private void initializePlaceholder() {
        // Create placeholder container with title and subtitle
        this.placeholderContainer = new VBox(8);
        this.placeholderContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(this.placeholderContainer, Priority.ALWAYS);
        this.placeholderContainer.setMaxWidth(Double.MAX_VALUE);
        this.placeholderContainer.setMaxHeight(Double.MAX_VALUE);

        SVGImage iconView = Icon.load("tabler--message-search.svg");
        iconView.getStyleClass().add("chat-placeholder-icon");

        Label titleLabel = new Label("RAGent");
        titleLabel.getStyleClass().add("chat-placeholder-title");

        Label subtitleLabel = new Label("An experience beyond just an assignment");
        subtitleLabel.getStyleClass().add("chat-placeholder-subtitle");

        this.placeholderContainer.getChildren().addAll(iconView, titleLabel, subtitleLabel);
    }

    /**
     * Show the placeholder label when no session is selected.
     */
    public void showPlaceholder() {
        chatContainer.getChildren().clear();
        chatContainer.getChildren().add(placeholderContainer);
    }

    /**
     * Hide the placeholder label when a session is selected.
     */
    private void hidePlaceholder() {
        chatContainer.getChildren().remove(placeholderContainer);
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

        hidePlaceholder();
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
                            """
                                    Start a conversation!

                                    Send a message to chat with your knowledgebase.""");
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
                    """
                            Start a conversation!

                            Send a message to chat with your knowledgebase.""");
            chatContainer.getChildren().add(emptyMessage);

            logger.info("Session successfully cleared: id={}, name='{}'",
                    currentSession.getId(), currentSession.getName());
            statusLabel.setText("Session cleared");
        } else {
            logger.info("Session clear cancelled by user");
        }
    }

    /**
     * Clear the chat container and show placeholder.
     */
    public void clearChatContainer() {
        showPlaceholder();
    }
}
