package dev.ragent.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.ragent.service.RAGService;
import dev.ragent.service.ResourceService;
import dev.ragent.view.ProgressDialog;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Handles single and multiple file import operations
 */
public class ResourceImportHandler {

    private final ResourceService resourceService;
    private final RAGService ragService;
    private final Runnable onImportComplete;

    public ResourceImportHandler(ResourceService resourceService, RAGService ragService,
            Runnable onImportComplete) {
        this.resourceService = resourceService;
        this.ragService = ragService;
        this.onImportComplete = onImportComplete;
    }

    /**
     * Import a single file with progress tracking
     */
    public void importSingleFile(File file, String targetFileName, boolean overwrite, Stage ownerStage,
            Runnable onSuccess, Consumer<String> onError) {
        ProgressDialog progressDialog = new ProgressDialog(ownerStage);

        Thread importThread = new Thread(() -> {
            try {
                if (progressDialog.isCancelled())
                    return;

                // Import resource
                resourceService.importResource(file, overwrite, message -> {
                    if (!progressDialog.isCancelled()) {
                        Platform.runLater(() -> progressDialog.updateProgress(1, 2, message));
                    }
                });

                if (progressDialog.isCancelled()) {
                    Platform.runLater(onImportComplete);
                    return;
                }

                // Index if available
                indexFile(targetFileName, progressDialog);

                // Show success
                Platform.runLater(() -> {
                    progressDialog.close();
                    if (!progressDialog.isCancelled()) {
                        onImportComplete.run();
                        onSuccess.run();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressDialog.close();
                    if (!progressDialog.isCancelled()) {
                        onImportComplete.run();
                        onError.accept(e.getMessage());
                    }
                });
            }
        });

        progressDialog.setOnCancel(importThread::interrupt);
        importThread.start();
        progressDialog.show();
    }

    /**
     * Import multiple files with batch progress tracking
     */
    public void importMultipleFiles(List<File> files, boolean overwriteAll, Stage ownerStage,
            Consumer<ImportResult> onComplete) {
        ProgressDialog progressDialog = new ProgressDialog(ownerStage);

        Thread processingThread = new Thread(() -> {
            ImportResult result = processFiles(files, overwriteAll, progressDialog);
            Platform.runLater(() -> {
                progressDialog.close();
                onImportComplete.run();
                onComplete.accept(result);
            });
        });

        progressDialog.setOnCancel(processingThread::interrupt);
        processingThread.setDaemon(true);
        processingThread.start();
        progressDialog.show();
    }

    private ImportResult processFiles(List<File> files, boolean overwriteAll, ProgressDialog progressDialog) {
        ImportResult result = new ImportResult();
        int total = files.size();

        for (int i = 0; i < files.size(); i++) {
            if (progressDialog.isCancelled() || Thread.currentThread().isInterrupted()) {
                result.cancelled = true;
                break;
            }

            File file = files.get(i);
            FileMetadata metadata = new FileMetadata(file);
            int currentIndex = i + 1;

            try {
                // Skip if exists and not overwriting
                if (!overwriteAll && resourceService.resourceExists(metadata.targetFileName)) {
                    result.skipped++;
                    continue;
                }

                // Remove from index if overwriting
                if (overwriteAll && resourceService.resourceExists(metadata.targetFileName)) {
                    removeFromIndex(metadata.targetFileName);
                }

                // Import file
                Platform.runLater(() -> progressDialog.updateProgress(currentIndex, total,
                        "Processing " + file.getName() + "..."));

                if (progressDialog.isCancelled())
                    break;

                resourceService.importResource(file, overwriteAll, message -> {
                    if (!progressDialog.isCancelled()) {
                        Platform.runLater(() -> progressDialog.updateProgress(currentIndex, total, message));
                    }
                });

                if (progressDialog.isCancelled())
                    break;

                // Index file
                if (ragService != null) {
                    Platform.runLater(() -> progressDialog.updateProgress(currentIndex, total,
                            "Indexing " + file.getName() + "..."));

                    File importedFile = new File(resourceService.getStoragePath().toFile(), metadata.targetFileName);
                    ragService.indexSingleFile(importedFile);
                }

                result.success++;

            } catch (Exception e) {
                result.failed++;
                result.failedFiles.add(file.getName());
                System.err.println("Failed to import " + file.getName() + ": " + e.getMessage());
            }

            // Brief pause for UI responsiveness
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return result;
    }

    private void indexFile(String targetFileName, ProgressDialog progressDialog) {
        if (ragService == null)
            return;

        Platform.runLater(() -> progressDialog.updateProgress(2, 2, "Indexing " + targetFileName + "..."));

        try {
            File importedFile = new File(resourceService.getStoragePath().toFile(), targetFileName);
            ragService.indexSingleFile(importedFile);
        } catch (Exception e) {
            System.err.println("Failed to index file: " + e.getMessage());
        }
    }

    private void removeFromIndex(String fileName) {
        if (ragService == null)
            return;

        try {
            ragService.removeFileFromIndexByName(fileName);
        } catch (Exception e) {
            System.err.println("Failed to remove file from index: " + e.getMessage());
        }
    }

    /**
     * Result of import operation
     */
    public static class ImportResult {
        public int success = 0;
        public int failed = 0;
        public int skipped = 0;
        public boolean cancelled = false;
        public List<String> failedFiles = new ArrayList<>();
    }

    /**
     * File metadata helper
     */
    private static class FileMetadata {
        final String fileName;
        final String extension;
        final boolean needsExtraction;
        final String targetFileName;

        FileMetadata(File file) {
            this.fileName = file.getName();
            this.extension = ResourceService.getFileExtension(fileName).toLowerCase();
            this.needsExtraction = extension.equals(".docx") || extension.equals(".pptx") ||
                    extension.equals(".ppt") || extension.equals(".pdf");
            this.targetFileName = needsExtraction ? ResourceService.getTextFileName(fileName) : fileName;
        }
    }
}
