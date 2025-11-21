package dev.ragent.view;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.ragent.service.APIKeyService;
import dev.ragent.util.Constants;
import javafx.scene.control.ComboBox;

/**
 * Utility class for configuring model ComboBoxes with custom cell rendering
 */
public class ModelComboBoxHelper {

    /**
     * Configure a ComboBox for model selection with custom cells and filtered
     * models
     * 
     * @param comboBox the ComboBox to configure
     * @return list of available models
     */
    public static List<String> configureModelComboBox(ComboBox<String> comboBox) {
        // Filter models to only show those with available API keys
        List<String> availableModels = Arrays.stream(Constants.AVAILABLE_MODELS)
                .filter(model -> {
                    String provider = Constants.getProvider(model);
                    return APIKeyService.getInstance().hasApiKey(provider);
                })
                .collect(Collectors.toList());

        // Set custom cell factory for model display
        comboBox.setCellFactory(param -> new ModelListCell());
        comboBox.setButtonCell(new ModelListCell());

        // Add available models to the ComboBox
        comboBox.getItems().addAll(availableModels);

        return availableModels;
    }

    /**
     * Set the default model in the ComboBox, or first available if default not
     * available
     * 
     * @param comboBox        the ComboBox to set the value for
     * @param availableModels the list of available models
     */
    public static void setDefaultModel(ComboBox<String> comboBox, List<String> availableModels) {
        if (!availableModels.isEmpty()) {
            String defaultModel = availableModels.contains(Constants.DEFAULT_MODEL)
                    ? Constants.DEFAULT_MODEL
                    : availableModels.get(0);
            comboBox.setValue(defaultModel);
        }
    }

    /**
     * Set a specific model or fallback to first available
     * 
     * @param comboBox        the ComboBox to set the value for
     * @param model           the preferred model
     * @param availableModels the list of available models
     */
    public static void setModelOrFallback(ComboBox<String> comboBox, String model, List<String> availableModels) {
        if (availableModels.contains(model)) {
            comboBox.setValue(model);
        } else if (!availableModels.isEmpty()) {
            comboBox.setValue(availableModels.get(0));
        }
    }

    private ModelComboBoxHelper() {
        // Prevent instantiation
    }
}
