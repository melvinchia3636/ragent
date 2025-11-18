package dev.assignment.controller;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.assignment.model.ChatMessage;
import dev.assignment.model.Resource;
import dev.assignment.model.Session;
import dev.assignment.service.APIKeyService;
import dev.assignment.service.DatabaseService;
import dev.assignment.service.RAGService;
import dev.assignment.service.ResourceService;
import dev.assignment.util.Constants;
import dev.assignment.view.AlertHelper;
import dev.assignment.view.ChatAreaMessage;
import dev.assignment.view.ChatMessageEntry;
import dev.assignment.view.SessionSidebar;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
    private final TextArea messageInput;
    private final Button sendButton;
    private final Label statusLabel;
    private final Label modelLabel;
    private final Button manageKnowledgebaseButton;
    private final Button clearSessionButton;
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
            TextArea messageInput,
            Button sendButton,
            Label statusLabel,
            Label modelLabel,
            Button manageKnowledgebaseButton,
            Button clearSessionButton,
            SessionSidebar sessionSidebar) {
        this.sessionNameLabel = sessionNameLabel;
        this.sessionCreatedLabel = sessionCreatedLabel;
        this.chatContainer = chatContainer;
        this.messageInput = messageInput;
        this.sendButton = sendButton;
        this.statusLabel = statusLabel;
        this.modelLabel = modelLabel;
        this.manageKnowledgebaseButton = manageKnowledgebaseButton;
        this.clearSessionButton = clearSessionButton;
        this.sessionSidebar = sessionSidebar;

        // Set up keyboard shortcut: Ctrl+Enter (Windows/Linux) or Cmd+Enter (Mac) to
        // send
        messageInput.setOnKeyPressed(event -> {
            if ((event.isShortcutDown() || event.isControlDown())
                    && event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                event.consume();
                handleSendMessage();
            }
        });

        // Set up dynamic row count adjustment based on content
        messageInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int lineCount = newValue.split("\n", -1).length;
                int newRowCount = Math.min(Math.max(1, lineCount), 10); // Min 1, Max 10 rows
                messageInput.setPrefRowCount(newRowCount);
            }
        });

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
        clearSessionButton.setDisable(disable);
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

                        AlertHelper.showError(
                                "Indexing Error",
                                "Failed to index knowledgebase",
                                e.getMessage());
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
            clearSessionButton.setVisible(true);
            clearSessionButton.setManaged(true);
            logger.debug("Updated session info: {}", session.getName());
        } else {
            sessionNameLabel.setText("No Session Selected");
            sessionCreatedLabel.setText("");
            modelLabel.setText("");
            manageKnowledgebaseButton.setVisible(false);
            manageKnowledgebaseButton.setManaged(false);
            clearSessionButton.setVisible(false);
            clearSessionButton.setManaged(false);
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
            AlertHelper.showError("Error", "Failed to open knowledgebase management", e.getMessage());
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

        // Check query length
        if (userMessage.length() > Constants.MAX_QUERY_LENGTH) {
            AlertHelper.showWarning("Query Too Long", "Your query exceeds the maximum length",
                    String.format("Please limit your query to %d characters. Current length: %d characters.",
                            Constants.MAX_QUERY_LENGTH, userMessage.length()));
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
        ChatMessageEntry userMessageBox = new ChatMessageEntry(userChatMessage);
        chatContainer.getChildren().add(userMessageBox);

        // Save user message to database
        DatabaseService.getInstance().saveChatMessage(currentSession.getId(), userChatMessage);

        // Create placeholder for AI response
        ChatMessage aiChatMessage = new ChatMessage("...", false);
        ChatMessageEntry aiMessageBox = new ChatMessageEntry(aiChatMessage);
        chatContainer.getChildren().add(aiMessageBox);

        // Disable all controls while processing
        toggleDisabilityOfAllControls(true);
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
                            toggleDisabilityOfAllControls(false);
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
                            toggleDisabilityOfAllControls(false);
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
                    ChatMessageEntry messageBox = new ChatMessageEntry(message);
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
                        .anyMatch(node -> node instanceof ChatMessageEntry);

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
     * Handle clearing the session history
     */
    public void handleClearSession() {
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

            // Clear from RAG service (session history)
            if (ragService != null) {
                ragService.clearHistory();
            }

            // Clear from UI
            chatContainer.getChildren().removeIf(node -> node instanceof ChatMessageEntry);

            logger.info("Session cleared for session: {}", currentSession.getName());
            statusLabel.setText("Session cleared");
        }

    }
}
