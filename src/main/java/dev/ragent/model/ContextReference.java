package dev.ragent.model;

/**
 * Represents a context reference used in generating an AI response
 */
public class ContextReference {
    private final String filePath;
    private final String chunkText;
    private final double score;

    public ContextReference(String filePath, String chunkText, double score) {
        this.filePath = filePath;
        this.chunkText = chunkText;
        this.score = score;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getChunkText() {
        return chunkText;
    }

    public double getScore() {
        return score;
    }

    /**
     * Get the file name from the full path
     */
    public String getFileName() {
        if (filePath == null) {
            return "Unknown";
        }
        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
    }

    /**
     * Get a formatted relevance score (0-100%)
     */
    public String getFormattedScore() {
        return String.format("%.1f%%", score * 100);
    }
}
