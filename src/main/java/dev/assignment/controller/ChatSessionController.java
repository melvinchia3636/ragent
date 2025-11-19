package dev.assignment.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.assignment.handler.ChatHistoryHandler;
import dev.assignment.handler.KnowledgebaseHandler;
import dev.assignment.handler.MessageHandler;
import dev.assignment.handler.SessionStateHandler;
import dev.assignment.model.Session;
import dev.assignment.view.SessionSidebar;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Controller for managing chat sessions, messages, and knowledgebase.
 * Coordinates between various handler classes for separation of concerns.
 */
public class ChatSessionController {

    private static final Logger logger = LogManager.getLogger(ChatSessionController.class);

    // UI Components
    private final Button manageKnowledgebaseButton;
    private final Button clearSessionButton;
    private final SessionSidebar sessionSidebar;

    // Processing state
    private boolean isProcessing = false;

    // Handlers
    private final SessionStateHandler sessionStateHandler;
    private final ChatHistoryHandler chatHistoryHandler;
    private final MessageHandler messageHandler;
    private final KnowledgebaseHandler knowledgebaseHandler;

    public ChatSessionController(
            Label sessionNameLabel,
            Label sessionCreatedLabel,
            VBox chatContainer,
            TextArea messageInput,
            Button sendButton,
            Label statusLabel,
            Label modelLabel,
            Button manageKnowledgebaseButton,
            Button clearSessionButton,
            SessionSidebar sessionSidebar) {
        this.manageKnowledgebaseButton = manageKnowledgebaseButton;
        this.clearSessionButton = clearSessionButton;
        this.sessionSidebar = sessionSidebar;

        this.sessionStateHandler = new SessionStateHandler(
                sessionNameLabel,
                sessionCreatedLabel,
                modelLabel,
                manageKnowledgebaseButton,
                clearSessionButton,
                messageInput,
                sendButton);

        this.chatHistoryHandler = new ChatHistoryHandler(
                chatContainer,
                statusLabel,
                sessionStateHandler);

        this.messageHandler = new MessageHandler(
                chatContainer,
                messageInput,
                statusLabel,
                sessionStateHandler,
                this::toggleControlsDuringProcessing);

        this.knowledgebaseHandler = new KnowledgebaseHandler(
                chatContainer,
                statusLabel,
                sessionStateHandler,
                chatHistoryHandler);

        // Set up keyboard shortcut: Ctrl+Enter (Windows/Linux) or Cmd+Enter (Mac) to
        // send
        messageInput.setOnKeyPressed(event -> {
            if ((event.isShortcutDown() || event.isControlDown())
                    && event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                event.consume();
                handleSendMessage();
            }
        });

        messageInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int lineCount = newValue.split("\n", -1).length;
                int newRowCount = Math.min(Math.max(1, lineCount), 10); // Min 1, Max 10 rows
                messageInput.setPrefRowCount(newRowCount);
            }
        });

        sessionStateHandler.setInputControlsDisabled(true);

        logger.info("ChatSessionController initialized");
    }

    /**
     * Toggle the disable state of all controls.
     */
    private void toggleDisabilityOfAllControls(boolean disable) {
        sessionStateHandler.setInputControlsDisabled(disable);
        manageKnowledgebaseButton.setDisable(disable);
        clearSessionButton.setDisable(disable);
        if (sessionSidebar != null) {
            sessionSidebar.setDisable(disable);
        }
    }

    /**
     * Toggle controls during message processing.
     * Called before processing starts and after it completes.
     */
    private void toggleControlsDuringProcessing() {
        isProcessing = !isProcessing;
        toggleDisabilityOfAllControls(isProcessing);
    }

    /**
     * Handle session selection.
     */
    public void handleSessionSelected(Session session) {
        logger.info("Session selected: {}", session.getName());
        sessionStateHandler.setCurrentSession(session);
        sessionStateHandler.updateSessionInfoDisplay(session);
        knowledgebaseHandler.initializeSession();
    }

    /**
     * Handle session changes (update, delete, etc.)
     */
    public void handleSessionChanged() {
        sessionStateHandler.handleSessionChanged();

        if (sessionStateHandler.getCurrentSession() == null) {
            chatHistoryHandler.clearChatContainer();
        }
    }

    /**
     * Handle opening the knowledgebase management window.
     */
    public void handleManageKnowledgebase() {
        knowledgebaseHandler.handleManageKnowledgebase();
    }

    /**
     * Handle sending a message.
     */
    public void handleSendMessage() {
        messageHandler.handleSendMessage();
    }

    /**
     * Handle clearing the session history.
     */
    public void handleClearSession() {
        chatHistoryHandler.handleClearSession();
    }

    /**
     * Get the current session.
     */
    public Session getCurrentSession() {
        return sessionStateHandler.getCurrentSession();
    }
}
