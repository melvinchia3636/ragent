package dev.ragent.view;

import dev.ragent.model.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Dialog for confirming session deletion.
 */
public class DeleteSessionDialog extends BaseDialog {
    @FXML
    private Label headerLabel;
    @FXML
    private TextField confirmationField;
    @FXML
    private Button okButton;

    private final Session session;
    private boolean confirmed = false;

    /**
     * Creates a new DeleteSessionDialog for the specified session.
     *
     * @param session the session to delete
     * @param owner   the owner window
     */
    public DeleteSessionDialog(Session session, Stage owner) {
        super(owner, "Delete Session", "/dev/ragent/delete_session_dialog.fxml");
        this.session = session;

        // Set header text
        headerLabel.setText("Delete \"" + session.getName() + "\"?");

        // Set prompt text
        confirmationField.setPromptText("Enter session name");

        // Disable OK button by default
        okButton.setDisable(true);

        // Enable OK button only when the entered text matches the session name
        confirmationField.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(!newValue.trim().equals(session.getName()));
        });
    }

    /**
     * Shows the dialog and waits for user input.
     *
     * @return true if the user confirmed the deletion, false otherwise
     */
    public boolean showAndWaitForResult() {
        super.showAndWait();
        return confirmed;
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
}
