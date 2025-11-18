package dev.assignment.view;

import java.util.function.Consumer;

import dev.assignment.model.Session;
import dev.assignment.service.DatabaseService;
import dev.assignment.util.Constants;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Custom sidebar component for displaying and managing sessions
 */
public class SessionSidebar extends VBox {

    private final VBox sessionListContainer;
    private final ScrollPane scrollPane;
    private final Button newConversationButton;
    private ObservableList<Session> sessions = FXCollections.observableArrayList();
    private Session currentSession;
    private Consumer<Session> onSessionSelected;
    private Runnable onSessionChanged;

    public SessionSidebar() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(20, 0, 20, 20));
        setSpacing(0);

        // Create scroll pane with session list
        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(-1.0);
        scrollPane.setPrefWidth(-1.0);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        // Create session list container
        sessionListContainer = new VBox();
        sessionListContainer.setSpacing(6.0);
        scrollPane.setContent(sessionListContainer);

        // Add margin to scroll pane
        VBox.setMargin(scrollPane, new Insets(0, 0, 20, 0));

        // Create "New Conversation" button
        newConversationButton = new Button("New Conversation");
        newConversationButton.setMaxWidth(Double.MAX_VALUE);
        newConversationButton.setMnemonicParsing(false);
        newConversationButton.setOnAction(e -> handleNewConversation());
        VBox.setMargin(newConversationButton, new Insets(0, 20, 0, 0));

        // Add icon to button
        try {
            ImageView icon = new ImageView(new Image(
                    getClass().getResourceAsStream("/dev/assignment/uil--comment-plus.png")));
            icon.setFitHeight(150.0);
            icon.setFitWidth(18.0);
            icon.setPreserveRatio(true);
            newConversationButton.setGraphic(icon);
        } catch (Exception e) {
            // Icon not found, continue without it
        }

        getChildren().addAll(scrollPane, newConversationButton);
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
        sessions = FXCollections.observableArrayList(
                DatabaseService.getInstance().getAllSessions());

        sessionListContainer.getChildren().clear();

        // Create individual SessionBox components for each session
        if (sessions.isEmpty()) {
            // Show "no conversations" message
            Label emptyLabel = new Label("No conversations yet.");
            emptyLabel.setStyle("-fx-text-fill: #909090; -fx-font-size: 13px;");
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setAlignment(Pos.CENTER);
            sessionListContainer.getChildren().add(emptyLabel);
        } else {
            for (Session session : sessions) {
                boolean isSelected = currentSession != null && currentSession.getId().equals(session.getId());
                SessionBox sessionBox = new SessionBox(
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
        // Check if the current session still exists before reloading
        String currentSessionId = currentSession != null ? currentSession.getId() : null;

        loadSessions();

        // If the current session was deleted, clear it
        if (currentSessionId != null) {
            boolean sessionStillExists = sessions.stream()
                    .anyMatch(s -> s.getId().equals(currentSessionId));
            if (!sessionStillExists) {
                currentSession = null;
            }
        }

        if (onSessionChanged != null) {
            onSessionChanged.run();
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
            if (sessionListContainer.getChildren().get(i) instanceof SessionBox) {
                SessionBox sessionBox = (SessionBox) sessionListContainer.getChildren().get(i);
                Session session = sessions.get(i);

                // Apply bold style if this is the selected session
                boolean isSelected = currentSession != null && currentSession.getId().equals(session.getId());
                sessionBox.updateStyling(isSelected);
            }
        }
    }

    /**
     * Handle creating a new conversation
     */
    private void handleNewConversation() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("New Conversation");
        dialog.setHeaderText("Create a new conversation");

        // Create form fields
        Label nameLabel = new Label("Conversation Name:");
        TextField nameField = new TextField();
        nameField.setPrefWidth(300);

        Label modelLabel = new Label("Model:");
        ComboBox<String> modelComboBox = new ComboBox<>();
        modelComboBox.getItems().addAll(Constants.AVAILABLE_MODELS);
        modelComboBox.setValue(Constants.DEFAULT_MODEL);
        modelComboBox.setPrefWidth(300);

        // Create layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                nameLabel,
                nameField,
                modelLabel,
                modelComboBox);

        dialog.getDialogPane().setContent(content);

        // Disable OK button if name is empty
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(newValue.trim().isEmpty());
        });

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String model = modelComboBox.getValue();

                if (!name.isEmpty()) {
                    Session newSession = DatabaseService.getInstance().createSession(name);
                    newSession.setModel(model);
                    DatabaseService.getInstance().updateSession(newSession.getId(), name, model);
                    loadSessions();
                    selectSession(newSession);
                    if (onSessionChanged != null) {
                        onSessionChanged.run();
                    }
                }
            }
        });
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
