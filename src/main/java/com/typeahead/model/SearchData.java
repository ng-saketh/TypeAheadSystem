package com.typeahead.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchData {
    private static final long DEFAULT_FREQUENCY = 1L;
    
    private String query;
    private long frequency;
    private long lastUpdatedAt;

    public SearchData(String query) {
        this(query, DEFAULT_FREQUENCY);
    }

    public SearchData(String query, long frequency) {
        this.query = query;
        this.frequency = frequency;
        this.lastUpdatedAt = System.currentTimeMillis();
    }
}
