package com.typeahead.service;

import com.typeahead.limiter.RateLimiter;
import com.typeahead.repository.SearchDataRepository;
import com.typeahead.validator.InputValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling search query ingestion (write path)
 * Validates, normalizes, and persists user search queries
 */
@Slf4j
@Service
public class IngestionService {
    private static final long DEFAULT_FREQUENCY = 1L;

    private final SearchDataRepository repository;
    private final InputValidator validator;
    private final RateLimiter rateLimiter;

    public IngestionService(
        SearchDataRepository repository,
        InputValidator validator,
        RateLimiter rateLimiter) {
        this.repository = repository;
        this.validator = validator;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Ingest a search query from a user
     *
     * @param query The search query
     * @param frequency How many times searched (default 1)
     * @param userId User identifier for rate limiting
     * @throws Exception if validation or rate limiting fails
     */
    public void ingest(String query, long frequency, String userId) throws Exception {
        // Rate limit check
        rateLimiter.checkOrThrow(userId);

        // Validate and normalize query
        String normalizedQuery = validator.validateAndNormalizeQuery(query);
        validator.validateFrequency(frequency);

        // Upsert into repository
        long timestamp = System.currentTimeMillis();
        repository.upsert(normalizedQuery, frequency, timestamp);

        log.debug("Ingested query: '{}' with frequency: {} by user: {}", normalizedQuery, frequency, userId);
    }

    /**
     * Ingest with default frequency of 1
     */
    public void ingest(String query, String userId) throws Exception {
        ingest(query, DEFAULT_FREQUENCY, userId);
    }
}

