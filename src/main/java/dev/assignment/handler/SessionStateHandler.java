package dev.assignment.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.assignment.model.Session;
import dev.assignment.service.APIKeyService;
import dev.assignment.service.DatabaseService;
import dev.assignment.service.RAGService;
import dev.assignment.service.ResourceService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

/**
 * Handles session state management including selection, updates, and UI state.
 */
public class SessionStateHandler {

    private static final Logger logger = LogManager.getLogger(SessionStateHandler.class);

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
            Label sessionNameLabel,
            Label sessionCreatedLabel,
            Label modelLabel,
            Button manageKnowledgebaseButton,
            Button clearSessionButton,
            TextArea messageInput,
            Button sendButton) {
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
     */
    public void setCurrentSession(Session session) {
        this.currentSession = session;
        if (session != null) {
            this.resourceService = new ResourceService(session.getId());
            if (APIKeyService.getInstance().hasApiKey()) {
                this.ragService = new RAGService(session.getId(), session.getModel());
            }
        } else {
            this.resourceService = null;
            this.ragService = null;
        }
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
            this.ragService = new RAGService(currentSession.getId(), newModel);
        }
    }

    /**
     * Handle session changes (update, delete, etc.)
     */
    public void handleSessionChanged() {
        logger.info("Session changed event");
        if (currentSession != null) {
            Session updatedSession = DatabaseService.getInstance().getSession(currentSession.getId());
            if (updatedSession != null) {
                String oldModel = currentSession.getModel();
                String newModel = updatedSession.getModel();
                boolean modelChanged = !oldModel.equals(newModel);

                logger.info("Session update - Old model: {}, New model: {}, Changed: {}",
                        oldModel, newModel, modelChanged);

                currentSession = updatedSession;
                updateSessionInfoDisplay(currentSession);
                if (modelChanged && ragService != null) {
                    logger.info("Model changed from {} to {}, reinitializing RAGService", oldModel, newModel);
                    updateRagService(newModel);
                }
            } else {
                logger.info("Current session was deleted, clearing session state");
                setCurrentSession(null);
                updateSessionInfoDisplay(null);
            }
        } else {
            updateSessionInfoDisplay(null);
        }
    }

    /**
     * Update session information display.
     */
    public void updateSessionInfoDisplay(Session session) {
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
     * Set the disable state for input controls.
     */
    public void setInputControlsDisabled(boolean disabled) {
        sendButton.setDisable(disabled);
        messageInput.setDisable(disabled);
    }
}
