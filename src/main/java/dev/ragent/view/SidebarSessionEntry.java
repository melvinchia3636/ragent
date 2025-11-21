package dev.ragent.view;

import dev.ragent.model.Session;
import dev.ragent.service.APIKeyService;
import dev.ragent.service.DatabaseService;
import dev.ragent.util.ChatExporter;
import dev.ragent.util.ChatSharer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
    private final Runnable onSessionChanged;
    private final SidebarSessionEntryContextMenu.MenuItemData[] menuItems = new SidebarSessionEntryContextMenu.MenuItemData[] {
            new SidebarSessionEntryContextMenu.MenuItemData("Edit Session", "tabler--pencil.svg", this::handleEdit,
                    false),
            new SidebarSessionEntryContextMenu.MenuItemData("Export Chat", "tabler--file-export.svg",
                    this::handleExportChat, false),
            new SidebarSessionEntryContextMenu.MenuItemData("Share Chat", "tabler--share.svg", this::handleShareChat,
                    !APIKeyService.getInstance().hasPastebinApiKey()),
            new SidebarSessionEntryContextMenu.MenuItemData("Delete Session", "tabler--trash-x.svg",
                    this::handleDelete, false)
    };

    public SidebarSessionEntry(
            Session session,
            boolean isSelected,
            Runnable onSessionSelected,
            Runnable onSessionChanged) {
        this.session = session;
        this.onSessionChanged = onSessionChanged;

        setAlignment(Pos.CENTER);
        getStyleClass().add("sidebarSessionEntry");

        initializeComponents();

        updateStyling(isSelected);

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && onSessionSelected != null) {
                onSessionSelected.run();
            }
        });
    }

    private void initializeComponents() {
        Label nameLabel = new Label(session.getName());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        SidebarSessionEntryContextMenu menuButton = new SidebarSessionEntryContextMenu();
        menuButton.setMenuItems(menuItems);

        getChildren().addAll(nameLabel, spacer, menuButton);
    }

    private void handleEdit() {
        EditSessionDialog dialog = new EditSessionDialog(session, (Stage) getScene().getWindow());

        if (dialog.showAndWaitForResult()) {
            // Notify about the change - this will reload sessions from database
            if (onSessionChanged != null) {
                onSessionChanged.run();
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

    private void handleExportChat() {
        ChatExporter.exportChat(session, (Stage) getScene().getWindow());
    }

    private void handleShareChat() {
        ChatSharer.shareChat(session, (Stage) getScene().getWindow());
    }

    public void updateStyling(boolean isSelected) {
        if (isSelected) {
            if (!getStyleClass().contains("selected")) {
                getStyleClass().add("selected");
            }
        } else {
            getStyleClass().remove("selected");
        }
    }

    public Session getSession() {
        return session;
    }
}
