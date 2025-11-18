package dev.assignment.service;

import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Service for indexing documents into embeddings
 */
public class DocumentIndexingService {

    private static final Logger logger = LogManager.getLogger(DocumentIndexingService.class);

    private final String sessionId;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Map<String, Long> indexedFiles;

    // Progress callback interface
    public interface ProgressCallback {
        void onProgress(String message, int current, int total);
    }

    public DocumentIndexingService(String sessionId, EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore, Map<String, Long> indexedFiles) {
        this.sessionId = sessionId;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.indexedFiles = indexedFiles;
    }

    /**
     * Index all documents from the knowledgebase (incremental) with progress
     * callback
     */
    public void indexKnowledgebase(ResourceService resourceService, ProgressCallback progressCallback)
            throws IOException {
        Path storagePath = resourceService.getStoragePath();
        File storageDir = storagePath.toFile();

        if (!storageDir.exists() || !storageDir.isDirectory()) {
            return;
        }

        File[] files = storageDir.listFiles();
        if (files == null) {
            return;
        }

        logger.debug("Starting incremental indexing");
        logger.debug("Found {} files in directory", files.length);

        // First pass: determine what needs to be indexed
        Set<String> currentFiles = new HashSet<>();
        List<File> filesToIndex = new ArrayList<>();

        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                currentFiles.add(fileName);
                long lastModified = file.lastModified();

                // Check if file needs indexing
                if (!indexedFiles.containsKey(fileName) || indexedFiles.get(fileName) != lastModified) {
                    filesToIndex.add(file);
                }
            }
        }

        int totalSteps = filesToIndex.size();
        int currentStep = 0;

        if (progressCallback != null && totalSteps > 0) {
            progressCallback.onProgress("Starting indexing...", 0, totalSteps);
        }

        // Index files with progress updates
        int newIndexed = 0;
        int updated = 0;

        for (File file : filesToIndex) {
            String fileName = file.getName();
            long lastModified = file.lastModified();

            logger.debug("Indexing {} file: {}",
                    indexedFiles.containsKey(fileName) ? "modified" : "new", fileName);

            if (progressCallback != null) {
                currentStep++;
                progressCallback.onProgress("Indexing " + fileName + "...", currentStep, totalSteps);
            }

            // Remove old segments if file was modified
            if (indexedFiles.containsKey(fileName)) {
                removeFileFromIndex(fileName);
                updated++;
            } else {
                newIndexed++;
            }

            indexDocument(file);
            indexedFiles.put(fileName, lastModified);
        }

        // Remove deleted files from index
        Set<String> deletedFiles = new HashSet<>(indexedFiles.keySet());
        deletedFiles.removeAll(currentFiles);

        for (String deletedFile : deletedFiles) {
            logger.debug("Removing deleted file from index: {}", deletedFile);
            removeFileFromIndex(deletedFile);
            indexedFiles.remove(deletedFile);
        }

        logger.info("Indexing complete. New: {}, Updated: {}, Deleted: {}", newIndexed, updated, deletedFiles.size());

        // Save to cache if there were changes
        if (newIndexed > 0 || updated > 0 || !deletedFiles.isEmpty()) {
            if (progressCallback != null) {
                progressCallback.onProgress("Saving cache...", totalSteps, totalSteps);
            }
            EmbeddingCacheService.saveCache(sessionId, embeddingStore, indexedFiles);
        }

        if (progressCallback != null) {
            if (totalSteps == 0) {
                progressCallback.onProgress("All files already indexed", 0, 0);
            } else {
                progressCallback.onProgress("Indexing complete", totalSteps, totalSteps);
            }
        }
    }

    /**
     * Index all documents from the knowledgebase (incremental) without callback
     */
    public void indexKnowledgebase(ResourceService resourceService) throws IOException {
        indexKnowledgebase(resourceService, null);
    }

    /**
     * Index a single file when added to knowledgebase
     */
    public void indexSingleFile(File file) throws IOException {
        String fileName = file.getName();
        long lastModified = file.lastModified();

        logger.debug("Indexing single file: {}", fileName);

        // Remove old segments if file exists
        if (indexedFiles.containsKey(fileName)) {
            removeFileFromIndex(fileName);
        }

        indexDocument(file);
        indexedFiles.put(fileName, lastModified);

        // Save to cache
        EmbeddingCacheService.saveCache(sessionId, embeddingStore, indexedFiles);
    }

    /**
     * Remove a file from the index when deleted from knowledgebase
     */
    public void removeFileFromIndexByName(String fileName) {
        logger.debug("Removing file from index: {}", fileName);

        removeFileFromIndex(fileName);
        indexedFiles.remove(fileName);

        // Save to cache
        EmbeddingCacheService.saveCache(sessionId, embeddingStore, indexedFiles);
    }

    /**
     * Remove all segments of a specific file from the index
     */
    private void removeFileFromIndex(String fileName) {
        // This is a workaround since InMemoryEmbeddingStore doesn't have a direct
        // remove by metadata
        // We'll need to rebuild the store without the file's segments

        List<EmbeddingMatch<TextSegment>> allEmbeddings = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(new Embedding(new float[1536])) // dummy embedding
                        .maxResults(Integer.MAX_VALUE)
                        .minScore(0.0)
                        .build())
                .matches();

        embeddingStore.removeAll();

        for (EmbeddingMatch<TextSegment> match : allEmbeddings) {
            TextSegment segment = match.embedded();
            if (segment.metadata() != null && segment.metadata().containsKey("fileName")) {
                String segmentFileName = segment.metadata().getString("fileName");
                if (!segmentFileName.equals(fileName)) {
                    embeddingStore.add(match.embedding(), segment);
                }
            }
        }
    }

    /**
     * Index a single document
     */
    private void indexDocument(File file) throws IOException {
        String content = Files.readString(file.toPath());

        // Create document with metadata
        Metadata metadata = new Metadata();
        metadata.put("fileName", file.getName());
        Document document = Document.from(content, metadata);

        // Split document into segments
        List<TextSegment> segments = recursive(500, 50).split(document);

        // Embed and store each segment with metadata
        for (TextSegment segment : segments) {
            // Ensure metadata is preserved in each segment
            Metadata segmentMetadata = new Metadata();
            segmentMetadata.put("fileName", file.getName());
            TextSegment segmentWithMetadata = TextSegment.from(segment.text(), segmentMetadata);

            Embedding embedding = embeddingModel.embed(segmentWithMetadata).content();
            embeddingStore.add(embedding, segmentWithMetadata);
        }
        logger.debug("Successfully indexed {} segments from {}", segments.size(), file.getName());
    }
}
