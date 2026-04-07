package com.typeahead.strategy;

import org.springframework.stereotype.Component;

/**
 * Ranking strategy combining frequency with recency weighting
 * More recent searches get a higher boost
 */
@Component
public class RecencyWeightedRanking implements RankingStrategy {
    private static final long HALF_LIFE_MS = 7L * 24 * 60 * 60 * 1000;  // 7 days
    private static final double FREQUENCY_WEIGHT = 0.8;
    private static final double RECENCY_WEIGHT = 0.2;
    private static final double RECENCY_SCALE = 100.0;

    @Override
    public double score(long frequency, long lastUpdatedAt) {
        long now = System.currentTimeMillis();
        long timeSinceLastSearch = Math.max(0, now - lastUpdatedAt);

        // Calculate recency factor: decay exponentially with half-life of 7 days
        // score = frequency * 2^(-timeSinceLastSearch / HALF_LIFE)
        double recencyFactor = Math.pow(2.0, -(double) timeSinceLastSearch / HALF_LIFE_MS);

        // Weighted combination: 80% frequency, 20% recency
        return frequency * FREQUENCY_WEIGHT + (recencyFactor * RECENCY_SCALE) * RECENCY_WEIGHT;
    }

    @Override
    public String getName() {
        return "RECENCY_WEIGHTED";
    }
}

