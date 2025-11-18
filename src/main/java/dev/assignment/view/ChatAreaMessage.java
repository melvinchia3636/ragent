package dev.assignment.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;

/**
 * Reusable component for displaying centered messages in the chat area
 */
public class ChatAreaMessage extends Label {

    public ChatAreaMessage(String message) {
        super(message);
        setStyle(
                "-fx-text-fill: #909090; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-alignment: center;");
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        setAlignment(Pos.CENTER);
    }
}
