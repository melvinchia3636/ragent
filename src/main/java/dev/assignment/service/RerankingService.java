package dev.assignment.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

/**
 * Service for re-ranking search results to improve relevance
 * Implements hybrid scoring combining semantic and lexical matching
 */
public class RerankingService {

    private static final Logger logger = LogManager.getLogger(RerankingService.class);

    /**
     * Re-rank results using hybrid scoring approach
     * Combines embedding similarity, term frequency, position, and exact matching
     * 
     * @param query   The user query
     * @param results The initial search results from embedding store
     * @return Re-ranked list of results sorted by relevance
     */
    public List<EmbeddingMatch<TextSegment>> rerank(String query, List<EmbeddingMatch<TextSegment>> results) {
        if (results.isEmpty()) {
            return results;
        }

        // Tokenize query
        String[] queryTokens = tokenize(query.toLowerCase());

        // Calculate re-ranking scores
        List<ScoredMatch> scoredMatches = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : results) {
            String text = match.embedded().text().toLowerCase();
            double rerankScore = calculateRerankScore(queryTokens, text, match.score());
            scoredMatches.add(new ScoredMatch(match, rerankScore));
        }

        // Sort by re-rank score (descending)
        scoredMatches.sort((a, b) -> Double.compare(b.score, a.score));

        // Convert back to EmbeddingMatch list
        List<EmbeddingMatch<TextSegment>> reranked = new ArrayList<>();
        for (ScoredMatch scored : scoredMatches) {
            reranked.add(scored.match);
        }

        logger.debug("Re-ranked {} results", reranked.size());
        return reranked;
    }

    /**
     * Calculate re-ranking score based on multiple signals:
     * - Original embedding similarity score (60% weight)
     * - Term frequency (TF) overlap (30% weight)
     * - Position-based scoring (5% weight) - earlier matches score higher
     * - Exact phrase matching bonus (5% weight)
     * 
     * @param queryTokens    Tokenized query terms
     * @param text           The document text to score
     * @param embeddingScore Original embedding similarity score
     * @return Combined re-ranking score
     */
    private double calculateRerankScore(String[] queryTokens, String text, double embeddingScore) {
        double score = embeddingScore * 0.6; // Base score from embedding similarity

        String[] textTokens = tokenize(text);

        // Term frequency scoring
        int matchCount = 0;
        int totalQueryTerms = queryTokens.length;
        for (String queryToken : queryTokens) {
            for (String textToken : textTokens) {
                if (textToken.equals(queryToken)) {
                    matchCount++;
                    break;
                }
            }
        }
        double tfScore = totalQueryTerms > 0 ? (double) matchCount / totalQueryTerms : 0;
        score += tfScore * 0.3;

        // Position-based scoring (earlier appearance = higher relevance)
        int firstMatchPosition = findFirstMatchPosition(queryTokens, textTokens);
        if (firstMatchPosition >= 0) {
            double positionScore = 1.0 / (1.0 + Math.log(firstMatchPosition + 1));
            score += positionScore * 0.05;
        }

        // Exact phrase matching bonus
        String queryPhrase = String.join(" ", queryTokens);
        if (text.contains(queryPhrase)) {
            score += 0.05;
        }

        return score;
    }

    /**
     * Tokenize text into words (simple whitespace and punctuation split)
     * 
     * @param text Text to tokenize
     * @return Array of tokens
     */
    private String[] tokenize(String text) {
        return text.replaceAll("[^a-zA-Z0-9\\s]", " ")
                .trim()
                .split("\\s+");
    }

    /**
     * Find the position of the first query token match in text
     * 
     * @param queryTokens Query tokens to search for
     * @param textTokens  Text tokens to search in
     * @return Position of first match, or -1 if no match
     */
    private int findFirstMatchPosition(String[] queryTokens, String[] textTokens) {
        for (int i = 0; i < textTokens.length; i++) {
            for (String queryToken : queryTokens) {
                if (textTokens[i].equals(queryToken)) {
                    return i;
                }
            }
        }
        return -1;
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
