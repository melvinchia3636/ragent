package dev.ragent.view;

import java.io.IOException;

import dev.ragent.controller.ContentViewerController;
import dev.ragent.model.Resource;
import dev.ragent.service.PreferencesService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Standalone view for displaying and editing resource content
 */
public class ContentViewer {

    private final Stage stage;
    private final ContentViewerController controller;

    /**
     * Create a new ContentViewer window
     * 
     * @param resource The resource to display
     * @param owner    The owner window
     * @throws IOException if the FXML file cannot be loaded
     */
    public ContentViewer(Resource resource, Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/dev/ragent/content_viewer.fxml"));
        Parent root = loader.load();

        controller = loader.getController();
        controller.setResource(resource);

        stage = new Stage();
        stage.setTitle("View: " + resource.getFileName());
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        Scene scene = new Scene(root, 700, 600);

        // Apply theme
        if (PreferencesService.getInstance().isDarkMode()) {
            scene.getRoot().getStyleClass().add("dark-theme");
        }

        stage.setScene(scene);
    }

    /**
     * Set callback to be invoked when content is saved
     * 
     * @param callback The callback to execute after save
     */
    public void setOnSaveCallback(Runnable callback) {
        controller.setOnSaveCallback(callback);
    }

    /**
     * Show the content viewer window
     */
    public void show() {
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    /**
     * Close the content viewer window
     */
    public void close() {
        stage.close();
    }

    /**
     * Get the underlying stage
     * 
     * @return The Stage object
     */
    public Stage getStage() {
        return stage;
    }
}
