package dev.assignment.handler;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.assignment.controller.ResourceManagementController;
import dev.assignment.model.Resource;
import dev.assignment.model.Session;
import dev.assignment.service.APIKeyService;
import dev.assignment.service.RAGService;
import dev.assignment.service.ResourceService;
import dev.assignment.view.AlertHelper;
import dev.assignment.view.ChatAreaMessage;
import dev.assignment.view.ChatMessageEntry;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Handles knowledgebase operations including indexing and management.
 */
public class KnowledgebaseHandler {

    private static final Logger logger = LogManager.getLogger(KnowledgebaseHandler.class);

    private final VBox chatContainer;
    private final Label statusLabel;
    private final SessionStateHandler sessionStateHandler;
    private final ChatHistoryHandler chatHistoryHandler;

    public KnowledgebaseHandler(
            VBox chatContainer,
            Label statusLabel,
            SessionStateHandler sessionStateHandler,
            ChatHistoryHandler chatHistoryHandler) {
        this.chatContainer = chatContainer;
        this.statusLabel = statusLabel;
        this.sessionStateHandler = sessionStateHandler;
        this.chatHistoryHandler = chatHistoryHandler;
    }

    /**
     * Initialize session and handle knowledgebase indexing.
     */
    public void initializeSession() {
        Session currentSession = sessionStateHandler.getCurrentSession();
        if (currentSession == null) {
            logger.warn("Cannot initialize: No session selected");
            return;
        }

        logger.info("========== Initializing Session ==========");
        logger.info("Session: id={}, name='{}'", currentSession.getId(), currentSession.getName());

        // Clear chat
        chatContainer.getChildren().clear();

        APIKeyService apiKeyService = APIKeyService.getInstance();
        if (!apiKeyService.hasApiKey()) {
            sessionStateHandler.setInputControlsDisabled(true);
            logger.warn("API key not available - chat functionality disabled for session: {}",
                    currentSession.getName());
            return;
        }

        ResourceService resourceService = sessionStateHandler.getResourceService();
        if (resourceService == null) {
            return;
        }

        // Check if knowledge base is empty
        List<Resource> resources = resourceService.getAllResources();
        if (resources.isEmpty()) {
            // Show empty knowledge base message
            ChatAreaMessage emptyMessage = new ChatAreaMessage(
                    "Your knowledge base is empty.\n\n" +
                            "Click 'Manage Knowledgebase' to add documents.");
            chatContainer.getChildren().add(emptyMessage);

            // Disable input
            sessionStateHandler.setInputControlsDisabled(true);
            statusLabel.setText("Knowledge base is empty");
            logger.info("Knowledge base empty for session '{}' - {} resources found",
                    currentSession.getName(), resources.size());
            return;
        }

        logger.info("Knowledge base has {} resources, proceeding with indexing", resources.size());

        // Disable inputs before indexing
        sessionStateHandler.setInputControlsDisabled(true);
        statusLabel.setText("Preparing to index...");

        // Show indexing message in chat area
        ChatAreaMessage indexingMessage = new ChatAreaMessage(
                "Indexing knowledgebase...\n\n" +
                        "Please check the bottom left corner for indexing progress.");
        chatContainer.getChildren().add(indexingMessage);

        // Index knowledgebase in background with progress updates
        new Thread(() -> {
            try {
                RAGService ragService = sessionStateHandler.getRagService();
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

                    // Load chat history after indexing
                    chatHistoryHandler.loadChatHistory();

                    logger.info("Knowledgebase indexed successfully");
                });
            } catch (IOException e) {
                logger.error("Error indexing knowledgebase", e);
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(indexingMessage);
                    statusLabel.setText("Error indexing knowledgebase");
                    sessionStateHandler.setInputControlsDisabled(false);

                    AlertHelper.showError(
                            "Indexing Error",
                            "Failed to index knowledgebase",
                            e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Handle opening the knowledgebase management window.
     */
    public void handleManageKnowledgebase() {
        Session currentSession = sessionStateHandler.getCurrentSession();
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
            controller.setResourceService(sessionStateHandler.getResourceService());
            controller.setRagService(sessionStateHandler.getRagService());

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
     * Recheck if knowledge base is empty and update UI accordingly.
     */
    public void recheckKnowledgebaseStatus() {
        Session currentSession = sessionStateHandler.getCurrentSession();
        ResourceService resourceService = sessionStateHandler.getResourceService();

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

                sessionStateHandler.setInputControlsDisabled(true);
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

                sessionStateHandler.setInputControlsDisabled(false);
                statusLabel.setText("Ready");
            }
        });
    }
}
