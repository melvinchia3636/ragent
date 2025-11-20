package dev.ragent.view;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * A dialog window that displays a progress bar for file import operations
 */
public class ProgressDialog {
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;
    @FXML
    private Label detailLabel;

    private final Stage stage;
    private volatile boolean cancelled = false;
    private Runnable onCancelCallback;

    public ProgressDialog(Stage owner) {
        this.stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Importing Resources");
        stage.setResizable(false);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dev/ragent/progress_dialog.fxml"));
            loader.setController(this);
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/dev/ragent/styles/index.css").toExternalForm());

            // Apply theme
            if (dev.ragent.service.PreferencesService.getInstance().isDarkMode()) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);

            // Handle window close request
            stage.setOnCloseRequest(event -> {
                cancelled = true;
                if (onCancelCallback != null) {
                    onCancelCallback.run();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load progress dialog", e);
        }
    }

    /**
     * Show the progress dialog
     */
    public void show() {
        stage.show();
    }

    /**
     * Close the progress dialog
     */
    public void close() {
        stage.close();
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
