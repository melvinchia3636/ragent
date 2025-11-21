package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Dialog for showing upload progress when sharing chat to Pastebin
 */
public class SharingProgressDialog extends BaseDialog {
    @FXML
    private Label messageLabel;

    /**
     * Creates a new SharingProgressDialog
     *
     * @param owner the owner window
     */
    public SharingProgressDialog(Stage owner) {
        super(owner, "Sharing Chat", "/dev/ragent/sharing_progress_dialog.fxml");
    }

    /**
     * Sets the progress message
     *
     * @param message the message to display
     */
    public void setMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }
}
