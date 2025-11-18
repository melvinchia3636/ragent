package dev.assignment.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.assignment.model.Resource;
import dev.assignment.service.RAGService;
import dev.assignment.service.ResourceService;
import dev.assignment.view.ProgressDialog;
import dev.assignment.view.ResourceListCell;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller for the Resource Management window
 */
public class ResourceManagementController {

    @FXML
    private ListView<Resource> resourceListView;

    private ResourceService resourceService;
    private RAGService ragService;
    private Runnable onResourcesChangedCallback;

    public void setResourceService(ResourceService resourceService) {
        this.resourceService = resourceService;
        loadResources();
    }

    public void setRagService(RAGService ragService) {
        this.ragService = ragService;
    }

    public void setOnResourcesChangedCallback(Runnable callback) {
        this.onResourcesChangedCallback = callback;
    }

    private void notifyResourcesChanged() {
        if (onResourcesChangedCallback != null) {
            onResourcesChangedCallback.run();
        }
    }

    @FXML
    private void initialize() {
        // Set custom cell factory for displaying resources
        resourceListView.setCellFactory(listView -> new ResourceListCell());

        // Add double-click listener to open content viewer
        resourceListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Resource selectedResource = resourceListView.getSelectionModel().getSelectedItem();
                if (selectedResource != null) {
                    openContentViewer(selectedResource);
                }
            }
        });

        // Don't load resources here, will be loaded when setResourceService is called
    }

    private void loadResources() {
        if (resourceService == null) {
            return;
        }
        resourceListView.getItems().clear();
        List<Resource> resources = resourceService.getAllResources();
        resourceListView.getItems().addAll(resources);
    }

    @FXML
    private void handleAddResource() {
        FileChooser fileChooser = createFileChooser();
        Stage stage = (Stage) resourceListView.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            if (selectedFiles.size() > 1) {
                processMultipleFiles(selectedFiles, stage);
            } else {
                importSingleFile(selectedFiles.get(0));
            }
        }
    }

    private FileChooser createFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Resource File");
        if (resourceService != null) {
            fileChooser.getExtensionFilters().addAll(resourceService.getValidExtensions());
        }
        return fileChooser;
    }

    private void importSingleFile(File file) {
        try {
            String fileName = file.getName();
            String fileExtension = ResourceService.getFileExtension(fileName).toLowerCase();

            boolean needsExtraction = fileExtension.equals(".docx") ||
                    fileExtension.equals(".pptx") ||
                    fileExtension.equals(".ppt") ||
                    fileExtension.equals(".pdf");

            String targetFileName = needsExtraction ? ResourceService.getTextFileName(fileName) : fileName;

            boolean overwrite = false;
            if (resourceService.resourceExists(targetFileName)) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("File Exists");
                alert.setHeaderText("File already exists");
                alert.setContentText("The file '" + targetFileName + "' already exists. Do you want to replace it?");
                overwrite = alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;

                if (!overwrite) {
                    return;
                }

                // Remove old file from index if RAG service is available
                if (ragService != null) {
                    try {
                        ragService.removeFileFromIndexByName(targetFileName);
                    } catch (Exception e) {
                        System.err.println("Failed to remove old file from index: " + e.getMessage());
                    }
                }
            } else {
                overwrite = true;
            }

            resourceService.importResource(file, overwrite);

            // Index the file if RAG service is available
            if (ragService != null) {
                Stage ownerStage = (Stage) resourceListView.getScene().getWindow();
                ProgressDialog progressDialog = new ProgressDialog(ownerStage);
                progressDialog.show();

                new Thread(() -> {
                    try {
                        Platform.runLater(
                                () -> progressDialog.updateProgress(0, 1, "Indexing " + targetFileName + "..."));

                        File importedFile = new File(resourceService.getStoragePath().toFile(), targetFileName);
                        ragService.indexSingleFile(importedFile);

                        Platform.runLater(() -> {
                            progressDialog.close();
                            loadResources();
                            notifyResourcesChanged();
                            showInfo("Success", "File imported and indexed successfully as '" + targetFileName + "'.");
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            progressDialog.close();
                            loadResources();
                            notifyResourcesChanged();
                            showError("Indexing Error", "File imported but failed to index: " + e.getMessage());
                        });
                    }
                }).start();
            } else {
                loadResources();
                notifyResourcesChanged();
                showInfo("Success", "File imported successfully as '" + targetFileName + "'.");
            }

        } catch (IOException e) {
            showError("Import Error", "Failed to import file: " + e.getMessage());
        }
    }

    private void processMultipleFiles(List<File> files, Stage ownerStage) {
        List<String> conflicts = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            String fileExtension = ResourceService.getFileExtension(fileName).toLowerCase();

            boolean needsExtraction = fileExtension.equals(".docx") ||
                    fileExtension.equals(".pptx") ||
                    fileExtension.equals(".ppt") ||
                    fileExtension.equals(".pdf");

            String targetFileName = needsExtraction ? ResourceService.getTextFileName(fileName) : fileName;

            if (resourceService.resourceExists(targetFileName)) {
                conflicts.add(targetFileName);
            }
        }

        boolean overwriteAll = false;
        if (!conflicts.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Files Already Exist");
            alert.setHeaderText(conflicts.size() + " file(s) already exist");
            alert.setContentText("The following files already exist:\n" +
                    String.join("\n", conflicts.subList(0, Math.min(5, conflicts.size()))) +
                    (conflicts.size() > 5 ? "\n... and " + (conflicts.size() - 5) + " more" : "") +
                    "\n\nDo you want to replace all existing files?");

            ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
            if (result == ButtonType.CANCEL) {
                return;
            }
            overwriteAll = (result == ButtonType.OK);
        } else {
            overwriteAll = true;
        }

        final boolean shouldOverwrite = overwriteAll;

        ProgressDialog progressDialog = new ProgressDialog(ownerStage);
        progressDialog.show();

        Thread processingThread = new Thread(() -> {
            int total = files.size();
            int successCount = 0;
            int failCount = 0;
            int skippedCount = 0;

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                final int currentIndex = i + 1;

                try {
                    String fileName = file.getName();
                    String fileExtension = ResourceService.getFileExtension(fileName).toLowerCase();

                    boolean needsExtraction = fileExtension.equals(".docx") ||
                            fileExtension.equals(".pptx") ||
                            fileExtension.equals(".ppt") ||
                            fileExtension.equals(".pdf");

                    String targetFileName = needsExtraction ? ResourceService.getTextFileName(fileName) : fileName;

                    if (!shouldOverwrite && resourceService.resourceExists(targetFileName)) {
                        skippedCount++;
                        continue;
                    }

                    // Remove old file from index if it exists and will be overwritten
                    if (shouldOverwrite && resourceService.resourceExists(targetFileName) && ragService != null) {
                        try {
                            ragService.removeFileFromIndexByName(targetFileName);
                        } catch (Exception e) {
                            System.err.println("Failed to remove old file from index: " + e.getMessage());
                        }
                    }

                    // Show importing progress
                    Platform.runLater(() -> progressDialog.updateProgress(currentIndex, total,
                            "Importing " + file.getName() + "..."));

                    resourceService.importResource(file, shouldOverwrite);

                    // Index the file if RAG service is available
                    if (ragService != null) {
                        try {
                            // Show indexing progress
                            Platform.runLater(() -> progressDialog.updateProgress(currentIndex, total,
                                    "Indexing " + file.getName() + "..."));

                            File importedFile = new File(resourceService.getStoragePath().toFile(), targetFileName);
                            ragService.indexSingleFile(importedFile);
                        } catch (Exception indexError) {
                            System.err.println("Failed to index " + file.getName() + ": " + indexError.getMessage());
                        }
                    }

                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    System.err.println("Failed to import " + file.getName() + ": " + e.getMessage());
                }

                // Small delay for UI responsiveness
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            final int finalSuccess = successCount;
            final int finalFail = failCount;
            final int finalSkipped = skippedCount;

            Platform.runLater(() -> {
                progressDialog.close();
                loadResources();
                notifyResourcesChanged();

                String message = String.format(
                        "Import complete!\n\nSuccessfully imported: %d\nFailed: %d" +
                                (finalSkipped > 0 ? "\nSkipped: %d" : ""),
                        finalSuccess, finalFail, finalSkipped);

                if (finalFail > 0) {
                    showWarning("Import Complete with Errors", message);
                } else {
                    showInfo("Import Complete", message);
                }
            });
        });

        processingThread.setDaemon(true);
        processingThread.start();
    }

    @FXML
    private void handleRemoveResource() {
        Resource selectedResource = resourceListView.getSelectionModel().getSelectedItem();
        if (selectedResource == null) {
            showWarning("No Selection", "Please select a resource to remove.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Remove Resource");
        alert.setContentText("Are you sure you want to remove '" + selectedResource.getFileName() +
                "' from the knowledgebase?\nThis will delete the file from storage.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                String fileName = selectedResource.getFileName();

                resourceService.deleteResource(selectedResource);

                // Remove from index if RAG service is available
                if (ragService != null) {
                    try {
                        ragService.removeFileFromIndexByName(fileName);
                    } catch (Exception e) {
                        System.err.println("Failed to remove file from index: " + e.getMessage());
                    }
                }

                loadResources();
                notifyResourcesChanged();
                showInfo("Success", "File '" + fileName + "' has been removed.");
            } catch (IOException e) {
                showError("Delete Error", "Failed to delete file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) resourceListView.getScene().getWindow();
        stage.close();
    }

    private void openContentViewer(Resource resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dev/assignment/content_viewer.fxml"));
            Parent root = loader.load();

            ContentViewerController controller = loader.getController();
            controller.setResource(resource);

            Stage stage = new Stage();
            stage.setTitle("View: " + resource.getFileName());
            stage.initOwner(resourceListView.getScene().getWindow());

            Scene scene = new Scene(root, 700, 600);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showError("Error", "Failed to open content viewer: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    private void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
