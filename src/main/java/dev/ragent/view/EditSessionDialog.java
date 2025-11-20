package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.util.Constants;

/**
 * Dialog for editing an existing session.
 */
public class EditSessionDialog {
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> modelComboBox;
    @FXML
    private CheckBox queryTransformationCheckBox;

    private final Stage stage;
    private final Session session;
    private boolean confirmed = false;

    /**
     * Creates a new EditSessionDialog for the specified session.
     *
     * @param session the session to edit
     * @param owner   the owner window
     */
    public EditSessionDialog(Session session, Stage owner) {
        this.session = session;
        this.stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Edit Session");
        stage.setResizable(false);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dev/ragent/session_dialog.fxml"));
            loader.setController(this);
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/dev/ragent/styles/index.css").toExternalForm());

            // Apply theme
            if (dev.ragent.service.PreferencesService.getInstance().isDarkMode()) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);

            // Initialize fields with session data
            nameField.setText(session.getName());
            modelComboBox.getItems().addAll(Constants.AVAILABLE_MODELS);
            modelComboBox.setValue(session.getModel());
            queryTransformationCheckBox.setSelected(session.isUseQueryTransformation());

            // Configure button for edit mode
            Button okBtn = (Button) scene.lookup("#okButton");
            okBtn.setText("Save");

            // Disable OK button if name is empty
            nameField.textProperty().addListener((observable, oldValue, newValue) -> {
                okBtn.setDisable(newValue.trim().isEmpty());
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load edit session dialog", e);
        }
    }

    /**
     * Shows the dialog and waits for user input.
     *
     * @return true if the user clicked OK and the session was updated, false
     *         otherwise
     */
    public boolean showAndWait() {
        stage.showAndWait();
        return confirmed && updateSession();
    }

    @FXML
    private void handleOk() {
        confirmed = true;
        stage.close();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        stage.close();
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
