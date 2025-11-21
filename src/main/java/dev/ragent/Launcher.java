package dev.ragent;

/**
 * Launcher class to start the JavaFX application
 * This class doesn't extend Application and is used as the main class
 * to avoid issues with JavaFX runtime detection in fat JARs
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
