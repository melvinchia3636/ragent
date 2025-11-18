package dev.assignment.controller;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.assignment.model.ChatMessage;
import dev.assignment.model.QueryResponse;
import dev.assignment.model.Resource;
import dev.assignment.model.Session;
import dev.assignment.service.APIKeyService;
import dev.assignment.service.DatabaseService;
import dev.assignment.service.RAGService;
import dev.assignment.service.ResourceService;
import dev.assignment.view.ChatAreaMessage;
import dev.assignment.view.ChatMessageBox;
import dev.assignment.view.SessionSidebar;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for managing chat sessions, messages, and knowledgebase
 * Consolidates session management, message handling, and resource management
 */
public class ChatSessionController {

    private static final Logger logger = LogManager.getLogger(ChatSessionController.class);

    // UI Components
    private final Label sessionNameLabel;
    private final Label sessionCreatedLabel;
    private final VBox chatContainer;
    private final TextField messageInput;
    private final Button sendButton;
    private final Label statusLabel;
    private final Label modelLabel;
    private final Button manageKnowledgebaseButton;
    private final Button clearConversationButton;
    private SessionSidebar sessionSidebar;

    // Services
    private ResourceService resourceService;
    private RAGService ragService;

    // Current session
    private Session currentSession;

    public ChatSessionController(
            Label sessionNameLabel,
            Label sessionCreatedLabel,
            VBox chatContainer,
            TextField messageInput,
            Button sendButton,
            Label statusLabel,
            Label modelLabel,
            Button manageKnowledgebaseButton,
            Button clearConversationButton,
            SessionSidebar sessionSidebar) {
        this.sessionNameLabel = sessionNameLabel;
        this.sessionCreatedLabel = sessionCreatedLabel;
        this.chatContainer = chatContainer;
        this.messageInput = messageInput;
        this.sendButton = sendButton;
        this.statusLabel = statusLabel;
        this.modelLabel = modelLabel;
        this.manageKnowledgebaseButton = manageKnowledgebaseButton;
        this.clearConversationButton = clearConversationButton;
        this.sessionSidebar = sessionSidebar;

        // Disable inputs by default when no session is selected
        setInputControlsDisabled(true);

        logger.info("ChatSessionController initialized");
    }

    /**
     * Set the disable state for input controls (send button and message input)
     * 
     * @param disabled true to disable the controls, false to enable them
     */
    private void setInputControlsDisabled(boolean disabled) {
        sendButton.setDisable(disabled);
        messageInput.setDisable(disabled);
    }

    private void toggleDisabilityOfAllControls(boolean disable) {
        setInputControlsDisabled(disable);
        manageKnowledgebaseButton.setDisable(disable);
        clearConversationButton.setDisable(disable);
        if (sessionSidebar != null) {
            sessionSidebar.setDisable(disable);
        }
    }

    /**
     * Handle session selection
     */
    public void handleSessionSelected(Session session) {
        logger.info("Session selected: {}", session.getName());
        this.currentSession = session;
        resourceService = new ResourceService(session.getId());
        updateSessionInfoDisplay(session);

        // Clear chat and initialize RAG service
        chatContainer.getChildren().clear();

        APIKeyService apiKeyService = APIKeyService.getInstance();
        if (apiKeyService.hasApiKey()) {
            ragService = new RAGService(session.getId(), session.getModel());

            // Check if knowledge base is empty
            List<Resource> resources = resourceService.getAllResources();
            if (resources.isEmpty()) {
                // Show empty knowledge base message
                ChatAreaMessage emptyMessage = new ChatAreaMessage(
                        "Your knowledge base is empty.\n\n" +
                                "Click 'Manage Knowledgebase' to add documents.");
                chatContainer.getChildren().add(emptyMessage);

                // Disable input
                setInputControlsDisabled(true);
                statusLabel.setText("Knowledge base is empty");
                logger.info("Knowledge base is empty for session: {}", session.getName());
                return;
            }

            // Disable inputs before indexing
            setInputControlsDisabled(true);
            statusLabel.setText("Preparing to index...");

            // Show indexing message in chat area
            ChatAreaMessage indexingMessage = new ChatAreaMessage(
                    "Indexing knowledgebase...\n\n" +
                            "Please check the bottom left corner for indexing progress.");
            chatContainer.getChildren().add(indexingMessage);

            // Index knowledgebase in background with progress updates
            new Thread(() -> {
                try {
                    ragService.indexKnowledgebase(resourceService, (message, current, total) -> {
                        Platform.runLater(() -> {
                            if (total > 0) {
                                statusLabel.setText(String.format("Indexing... (%d/%d) - %s", current, total, message));
                            } else {
                                statusLabel.setText(message);
                            }
                        });
                    });
                    Platform.runLater(() -> {
                        chatContainer.getChildren().remove(indexingMessage);

                        // Load chat history after indexing (it will handle status and button state)
                        loadChatHistory();

                        logger.info("Knowledgebase indexed successfully");
                    });
                } catch (IOException e) {
                    logger.error("Error indexing knowledgebase", e);
                    Platform.runLater(() -> {
                        chatContainer.getChildren().remove(indexingMessage);
                        statusLabel.setText("Error indexing knowledgebase");
                        setInputControlsDisabled(false);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Indexing Error");
                        alert.setHeaderText("Failed to index knowledgebase");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        } else {
            setInputControlsDisabled(true);
            logger.warn("No API key - chat disabled");
        }
    }

    /**
     * Handle session changes (update, delete, etc.)
     */
    public void handleSessionChanged() {
        logger.info("Session changed event");
        if (currentSession != null) {
            // Check if current session still exists
            Session updatedSession = DatabaseService.getInstance().getSession(currentSession.getId());
            if (updatedSession != null) {
                // Check if model changed BEFORE updating currentSession
                String oldModel = currentSession.getModel();
                String newModel = updatedSession.getModel();
                boolean modelChanged = !oldModel.equals(newModel);

                logger.info("Session update - Old model: {}, New model: {}, Changed: {}",
                        oldModel, newModel, modelChanged);

                // Session was updated (renamed or model changed), refresh the display
                currentSession = updatedSession;
                updateSessionInfoDisplay(currentSession);

                // If model changed, reinitialize RAGService with the new model
                if (modelChanged && ragService != null) {
                    logger.info("Model changed from {} to {}, reinitializing RAGService", oldModel, newModel);
                    ragService = new RAGService(currentSession.getId(), currentSession.getModel());
                }
            } else {
                // Session was deleted, reinitialize the chat area
                logger.info("Current session was deleted, reinitializing chat area");
                currentSession = null;
                ragService = null;
                resourceService = null;
                chatContainer.getChildren().clear();
                updateSessionInfoDisplay(null);
            }
        } else {
            updateSessionInfoDisplay(null);
        }
    }

    /**
     * Update session information display
     */
    private void updateSessionInfoDisplay(Session session) {
        if (session != null) {
            sessionNameLabel.setText(session.getName());
            sessionCreatedLabel.setText("Created on " + session.getFormattedCreatedAt());
            modelLabel.setText("Model: " + session.getModel());
            manageKnowledgebaseButton.setVisible(true);
            manageKnowledgebaseButton.setManaged(true);
            clearConversationButton.setVisible(true);
            clearConversationButton.setManaged(true);
            logger.debug("Updated session info: {}", session.getName());
        } else {
            sessionNameLabel.setText("No Session Selected");
            sessionCreatedLabel.setText("");
            modelLabel.setText("");
            manageKnowledgebaseButton.setVisible(false);
            manageKnowledgebaseButton.setManaged(false);
            clearConversationButton.setVisible(false);
            clearConversationButton.setManaged(false);
            setInputControlsDisabled(true);
            logger.debug("Cleared session info");
        }
    }

    /**
     * Handle opening the knowledgebase management window
     */
    public void handleManageKnowledgebase() {
        logger.info("Opening knowledgebase management");

        if (currentSession == null) {
            logger.warn("No session selected for knowledgebase management");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dev/assignment/manage_resources.fxml"));
            Parent root = loader.load();

            // Pass resource service to the controller
            ResourceManagementController controller = loader.getController();
            controller.setResourceService(resourceService);
            controller.setRagService(ragService);

            // Set callback for when resources change
            controller.setOnResourcesChangedCallback(() -> {
                recheckKnowledgebaseStatus();
            });

            Stage stage = new Stage();
            stage.setTitle("Manage Knowledgebase - " + currentSession.getName());
            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);

            stage.centerOnScreen();

            stage.show();
            logger.info("Knowledgebase management window opened");
        } catch (IOException e) {
            logger.error("Error opening knowledgebase management", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open knowledgebase management");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Handle sending a message
     */
    public void handleSendMessage() {
        String userMessage = messageInput.getText().trim();

        if (userMessage.isEmpty()) {
            return;
        }

        if (ragService == null) {
            logger.warn("Attempted to send message without RAG service");
            return;
        }

        logger.info("Sending message: {}", userMessage);

        // Clear input
        messageInput.clear();

        // Add user message to chat
        ChatMessage userChatMessage = new ChatMessage(userMessage, true);
        ChatMessageBox userMessageBox = new ChatMessageBox(userChatMessage);
        chatContainer.getChildren().add(userMessageBox);

        // Save user message to database
        DatabaseService.getInstance().saveChatMessage(currentSession.getId(), userChatMessage);

        // Show loading indicator
        Label loadingLabel = new Label("Getting response...");
        loadingLabel.setStyle("-fx-text-fill: #0000008b;");
        loadingLabel.setMaxWidth(Double.MAX_VALUE);
        loadingLabel.setAlignment(Pos.CENTER_LEFT);
        chatContainer.getChildren().add(loadingLabel);

        // Disable all controls while processing
        toggleDisabilityOfAllControls(true);
        statusLabel.setText("Generating response...");

        // Query RAG in background
        String finalUserMessage = userMessage;
        new Thread(() -> {
            try {
                QueryResponse queryResponse = ragService.query(finalUserMessage);
                logger.info("Received response with {} sources", queryResponse.getSources().size());

                Platform.runLater(() -> {
                    // Remove loading indicator
                    chatContainer.getChildren().remove(loadingLabel);

                    // Prepare sources for AI message
                    String sources = queryResponse.hasSources()
                            ? String.join(", ", queryResponse.getSources())
                            : null;

                    // Add AI response to chat
                    ChatMessage aiChatMessage = new ChatMessage(
                            queryResponse.getResponse(),
                            false,
                            sources);
                    ChatMessageBox aiMessageBox = new ChatMessageBox(aiChatMessage);
                    chatContainer.getChildren().add(aiMessageBox);

                    // Save AI message to database
                    DatabaseService.getInstance().saveChatMessage(currentSession.getId(), aiChatMessage);

                    // Re-enable all controls
                    toggleDisabilityOfAllControls(false);
                    statusLabel.setText("Ready");
                    messageInput.requestFocus();
                });
            } catch (Exception e) {
                logger.error("Error getting response", e);
                Platform.runLater(() -> {
                    // Remove loading indicator
                    chatContainer.getChildren().remove(loadingLabel);

                    // Show error
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to get response");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();

                    // Re-enable all controls
                    toggleDisabilityOfAllControls(false);
                    statusLabel.setText("Error getting response");
                });
            }
        }).start();
    }

    /**
     * Load chat history from database
     */
    private void loadChatHistory() {
        if (currentSession == null) {
            return;
        }

        logger.info("Loading chat history for session: {}", currentSession.getName());

        // Show loading message in chat area
        ChatAreaMessage loadingMessage = new ChatAreaMessage("Loading chat history...");
        chatContainer.getChildren().add(loadingMessage);

        // Update status bar
        statusLabel.setText("Loading chat history...");

        // Disable input while loading
        setInputControlsDisabled(true);

        // Load history in background to keep UI responsive
        new Thread(() -> {
            List<ChatMessage> history = DatabaseService.getInstance().getChatHistory(currentSession.getId());

            Platform.runLater(() -> {
                // Remove loading message
                chatContainer.getChildren().remove(loadingMessage);

                // Add messages to chat
                for (ChatMessage message : history) {
                    ChatMessageBox messageBox = new ChatMessageBox(message);
                    chatContainer.getChildren().add(messageBox);
                }

                logger.info("Loaded {} messages from chat history", history.size());

                // Update status and re-enable input
                statusLabel.setText("Ready");
                setInputControlsDisabled(false);
            });
        }).start();
    }

    /**
     * Get the current session
     */
    public Session getCurrentSession() {
        return currentSession;
    }

    /**
     * Recheck if knowledge base is empty and update UI accordingly
     */
    public void recheckKnowledgebaseStatus() {
        if (currentSession == null || resourceService == null) {
            return;
        }

        Platform.runLater(() -> {
            List<Resource> resources = resourceService.getAllResources();
            boolean isEmpty = resources.isEmpty();

            logger.info("Rechecking knowledge base status: {} resources found", resources.size());

            if (isEmpty) {
                boolean hasChatHistory = chatContainer.getChildren().stream()
                        .anyMatch(node -> node instanceof ChatMessageBox);

                if (hasChatHistory) {
                    // Preserve chat history but show empty message at the bottom
                    // Remove any existing ChatAreaMessage
                    chatContainer.getChildren().removeIf(node -> node instanceof ChatAreaMessage);

                    ChatAreaMessage emptyMessage = new ChatAreaMessage(
                            "Your knowledge base is empty.\n\n" +
                                    "Click 'Manage Knowledgebase' to add documents.");
                    chatContainer.getChildren().add(emptyMessage);
                    logger.info("Knowledge base is now empty - preserving chat history");
                } else {
                    chatContainer.getChildren().clear();
                    ChatAreaMessage emptyMessage = new ChatAreaMessage(
                            "Your knowledge base is empty.\n\n" +
                                    "Click 'Manage Knowledgebase' to add documents.");
                    chatContainer.getChildren().add(emptyMessage);
                    logger.info("Knowledge base is now empty - no chat history to preserve");
                }

                setInputControlsDisabled(true);
                statusLabel.setText("Knowledge base is empty");
            } else {
                // Knowledge base has content - check if we need to re-enable chat
                boolean hasEmptyMessage = chatContainer.getChildren().stream()
                        .anyMatch(node -> node instanceof ChatAreaMessage);

                if (hasEmptyMessage) {
                    logger.info("Knowledge base now has content - re-enabling chat");
                    // Remove the empty message
                    chatContainer.getChildren().removeIf(node -> node instanceof ChatAreaMessage);
                }

                setInputControlsDisabled(false);
                statusLabel.setText("Ready");
            }
        });
    }

    /**
     * Handle clearing the conversation history
     */
    public void handleClearConversation() {
        logger.info("Clearing conversation");

        if (currentSession == null) {
            logger.warn("No session selected for clearing conversation");
            return;
        }

        // Confirm with user
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Clear Conversation");
        confirmAlert.setHeaderText("Clear all messages in this conversation?");
        confirmAlert.setContentText("This action cannot be undone.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Clear from database
                DatabaseService.getInstance().clearChatHistory(currentSession.getId());

                // Clear from RAG service (conversation history)
                if (ragService != null) {
                    ragService.clearHistory();
                }

                // Clear from UI
                chatContainer.getChildren().removeIf(node -> node instanceof ChatMessageBox);

                logger.info("Conversation cleared for session: {}", currentSession.getName());
                statusLabel.setText("Conversation cleared");
            }
        });
    }
}
