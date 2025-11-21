package dev.ragent.view;

import dev.ragent.util.Constants;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Custom ListCell for displaying model options with styled provider and model
 * name
 */
public class ModelListCell extends ListCell<String> {

    private final HBox container;
    private final Label providerLabel;
    private final Label modelNameLabel;

    public ModelListCell() {
        container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);

        providerLabel = new Label();
        providerLabel.getStyleClass().add("model-provider-label");

        modelNameLabel = new Label();
        modelNameLabel.getStyleClass().add("model-name-label");
        HBox.setHgrow(modelNameLabel, Priority.ALWAYS);

        container.getChildren().addAll(providerLabel, modelNameLabel);
    }

    @Override
    protected void updateItem(String model, boolean empty) {
        super.updateItem(model, empty);

        if (empty || model == null) {
            setText(null);
            setGraphic(null);
        } else {
            String provider = Constants.getProvider(model);
            String modelName = Constants.getModelName(model);

            providerLabel.setText(provider.toUpperCase());
            modelNameLabel.setText(modelName);

            setText(null);
            setGraphic(container);
        }
    }
}
