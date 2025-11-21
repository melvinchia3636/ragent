package dev.ragent.view;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.service.PreferencesService;
import dev.ragent.util.Icon;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Custom sidebar component for displaying and managing sessions
 */
public class SessionSidebar extends VBox {

    private static final Logger logger = LogManager.getLogger(SessionSidebar.class);

    @FXML
    private VBox sessionListContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Button newSessionButton;
    @FXML
    private Button preferencesButton;

    private ObservableList<Session> sessions = FXCollections.observableArrayList();
    private Session currentSession;
    private Consumer<Session> onSessionSelected;
    private Runnable onSessionChanged;

    public SessionSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dev/ragent/session_sidebar.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            logger.error("Failed to load session sidebar", e);
            throw new RuntimeException("Failed to load session sidebar", e);
        }
    }

    @FXML
    private void initialize() {
        scrollPane.setId("sessionList");

        newSessionButton.setGraphic(
                Icon.load("tabler--edit.svg"));

        preferencesButton.setGraphic(
                Icon.load("tabler--settings-2.svg"));
    }

    /**
     * Set callback for when a session is selected
     * 
     * @param onSessionSelected - the callback to set
     */
    public void setOnSessionSelected(Consumer<Session> onSessionSelected) {
        this.onSessionSelected = onSessionSelected;
    }

    /**
     * Set callback for when sessions are changed (added, renamed, deleted)
     * 
     * @param onSessionChanged - the callback to set
     */
    public void setOnSessionChanged(Runnable onSessionChanged) {
        this.onSessionChanged = onSessionChanged;
    }

    /**
     * Load sessions from the database and display them in the sidebar
     */
    public void loadSessions() {
        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            sessions = FXCollections.observableArrayList();
            sessionListContainer.getChildren().clear();

            Label errorLabel = new Label("Database unavailable");
            errorLabel.setStyle("-fx-text-fill: #ff0000; -fx-font-size: 13px;");
            errorLabel.setMaxWidth(Double.MAX_VALUE);
            errorLabel.setAlignment(Pos.CENTER);
            sessionListContainer.getChildren().add(errorLabel);
            return;
        }

        try {
            sessions = FXCollections.observableArrayList(
                    databaseService.getAllSessions());
        } catch (java.sql.SQLException e) {
            // Database schema mismatch - likely missing column from old database
            AlertHelper.showError(
                    "Database Error",
                    "Database Schema Incompatible",
                    "The database schema is incompatible with this version of the application.\\n\\n" +
                            "Please delete the 'rag_sessions.db' file and restart the application.\\n\\n" +
                            "Error: " + e.getMessage());

            // Exit the application
            Platform.exit();
            System.exit(1);
            return;
        }

        sessionListContainer.getChildren().clear();

        // Create individual SidebarSessionEntry components for each session
        if (sessions.isEmpty()) {
            // Show "no sessions" message
            Label emptyLabel = new Label("No sessions yet.");
            emptyLabel.setStyle("-fx-text-fill: #909090; -fx-font-size: 13px;");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setAlignment(Pos.CENTER);
            sessionListContainer.getChildren().add(emptyLabel);
        } else {
            for (Session session : sessions) {
                boolean isSelected = currentSession != null && currentSession.getId().equals(session.getId());
                SidebarSessionEntry sessionBox = new SidebarSessionEntry(
                        session,
                        isSelected,
                        () -> selectSession(session),
                        this::handleSessionChanged);
                sessionListContainer.getChildren().add(sessionBox);
            }
        }
    }

    /**
     * Handle session changes (rename, delete, etc.) and notify parent controller
     */
    private void handleSessionChanged() {
        logger.debug("handleSessionChanged called");

        // Check if the current session still exists before reloading
        String currentSessionId = currentSession != null ? currentSession.getId() : null;
        logger.debug("Current session ID: {}", currentSessionId);

        loadSessions();

        // If the current session was deleted, clear it
        if (currentSessionId != null) {
            boolean sessionStillExists = sessions.stream()
                    .anyMatch(s -> s.getId().equals(currentSessionId));
            logger.debug("Session still exists: {}", sessionStillExists);
            if (!sessionStillExists) {
                currentSession = null;
            }
        }

        if (onSessionChanged != null) {
            logger.debug("Calling parent onSessionChanged callback");
            onSessionChanged.run();
        } else {
            logger.warn("Parent onSessionChanged is null!");
        }
    }

    @FXML
    private void handleOpenPreferences() {
        PreferencesDialog dialog = new PreferencesDialog((Stage) getScene().getWindow());
        boolean saved = dialog.showAndWaitForResult();

        if (saved) {
            logger.debug("Preference updated");
            PreferencesService prefsService = PreferencesService.getInstance();
            // Ensure scene is set before applying theme
            if (prefsService != null) {
                prefsService.setScene(getScene());
                prefsService.applyTheme();
            }
        }
    }

    /**
     * Select a session and notify listeners
     * 
     * @param session - the session to select
     */
    private void selectSession(Session session) {
        currentSession = session;
        if (onSessionSelected != null) {
            onSessionSelected.accept(session);
        }
        refreshSessionStyling();
    }

    /**
     * Refresh the styling of session boxes to reflect the currently selected
     * session
     */
    private void refreshSessionStyling() {
        for (int i = 0; i < sessionListContainer.getChildren().size(); i++) {
            if (sessionListContainer.getChildren().get(i) instanceof SidebarSessionEntry) {
                SidebarSessionEntry sessionBox = (SidebarSessionEntry) sessionListContainer.getChildren().get(i);
                Session session = sessions.get(i);

                // Apply bold style if this is the selected session
                boolean isSelected = currentSession != null && currentSession.getId().equals(session.getId());
                sessionBox.updateStyling(isSelected);
            }
        }
    }

    /**
     * Handle creating a new session
     */
    @FXML
    private void handleNewSession() {
        NewSessionDialog dialog = new NewSessionDialog((Stage) getScene().getWindow());
        Session newSession = dialog.showAndWaitForSession();

        if (newSession != null) {
            loadSessions();
            selectSession(newSession);
            if (onSessionChanged != null) {
                onSessionChanged.run();
            }
        }
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public void clearCurrentSession() {
        currentSession = null;
        refreshSessionStyling();
    }

    public ObservableList<Session> getSessions() {
        return sessions;
    }
}
