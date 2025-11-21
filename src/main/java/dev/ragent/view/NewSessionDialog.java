package dev.ragent.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;

import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.util.Icon;

/**
 * Dialog for creating a new session session
 */
public class NewSessionDialog extends BaseDialog {
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> modelComboBox;
    @FXML
    private Slider temperatureSlider;
    @FXML
    private Label temperatureValueLabel;
    @FXML
    private Slider topKSlider;
    @FXML
    private Label topKValueLabel;
    @FXML
    private CheckBox queryTransformationCheckBox;
    @FXML
    private Button okButton;

    private Session createdSession = null;

    /**
     * Create a new session dialog
     *
     * @param owner the owner window
     */
    public NewSessionDialog(Stage owner) {
        super(owner, "New Session", "/dev/ragent/session_dialog.fxml");

        List<String> availableModels = ModelComboBoxHelper.configureModelComboBox(modelComboBox);
        ModelComboBoxHelper.setDefaultModel(modelComboBox, availableModels);

        queryTransformationCheckBox.setSelected(true);

        // Setup slider listeners before setting values
        temperatureSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            temperatureValueLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        topKSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            topKValueLabel.setText(String.valueOf(newVal.intValue()));
        });

        // Initialize labels with current slider values
        temperatureValueLabel.setText(String.format("%.1f", temperatureSlider.getValue()));
        topKValueLabel.setText(String.valueOf((int) topKSlider.getValue()));

        okButton.setText("Create");
        okButton.setDisable(true);

        // Disable OK button if name is empty
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        });
    }

    @FXML
    private void initialize() {
        okButton.setGraphic(Icon.load("tabler--plus"));
    }

    /**
     * Show the dialog and return the created session if confirmed
     * 
     * @return The created Session, or null if cancelled
     */
    public Session showAndWaitForSession() {
        super.showAndWait();
        return createdSession;
    }

    @FXML
    private void handleOk() {
        createdSession = createSession();
        close();
    }

    @FXML
    private void handleCancel() {
        createdSession = null;
        close();
    }

    /**
     * Create a new session with the form data
     */
    private Session createSession() {
        String name = nameField.getText().trim();
        String model = modelComboBox.getValue();
        boolean useQueryTransformation = queryTransformationCheckBox.isSelected();
        double temperature = temperatureSlider.getValue();
        int topK = (int) topKSlider.getValue();

        if (name.isEmpty()) {
            return null;
        }

        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            AlertHelper.showError(
                    "Database Error",
                    "Cannot Create Session",
                    "The database is unavailable. Please restart the application.");
            return null;
        }

        Session newSession = databaseService.createSession(name);
        newSession.setModel(model);
        newSession.setUseQueryTransformation(useQueryTransformation);
        newSession.setTemperature(temperature);
        newSession.setTopK(topK);
        databaseService.updateSession(newSession.getId(), name, model, useQueryTransformation, temperature, topK);

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
