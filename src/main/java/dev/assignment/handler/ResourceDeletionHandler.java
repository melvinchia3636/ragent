package dev.assignment.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.assignment.model.Resource;
import dev.assignment.service.RAGService;
import dev.assignment.service.ResourceService;

/**
 * Handles resource deletion operations
 */
public class ResourceDeletionHandler {

    private final ResourceService resourceService;
    private final RAGService ragService;

    public ResourceDeletionHandler(ResourceService resourceService, RAGService ragService) {
        this.resourceService = resourceService;
        this.ragService = ragService;
    }

    /**
     * Delete a single resource and remove from index
     */
    public void deleteResource(Resource resource) throws IOException {
        String fileName = resource.getFileName();
        resourceService.deleteResource(resource);
        removeFromIndex(fileName);
    }

    /**
     * Delete multiple resources and track results
     */
    public DeletionResult deleteMultipleResources(List<Resource> resources) {
        DeletionResult result = new DeletionResult();

        for (Resource resource : resources) {
            try {
                deleteResource(resource);
                result.successCount++;
            } catch (IOException e) {
                result.failCount++;
                result.failedFiles.add(resource.getFileName());
            }
        }

        return result;
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
     * Result of deletion operation
     */
    public static class DeletionResult {
        public int successCount = 0;
        public int failCount = 0;
        public List<String> failedFiles = new ArrayList<>();

        public boolean hasFailures() {
            return failCount > 0;
        }

        public String getFailedFilesMessage() {
            return failedFiles.stream()
                    .limit(5)
                    .collect(Collectors.joining("\n"))
                    + (failedFiles.size() > 5 ? "\n... and " + (failedFiles.size() - 5) + " more" : "");
        }
    }
}
