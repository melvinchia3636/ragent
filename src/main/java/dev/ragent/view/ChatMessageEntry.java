package dev.ragent.view;

import dev.ragent.model.ChatMessage;
import dev.ragent.util.Icon;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Custom component for displaying a chat message
 */
public final class ChatMessageEntry extends VBox {

    private final Label messageLabel;
    private Label topLabel;
    private HBox messageContainer;
    private HBox buttonsContainer;
    private final boolean isUserMessage;
    private final ChatMessage message;
    private Runnable onRegenerateCallback;

    public ChatMessageEntry(ChatMessage message) {
        this(message, null);
    }

    public ChatMessageEntry(ChatMessage message, Runnable onRegenerateCallback) {
        this.message = message;
        this.onRegenerateCallback = onRegenerateCallback;
        this.messageLabel = new Label(message.getContent());
        this.isUserMessage = message.isUser();

        setupMessageContainer();
        setupActionButtons();

        setupMessageLayout();

        setSpacing(0);
    }

    private HBox setupMessageContainer() {
        messageContainer = new HBox();

        messageContainer.setSpacing(12);

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

        return messageContainer;
    }

    private void setupActionButtons() {
        ChatMessageEntryActionButtons.ActionButtonData[] actionButtonsData = new ChatMessageEntryActionButtons.ActionButtonData[] {
                new ChatMessageEntryActionButtons.ActionButtonData(
                        "tabler--info-circle-small",
                        e -> this.handleShowDetails(),
                        !message.isUser()),
                new ChatMessageEntryActionButtons.ActionButtonData(
                        "tabler--copy",
                        e -> this.handleCopy(e),
                        true),
                new ChatMessageEntryActionButtons.ActionButtonData(
                        "tabler--refresh",
                        e -> this.handleRegenerate(),
                        !message.isUser()),
        };

        ChatMessageEntryActionButtons actionButtons = new ChatMessageEntryActionButtons();
        actionButtons.addActionButtons(actionButtonsData);

        buttonsContainer = actionButtons;
    }

    private void handleCopy(ActionEvent event) {
        Button copyButton = (Button) event.getSource();

        ClipboardContent content = new ClipboardContent();
        content.putString(messageLabel.getText());
        Clipboard.getSystemClipboard().setContent(content);

        copyButton.setGraphic(Icon.load("tabler--check"));

        new Thread(() -> {
            try {
                Thread.sleep(1000); // Show the check icon for 1 second
            } catch (InterruptedException ignored) {
            }

            Platform.runLater(() -> {
                copyButton.setGraphic(Icon.load("tabler--copy"));
            });
        }).start();
    }

    private void handleRegenerate() {
        if (onRegenerateCallback != null) {
            boolean confirmed = AlertHelper.showConfirm(
                    "Regenerate Response",
                    "Are you sure you want to regenerate this response?",
                    "All chat history after this message will be removed. This action cannot be undone.");
            if (confirmed) {
                onRegenerateCallback.run();
            }
        } else {
            AlertHelper.showError("Error", "Cannot regenerate this message. Regeneration callback not set.");
        }
    }

    private void handleShowDetails() {
        // Get the parent stage
        javafx.stage.Stage owner = (javafx.stage.Stage) getScene().getWindow();

        // Create and show the message details dialog
        MessageDetailsDialog dialog = new MessageDetailsDialog(owner, message);
        dialog.showAndWait();
    }

    private void setupMessageLayout() {
        messageContainer.getChildren().add(messageLabel);

        if (message.isUser()) {
            messageContainer.getChildren().add(0, buttonsContainer);
        } else {
            messageContainer.getChildren().add(buttonsContainer);
        }

        messageContainer.setOnMouseEntered(e -> {
            if (!buttonsContainer.getStyleClass().contains("hovered")) {
                buttonsContainer.getStyleClass().add("hovered");
            }
        });

        messageContainer.setOnMouseExited(e -> {
            buttonsContainer.getStyleClass().remove("hovered");
        });

        // Add message container first
        getChildren().add(messageContainer);

        // Add sources label if available (for AI messages)
        if (!message.isUser() && message.hasSources()) {
            setTopLabel("Referenced from: " + message.getSources());
        }
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

    /**
     * Get the chat message
     */
    public ChatMessage getMessage() {
        return message;
    }

    /**
     * Set the regeneration callback (for lazy initialization)
     */
    public void setOnRegenerateCallback(Runnable callback) {
        this.onRegenerateCallback = callback;
    }

    /**
     * Enable or disable the buttons container
     */
    public void setButtonsEnabled(boolean enabled) {
        buttonsContainer.setDisable(!enabled);
    }
}
