package dev.ragent.view;

import dev.ragent.model.ChatMessage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Custom component for displaying a chat message
 */
public final class ChatMessageEntry extends VBox {

    private final Label messageLabel;
    private Label topLabel;
    private final HBox messageContainer;
    private final boolean isUserMessage;

    public ChatMessageEntry(ChatMessage message) {
        this.messageLabel = new Label(message.getContent());
        this.isUserMessage = message.isUser();

        // Create container for message alignment
        this.messageContainer = new HBox();

        // Set alignment and style class based on message type
        if (message.isUser()) {
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            messageLabel.getStyleClass().addAll("chat-message", "user-message");
        } else {
            messageContainer.setAlignment(Pos.CENTER_LEFT);
            messageLabel.getStyleClass().addAll("chat-message", "ai-message");
        }

        messageLabel.setMaxWidth(500);
        messageLabel.setWrapText(true);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);

        messageContainer.getChildren().add(messageLabel);

        // Add message container first
        getChildren().add(messageContainer);

        // Add sources label if available (for AI messages)
        if (!message.isUser() && message.hasSources()) {
            setTopLabel("Referenced from: " + message.getSources());
        }

        setSpacing(0);
    }

    /**
     * Update the message text (for streaming updates)
     */
    public void updateText(String newText) {
        messageLabel.setText(newText);
    }

    /**
     * Append text to the message (for streaming updates)
     */
    public void appendText(String text) {
        messageLabel.setText(messageLabel.getText() + text);
    }

    /**
     * Set the top label (for sources or progress updates)
     */
    public void setTopLabel(String text) {
        if (topLabel != null) {
            topLabel.setText(text);
        } else if (!isUserMessage) {
            // Create the label if it doesn't exist yet (for streaming responses)
            topLabel = new Label(text);
            topLabel.getStyleClass().add("chat-sources");
            topLabel.setAlignment(Pos.CENTER_LEFT);

            // Add the label before the message container
            getChildren().add(0, topLabel);
        }
    }
}
