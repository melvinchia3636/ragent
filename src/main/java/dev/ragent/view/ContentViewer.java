package dev.ragent.view;

import java.io.IOException;

import dev.ragent.model.Resource;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * Dialog for displaying and editing resource content
 */
public class ContentViewer extends BaseDialog {

    @FXML
    private TextArea contentArea;

    @FXML
    private Button editButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private final Resource resource;
    private String originalContent;
    private Runnable onSaveCallback;

    /**
     * Create a new ContentViewer dialog
     * 
     * @param resource The resource to display
     * @param owner    The owner stage
     */
    public ContentViewer(Resource resource, Stage owner) {
        super(owner, "View: " + resource.getFileName(), "/dev/ragent/content_viewer.fxml", true);
        this.resource = resource;
        stage.setWidth(700);
        stage.setHeight(600);
        loadContent();
    }

    /**
     * Set callback to be invoked when content is saved
     * 
     * @param callback The callback to execute after save
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
        contentArea.setEditable(true);
        originalContent = contentArea.getText();
        editButton.setVisible(false);
        editButton.setManaged(false);
        saveButton.setVisible(true);
        saveButton.setManaged(true);
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
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
        ProgressDialog progressDialog = new ProgressDialog(stage);
        progressDialog.show();
        progressDialog.updateProgress(0, 2, "Saving document...");

        // Run save and indexing in background thread
        new Thread(() -> {
            try {
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
                    editButton.setManaged(true);
                    saveButton.setVisible(false);
                    saveButton.setManaged(false);
                    cancelButton.setVisible(false);
                    cancelButton.setManaged(false);
                    contentArea.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressDialog.close();

                    AlertHelper.showError("Error", "Save Failed", "Failed to save the document: " + e.getMessage());

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
        editButton.setManaged(true);
        saveButton.setVisible(false);
        saveButton.setManaged(false);
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
    }

    @FXML
    private void handleClose() {
        close();
    }
}
