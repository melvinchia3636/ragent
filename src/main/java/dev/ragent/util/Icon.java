package dev.ragent.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

/**
 * Utility class for loading SVG icons
 */
public class Icon {

    private static final Logger logger = LogManager.getLogger(Icon.class);

    /**
     * Load an SVG icon from the resources
     * 
     * @param iconName The name of the icon resource relative to the icons directory
     *                 (e.g.,
     *                 "tabler--dots-vertical")
     * @return The loaded SVGImage
     */
    public static SVGImage load(String iconName) {
        try {
            SVGImage svgImage = SVGLoader
                    .load(Icon.class.getResource(String.format("/dev/ragent/icons/%s.svg", iconName)));
            if (svgImage == null) {
                throw new RuntimeException("Icon not found: " + iconName);
            }

            svgImage.getStylesheets().add(Icon.class.getResource("/dev/ragent/styles/index.css").toExternalForm());
            svgImage.getStyleClass().add("icon");

            return svgImage;
        } catch (RuntimeException e) {
            logger.error("Failed to load icon: " + iconName, e.getMessage());
            return null;
        }
    }
}
