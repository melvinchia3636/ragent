package dev.assignment.controller;

import java.io.IOException;

import dev.assignment.model.Resource;
import dev.assignment.view.AlertHelper;
import dev.assignment.view.ProgressDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * Controller for the Content Viewer window
 */
public class ContentViewerController {

    @FXML
    private TextArea contentArea;

    @FXML
    private Button editButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Resource resource;
    private String originalContent;
    private Runnable onSaveCallback;

    /**
     * Set the resource to display
     */
    public void setResource(Resource resource) {
        this.resource = resource;
        loadContent();
    }

    /**
     * Set callback to be called when content is saved
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void loadContent() {
        try {
            if (resource != null && resource.exists()) {
                String content = resource.getContent();
                contentArea.setText(content);
                contentArea.setEditable(false);
            } else {
                contentArea.setText("File not found: " + (resource != null ? resource.getFileName() : "null"));
            }
        } catch (IOException e) {
            contentArea.setText("Error loading file: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        // Enable editing mode
        contentArea.setEditable(true);
        originalContent = contentArea.getText();

        // Toggle button visibility
        editButton.setVisible(false);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);

        // Focus on text area
        contentArea.requestFocus();
    }

    @FXML
    private void handleSave() {
        String newContent = contentArea.getText();

        // Disable controls during save and indexing
        editButton.setDisable(true);
        saveButton.setDisable(true);
        cancelButton.setDisable(true);
        contentArea.setEditable(false);

        // Create and show progress dialog
        Stage ownerStage = (Stage) contentArea.getScene().getWindow();
        ProgressDialog progressDialog = new ProgressDialog(ownerStage);
        progressDialog.show();
        progressDialog.updateProgress(0, 2, "Saving document...");

        // Run save and indexing in background thread
        new Thread(() -> {
            try {
                // Save the content
                resource.saveContent(newContent);

                Platform.runLater(() -> {
                    progressDialog.updateProgress(1, 2, "Re-indexing document...");
                });

                // Notify that content was saved (triggers re-indexing)
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }

                Platform.runLater(() -> {
                    progressDialog.updateProgress(2, 2, "Complete");
                    progressDialog.close();

                    AlertHelper.showInfo("Success", "Content Saved",
                            "The document has been saved and re-indexed successfully.");

                    // Exit edit mode
                    contentArea.setEditable(false);
                    editButton.setVisible(true);
                    saveButton.setVisible(false);
                    cancelButton.setVisible(false);

                    // Re-enable controls
                    editButton.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressDialog.close();

                    AlertHelper.showError("Error", "Save Failed", "Failed to save the document: " + e.getMessage());

                    // Re-enable controls
                    editButton.setDisable(false);
                    saveButton.setDisable(false);
                    cancelButton.setDisable(false);
                    contentArea.setEditable(true);
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        // Restore original content
        contentArea.setText(originalContent);

        // Exit edit mode
        contentArea.setEditable(false);
        editButton.setVisible(true);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }
}
