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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for sharing chat history to Pastebin
 */
public class SessionSharer {
    private static final Logger logger = LogManager.getLogger(SessionSharer.class);
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

        String chatContent = SessionExporter.formatChatContent(session, messages);

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
     * Upload content to Pastebin
     * 
     * @param content the content to upload
     * @param title   the paste title
     * @param apiKey  the Pastebin API key
     * @return the Pastebin URL
     * @throws Exception if upload fails
     */
    private static String uploadToPastebin(String content, String title, String apiKey) throws Exception {
        // Build POST parameters
        String postData = "api_dev_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&api_option=paste"
                + "&api_paste_code=" + URLEncoder.encode(content, StandardCharsets.UTF_8)
                + "&api_paste_name=" + URLEncoder.encode("RAGent Chat - " + title, StandardCharsets.UTF_8)
                + "&api_paste_private=1" // 0=public, 1=unlisted, 2=private
                + "&api_paste_expire_date=1M"; // Expire after 1 month

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PASTEBIN_API_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int responseCode = response.statusCode();
        logger.info("Pastebin API response code: {}", responseCode);

        if (responseCode == 200) {
            String pastebinUrl = response.body();
            logger.info("Chat shared successfully to Pastebin: {}", pastebinUrl);
            return pastebinUrl;
        } else {
            String errorResponse = response.body();
            logger.error("Pastebin API error response: {}", errorResponse);
            throw new Exception("Pastebin API error: " + errorResponse);
        }
    }

    private SessionSharer() {
        // Prevent instantiation
    }
}
