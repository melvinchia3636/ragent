package dev.assignment;

import java.io.IOException;

import dev.assignment.controller.ResourceManagementController;
import dev.assignment.model.Session;
import dev.assignment.service.DatabaseService;
import dev.assignment.service.ResourceService;
import dev.assignment.view.SessionSidebar;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private SessionSidebar sessionSidebar;

    @FXML
    private Label sessionNameLabel;

    @FXML
    private Label sessionCreatedLabel;

    private ResourceService resourceService;

    @FXML
    private void initialize() {
        // Initialize database
        DatabaseService.getInstance();

        // Set up sidebar callbacks
        sessionSidebar.setOnSessionSelected(this::handleSessionSelected);
        sessionSidebar.setOnSessionChanged(this::handleSessionChanged);

        // Load sessions
        sessionSidebar.loadSessions();
    }

    private void handleSessionSelected(Session session) {
        resourceService = new ResourceService(session.getId());
        updateSessionInfo(session);
    }

    private void handleSessionChanged() {
        // Reload session info if current session still exists
        Session currentSession = sessionSidebar.getCurrentSession();
        if (currentSession != null) {
            updateSessionInfo(currentSession);
        } else {
            updateSessionInfo(null);
        }
    }

    private void updateSessionInfo(Session session) {
        if (session != null) {
            sessionNameLabel.setText(session.getName());
            sessionCreatedLabel.setText("Created on " + session.getFormattedCreatedAt());
        } else {
            sessionNameLabel.setText("No Session Selected");
            sessionCreatedLabel.setText("");
        }
    }

    @FXML
    private void handleManageKnowledgebase() {
        Session currentSession = sessionSidebar.getCurrentSession();

        if (currentSession == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Session Selected");
            alert.setHeaderText("Please select or create a session first");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("manage_resources.fxml"));
            Parent root = loader.load();

            // Pass resource service to the controller
            ResourceManagementController controller = loader.getController();
            controller.setResourceService(resourceService);

            Stage stage = new Stage();
            stage.setTitle("Manage Knowledgebase - " + currentSession.getName());
            stage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);

            stage.centerOnScreen();

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
