package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.util.Constants;
import dev.ragent.util.Icon;

/**
 * Dialog for creating a new session session
 */
public class NewSessionDialog {
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> modelComboBox;
    @FXML
    private CheckBox queryTransformationCheckBox;
    @FXML
    private Button okButton;

    private final Stage stage;
    private Session createdSession = null;

    /**
     * Create a new session dialog
     *
     * @param owner the owner window
     */
    public NewSessionDialog(Stage owner) {
        this.stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("New Session");
        stage.setResizable(false);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dev/ragent/session_dialog.fxml"));
            loader.setController(this);
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/dev/ragent/styles/index.css").toExternalForm());

            // Apply theme
            if (dev.ragent.service.PreferencesService.getInstance().isDarkMode()) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }

            stage.setScene(scene);

            // Initialize fields
            modelComboBox.getItems().addAll(Constants.AVAILABLE_MODELS);
            modelComboBox.setValue(Constants.DEFAULT_MODEL);
            queryTransformationCheckBox.setSelected(true);

            // Configure button for create mode
            okButton.setText("Create");
            okButton.setDisable(true);

            // Disable OK button if name is empty
            nameField.textProperty().addListener((observable, oldValue, newValue) -> {
                okButton.setDisable(newValue.trim().isEmpty());
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load new session dialog", e);
        }
    }

    @FXML
    private void initialize() {
        okButton.setGraphic(Icon.load("tabler--plus.svg"));
    }

    /**
     * Show the dialog and return the created session if confirmed
     * 
     * @return The created Session, or null if cancelled
     */
    public Session showAndWait() {
        stage.showAndWait();
        return createdSession;
    }

    @FXML
    private void handleOk() {
        createdSession = createSession();
        stage.close();
    }

    @FXML
    private void handleCancel() {
        createdSession = null;
        stage.close();
    }

    /**
     * Create a new session with the form data
     */
    private Session createSession() {
        String name = nameField.getText().trim();
        String model = modelComboBox.getValue();
        boolean useQueryTransformation = queryTransformationCheckBox.isSelected();

        if (name.isEmpty()) {
            return null;
        }

        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            dev.ragent.view.AlertHelper.showError(
                    "Database Error",
                    "Cannot Create Session",
                    "The database is unavailable. Please restart the application.");
            return null;
        }

        Session newSession = databaseService.createSession(name);
        newSession.setModel(model);
        newSession.setUseQueryTransformation(useQueryTransformation);
        databaseService.updateSession(newSession.getId(), name, model, useQueryTransformation);

        return newSession;
    }

    /**
     * Set the default session name
     */
    public void setDefaultName(String name) {
        nameField.setText(name);
    }

    /**
     * Set the default model
     */
    public void setDefaultModel(String model) {
        modelComboBox.setValue(model);
    }
}
