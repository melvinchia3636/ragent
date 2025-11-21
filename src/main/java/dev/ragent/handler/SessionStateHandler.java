package dev.ragent.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.ragent.App;
import dev.ragent.model.Session;
import dev.ragent.service.APIKeyService;
import dev.ragent.service.DatabaseService;
import dev.ragent.service.RAGService;
import dev.ragent.service.ResourceService;
import dev.ragent.util.Constants;
import dev.ragent.view.AlertHelper;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;

/**
 * Handles session state management including selection, updates, and UI state.
 */
public class SessionStateHandler {

    private static final Logger logger = LogManager.getLogger(SessionStateHandler.class);

    private final HBox sessionHeader;
    private final Label sessionNameLabel;
    private final Label sessionCreatedLabel;
    private final Label modelLabel;
    private final Button manageKnowledgebaseButton;
    private final Button clearSessionButton;
    private final TextArea messageInput;
    private final Button sendButton;

    private Session currentSession;
    private ResourceService resourceService;
    private RAGService ragService;

    public SessionStateHandler(
            HBox sessionHeader,
            Label sessionNameLabel,
            Label sessionCreatedLabel,
            Label modelLabel,
            Button manageKnowledgebaseButton,
            Button clearSessionButton,
            TextArea messageInput,
            Button sendButton) {
        this.sessionHeader = sessionHeader;
        this.sessionNameLabel = sessionNameLabel;
        this.sessionCreatedLabel = sessionCreatedLabel;
        this.modelLabel = modelLabel;
        this.manageKnowledgebaseButton = manageKnowledgebaseButton;
        this.clearSessionButton = clearSessionButton;
        this.messageInput = messageInput;
        this.sendButton = sendButton;
    }

    /**
     * Get the current session.
     */
    public Session getCurrentSession() {
        return currentSession;
    }

    /**
     * Set the current session.
     * 
     * @param session the session to set
     * @return true if session was set successfully, false if API key is missing
     */
    public boolean setCurrentSession(Session session) {
        if (session != null) {
            logger.info("Setting current session: id={}, name='{}', model={}, queryTransformation={}",
                    session.getId(), session.getName(), session.getModel(), session.isUseQueryTransformation());

            // Check if API key for the session's model provider is available
            String provider = Constants.getProvider(session.getModel());
            if (!APIKeyService.getInstance().hasApiKey(provider)) {
                logger.warn("API key for provider '{}' not available, cannot set session", provider);
                AlertHelper.showError(
                        "API Key Missing",
                        "Cannot Load Session",
                        "The API key for " + provider.toUpperCase() + " is not configured. " +
                                "Please add the " + provider.toUpperCase() + "_API_KEY to your .env file.");
                // Clear session since we can't use it
                this.currentSession = null;
                this.resourceService = null;
                this.ragService = null;
                updateSessionInfoDisplay(null);
                return false;
            }
        } else {
            logger.info("Clearing current session");
        }

        this.currentSession = session;
        if (session != null) {
            this.resourceService = new ResourceService(session.getId());
            logger.debug("Initialized ResourceService for session: {}", session.getId());

            String provider = Constants.getProvider(session.getModel());
            if (APIKeyService.getInstance().hasApiKey(provider)) {
                this.ragService = new RAGService(session.getId(), session.getModel(),
                        session.isUseQueryTransformation());
                logger.info("Initialized RAGService with model={}, queryTransformation={}",
                        session.getModel(), session.isUseQueryTransformation());
            } else {
                logger.warn("API key for provider '{}' not available, RAGService not initialized", provider);
            }
        } else {
            this.resourceService = null;
            this.ragService = null;
            logger.debug("Cleared ResourceService and RAGService");
        }
        return true;
    }

    /**
     * Get the resource service for the current session.
     */
    public ResourceService getResourceService() {
        return resourceService;
    }

    /**
     * Get the RAG service for the current session.
     */
    public RAGService getRagService() {
        return ragService;
    }

    /**
     * Update the RAG service with a new model.
     */
    public void updateRagService(String newModel) {
        if (currentSession != null) {
            logger.info("Updating RAGService: sessionId={}, model={}, queryTransformation={}",
                    currentSession.getId(), newModel, currentSession.isUseQueryTransformation());

            this.ragService = new RAGService(currentSession.getId(), newModel,
                    currentSession.isUseQueryTransformation());

            logger.info("RAGService successfully updated");
        } else {
            logger.warn("Cannot update RAGService: currentSession is null");
        }
    }

    /**
     * Handle session changes (update, delete, etc.)
     */
    public void handleSessionChanged() {
        logger.info("========== Session Changed Event ==========");

        if (currentSession == null) {
            logger.info("No current session, clearing UI display");
            updateSessionInfoDisplay(null);
            return;
        }

        logger.info("Current session: id={}, name='{}'", currentSession.getId(), currentSession.getName());

        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            logger.error("Database unavailable, cannot refresh session");
            return;
        }

        Session updatedSession = databaseService.getSession(currentSession.getId());
        if (updatedSession == null) {
            logger.info("Session deleted from database: id={}, name='{}'",
                    currentSession.getId(), currentSession.getName());
            setCurrentSession(null);
            updateSessionInfoDisplay(null);
            return;
        }

        // Detect changes
        String oldModel = currentSession.getModel();
        String newModel = updatedSession.getModel();
        String oldName = currentSession.getName();
        String newName = updatedSession.getName();
        boolean oldUseQueryTransformation = currentSession.isUseQueryTransformation();
        boolean newUseQueryTransformation = updatedSession.isUseQueryTransformation();

        boolean nameChanged = !oldName.equals(newName);
        boolean modelChanged = !oldModel.equals(newModel);
        boolean queryTransformationChanged = oldUseQueryTransformation != newUseQueryTransformation;

        // Log all changes
        if (nameChanged) {
            logger.info("Session name changed: '{}' -> '{}'", oldName, newName);
        }
        if (modelChanged) {
            logger.info("Model changed: {} -> {}", oldModel, newModel);
        }
        if (queryTransformationChanged) {
            logger.info("Query transformation changed: {} -> {}", oldUseQueryTransformation, newUseQueryTransformation);
        }

        if (!nameChanged && !modelChanged && !queryTransformationChanged) {
            logger.debug("No changes detected in session properties");
        }

        // Update current session reference
        currentSession = updatedSession;
        updateSessionInfoDisplay(currentSession);

        // Reinitialize RAGService if needed
        if ((modelChanged || queryTransformationChanged)) {
            if (!APIKeyService.getInstance().hasApiKey()) {
                logger.warn("API key not available, cannot reinitialize RAGService");
            } else {
                logger.info("RAG configuration changed, reinitializing RAGService");
                updateRagService(newModel);
            }
        }

        logger.info("========== Session Update Complete ==========");
    }

    /**
     * Update session information display.
     */
    public void updateSessionInfoDisplay(Session session) {
        if (session != null) {
            logger.debug("Updating UI display for session: id={}, name='{}'",
                    session.getId(), session.getName());

            sessionNameLabel.setText(session.getName());
            sessionCreatedLabel.setText("Created on " + session.getFormattedCreatedAt());

            String queryTransformationStatus = session.isUseQueryTransformation()
                    ? "enabled"
                    : "disabled";
            modelLabel.setText(session.getModel() + " (query transformation " + queryTransformationStatus + ")");

            sessionHeader.setVisible(true);
            sessionHeader.setManaged(true);
            manageKnowledgebaseButton.setVisible(true);
            manageKnowledgebaseButton.setManaged(true);
            clearSessionButton.setVisible(true);
            clearSessionButton.setManaged(true);

            // Update window title
            Platform.runLater(() -> {
                if (App.getPrimaryStage() != null) {
                    App.getPrimaryStage().setTitle(session.getName() + " - RAGent");
                }
            });

            logger.debug("UI display updated successfully");
        } else {
            logger.debug("Clearing UI display (no session)");

            sessionNameLabel.setText("No Session Selected");
            sessionCreatedLabel.setText("");
            modelLabel.setText("N/A");
            sessionHeader.setVisible(false);
            sessionHeader.setManaged(false);
            manageKnowledgebaseButton.setVisible(false);
            manageKnowledgebaseButton.setManaged(false);
            clearSessionButton.setVisible(false);
            clearSessionButton.setManaged(false);
            setInputControlsDisabled(true);

            // Reset window title
            Platform.runLater(() -> {
                if (App.getPrimaryStage() != null) {
                    App.getPrimaryStage().setTitle("RAGent");
                }
            });

            logger.debug("UI display cleared");
        }
    }

    /**
     * Set the disable state for input controls.
     */
    public void setInputControlsDisabled(boolean disabled) {
        sendButton.setDisable(disabled);
        messageInput.setDisable(disabled);
    }
}
