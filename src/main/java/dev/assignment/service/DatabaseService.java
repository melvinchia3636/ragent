package dev.assignment.service;

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

import dev.assignment.model.Session;

/**
 * Service for managing SQLite database operations
 */
public class DatabaseService {
    private static final String DB_PATH = "rag_sessions.db";
    private static DatabaseService instance;
    private Connection connection;

    private DatabaseService() {
        initializeDatabase();
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * Initialize database connection and create tables if they don't exist
     */
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Create necessary tables
     */
    private void createTables() throws SQLException {
        String createSessionsTable = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "created_at TEXT NOT NULL" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSessionsTable);
        }
    }

    /**
     * Create a new session and its knowledgebase folder
     */
    public Session createSession(String name) {
        Session session = new Session(name);

        String sql = "INSERT INTO sessions (id, name, created_at) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, session.getId());
            pstmt.setString(2, session.getName());
            pstmt.setString(3, session.getCreatedAt().toString());
            pstmt.executeUpdate();

            File sessionFolder = new File("knowledgebase_storage/" + session.getId());
            sessionFolder.mkdirs();

            return session;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create session", e);
        }
    }

    /**
     * Get all sessions ordered by creation date (newest first)
     */
    public List<Session> getAllSessions() {
        List<Session> sessions = new ArrayList<>();
        String sql = "SELECT id, name, created_at FROM sessions ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
                sessions.add(new Session(id, name, createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sessions;
    }

    /**
     * Get a session by ID
     */
    public Session getSession(String id) {
        String sql = "SELECT id, name, created_at FROM sessions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
                return new Session(id, name, createdAt);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Update session name
     */
    public void updateSession(String id, String newName) {
        String sql = "UPDATE sessions SET name = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update session", e);
        }
    }

    /**
     * Delete a session and its knowledgebase folder
     */
    public void deleteSession(String id) {
        String sql = "DELETE FROM sessions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();

            File sessionFolder = new File("knowledgebase_storage/" + id);
            if (sessionFolder.exists()) {
                deleteDirectory(sessionFolder);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete session", e);
        }
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
            e.printStackTrace();
        }
    }
}
