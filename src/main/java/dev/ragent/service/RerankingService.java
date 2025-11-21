package dev.ragent.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

/**
 * Service for re-ranking search results using RRF + MMR diversity
 * Implements Reciprocal Rank Fusion and Maximal Marginal Relevance
 */
public class RerankingService {

    private static final Logger logger = LogManager.getLogger(RerankingService.class);

    // RRF (Reciprocal Rank Fusion) constant
    private static final int RRF_K = 60;

    // MMR (Maximal Marginal Relevance) parameters
    private static final double MMR_LAMBDA = 0.85; // Increased relevance weight (was 0.7)
    private static final int MMR_LIMIT = 10; // Maximum results after diversity filtering

    // Weight for combining embedding score with RRF score
    private static final double EMBEDDING_WEIGHT = 0.6; // 60% embedding similarity
    private static final double RRF_WEIGHT = 0.4; // 40% rank position

    /**
     * Re-rank results using RRF (Reciprocal Rank Fusion) + MMR (Maximal Marginal
     * Relevance)
     * 
     * This implementation:
     * 1. Combines original embedding similarity scores with RRF position-based
     * scoring
     * 2. Applies MMR to diversify results and reduce redundancy
     * 
     * @param query   The user query (used for logging, diversity based on content
     *                similarity)
     * @param results The initial search results from embedding store
     * @return Re-ranked and diversified list of results
     */
    public List<EmbeddingMatch<TextSegment>> rerank(String query, List<EmbeddingMatch<TextSegment>> results) {
        if (results.isEmpty()) {
            return results;
        }

        logger.debug("Re-ranking {} results using combined scoring + MMR", results.size());

        // Step 1: Combine embedding similarity scores with RRF position-based scoring
        List<ScoredMatch> combinedScored = new ArrayList<>();

        // Normalize embedding scores to 0-1 range
        double maxEmbeddingScore = results.stream()
                .mapToDouble(EmbeddingMatch::score)
                .max()
                .orElse(1.0);

        for (int i = 0; i < results.size(); i++) {
            EmbeddingMatch<TextSegment> match = results.get(i);

            // Normalize embedding score
            double normalizedEmbeddingScore = match.score() / maxEmbeddingScore;

            // RRF formula: 1 / (k + rank)
            double rrfScore = 1.0 / (RRF_K + i + 1.0);

            // Normalize RRF score to 0-1 range
            double maxRRF = 1.0 / (RRF_K + 1.0);
            double normalizedRRFScore = rrfScore / maxRRF;

            // Combine both scores with weights
            double combinedScore = (EMBEDDING_WEIGHT * normalizedEmbeddingScore) +
                    (RRF_WEIGHT * normalizedRRFScore);

            combinedScored.add(new ScoredMatch(match, combinedScore));

            logger.trace("Result {}: embedding={}, rrf={}, combined={}",
                    i + 1, normalizedEmbeddingScore, normalizedRRFScore, combinedScore);
        }

        // Sort by combined score (descending)
        combinedScored.sort((a, b) -> Double.compare(b.score, a.score));

        logger.debug("Combined scoring complete, top score: {}",
                combinedScored.isEmpty() ? 0 : combinedScored.get(0).score);

        // Step 2: Apply MMR for diversity
        List<EmbeddingMatch<TextSegment>> diversified = applyMMR(combinedScored, MMR_LAMBDA,
                Math.min(MMR_LIMIT, combinedScored.size()));

        logger.debug("MMR diversification complete: {} results (from {} candidates)",
                diversified.size(), combinedScored.size());

        return diversified;
    }

    /**
     * Apply Maximal Marginal Relevance (MMR) to promote diversity
     * 
     * MMR Formula: λ × relevance - (1-λ) × max_redundancy
     * 
     * @param candidates List of scored candidates
     * @param lambda     Balance between relevance and diversity (0.0 = max
     *                   diversity, 1.0 = max relevance)
     * @param limit      Maximum number of results to return
     * @return Diversified list of results
     */
    private List<EmbeddingMatch<TextSegment>> applyMMR(List<ScoredMatch> candidates, double lambda, int limit) {
        List<ScoredMatch> selected = new ArrayList<>();
        List<ScoredMatch> remaining = new ArrayList<>(candidates);

        while (selected.size() < limit && !remaining.isEmpty()) {
            ScoredMatch best = null;
            double bestMMRScore = Double.NEGATIVE_INFINITY;

            for (ScoredMatch candidate : remaining) {
                double relevance = candidate.score;

                // Calculate maximum redundancy with already selected items
                double maxRedundancy = 0.0;
                for (ScoredMatch selectedItem : selected) {
                    double redundancy = calculateTokenOverlap(
                            candidate.match.embedded().text(),
                            selectedItem.match.embedded().text());
                    maxRedundancy = Math.max(maxRedundancy, redundancy);
                }

                // MMR score: balance relevance and diversity
                double mmrScore = lambda * relevance - (1.0 - lambda) * maxRedundancy;

                if (mmrScore > bestMMRScore) {
                    bestMMRScore = mmrScore;
                    best = candidate;
                }
            }

            if (best != null) {
                selected.add(best);
                remaining.remove(best);
            }
        }

        // Convert back to EmbeddingMatch list
        return selected.stream()
                .map(scored -> scored.match)
                .collect(Collectors.toList());
    }

    /**
     * Calculate token overlap between two text segments
     * Returns a normalized score (0.0 to 1.0) representing similarity
     * 
     * @param text1 First text segment
     * @param text2 Second text segment
     * @return Overlap score (0.0 = no overlap, 1.0 = complete overlap)
     */
    private double calculateTokenOverlap(String text1, String text2) {
        Set<String> tokens1 = tokenize(text1);
        Set<String> tokens2 = tokenize(text2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        // Calculate intersection
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        // Normalize by the smaller set size (Jaccard-like similarity)
        int minSize = Math.min(tokens1.size(), tokens2.size());
        return (double) intersection.size() / minSize;
    }

    /**
     * Tokenize text into unique words (simple whitespace and punctuation split)
     * 
     * @param text Text to tokenize
     * @return Set of unique tokens
     */
    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .trim()
                .split("\\s+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Helper class to hold match with re-rank score
     */
    private static class ScoredMatch {
        final EmbeddingMatch<TextSegment> match;
        final double score;

        ScoredMatch(EmbeddingMatch<TextSegment> match, double score) {
            this.match = match;
            this.score = score;
        }
    }
}
