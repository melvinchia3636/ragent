package dev.assignment.view;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A dialog window that displays a progress bar for file import operations
 */
public class ProgressDialog {

    private final Stage stage;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label detailLabel;
    private volatile boolean cancelled = false;
    private Runnable onCancelCallback;

    public ProgressDialog(Stage owner) {
        stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Importing Resources");
        stage.setResizable(false);

        // Create UI components
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setPrefWidth(400);

        statusLabel = new Label("Importing files...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(360);
        progressBar.setPrefHeight(25);

        detailLabel = new Label("");
        detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #606060;");
        detailLabel.setWrapText(true);
        detailLabel.setPrefWidth(360);

        root.getChildren().addAll(statusLabel, progressBar, detailLabel);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Handle window close request
        stage.setOnCloseRequest(event -> {
            cancelled = true;
            if (onCancelCallback != null) {
                onCancelCallback.run();
            }
        });
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
