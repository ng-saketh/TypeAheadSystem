package com.typeahead.cache;

import com.typeahead.model.ScoredSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LRU Cache decorator for Trie to cache frequently accessed prefixes
 * Maintains up to MAX_CACHE_SIZE entries with access-order eviction
 */
@Slf4j
@Component
public class PrefixLRUCache {

    private final int maxCacheSize;
    private final Map<String, List<ScoredSuggestion>> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public PrefixLRUCache(@Value("${typeahead.cache.maxSize:10000}") int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        // LinkedHashMap with access-order mode (true) for LRU behavior
        this.cache = new LinkedHashMap<String, List<ScoredSuggestion>>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<ScoredSuggestion>> eldest) {
                return size() > PrefixLRUCache.this.maxCacheSize;
            }
        };
    }

    /**
     * Get cached suggestions for a prefix
     * Returns null if not in cache (cache miss)
     */
    public List<ScoredSuggestion> get(String prefix) {
        lock.readLock().lock();
        try {
            List<ScoredSuggestion> result = cache.get(prefix);
            if (result != null) {
                log.debug("Cache HIT for prefix: {}", prefix);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Put suggestions into cache for a prefix
     */
    public void put(String prefix, List<ScoredSuggestion> suggestions) {
        lock.writeLock().lock();
        try {
            cache.put(prefix, new ArrayList<>(suggestions));
            log.debug("Cached {} suggestions for prefix: {}", suggestions.size(), prefix);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Invalidate all cache entries (called after trie rebuild)
     */
    public void invalidateAll() {
        lock.writeLock().lock();
        try {
            cache.clear();
            log.info("Prefix LRU cache invalidated completely");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getStats() {
        lock.readLock().lock();
        try {
            return Map.of(
                "cacheSize", cache.size(),
                "maxCacheSize", maxCacheSize
                        );
        } finally {
            lock.readLock().unlock();
        }
    }
}

