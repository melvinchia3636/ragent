package dev.ragent.service;

import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.scene.Scene;

/**
 * Service for managing application preferences
 */
public class PreferencesService {
    private static final Logger logger = LogManager.getLogger(PreferencesService.class);
    private static PreferencesService instance;
    private final Preferences prefs;
    private Scene scene;

    private static final String DARK_MODE_KEY = "darkMode";

    private PreferencesService() {
        prefs = Preferences.userNodeForPackage(PreferencesService.class);
        logger.debug("PreferencesService initialized");
    }

    /**
     * Get the singleton instance of PreferencesService
     */
    public static synchronized PreferencesService getInstance() {
        if (instance == null) {
            instance = new PreferencesService();
        }
        return instance;
    }

    /**
     * Check if dark mode is enabled
     */
    public boolean isDarkMode() {
        return prefs.getBoolean(DARK_MODE_KEY, false);
    }

    /**
     * Set dark mode preference
     */
    public void setDarkMode(boolean enabled) {
        prefs.putBoolean(DARK_MODE_KEY, enabled);
        logger.info("Dark mode {} ", enabled ? "enabled" : "disabled");
    }

    /**
     * Set the scene for theme application
     */
    public void setScene(Scene scene) {
        this.scene = scene;
        logger.debug("Scene set for theme management");
    }

    /**
     * Apply the current theme to the scene
     */
    public void applyTheme() {
        if (scene == null || scene.getRoot() == null) {
            logger.warn("Cannot apply theme: scene not initialized");
            return;
        }

        boolean isDarkMode = isDarkMode();
        if (isDarkMode) {
            if (!scene.getRoot().getStyleClass().contains("dark-theme")) {
                scene.getRoot().getStyleClass().add("dark-theme");
            }
            logger.info("Dark theme applied");
        } else {
            scene.getRoot().getStyleClass().remove("dark-theme");
            logger.info("Light theme applied");
        }
    }
}
