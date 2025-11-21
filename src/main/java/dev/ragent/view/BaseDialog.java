package dev.ragent.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import dev.ragent.service.PreferencesService;

/**
 * Base class for all dialog windows with common setup logic
 */
public abstract class BaseDialog {
    protected final Stage stage;

    /**
     * Creates a new BaseDialog
     * 
     * @param owner     the owner window
     * @param title     the dialog title
     * @param fxmlPath  the path to the FXML file (relative to resources)
     * @param resizable whether the dialog should be resizable
     */
    protected BaseDialog(Stage owner, String title, String fxmlPath, boolean resizable) {
        this.stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle(title);
        stage.setResizable(resizable);

        loadFXML(fxmlPath);
    }

    /**
     * Creates a new BaseDialog (non-resizable by default)
     * 
     * @param owner    the owner window
     * @param title    the dialog title
     * @param fxmlPath the path to the FXML file (relative to resources)
     */
    protected BaseDialog(Stage owner, String title, String fxmlPath) {
        this(owner, title, fxmlPath, false);
    }

    /**
     * Loads the FXML file and applies styling
     */
    private void loadFXML(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setController(this);
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/dev/ragent/styles/index.css").toExternalForm());

            // Apply theme
            if (PreferencesService.getInstance().isDarkMode()) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load dialog: " + fxmlPath, e);
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

    /**
     * Closes the dialog
     */
    public void close() {
        stage.close();
    }

    /**
     * Gets the stage instance
     */
    protected Stage getStage() {
        return stage;
    }
}
