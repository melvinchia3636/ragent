package dev.assignment.controller;

import java.io.IOException;

import dev.assignment.model.Resource;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * Controller for the Content Viewer window
 */
public class ContentViewerController {

    @FXML
    private TextArea contentArea;

    private Resource resource;

    /**
     * Set the resource to display
     */
    public void setResource(Resource resource) {
        this.resource = resource;
        loadContent();
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
    private void handleClose() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }
}
