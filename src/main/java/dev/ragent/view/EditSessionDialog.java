package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;

import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;

/**
 * Dialog for editing an existing session.
 */
public class EditSessionDialog extends BaseDialog {
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> modelComboBox;
    @FXML
    private CheckBox queryTransformationCheckBox;

    private final Session session;
    private boolean confirmed = false;

    /**
     * Creates a new EditSessionDialog for the specified session.
     *
     * @param session the session to edit
     * @param owner   the owner window
     */
    public EditSessionDialog(Session session, Stage owner) {
        super(owner, "Edit Session", "/dev/ragent/session_dialog.fxml");
        this.session = session;

        nameField.setText(session.getName());

        List<String> availableModels = ModelComboBoxHelper.configureModelComboBox(modelComboBox);
        ModelComboBoxHelper.setModelOrFallback(modelComboBox, session.getModel(), availableModels);

        queryTransformationCheckBox.setSelected(session.isUseQueryTransformation());

        Button okBtn = (Button) stage.getScene().lookup("#okButton");
        okBtn.setText("Save");

        // Disable OK button if name is empty
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            okBtn.setDisable(newValue.trim().isEmpty());
        });
    }

    /**
     * Shows the dialog and waits for user input.
     *
     * @return true if the user clicked OK and the session was updated, false
     *         otherwise
     */
    public boolean showAndWaitForResult() {
        super.showAndWait();
        return confirmed && updateSession();
    }

    @FXML
    private void handleOk() {
        confirmed = true;
        close();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        close();
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
