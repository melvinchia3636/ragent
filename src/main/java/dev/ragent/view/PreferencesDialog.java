package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import dev.ragent.service.PreferencesService;
import dev.ragent.util.Icon;
import javafx.scene.control.Button;

/**
 * Dialog for application preferences
 */
public class PreferencesDialog {
    @FXML
    private CheckBox darkModeCheckBox;

    private final Stage stage;
    private final PreferencesService prefsService;
    private boolean saved = false;

    @FXML
    private Button saveButton;

    /**
     * Creates a new PreferencesDialog
     *
     * @param owner the owner window
     */
    public PreferencesDialog(Stage owner) {
        this.stage = new Stage();
        this.prefsService = PreferencesService.getInstance();

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Preferences");
        stage.setResizable(false);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dev/ragent/preferences_dialog.fxml"));
            loader.setController(this);
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/dev/ragent/styles/index.css").toExternalForm());

            // Apply theme
            if (prefsService.isDarkMode()) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);

            // Load current preferences
            loadPreferences();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load preferences dialog", e);
        }
    }

    @FXML
    private void initialize() {
        saveButton.setGraphic(Icon.load("tabler--device-floppy.svg"));
    }

    /**
     * Load current preferences
     */
    private void loadPreferences() {
        boolean darkMode = prefsService.isDarkMode();
        darkModeCheckBox.setSelected(darkMode);
    }

    /**
     * Shows the dialog and waits for user input
     *
     * @return true if settings were saved
     */
    public boolean showAndWait() {
        stage.showAndWait();
        return saved;
    }

    @FXML
    private void handleSave() {
        // Save preferences
        prefsService.setDarkMode(darkModeCheckBox.isSelected());
        saved = true;
        stage.close();
    }

    @FXML
    private void handleCancel() {
        saved = false;
        stage.close();
    }
}