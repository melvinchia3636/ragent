package dev.assignment.view;

import dev.assignment.model.Session;
import dev.assignment.service.DatabaseService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Custom component for displaying a session in the sidebar
 */
public class SidebarSessionEntry extends HBox {

    private final Session session;
    private final Label nameLabel;
    private final Runnable onSessionChanged;

    public SidebarSessionEntry(
            Session session,
            boolean isSelected,
            Runnable onSessionSelected,
            Runnable onSessionChanged) {
        this.session = session;
        this.onSessionChanged = onSessionChanged;

        setAlignment(Pos.CENTER);
        setStyle("-fx-cursor: hand; -fx-padding: 4 10 0 0;");

        nameLabel = new Label(session.getName());
        MenuButton menuButton = createMenuButton();

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(nameLabel, spacer, menuButton);
        updateStyling(isSelected);

        setOnMouseClicked(e -> {
            if (onSessionSelected != null) {
                onSessionSelected.run();
            }
        });
    }

    private MenuButton createMenuButton() {
        MenuButton menuButton = new MenuButton();
        menuButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        menuButton.setGraphicTextGap(0.0);
        menuButton.setMnemonicParsing(false);
        menuButton.setStyle("-fx-background-color: transparent;");

        try {
            menuButton.getStylesheets().add(
                    getClass().getResource("/dev/assignment/main.css").toExternalForm());
        } catch (Exception e) {
            // CSS not found, continue without it
        }

        menuButton.setPadding(new Insets(0, -4, 0, -4));

        try {
            ImageView icon = new ImageView(new Image(
                    getClass().getResourceAsStream("/dev/assignment/uil--ellipsis-v.png")));
            icon.setFitHeight(16);
            icon.setFitWidth(16);
            icon.setPreserveRatio(true);
            menuButton.setGraphic(icon);
        } catch (Exception e) {
            // Icon not found, continue without it
        }

        MenuItem renameItem = new MenuItem("Edit");
        renameItem.setOnAction(e -> handleEdit());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDelete());

        menuButton.getItems().addAll(renameItem, deleteItem);

        return menuButton;
    }

    private void handleEdit() {
        EditSessionDialog dialog = new EditSessionDialog(session);

        if (dialog.showAndWait()) {
            System.out.println("[SidebarSessionEntry] Edit confirmed, triggering refresh");

            // Notify about the change - this will reload sessions from database
            if (onSessionChanged != null) {
                System.out.println("[SidebarSessionEntry] Calling onSessionChanged callback");
                onSessionChanged.run();
            } else {
                System.out.println("[SidebarSessionEntry] WARNING: onSessionChanged is null!");
            }
        }
    }

    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Session");
        alert.setHeaderText("Delete \"" + session.getName() + "\"?");
        alert.setContentText("This will permanently delete the session and all its knowledgebase files.\n\n" +
                "To confirm, please type the session name below:");
        AlertHelper.showWarning("Delete Session", "Delete \"" + session.getName() + "\"?",
                "This will permanently delete the session and all its knowledgebase files.\n\n" +
                        "To confirm, please type the session name below:");

        // Create a TextField for user input
        TextField confirmationField = new TextField();
        confirmationField.setPromptText("Enter session name");

        // Create a VBox to hold the content and text field
        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("This will permanently delete the session and all its knowledgebase files."),
                new Label("To confirm, please type the session name below:"),
                confirmationField);

        alert.getDialogPane().setContent(content);

        // Disable OK button by default
        alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

        // Enable OK button only when the entered text matches the session name
        confirmationField.textProperty().addListener((observable, oldValue, newValue) -> {
            alert.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(!newValue.trim().equals(session.getName()));
        });

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                DatabaseService databaseService = DatabaseService.getInstance();
                if (databaseService == null) {
                    AlertHelper.showError(
                            "Database Error",
                            "Cannot Delete Session",
                            "The database is unavailable.");
                    return;
                }

                databaseService.deleteSession(session.getId());
                if (onSessionChanged != null) {
                    onSessionChanged.run();
                }
            }
        });
    }

    public void updateStyling(boolean isSelected) {
        if (isSelected) {
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            nameLabel.setStyle("-fx-font-size: 13px;");
        }
    }

    public Session getSession() {
        return session;
    }
}
