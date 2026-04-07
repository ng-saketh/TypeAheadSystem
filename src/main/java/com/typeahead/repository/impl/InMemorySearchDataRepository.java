package com.typeahead.repository.impl;

import com.typeahead.model.SearchData;
import com.typeahead.repository.SearchDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class InMemorySearchDataRepository implements SearchDataRepository {
    
    private final Map<String, SearchData> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(String query, long frequency, long timestamp) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        String normalizedQuery = query.toLowerCase().trim();
        
        store.merge(normalizedQuery, 
            SearchData.builder()
                .query(normalizedQuery)
                .frequency(frequency)
                .lastUpdatedAt(timestamp)
                .build(),
            (existing, incoming) -> {
                existing.setFrequency(existing.getFrequency() + incoming.getFrequency());
                existing.setLastUpdatedAt(Math.max(existing.getLastUpdatedAt(), incoming.getLastUpdatedAt()));
                return existing;
            });
    }

    @Override
    public List<SearchData> findAll() {
        return new ArrayList<>(store.values());
    }


    @Override
    public long count() {
        return store.size();
    }
}
