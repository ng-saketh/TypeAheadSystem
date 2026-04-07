package com.typeahead.strategy;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Ranking strategy based purely on search frequency
 * Higher frequency = higher score
 */
@Primary
@Component
public class FrequencyRanking implements RankingStrategy {

    @Override
    public double score(long frequency, long lastUpdatedAt) {
        return (double) frequency;
    }

    @Override
    public String getName() {
        return "FREQUENCY";
    }
}

