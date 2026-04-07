package com.typeahead.observer;

import com.typeahead.cache.PrefixLRUCache;
import com.typeahead.manager.TrieManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Observer that invalidates the LRU cache whenever trie is rebuilt
 * Ensures cache stays consistent with trie data
 */
@Slf4j
@Component
public class CacheInvalidationObserver implements TrieManager.RebuildObserver {

    private final PrefixLRUCache cache;
    private final TrieManager trieManager;

    public CacheInvalidationObserver(PrefixLRUCache cache, TrieManager trieManager) {
        this.cache = cache;
        this.trieManager = trieManager;
    }

    @PostConstruct
    public void init() {
        // Register this observer with trie manager
        trieManager.registerObserver(this);
        log.info("CacheInvalidationObserver registered with TrieManager");
    }

    @Override
    public void onRebuildStart() {
        log.debug("Trie rebuild started, cache will be invalidated on completion");
    }

    @Override
    public void onRebuildComplete() {
        try {
            cache.invalidateAll();
            log.info("Cache invalidated after trie rebuild");
        } catch (Exception e) {
            log.error("Error invalidating cache", e);
        }
    }
}

