package com.typeahead.repository;

import com.typeahead.model.SearchData;
import java.util.List;

public interface SearchDataRepository {
    
    /**
     * Upsert a search record (update if exists, insert if not)
     */
    void upsert(String query, long frequency, long timestamp);
    
    /**
     * Get all search data records
     */
    List<SearchData> findAll();
    
    /**
     * Get count of records
     */
    long count();
}


