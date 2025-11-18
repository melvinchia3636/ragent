package dev.assignment.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Service for managing persistent embedding cache
 */
public class EmbeddingCacheService {

    private static final Logger logger = LogManager.getLogger(EmbeddingCacheService.class);
    private static final String EMBEDDINGS_DIR = "embeddings_cache";

    /**
     * Get the cache file path for a session
     */
    private static File getCacheFile(String sessionId) {
        File cacheDir = new File(EMBEDDINGS_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return new File(cacheDir, sessionId + "_embeddings.cache");
    }

    /**
     * Load cached embeddings from disk
     */
    @SuppressWarnings("unchecked")
    public static void loadCache(String sessionId, EmbeddingStore<TextSegment> embeddingStore,
            Map<String, Long> indexedFiles) {
        File cacheFile = getCacheFile(sessionId);
        if (!cacheFile.exists()) {
            logger.debug("No cached embeddings found for session {}", sessionId);
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
            // Read indexed files map
            Map<String, Long> cachedFiles = (Map<String, Long>) ois.readObject();
            indexedFiles.putAll(cachedFiles);

            // Read embeddings count
            int count = ois.readInt();

            // Read each embedding and segment
            for (int i = 0; i < count; i++) {
                float[] embeddingVector = (float[]) ois.readObject();
                String segmentText = (String) ois.readObject();
                Map<String, String> metadataMap = (Map<String, String>) ois.readObject();

                // Reconstruct embedding and segment
                Embedding embedding = new Embedding(embeddingVector);
                Metadata metadata = new Metadata();
                metadataMap.forEach(metadata::put);
                TextSegment segment = TextSegment.from(segmentText, metadata);

                embeddingStore.add(embedding, segment);
            }

            logger.info("Loaded {} cached embeddings for {} files", count, indexedFiles.size());
        } catch (Exception e) {
            logger.error("Failed to load cached embeddings: {}", e.getMessage(), e);
            indexedFiles.clear();
        }
    }

    /**
     * Save embeddings to disk
     */
    public static void saveCache(String sessionId, EmbeddingStore<TextSegment> embeddingStore,
            Map<String, Long> indexedFiles) {
        File cacheFile = getCacheFile(sessionId);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
            // Write indexed files map
            oos.writeObject(indexedFiles);

            // Get all embeddings from store
            List<EmbeddingMatch<TextSegment>> allEmbeddings = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(new Embedding(new float[1536])) // dummy embedding
                            .maxResults(Integer.MAX_VALUE)
                            .minScore(0.0)
                            .build())
                    .matches();

            // Write count
            oos.writeInt(allEmbeddings.size());

            // Write each embedding and segment
            for (EmbeddingMatch<TextSegment> match : allEmbeddings) {
                oos.writeObject(match.embedding().vector());
                oos.writeObject(match.embedded().text());

                // Convert metadata to serializable map
                Map<String, String> metadataMap = new HashMap<>();
                if (match.embedded().metadata() != null) {
                    match.embedded().metadata().toMap().forEach((k, v) -> metadataMap.put(k, v.toString()));
                }
                oos.writeObject(metadataMap);
            }

            logger.info("Saved {} embeddings to cache", allEmbeddings.size());
        } catch (Exception e) {
            logger.error("Failed to save cached embeddings: {}", e.getMessage(), e);
        }
    }

    /**
     * Delete the embedding cache for a session
     */
    public static void deleteCache(String sessionId) {
        File cacheFile = getCacheFile(sessionId);
        if (cacheFile.exists()) {
            if (cacheFile.delete()) {
                logger.info("Deleted embedding cache for session {}", sessionId);
            } else {
                logger.warn("Failed to delete embedding cache for session {}", sessionId);
            }
        }
    }
}
