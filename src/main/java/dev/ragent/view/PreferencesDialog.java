package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

import dev.ragent.service.PreferencesService;
import dev.ragent.util.Icon;
import javafx.scene.control.Button;

/**
 * Dialog for application preferences
 */
public class PreferencesDialog extends BaseDialog {
    @FXML
    private CheckBox darkModeCheckBox;

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
        super(owner, "Preferences", "/dev/ragent/preferences_dialog.fxml");
        this.prefsService = PreferencesService.getInstance();

        // Load current preferences
        loadPreferences();
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
    public boolean showAndWaitForResult() {
        super.showAndWait();
        return saved;
    }

    @FXML
    private void handleSave() {
        // Save preferences
        prefsService.setDarkMode(darkModeCheckBox.isSelected());
        saved = true;
        close();
    }

    @FXML
    private void handleCancel() {
        saved = false;
        close();
    }
}