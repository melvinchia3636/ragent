package dev.assignment;

import dev.assignment.controller.ChatSessionController;
import dev.assignment.service.APIKeyService;
import dev.assignment.service.DatabaseService;
import dev.assignment.view.AlertHelper;
import dev.assignment.view.SessionSidebar;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
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
    private TextArea messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label modelLabel;

    @FXML
    private Button manageKnowledgebaseButton;

    @FXML
    private Button clearSessionButton;

    private ChatSessionController chatSessionController;

    @FXML
    private void initialize() {
        // Initialize database
        DatabaseService.getInstance();

        // Initialize API key service and load key
        APIKeyService apiKeyService = APIKeyService.getInstance();
        boolean hasApiKey = apiKeyService.loadApiKey();

        // Update status based on API key availability and validity
        if (hasApiKey) {
            statusLabel.setText("Validating API Key...");

            // Validate API key in background thread
            new Thread(() -> {
                boolean isValid = apiKeyService.validateApiKey();

                javafx.application.Platform.runLater(() -> {
                    if (isValid) {
                        statusLabel.setText("API Key validated successfully");
                    } else {
                        statusLabel.setText("Invalid API Key - Chat disabled");
                        sendButton.setDisable(true);
                        messageInput.setDisable(true);

                        AlertHelper.showError(
                                "Invalid API Key",
                                "API Key Validation Failed",
                                "The provided OpenAI API key is invalid. Please check your .env file or provide a valid key.");
                    }
                });
            }).start();
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
                clearSessionButton,
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
    private void handleClearSession() {
        chatSessionController.handleClearSession();
    }

    @FXML
    private void handleSendMessage() {
        chatSessionController.handleSendMessage();
    }
}
