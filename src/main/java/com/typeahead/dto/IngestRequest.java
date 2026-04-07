package com.typeahead.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestRequest {
    private String query;
    private Long frequency;
    private String userId;

    public Long getFrequency() {
        return frequency != null ? frequency : 1L;
    }
}
