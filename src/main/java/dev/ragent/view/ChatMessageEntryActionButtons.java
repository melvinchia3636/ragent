package dev.ragent.view;

import java.util.function.Consumer;

import dev.ragent.util.Icon;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;

public class ChatMessageEntryActionButtons extends HBox {
    public ChatMessageEntryActionButtons() {
        getStyleClass().add("chat-message-buttons");
        setSpacing(4);
        setAlignment(Pos.CENTER);
    }

    public void addActionButtons(ActionButtonData... buttonsData) {
        for (ActionButtonData buttonData : buttonsData) {
            if (buttonData.isShown()) {
                Button button = new Button();
                button.getStyleClass().add("icon-button");
                button.setGraphic(Icon.load(buttonData.iconName()));
                button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                button.setOnAction(e -> buttonData.onClick().accept(e));
                getChildren().add(button);
            }
        }
    }

    public record ActionButtonData(String iconName, Consumer<ActionEvent> onClick, boolean isShown) {
    }
}
