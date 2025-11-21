package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Dialog for showing successful share result with Pastebin URL
 */
public class ShareSuccessDialog extends BaseDialog {
    private static final Logger logger = LogManager.getLogger(ShareSuccessDialog.class);

    @FXML
    private TextArea urlTextArea;

    private final String pastebinUrl;

    /**
     * Creates a new ShareSuccessDialog
     *
     * @param pastebinUrl the Pastebin URL to display
     * @param owner       the owner window
     */
    public ShareSuccessDialog(String pastebinUrl, Stage owner) {
        super(owner, "Share Successful", "/dev/ragent/share_success_dialog.fxml");
        this.pastebinUrl = pastebinUrl;

        // Set the URL in the text area
        urlTextArea.setText(pastebinUrl);
        urlTextArea.setEditable(false);
        urlTextArea.setWrapText(true);
    }

    @FXML
    private void handleCopyUrl() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(pastebinUrl);
        clipboard.setContent(clipboardContent);

        logger.info("Pastebin URL copied to clipboard");
        close();
    }

    @FXML
    private void handleOk() {
        close();
    }
}
