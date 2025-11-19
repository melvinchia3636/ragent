package dev.assignment.view;

import dev.assignment.model.Session;
import dev.assignment.service.DatabaseService;
import dev.assignment.util.Constants;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/**
 * Dialog for creating a new session session
 */
public class NewSessionDialog {

    private final Alert dialog;
    private final TextField nameField;
    private final ComboBox<String> modelComboBox;
    private final CheckBox queryTransformationCheckBox;

    /**
     * Create a new session dialog
     */
    public NewSessionDialog() {
        dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("New Session");
        dialog.setHeaderText("Create a new session");

        // Create form fields
        Label nameLabel = new Label("Session Name:");
        nameField = new TextField();
        nameField.setPrefWidth(300);

        Label modelLabel = new Label("Model:");
        modelComboBox = new ComboBox<>();
        modelComboBox.getItems().addAll(Constants.AVAILABLE_MODELS);
        modelComboBox.setValue(Constants.DEFAULT_MODEL);
        modelComboBox.setPrefWidth(300);

        queryTransformationCheckBox = new CheckBox("Enable Query Transformation");
        queryTransformationCheckBox.setSelected(true); // Enabled by default
        queryTransformationCheckBox.setTooltip(
                new Tooltip("When enabled, generates multiple query variations for better retrieval coverage"));

        // Create layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                nameLabel,
                nameField,
                modelLabel,
                modelComboBox,
                queryTransformationCheckBox);

        dialog.getDialogPane().setContent(content);

        // Disable OK button if name is empty
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(newValue.trim().isEmpty());
        });
    }

    /**
     * Show the dialog and return the created session if confirmed
     * 
     * @return The created Session, or null if cancelled
     */
    public Session showAndWait() {
        return dialog.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .map(response -> createSession())
                .orElse(null);
    }

    /**
     * Create a new session with the form data
     */
    private Session createSession() {
        String name = nameField.getText().trim();
        String model = modelComboBox.getValue();
        boolean useQueryTransformation = queryTransformationCheckBox.isSelected();

        if (name.isEmpty()) {
            return null;
        }

        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            dev.assignment.view.AlertHelper.showError(
                    "Database Error",
                    "Cannot Create Session",
                    "The database is unavailable. Please restart the application.");
            return null;
        }

        Session newSession = databaseService.createSession(name);
        newSession.setModel(model);
        newSession.setUseQueryTransformation(useQueryTransformation);
        databaseService.updateSession(newSession.getId(), name, model, useQueryTransformation);

        return newSession;
    }

    /**
     * Set the default session name
     */
    public void setDefaultName(String name) {
        nameField.setText(name);
    }

    /**
     * Set the default model
     */
    public void setDefaultModel(String model) {
        modelComboBox.setValue(model);
    }
}
