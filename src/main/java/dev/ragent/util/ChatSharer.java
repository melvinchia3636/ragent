package dev.ragent.util;

import dev.ragent.model.ChatMessage;
import dev.ragent.model.Session;
import dev.ragent.service.APIKeyService;
import dev.ragent.service.DatabaseService;
import dev.ragent.view.AlertHelper;
import dev.ragent.view.ShareSuccessDialog;
import dev.ragent.view.SharingProgressDialog;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for sharing chat history to Pastebin
 */
public class ChatSharer {
    private static final Logger logger = LogManager.getLogger(ChatSharer.class);
    private static final String PASTEBIN_API_URL = "https://pastebin.com/api/api_post.php";

    /**
     * Share chat history for a session to Pastebin
     * 
     * @param session the session to share
     * @param owner   the owner window for dialogs
     */
    public static void shareChat(Session session, Stage owner) {
        logger.info("Sharing chat for session: {}", session.getName());

        APIKeyService apiKeyService = APIKeyService.getInstance();
        if (!apiKeyService.hasPastebinApiKey()) {
            logger.error("Pastebin API key is not configured");
            AlertHelper.showError(
                    "Configuration Error",
                    "Cannot Share Chat",
                    "Pastebin API key is not configured.");
            return;
        }

        DatabaseService databaseService = DatabaseService.getInstance();
        if (databaseService == null) {
            logger.error("Database service is unavailable. Cannot share chat for session: {}", session.getName());
            AlertHelper.showError(
                    "Database Error",
                    "Cannot Share Chat",
                    "The database is unavailable.");
            return;
        }

        List<ChatMessage> messages = databaseService.getChatHistory(session.getId());
        if (messages.isEmpty()) {
            logger.warn("No messages found for session: {}", session.getName());
            AlertHelper.showInfo(
                    "No Messages",
                    "Cannot Share Chat",
                    "There are no messages in this session to share.");
            return;
        }

        // Format the chat content
        String chatContent = formatChatContent(session, messages);

        // Show progress and upload in background thread
        Platform.runLater(() -> {
            SharingProgressDialog progressDialog = new SharingProgressDialog(owner);
            progressDialog.show();

            new Thread(() -> {
                try {
                    String pastebinUrl = uploadToPastebin(chatContent, session.getName(),
                            apiKeyService.getPastebinApiKey());

                    Platform.runLater(() -> {
                        progressDialog.close();
                        ShareSuccessDialog successDialog = new ShareSuccessDialog(pastebinUrl, owner);
                        successDialog.showAndWait();
                    });
                } catch (Exception e) {
                    logger.error("Failed to share chat for session: {}: {}", session.getName(), e.getMessage(), e);
                    Platform.runLater(() -> {
                        progressDialog.close();
                        AlertHelper.showError(
                                "Upload Failed",
                                "Cannot Share Chat",
                                "Failed to upload to Pastebin:\n" + e.getMessage());
                    });
                }
            }).start();
        });
    }

    /**
     * Format chat content for sharing
     * 
     * @param session  the session
     * @param messages the chat messages
     * @return formatted chat content
     */
    private static String formatChatContent(Session session, List<ChatMessage> messages) {
        StringBuilder content = new StringBuilder();

        content.append("Chat Export - ").append(session.getName()).append("\n");
        content.append("Created: ").append(session.getFormattedCreatedAt()).append("\n");
        content.append("Exported: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n");
        content.append("Model: ").append(session.getModel()).append("\n");
        content.append("=".repeat(80)).append("\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ChatMessage message : messages) {
            String role = message.isUser() ? "USER" : "ASSISTANT";
            String timestamp = message.getTimestamp().format(formatter);
            content.append("[").append(timestamp).append("] ").append(role).append(":\n");
            content.append(message.getContent()).append("\n\n");

            if (!message.isUser() && message.getSources() != null && !message.getSources().isEmpty()) {
                content.append("Sources: ").append(message.getSources()).append("\n\n");
            }
        }

        return content.toString();
    }

    /**
     * Upload content to Pastebin
     * 
     * @param content the content to upload
     * @param title   the paste title
     * @param apiKey  the Pastebin API key
     * @return the Pastebin URL
     * @throws Exception if upload fails
     */
    private static String uploadToPastebin(String content, String title, String apiKey) throws Exception {
        URL url = new URL(PASTEBIN_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Build POST parameters
        String postData = "api_dev_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&api_option=paste"
                + "&api_paste_code=" + URLEncoder.encode(content, StandardCharsets.UTF_8)
                + "&api_paste_name=" + URLEncoder.encode("RAGent Chat - " + title, StandardCharsets.UTF_8)
                + "&api_paste_private=1" // 0=public, 1=unlisted, 2=private
                + "&api_paste_expire_date=1M"; // Expire after 1 month

        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = postData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read response
        int responseCode = connection.getResponseCode();
        logger.info("Pastebin API response code: {}", responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String pastebinUrl = response.toString();
                logger.info("Chat shared successfully to Pastebin: {}", pastebinUrl);
                return pastebinUrl;
            }
        } else {
            // Read error response
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
                logger.error("Pastebin API error response: {}", errorResponse.toString());
                throw new Exception("Pastebin API error: " + errorResponse.toString());
            }
        }
    }

    private ChatSharer() {
        // Prevent instantiation
    }
}
