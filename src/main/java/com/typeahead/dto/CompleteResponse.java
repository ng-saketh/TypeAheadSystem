package com.typeahead.dto;

import com.typeahead.model.ScoredSuggestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteResponse {
    private String prefix;
    private List<ScoredSuggestion> results;
    private long timestamp;
    private int resultCount;

    public CompleteResponse(String prefix, List<ScoredSuggestion> results) {
        this.prefix = prefix;
        this.results = results;
        this.resultCount = results.size();
        this.timestamp = System.currentTimeMillis();
    }
}
