package dev.ragent.view;

import dev.ragent.util.Icon;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuButton;

public class SidebarSessionEntryContextMenu extends MenuButton {

    public SidebarSessionEntryContextMenu() {
        super();

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setGraphicTextGap(0.0);
        setMnemonicParsing(false);
        setStyle("-fx-background-color: transparent;");
        setGraphic(Icon.load("tabler--dots-vertical"));
    }

    public void setMenuItems(MenuItemData[] menuItems) {
        getItems().clear();
        for (MenuItemData itemData : menuItems) {
            javafx.scene.control.MenuItem menuItem = new javafx.scene.control.MenuItem(itemData.title());
            menuItem.setGraphic(Icon.load(itemData.iconName()));
            menuItem.setOnAction(e -> itemData.action().run());
            getItems().add(menuItem);
        }
    }

    public record MenuItemData(String title, String iconName, Runnable action, boolean disabled) {
    }
}
