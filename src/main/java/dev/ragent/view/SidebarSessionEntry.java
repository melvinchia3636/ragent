package dev.ragent.view;

import org.girod.javafx.svgimage.SVGImage;

import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.util.Icon;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Custom component for displaying a session in the sidebar
 */
public final class SidebarSessionEntry extends HBox {

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
        getStyleClass().add("sidebarSessionEntry");

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

        menuButton.setGraphic(
                Icon.load("tabler--dots-vertical.svg"));

        MenuItem renameItem = new MenuItem("Edit");
        renameItem.setGraphic(Icon.load("tabler--pencil.svg"));
        renameItem.setOnAction(e -> handleEdit());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.getStyleClass().add("dangerous");
        deleteItem.setGraphic(Icon.load("tabler--trash-x.svg"));
        deleteItem.setOnAction(e -> handleDelete());

        menuButton.getItems().addAll(renameItem, deleteItem);

        return menuButton;
    }

    private void handleEdit() {
        EditSessionDialog dialog = new EditSessionDialog(session, (javafx.stage.Stage) getScene().getWindow());

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
            getStyleClass().add("selected");
        } else {
            getStyleClass().remove("selected");
        }
    }

    public Session getSession() {
        return session;
    }
}
