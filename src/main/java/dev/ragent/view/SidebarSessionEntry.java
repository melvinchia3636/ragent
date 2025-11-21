package dev.ragent.view;

import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.util.Icon;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

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
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(nameLabel, spacer, menuButton);
        updateStyling(isSelected);

        setOnMouseClicked(e -> {
            // Only respond to primary (left) mouse button clicks
            if (e.getButton() == MouseButton.PRIMARY) {
                if (onSessionSelected != null) {
                    onSessionSelected.run();
                }
            }
            // Consume the event to prevent default behavior
            e.consume();
        });

        // Disable focus traversal to prevent focus styling issues
        setFocusTraversable(false);
    }

    private MenuButton createMenuButton() {
        MenuButton menuButton = new MenuButton();
        menuButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
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
        EditSessionDialog dialog = new EditSessionDialog(session, (Stage) getScene().getWindow());

        if (dialog.showAndWaitForResult()) {
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
        DeleteSessionDialog dialog = new DeleteSessionDialog(session, (Stage) getScene().getWindow());

        if (dialog.showAndWaitForResult()) {
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
