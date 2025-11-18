package dev.assignment;

import dev.assignment.controller.ChatSessionController;
import dev.assignment.service.APIKeyService;
import dev.assignment.service.DatabaseService;
import dev.assignment.view.SessionSidebar;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML
    private SessionSidebar sessionSidebar;

    @FXML
    private Label sessionNameLabel;

    @FXML
    private Label sessionCreatedLabel;

    @FXML
    private VBox chatContainer;

    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label modelLabel;

    @FXML
    private Button manageKnowledgebaseButton;

    @FXML
    private Button clearConversationButton;

    private ChatSessionController chatSessionController;

    @FXML
    private void initialize() {
        // Initialize database
        DatabaseService.getInstance();

        // Initialize API key service and load key
        APIKeyService apiKeyService = APIKeyService.getInstance();
        boolean hasApiKey = apiKeyService.loadApiKey();

        // Update status based on API key availability
        if (hasApiKey) {
            statusLabel.setText("API Key loaded from .env");
        } else {
            statusLabel.setText("No API Key - Chat disabled");
            sendButton.setDisable(true);
            messageInput.setDisable(true);
        }

        // Initialize chat session controller
        chatSessionController = new ChatSessionController(
                sessionNameLabel,
                sessionCreatedLabel,
                chatContainer,
                messageInput,
                sendButton,
                statusLabel,
                modelLabel,
                manageKnowledgebaseButton,
                clearConversationButton,
                sessionSidebar);

        // Set up sidebar callbacks
        sessionSidebar.setOnSessionSelected(chatSessionController::handleSessionSelected);
        sessionSidebar.setOnSessionChanged(chatSessionController::handleSessionChanged);

        // Load sessions
        sessionSidebar.loadSessions();

        // Set up auto-scroll for chat
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void handleManageKnowledgebase() {
        chatSessionController.handleManageKnowledgebase();
    }

    @FXML
    private void handleClearConversation() {
        chatSessionController.handleClearConversation();
    }

    @FXML
    private void handleSendMessage() {
        chatSessionController.handleSendMessage();
    }
}
