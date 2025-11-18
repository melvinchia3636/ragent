package dev.assignment.view;

import dev.assignment.model.ChatMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Custom component for displaying a chat message
 */
public class ChatMessageBox extends VBox {

    private final Label messageLabel;
    private final Label sourcesLabel;

    public ChatMessageBox(ChatMessage message) {
        this.messageLabel = new Label(message.getContent());

        // Create container for message alignment
        HBox messageContainer = new HBox();

        // Set alignment based on message type
        if (message.isUser()) {
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            messageLabel.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 4; " +
                            "-fx-border-color: lightgrey; " +
                            "-fx-border-radius: 4;");
        } else {
            messageContainer.setAlignment(Pos.CENTER_LEFT);
            messageLabel.setStyle(
                    "-fx-background-color: lightgrey; " +
                            "-fx-background-radius: 4; " +
                            "-fx-border-color: grey; " +
                            "-fx-border-radius: 4;");
        }

        messageLabel.setPadding(new Insets(10, 10, 10, 10));
        messageLabel.setMaxWidth(500);
        messageLabel.setWrapText(true);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);

        messageContainer.getChildren().add(messageLabel);

        // Add sources label if available (for AI messages)
        if (!message.isUser() && message.hasSources()) {
            this.sourcesLabel = new Label("Referenced from: " + message.getSources());
            sourcesLabel.setStyle("-fx-text-fill: #909090; -fx-font-size: 11px;");
            sourcesLabel.setMaxWidth(Double.MAX_VALUE);
            sourcesLabel.setAlignment(Pos.CENTER_LEFT);
            sourcesLabel.setPadding(new Insets(2, 0, 5, 0));
            sourcesLabel.setWrapText(true);
            sourcesLabel.setMaxWidth(500);
            sourcesLabel.setMinHeight(Region.USE_PREF_SIZE);

            getChildren().addAll(sourcesLabel, messageContainer);
        } else {
            this.sourcesLabel = null;
            getChildren().add(messageContainer);
        }

        setSpacing(0);
    }
}
