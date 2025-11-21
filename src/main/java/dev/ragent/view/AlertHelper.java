package dev.ragent.view;

import org.girod.javafx.svgimage.SVGImage;

import dev.ragent.service.PreferencesService;
import dev.ragent.util.Icon;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

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
        applyTheme(alert);

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
        applyTheme(alert);
        alert.showAndWait();
    }

    /**
     * Apply theme styling to alert dialog
     */
    private static void applyTheme(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();

        // Add stylesheet
        dialogPane.getStylesheets().add(
                AlertHelper.class.getResource("/dev/ragent/styles/index.css").toExternalForm());

        // Apply dark theme if enabled
        if (PreferencesService.getInstance().isDarkMode()) {
            dialogPane.getStyleClass().add("dark-theme");
        }

        // Add alert-specific styling
        dialogPane.getStyleClass().add("alert-dialog");

        boolean noHeader = alert.getHeaderText() == null || alert.getHeaderText().isEmpty();

        if (noHeader) {
            dialogPane.getStyleClass().add("no-header");
        }

        // Add alert type class for styling
        switch (alert.getAlertType()) {
            case ERROR -> dialogPane.getStyleClass().add("error");
            case WARNING -> dialogPane.getStyleClass().add("warning");
            case INFORMATION -> dialogPane.getStyleClass().add("information");
            case CONFIRMATION -> dialogPane.getStyleClass().add("confirmation");
            default -> {
            }
        }

        // Apply button styles
        var okButton = dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().removeAll("button");
            okButton.getStyleClass().add("primary-btn");
        }

        var cancelButton = dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().removeAll("button");
            cancelButton.getStyleClass().add("secondary-btn");
        }

        // Set custom icons based on alert type
        setCustomIcon(alert, noHeader);
    }

    /**
     * Set custom SVG icons for alert dialogs
     */
    private static void setCustomIcon(Alert alert, boolean noHeader) {
        String iconName;

        switch (alert.getAlertType()) {
            case ERROR -> iconName = "tabler--square-x";
            case WARNING -> iconName = "tabler--alert-triangle";
            case INFORMATION -> iconName = "tabler--info-circle";
            case CONFIRMATION -> iconName = "tabler--help-hexagon";
            default -> {
                return;
            }
        }

        try {
            SVGImage icon = Icon.load(iconName);
            if (noHeader) {
                icon.setScaleX(1.5);
                icon.setScaleY(1.5);
            }
            // SVGImage handles its own sizing based on the SVG viewBox
            alert.setGraphic(icon);
        } catch (Exception e) {
            // Icon not found, use default
        }
    }
}
