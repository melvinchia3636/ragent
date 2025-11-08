package dev.assignment.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import dev.assignment.model.Resource;
import dev.assignment.util.TextExtractor;
import javafx.stage.FileChooser;

/**
 * Service class for managing resources in the knowledgebase
 */
public class ResourceService {

    private static final String STORAGE_DIR = "knowledgebase_storage";
    private final String sessionId;
    private Path storagePath;

    private static final FileChooser.ExtensionFilter[] VALID_EXTENSIONS = new FileChooser.ExtensionFilter[] {
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
            new FileChooser.ExtensionFilter("PDF Files (text will be extracted)", "*.pdf"),
            new FileChooser.ExtensionFilter("Word Documents (text will be extracted)", "*.docx"),
            new FileChooser.ExtensionFilter("PowerPoint Presentations (text will be extracted)",
                    "*.ppt", "*.pptx")
    };

    /**
     * Create ResourceService for a specific session
     */
    public ResourceService(String sessionId) {
        this.sessionId = sessionId;
        try {
            initialize();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize resource service", e);
        }
    }

    /**
     * Initialize the storage directory for this session
     */
    private void initialize() throws IOException {
        storagePath = Paths.get(STORAGE_DIR, sessionId);
        Files.createDirectories(storagePath);
    }

    /**
     * Get the storage directory path for this session
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * Get all resources in the knowledgebase
     */
    public List<Resource> getAllResources() {
        List<Resource> resources = new ArrayList<>();
        File storageDir = getStoragePath().toFile();

        if (storageDir.exists() && storageDir.isDirectory()) {
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        resources.add(new Resource(file.getName(), file));
                    }
                }
            }
        }
        return resources;
    }

    /**
     * Get a specific resource by filename
     */
    public Resource getResource(String fileName) {
        Path filePath = getStoragePath().resolve(fileName);
        File file = filePath.toFile();
        if (file.exists()) {
            return new Resource(fileName, file);
        }
        return null;
    }

    /**
     * Check if a resource exists
     */
    public boolean resourceExists(String fileName) {
        return getResource(fileName) != null;
    }

    public boolean resourceExtensionValid(String extension) {

        for (String ext : getValidExtensionStrings()) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    public FileChooser.ExtensionFilter[] getValidExtensions() {
        String[] extensions = Arrays.stream(getValidExtensionStrings())
                .map(ext -> "*" + ext)
                .toArray(String[]::new);

        FileChooser.ExtensionFilter allSupported = new FileChooser.ExtensionFilter("All Supported Files", extensions);
        FileChooser.ExtensionFilter allFiles = new FileChooser.ExtensionFilter("All Files", "*.*");

        List<FileChooser.ExtensionFilter> filters = new ArrayList<>();
        filters.add(allSupported);

        Collections.addAll(filters, VALID_EXTENSIONS);

        filters.add(allFiles);

        return filters.toArray(FileChooser.ExtensionFilter[]::new);
    }

    public String[] getValidExtensionStrings() {
        List<String> extensions = new ArrayList<>();
        for (FileChooser.ExtensionFilter filter : VALID_EXTENSIONS) {
            extensions.addAll(filter.getExtensions().stream()
                    .map(ext -> ext.startsWith("*") ? ext.substring(1) : ext)
                    .collect(Collectors.toList()));
        }

        return extensions.toArray(String[]::new);
    }

    /**
     * Import a file into the knowledgebase
     * 
     * @param sourceFile The file to import
     * @param overwrite  Whether to overwrite if file already exists
     * @return The imported Resource
     */
    public Resource importResource(File sourceFile, boolean overwrite) throws IOException {
        String fileName = sourceFile.getName();
        String fileExtension = getFileExtension(fileName).toLowerCase();

        if (!resourceExtensionValid(fileExtension)) {
            throw new IOException("Unsupported file format: " + fileExtension);
        }

        boolean needsExtraction = fileExtension.equals(".docx") ||
                fileExtension.equals(".pptx") ||
                fileExtension.equals(".ppt") ||
                fileExtension.equals(".pdf");

        String targetFileName = needsExtraction ? TextExtractor.getTextFileName(fileName) : fileName;
        Path destinationPath = getStoragePath().resolve(targetFileName);

        if (!overwrite && Files.exists(destinationPath)) {
            throw new IOException("File already exists: " + targetFileName);
        }

        if (needsExtraction) {
            // Extract text and save
            String extractedText = TextExtractor.extractText(sourceFile);
            TextExtractor.saveTextToFile(extractedText, destinationPath.toFile());
        } else {
            // Copy file directly
            Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return new Resource(targetFileName, destinationPath.toFile());
    }

    /**
     * Delete a resource
     */
    public boolean deleteResource(Resource resource) throws IOException {
        return resource.delete();
    }

    /**
     * Delete a resource by filename
     */
    public boolean deleteResource(String fileName) throws IOException {
        Resource resource = getResource(fileName);
        if (resource != null) {
            return deleteResource(resource);
        }
        return false;
    }

    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    public static String getTextFileName(String originalFileName) {
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return originalFileName.substring(0, lastDotIndex) + ".txt";
        }
        return originalFileName + ".txt";
    }
}
