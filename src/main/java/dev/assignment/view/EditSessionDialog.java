package dev.assignment.view;

import dev.assignment.model.Session;
import dev.assignment.service.DatabaseService;
import dev.assignment.util.Constants;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Dialog for editing an existing session.
 */
public class EditSessionDialog {
    private final Alert dialog;
    private final TextField nameField;
    private final ComboBox<String> modelComboBox;
    private final CheckBox queryTransformationCheckBox;
    private final Session session;

    /**
     * Creates a new EditSessionDialog for the specified session.
     *
     * @param session the session to edit
     */
    public EditSessionDialog(Session session) {
        this.session = session;
        this.dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Edit Session");
        dialog.setHeaderText("Edit session settings");

        // Create form fields
        Label sessionNameLabel = new Label("Session Name:");
        nameField = new TextField(session.getName());
        nameField.setPrefWidth(300);

        Label modelLabel = new Label("Model:");
        modelComboBox = new ComboBox<>();
        modelComboBox.getItems().addAll(Constants.AVAILABLE_MODELS);
        modelComboBox.setValue(session.getModel());
        modelComboBox.setPrefWidth(300);

        queryTransformationCheckBox = new CheckBox("Enable Query Transformation");
        queryTransformationCheckBox.setSelected(session.isUseQueryTransformation());
        queryTransformationCheckBox.setTooltip(
                new Tooltip("When enabled, generates multiple query variations for better retrieval coverage"));

        // Create layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                sessionNameLabel,
                nameField,
                modelLabel,
                modelComboBox,
                queryTransformationCheckBox);

        dialog.getDialogPane().setContent(content);

        // Disable OK button if name is empty
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(newValue.trim().isEmpty());
        });
    }

    /**
     * Shows the dialog and waits for user input.
     *
     * @return true if the user clicked OK and the session was updated, false
     *         otherwise
     */
    public boolean showAndWait() {
        return dialog.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .map(response -> updateSession())
                .orElse(false);
    }

    /**
     * Updates the session in the database and in-memory.
     *
     * @return true if the session was updated successfully
     */
    private boolean updateSession() {
        String newName = nameField.getText().trim();
        String newModel = modelComboBox.getValue();
        boolean useQueryTransformation = queryTransformationCheckBox.isSelected();

        System.out.println("[EditSessionDialog] Updating session:");
        System.out.println("  - Name: " + newName);
        System.out.println("  - Model: " + newModel);
        System.out.println("  - useQueryTransformation: " + useQueryTransformation);

        if (!newName.isEmpty()) {
            // Update database - the in-memory session will be refreshed by
            // handleSessionChanged()
            DatabaseService.getInstance().updateSession(session.getId(), newName, newModel, useQueryTransformation);

            System.out.println("[EditSessionDialog] Database updated, session will be refreshed by callback");

            return true;
        }

        return false;
    }
}
