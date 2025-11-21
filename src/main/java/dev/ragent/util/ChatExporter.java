package dev.ragent.util;

import dev.ragent.model.ChatMessage;
import dev.ragent.model.Session;
import dev.ragent.service.DatabaseService;
import dev.ragent.view.AlertHelper;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for exporting chat history to text files
 */
public class ChatExporter {
    private static final Logger logger = LogManager.getLogger(ChatExporter.class);

    /**
     * Export chat history for a session to a text file
     * 
     * @param session the session to export
     * @param owner   the owner window for the file chooser dialog
     */
    public static void exportChat(Session session, Stage owner) {
        logger.info("Exporting chat for session: {}", session.getName());

        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            logger.error("Database service is unavailable. Cannot export chat for session: {}", session.getName());
            AlertHelper.showError(
                    "Database Error",
                    "Cannot Export Chat",
                    "The database is unavailable.");
            return;
        }

        List<ChatMessage> messages = databaseService.getChatHistory(session.getId());
        if (messages.isEmpty()) {
            logger.warn("No messages found for session: {}", session.getName());
            AlertHelper.showInfo(
                    "No Messages",
                    "Cannot Export Chat",
                    "There are no messages in this session to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Chat - " + session.getName());
        // Format: RAGent_chat_export_<session_name>_<timestamp>.txt
        fileChooser.setInitialFileName(String.format("RAGent_chat_export_%s_%s.txt",
                session.getName().replaceAll("[^a-zA-Z0-9.-]", "_"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            logger.info("Export cancelled by user for session: {}", session.getName());
            return;
        }

        logger.info("Exporting chat to file: {}", file.getAbsolutePath());

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Chat Export - " + session.getName() + "\n");
            writer.write("Created: " + session.getFormattedCreatedAt() + "\n");
            writer.write("Exported: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Model: " + session.getModel() + "\n");
            writer.write("=".repeat(80) + "\n\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (ChatMessage message : messages) {
                String role = message.isUser() ? "USER" : "ASSISTANT";
                String timestamp = message.getTimestamp().format(formatter);
                writer.write("[" + timestamp + "] " + role + ":\n");
                writer.write(message.getContent() + "\n\n");

                if (!message.isUser() && message.getSources() != null && !message.getSources().isEmpty()) {
                    writer.write("Sources: " + message.getSources() + "\n\n");
                }
            }

            logger.info("Chat export completed for session: {}", session.getName());

            AlertHelper.showInfo(
                    "Export Successful",
                    "Chat Exported",
                    "Chat history has been exported to:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to export chat for session: {}: {}", session.getName(), e.getMessage());

            AlertHelper.showError(
                    "Export Failed",
                    "Cannot Export Chat",
                    "Failed to write to file:\n" + e.getMessage());
        }
    }

    private ChatExporter() {
        // Prevent instantiation
    }
}
