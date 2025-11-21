package dev.ragent.view;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Visual test application for AlertHelper
 * Run this as a JavaFX application to see all alert types
 */
public class AlertHelperVisualTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Show Error Alert with header
        AlertHelper.showError("Test Error", "Error Header", "This is a test error message.");

        // Show Warning Alert with header
        AlertHelper.showWarning("Test Warning", "Warning Header", "This is a test warning message.");

        // Show Info Alert with header
        AlertHelper.showInfo("Test Information", "Info Header", "This is a test information message.");

        // Show Confirm Alert
        boolean result = AlertHelper.showConfirm("Test Confirmation", "Confirm Action",
                "Do you want to proceed with this test?");
        System.out.println("User clicked: " + (result ? "OK" : "Cancel"));

        // Show Error Alert without header
        AlertHelper.showError("Simple Error", "This is an error without a header.");

        // Show Warning Alert without header
        AlertHelper.showWarning("Simple Warning", "This is a warning without a header.");

        // Show Info Alert without header
        AlertHelper.showInfo("Simple Info", "This is information without a header.");

        // Exit after all alerts
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
