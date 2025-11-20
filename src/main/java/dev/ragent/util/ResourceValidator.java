package dev.ragent.util;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import dev.ragent.service.ResourceService;

/**
 * Handles validation of file imports
 */
public class ResourceValidator {

    private final ResourceService resourceService;

    public ResourceValidator(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Check if document limit has been reached
     */
    public boolean isDocumentLimitReached() {
        return resourceService.getResourceCount() >= Constants.MAX_DOCUMENTS_PER_SESSION;
    }

    /**
     * Validate a single file for import
     */
    public ValidationResult validateSingleFile(File file) {
        if (file.length() > Constants.MAX_DOCUMENT_SIZE_BYTES) {
            return ValidationResult.error(
                    String.format("The file '%s' is %.2f MB, which exceeds the maximum allowed size of 50 MB.",
                            file.getName(), file.length() / (1024.0 * 1024.0)));
        }
        return ValidationResult.success();
    }

    /**
     * Validate multiple files for import
     */
    public ValidationResult validateMultipleFiles(List<File> files) {
        int currentCount = resourceService.getResourceCount();
        int availableSlots = Constants.MAX_DOCUMENTS_PER_SESSION - currentCount;

        if (availableSlots <= 0) {
            return new ValidationResult(false, "Document limit reached. You cannot add more documents.");
        }

        if (files.size() > availableSlots) {
            return ValidationResult.error(
                    String.format("You selected %d files, but only %d slots are available (limit: %d documents).",
                            files.size(), availableSlots, Constants.MAX_DOCUMENTS_PER_SESSION));
        }

        List<File> oversizedFiles = files.stream()
                .filter(f -> f.length() > Constants.MAX_DOCUMENT_SIZE_BYTES)
                .collect(Collectors.toList());

        if (!oversizedFiles.isEmpty()) {
            String fileList = oversizedFiles.stream()
                    .limit(5)
                    .map(f -> String.format("%s (%.2f MB)", f.getName(), f.length() / (1024.0 * 1024.0)))
                    .collect(Collectors.joining("\n"));

            String message = "The following files exceed the 50 MB limit:\n" + fileList
                    + (oversizedFiles.size() > 5 ? "\n... and " + (oversizedFiles.size() - 5) + " more" : "");

            return ValidationResult.error(message);
        }

        return ValidationResult.success();
    }

    /**
     * Find conflicting files (files that already exist)
     */
    public List<String> findConflicts(List<File> files) {
        return files.stream()
                .map(this::getTargetFileName)
                .filter(resourceService::resourceExists)
                .collect(Collectors.toList());
    }

    private String getTargetFileName(File file) {
        String fileName = file.getName();
        String extension = ResourceService.getFileExtension(fileName).toLowerCase();
        boolean needsExtraction = extension.equals(".docx") || extension.equals(".pptx") ||
                extension.equals(".ppt") || extension.equals(".pdf");
        return needsExtraction ? ResourceService.getTextFileName(fileName) : fileName;
    }

    /**
     * Result of validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
