package dev.assignment.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Helper class for showing standardized alert dialogs
 */
public class AlertHelper {

    /**
     * Show an error alert
     */
    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, null, message);
    }

    /**
     * Show an error alert with header
     */
    public static void showError(String title, String header, String content) {
        showAlert(Alert.AlertType.ERROR, title, header, content);
    }

    /**
     * Show a warning alert
     */
    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, null, message);
    }

    /**
     * Show a warning alert with header
     */
    public static void showWarning(String title, String header, String content) {
        showAlert(Alert.AlertType.WARNING, title, header, content);
    }

    /**
     * Show an information alert
     */
    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, null, message);
    }

    /**
     * Show an information alert with header
     */
    public static void showInfo(String title, String header, String content) {
        showAlert(Alert.AlertType.INFORMATION, title, header, content);
    }

    /**
     * Show a confirmation dialog and return user's choice
     * 
     * @return true if user clicked OK, false otherwise
     */
    public static boolean showConfirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Show a generic alert dialog
     */
    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
