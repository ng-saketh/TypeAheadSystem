package com.typeahead.strategy;

/**
 * Strategy interface for ranking search results
 */
public interface RankingStrategy {
    /**
     * Calculate a score based on frequency and recency
     *
     * @param frequency Total number of searches
     * @param lastUpdatedAt Timestamp of last search in milliseconds
     * @return Computed ranking score
     */
    double score(long frequency, long lastUpdatedAt);

    /**
     * Get strategy name for monitoring/logging
     */
    String getName();
}

