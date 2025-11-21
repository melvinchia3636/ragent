package dev.ragent.controller;

import dev.ragent.service.APIKeyService;
import dev.ragent.service.DatabaseService;
import dev.ragent.service.PreferencesService;
import dev.ragent.util.Icon;
import dev.ragent.view.AlertHelper;
import dev.ragent.view.SessionSidebar;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML
    private VBox mainWrapper;

    @FXML
    private SessionSidebar sessionSidebar;

    @FXML
    private HBox sessionHeader;

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
        initializeIcons();

        // Initialize database
        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            AlertHelper.showError(
                    "Database Error",
                    "Failed to Initialize Database",
                    "The application database could not be initialized. Please check file permissions and disk space.\\n\\nThe application will continue with limited functionality.");

            // Disable session-related features
            if (sessionSidebar != null) {
                sessionSidebar.setDisable(true);
            }
            sendButton.setDisable(true);
            messageInput.setDisable(true);
            manageKnowledgebaseButton.setDisable(true);
            clearSessionButton.setDisable(true);
            statusLabel.setText("Database unavailable");
            return;
        }

        initializeAPIKeys();

        // Initialize chat session controller
        chatSessionController = new ChatSessionController(
                sessionHeader,
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

        sessionSidebar.loadSessions();

        // Initialize theme when scene is available
        if (mainWrapper.getScene() != null) {
            initializeTheme();
        } else {
            mainWrapper.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    initializeTheme();
                }
            });
        }

        // Auto-scroll chat to bottom on new messages
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void initializeAPIKeys() {
        // Initialize API key service and load key
        APIKeyService apiKeyService = APIKeyService.getInstance();
        boolean hasApiKey = apiKeyService.loadApiKey();

        // Exit application if no API key found
        if (!hasApiKey) {
            AlertHelper.showError(
                    "Missing API Key",
                    "OpenAI API Key Not Found",
                    "No OPENAI_API_KEY found in .env file.\n\n" +
                            "Please create a .env file in the project root directory with:\n" +
                            "OPENAI_API_KEY=your-api-key-here\n\n" +
                            "The application will now exit.");

            Platform.exit();
            return;
        }

        // Validate API key in background thread
        statusLabel.setText("Validating API Key...");
        new Thread(() -> {
            boolean isValid = apiKeyService.validateApiKey();

            Platform.runLater(() -> {
                if (isValid) {
                    statusLabel.setText("API Key validated successfully");
                } else {
                    statusLabel.setText("Invalid API Key");

                    AlertHelper.showError(
                            "Invalid API Key",
                            "API Key Validation Failed",
                            "The OPENAI_API_KEY in your .env file is invalid.\n\n" +
                                    "Please check your .env file and ensure it contains a valid OpenAI API key.\n\n" +
                                    "The application will now exit.");

                    Platform.exit();
                }
            });
        }).start();
    }

    private void initializeIcons() {
        clearSessionButton.setGraphic(
                Icon.load("tabler--trash-x.svg"));
        manageKnowledgebaseButton.setGraphic(
                Icon.load("tabler--database-cog.svg"));
        sendButton.setGraphic(
                Icon.load("tabler--arrow-up.svg"));
        sendButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        modelLabel.setGraphic(
                Icon.load("tabler--robot.svg"));
    }

    private void initializeTheme() {
        PreferencesService prefsService = PreferencesService.getInstance();
        prefsService.setScene(mainWrapper.getScene());
        prefsService.applyTheme();
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
