package dev.ragent.view;

import java.io.IOException;

import dev.ragent.controller.ResourceManagementController;
import dev.ragent.service.PreferencesService;
import dev.ragent.service.RAGService;
import dev.ragent.service.ResourceService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Dialog for managing knowledgebase resources
 */
public class KnowledgebaseManagementDialog {
    private final Stage stage;
    private final ResourceManagementController controller;

    /**
     * Creates a new KnowledgebaseManagementDialog
     *
     * @param owner              the owner window
     * @param sessionName        the name of the current session
     * @param resourceService    the resource service
     * @param ragService         the RAG service
     * @param onResourcesChanged callback when resources change
     */
    public KnowledgebaseManagementDialog(
            Stage owner,
            String sessionName,
            ResourceService resourceService,
            RAGService ragService,
            Runnable onResourcesChanged) {
        this.stage = new Stage();

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Manage Knowledgebase - " + sessionName);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dev/ragent/manage_resources.fxml"));
            Parent root = loader.load();

            // Get controller and configure it
            controller = loader.getController();
            controller.setResourceService(resourceService);
            controller.setRagService(ragService);
            controller.setOnResourcesChangedCallback(onResourcesChanged);

            Scene scene = new Scene(root, 700, 600);
            scene.getStylesheets().add(getClass().getResource("/dev/ragent/styles/index.css").toExternalForm());

            // Apply theme
            if (PreferencesService.getInstance().isDarkMode()) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load knowledgebase management dialog", e);
        }
    }

    /**
     * Shows the dialog
     */
    public void show() {
        stage.show();
    }

    /**
     * Shows the dialog and waits for it to close
     */
    public void showAndWait() {
        stage.showAndWait();
    }
}
