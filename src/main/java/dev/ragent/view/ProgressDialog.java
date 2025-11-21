package dev.ragent.view;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

/**
 * A dialog window that displays a progress bar for file import operations
 */
public class ProgressDialog extends BaseDialog {
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;
    @FXML
    private Label detailLabel;

    private volatile boolean cancelled = false;
    private Runnable onCancelCallback;

    public ProgressDialog(Stage owner) {
        super(owner, "Importing Resources", "/dev/ragent/progress_dialog.fxml");

        // Handle window close request
        stage.setOnCloseRequest(event -> {
            cancelled = true;
            if (onCancelCallback != null) {
                onCancelCallback.run();
            }
        });
    }

    /**
     * Update the progress
     * 
     * @param current Current progress value
     * @param total   Total progress value
     * @param message Detail message to display
     */
    public void updateProgress(int current, int total, String message) {
        double progress = (double) current / total;
        progressBar.setProgress(progress);
        detailLabel.setText(String.format("Processing file %d of %d: %s", current, total, message));
    }

    /**
     * Set the status message
     */
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Bind the progress bar to a Task
     */
    public void bindToTask(Task<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        detailLabel.textProperty().bind(task.messageProperty());
    }

    /**
     * Check if the dialog has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Set callback to be called when dialog is closed/cancelled
     */
    public void setOnCancel(Runnable callback) {
        this.onCancelCallback = callback;
    }
}
