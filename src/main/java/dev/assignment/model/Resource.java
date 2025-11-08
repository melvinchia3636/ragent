package dev.assignment.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Model class representing a resource file in the knowledgebase
 */
public class Resource {

    private final String fileName;
    private final File file;
    private String content;
    private Integer characterCount;

    public Resource(String fileName, File file) {
        this.fileName = fileName;
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile() {
        return file;
    }

    /**
     * Get the content of the file, loading it if necessary
     */
    public String getContent() throws IOException {
        if (content == null) {
            loadContent();
        }
        return content;
    }

    /**
     * Get the character count, calculating it if necessary
     */
    public int getCharacterCount() throws IOException {
        if (characterCount == null) {
            if (content == null) {
                loadContent();
            }
            characterCount = content.length();
        }
        return characterCount;
    }

    /**
     * Get formatted character count
     */
    public String getFormattedCharacterCount() {
        try {
            int count = getCharacterCount();
            if (count >= 1000000) {
                return String.format("%.1fM chars", count / 1000000.0);
            } else if (count >= 1000) {
                return String.format("%.1fK chars", count / 1000.0);
            } else {
                return count + " chars";
            }
        } catch (IOException e) {
            return "? chars";
        }
    }

    private void loadContent() throws IOException {
        if (file != null && file.exists()) {
            content = Files.readString(file.toPath());
        } else {
            throw new IOException("File not found: " + fileName);
        }
    }

    /**
     * Check if the file exists
     */
    public boolean exists() {
        return file != null && file.exists();
    }

    /**
     * Delete the resource file
     */
    public boolean delete() throws IOException {
        if (file != null && file.exists()) {
            return Files.deleteIfExists(file.toPath());
        }
        return false;
    }

    @Override
    public String toString() {
        return fileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Resource resource = (Resource) obj;
        return fileName.equals(resource.fileName);
    }

    @Override
    public int hashCode() {
        return fileName.hashCode();
    }
}
