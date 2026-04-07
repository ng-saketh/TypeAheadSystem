package com.typeahead.service;

import com.typeahead.cache.PrefixLRUCache;
import com.typeahead.limiter.RateLimiter;
import com.typeahead.manager.TrieManager;
import com.typeahead.model.ScoredSuggestion;
import com.typeahead.validator.InputValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for handling autocomplete requests (read path)
 * Checks cache first, then queries trie, and caches results
 */
@Slf4j
@Service
public class CompletionService {
    private static final int DEFAULT_K = 5;

    private final TrieManager trieManager;
    private final PrefixLRUCache cache;
    private final InputValidator validator;
    private final RateLimiter rateLimiter;

    public CompletionService(
        TrieManager trieManager,
        PrefixLRUCache cache,
        InputValidator validator,
        RateLimiter rateLimiter) {
        this.trieManager = trieManager;
        this.cache = cache;
        this.validator = validator;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Get autocomplete suggestions for a prefix
     *
     * @param prefix The search prefix
     * @param k Number of suggestions (will be capped at 10)
     * @param userId User identifier for rate limiting
     * @return List of top-K scored suggestions
     * @throws Exception if validation or rate limiting fails
     */
    public List<ScoredSuggestion> complete(String prefix, int k, String userId) throws Exception {
        // Rate limit check
        rateLimiter.checkOrThrow(userId);

        // Validate and normalize prefix
        String normalizedPrefix = validator.validateAndNormalizePrefix(prefix);
        int effectiveK = validator.validateK(k);

        // Check cache first (O(1) hit)
        List<ScoredSuggestion> cached = cache.get(normalizedPrefix);
        if (cached != null) {
            log.debug("Cache HIT for prefix: '{}', returning {} suggestions", normalizedPrefix, cached.size());
            return cached.stream().limit(effectiveK).toList();
        }

        // Cache miss: query trie
        log.debug("Cache MISS for prefix: '{}', querying trie", normalizedPrefix);
        List<ScoredSuggestion> results = trieManager.getActiveTrie().complete(normalizedPrefix, effectiveK);

        // Store in cache for future requests
        if (!results.isEmpty()) {
            cache.put(normalizedPrefix, results);
        }

        return results;
    }

    /**
     * Get autocomplete suggestions with default K=5
     */
    public List<ScoredSuggestion> complete(String prefix, String userId) throws Exception {
        return complete(prefix, DEFAULT_K, userId);
    }

    /**
     * Get cache statistics
     */
    public Object getCacheStats() {
        return cache.getStats();
    }
}

