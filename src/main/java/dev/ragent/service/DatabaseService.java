package dev.ragent.service;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.ragent.model.ChatMessage;
import dev.ragent.model.Session;

/**
 * Service for managing SQLite database operations
 */
public class DatabaseService {
    private static final Logger logger = LogManager.getLogger(DatabaseService.class);
    private static final String DB_PATH = "rag_sessions.db";
    private static DatabaseService instance;
    private Connection connection;

    private DatabaseService() throws SQLException {
        initializeDatabase();
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            try {
                instance = new DatabaseService();
            } catch (Exception e) {
                logger.error("Failed to create DatabaseService instance", e);
                return null;
            }
        }
        return instance;
    }

    /**
     * Initialize database connection and create tables if they don't exist
     */
    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        createTables();
    }

    /**
     * Create necessary tables
     */
    private void createTables() throws SQLException {
        String createSessionsTable = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "model TEXT NOT NULL DEFAULT 'gpt-4o-mini', " +
                "use_query_transformation INTEGER NOT NULL DEFAULT 1, " +
                "temperature REAL NOT NULL DEFAULT 1.0, " +
                "top_k INTEGER NOT NULL DEFAULT 5, " +
                "created_at TEXT NOT NULL" +
                ")";

        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                "id TEXT PRIMARY KEY, " +
                "session_id TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "is_user INTEGER NOT NULL, " +
                "timestamp TEXT NOT NULL, " +
                "sources TEXT, " +
                "FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE" +
                ")";

        String createMessageContextsTable = "CREATE TABLE IF NOT EXISTS message_contexts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "message_id TEXT NOT NULL, " +
                "file_path TEXT NOT NULL, " +
                "chunk_text TEXT NOT NULL, " +
                "score REAL NOT NULL, " +
                "FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE" +
                ")";

        String createQueryVariationsTable = "CREATE TABLE IF NOT EXISTS message_query_variations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "message_id TEXT NOT NULL, " +
                "variation_text TEXT NOT NULL, " +
                "variation_order INTEGER NOT NULL, " +
                "FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSessionsTable);
            stmt.execute(createMessagesTable);
            stmt.execute(createMessageContextsTable);
            stmt.execute(createQueryVariationsTable);

            // Add columns if they don't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE sessions ADD COLUMN temperature REAL NOT NULL DEFAULT 1.0");
                stmt.execute("ALTER TABLE sessions ADD COLUMN top_k INTEGER NOT NULL DEFAULT 5");
            } catch (SQLException e) {
                // Columns already exist, ignore
            }

            logger.info("Database tables initialized successfully");
        }
    }

    /**
     * Create a new session and its knowledgebase folder
     */
    public Session createSession(String name) {
        Session session = new Session(name);

        logger.info("Creating new session: id={}, name='{}', model={}, queryTransformation={}, temperature={}, topK={}",
                session.getId(), name, session.getModel(), session.isUseQueryTransformation(), session.getTemperature(),
                session.getTopK());

        String sql = "INSERT INTO sessions (id, name, model, use_query_transformation, temperature, top_k, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, session.getId());
            pstmt.setString(2, session.getName());
            pstmt.setString(3, session.getModel());
            pstmt.setInt(4, session.isUseQueryTransformation() ? 1 : 0);
            pstmt.setDouble(5, session.getTemperature());
            pstmt.setInt(6, session.getTopK());
            pstmt.setString(7, session.getCreatedAt().toString());
            pstmt.executeUpdate();

            File sessionFolder = new File("knowledgebase_storage/" + session.getId());
            sessionFolder.mkdirs();

            logger.info("Session created successfully: id={}, folder created", session.getId());
            return session;
        } catch (SQLException e) {
            logger.error("Failed to create session: name='{}'", name, e);
            throw new RuntimeException("Failed to create session", e);
        }
    }

    /**
     * Get all sessions ordered by creation date (newest first)
     */
    public List<Session> getAllSessions() throws SQLException {
        List<Session> sessions = new ArrayList<>();
        String sql = "SELECT id, name, model, use_query_transformation, temperature, top_k, created_at FROM sessions ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String model = rs.getString("model");
                boolean useQueryTransformation = rs.getInt("use_query_transformation") == 1;
                double temperature = rs.getDouble("temperature");
                int topK = rs.getInt("top_k");
                LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
                sessions.add(new Session(id, name, model, useQueryTransformation, temperature, topK, createdAt));
            }
        }

        return sessions;
    }

    /**
     * Get a session by ID
     */
    public Session getSession(String id) {
        String sql = "SELECT id, name, model, use_query_transformation, temperature, top_k, created_at FROM sessions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                String model = rs.getString("model");
                int useQueryTransformationInt = rs.getInt("use_query_transformation");
                boolean useQueryTransformation = useQueryTransformationInt == 1;
                double temperature = rs.getDouble("temperature");
                int topK = rs.getInt("top_k");
                LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));

                logger.debug(
                        "Retrieved session: id={}, name='{}', model={}, queryTransformation={}, temperature={}, topK={}",
                        id, name, model, useQueryTransformation, temperature, topK);

                return new Session(id, name, model, useQueryTransformation, temperature, topK, createdAt);
            } else {
                logger.debug("No session found with id: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Failed to get session: id={}", id, e);
        }

        return null;
    }

    /**
     * Update a session's name, model, query transformation setting, temperature,
     * and top K
     */
    public void updateSession(String id, String newName, String newModel, boolean useQueryTransformation,
            double temperature, int topK) {
        String sql = "UPDATE sessions SET name = ?, model = ?, use_query_transformation = ?, temperature = ?, top_k = ? WHERE id = ?";

        logger.info("Updating session: id={}, name='{}', model={}, queryTransformation={}, temperature={}, topK={}",
                id, newName, newModel, useQueryTransformation, temperature, topK);

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newModel);
            pstmt.setInt(3, useQueryTransformation ? 1 : 0);
            pstmt.setDouble(4, temperature);
            pstmt.setInt(5, topK);
            pstmt.setString(6, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Session updated successfully: {} row(s) affected", rowsAffected);
            } else {
                logger.warn("No session found with id: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Failed to update session: id={}, name='{}'", id, newName, e);
            throw new RuntimeException("Failed to update session", e);
        }
    }

    /**
     * Delete a session and its knowledgebase folder
     */
    public void deleteSession(String id) {
        String sql = "DELETE FROM sessions WHERE id = ?";

        logger.info("Deleting session: id={}", id);

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int rowsAffected = pstmt.executeUpdate();

            logger.info("Session deleted from database: {} row(s) affected", rowsAffected);

            File sessionFolder = new File("knowledgebase_storage/" + id);
            if (sessionFolder.exists()) {
                deleteDirectory(sessionFolder);
                logger.info("Session folder deleted: {}", sessionFolder.getPath());
            }

            // Delete embedding cache
            EmbeddingCacheService.deleteCache(id);
            logger.info("Session deletion complete: id={}", id);
        } catch (SQLException e) {
            logger.error("Failed to delete session: id={}", id, e);
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    /**
     * Save a chat message to the database
     */
    public void saveChatMessage(String sessionId, ChatMessage message) {
        String sql = "INSERT INTO messages (id, session_id, content, is_user, timestamp, sources) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, message.getId());
            pstmt.setString(2, sessionId);
            pstmt.setString(3, message.getContent());
            pstmt.setInt(4, message.isUser() ? 1 : 0);
            pstmt.setString(5, message.getTimestamp().toString());
            pstmt.setString(6, message.getSources());
            pstmt.executeUpdate();
            logger.debug("Saved message {} for session {}", message.getId(), sessionId);
        } catch (SQLException e) {
            logger.error("Failed to save chat message", e);
            throw new RuntimeException("Failed to save chat message", e);
        }
    }

    /**
     * Get all chat messages for a session ordered by timestamp
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT id, content, is_user, timestamp, sources FROM messages WHERE session_id = ? ORDER BY timestamp ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String content = rs.getString("content");
                boolean isUser = rs.getInt("is_user") == 1;
                LocalDateTime timestamp = LocalDateTime.parse(rs.getString("timestamp"));
                String sources = rs.getString("sources");
                messages.add(new ChatMessage(id, content, isUser, timestamp, sources));
            }
            logger.debug("Loaded {} messages for session {}", messages.size(), sessionId);
        } catch (SQLException e) {
            logger.error("Failed to get chat history", e);
        }

        return messages;
    }

    /**
     * Delete all chat messages for a session
     */
    public void clearChatHistory(String sessionId) {
        String sql = "DELETE FROM messages WHERE session_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            int deleted = pstmt.executeUpdate();
            logger.info("Cleared {} messages for session {}", deleted, sessionId);
        } catch (SQLException e) {
            logger.error("Failed to clear chat history", e);
            throw new RuntimeException("Failed to clear chat history", e);
        }
    }

    /**
     * Delete all messages after (and including) a specific message timestamp in a
     * session
     */
    public void deleteMessagesAfter(String sessionId, LocalDateTime timestamp) {
        String sql = "DELETE FROM messages WHERE session_id = ? AND timestamp >= ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setString(2, timestamp.toString());
            int deleted = pstmt.executeUpdate();
            logger.info("Deleted {} messages after timestamp {} for session {}", deleted, timestamp, sessionId);
        } catch (SQLException e) {
            logger.error("Failed to delete messages after timestamp", e);
            throw new RuntimeException("Failed to delete messages after timestamp", e);
        }
    }

    /**
     * Save context references for a message
     */
    public void saveMessageContexts(String messageId, List<dev.ragent.model.ContextReference> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO message_contexts (message_id, file_path, chunk_text, score) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (dev.ragent.model.ContextReference context : contexts) {
                pstmt.setString(1, messageId);
                pstmt.setString(2, context.getFilePath());
                pstmt.setString(3, context.getChunkText());
                pstmt.setDouble(4, context.getScore());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            logger.debug("Saved {} context references for message {}", contexts.size(), messageId);
        } catch (SQLException e) {
            logger.error("Failed to save message contexts", e);
        }
    }

    /**
     * Save query variations for a message
     */
    public void saveQueryVariations(String messageId, List<String> variations) {
        if (variations == null || variations.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO message_query_variations (message_id, variation_text, variation_order) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < variations.size(); i++) {
                pstmt.setString(1, messageId);
                pstmt.setString(2, variations.get(i));
                pstmt.setInt(3, i);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            logger.debug("Saved {} query variations for message {}", variations.size(), messageId);
        } catch (SQLException e) {
            logger.error("Failed to save query variations", e);
        }
    }

    /**
     * Get context references for a message
     */
    public List<dev.ragent.model.ContextReference> getMessageContexts(String messageId) {
        List<dev.ragent.model.ContextReference> contexts = new ArrayList<>();
        String sql = "SELECT file_path, chunk_text, score FROM message_contexts WHERE message_id = ? ORDER BY score DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, messageId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String filePath = rs.getString("file_path");
                String chunkText = rs.getString("chunk_text");
                double score = rs.getDouble("score");
                contexts.add(new dev.ragent.model.ContextReference(filePath, chunkText, score));
            }
            logger.debug("Loaded {} context references for message {}", contexts.size(), messageId);
        } catch (SQLException e) {
            logger.error("Failed to get message contexts", e);
        }

        return contexts;
    }

    /**
     * Get query variations for a message
     */
    public List<String> getQueryVariations(String messageId) {
        List<String> variations = new ArrayList<>();
        String sql = "SELECT variation_text FROM message_query_variations WHERE message_id = ? ORDER BY variation_order";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, messageId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                variations.add(rs.getString("variation_text"));
            }
            logger.debug("Loaded {} query variations for message {}", variations.size(), messageId);
        } catch (SQLException e) {
            logger.error("Failed to get query variations", e);
        }

        return variations;
    }

    /**
     * Recursively delete a directory
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection", e);
        }
    }
}
