package com.typeahead.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoredSuggestion {
    private String query;
    private long frequency;
    private long lastUpdatedAt;
    private double score;
}

