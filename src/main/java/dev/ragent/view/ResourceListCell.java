package dev.ragent.view;

import dev.ragent.model.Resource;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Custom ListCell for displaying Resource items
 */
public class ResourceListCell extends ListCell<Resource> {

    private final HBox container;
    private final Label nameLabel;
    private final Label countLabel;
    private final Region spacer;

    public ResourceListCell() {
        super();

        container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);

        nameLabel = new Label();
        nameLabel.getStyleClass().add("resource-list-cell-name");

        spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        countLabel = new Label();
        countLabel.getStyleClass().add("resource-list-cell-count");

        container.getChildren().addAll(nameLabel, spacer, countLabel);
    }

    @Override
    protected void updateItem(Resource resource, boolean empty) {
        super.updateItem(resource, empty);

        if (empty || resource == null) {
            setText(null);
            setGraphic(null);
        } else {
            nameLabel.setText(resource.getFileName());
            countLabel.setText(resource.getFormattedCharacterCount());
            setText(null);
            setGraphic(container);
        }
    }
}
