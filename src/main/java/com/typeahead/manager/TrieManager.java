package com.typeahead.manager;

import com.typeahead.strategy.RankingStrategy;
import com.typeahead.trie.Trie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton manager for the active Trie instance
 * Handles atomic trie swaps for zero-downtime rebuilds
 * Manages observers for rebuild events
 */
@Slf4j
@Component
public class TrieManager {

    private final AtomicReference<Trie> activeTrie;
    private final List<RebuildObserver> observers = new ArrayList<>();
    private final RankingStrategy rankingStrategy;

    public TrieManager(RankingStrategy rankingStrategy) {
        this.rankingStrategy = rankingStrategy;
        // Initialize with empty trie
        this.activeTrie = new AtomicReference<>(new Trie(rankingStrategy));
    }

    /**
     * Get the currently active trie
     * Thread-safe and atomic - always returns a consistent trie instance
     */
    public Trie getActiveTrie() {
        return activeTrie.get();
    }

    /**
     * Atomically swap to a new trie (zero-downtime rebuild)
     * Notifies all observers after swap
     */
    public void rebuild(Trie newTrie) {
        log.info("Starting atomic trie swap...");
        activeTrie.getAndSet(newTrie);
        log.info("Atomic trie swap completed. Old trie released for garbage collection");
        
        // Notify observers after swap
        notifyObserversOnRebuildComplete();
    }

    /**
     * Register an observer for rebuild events
     */
    public void registerObserver(RebuildObserver observer) {
        observers.add(observer);
        log.debug("Registered observer: {}", observer.getClass().getSimpleName());
    }

    /**
     * Notify all observers about rebuild starting
     */
    public void notifyObserversOnRebuildStart() {
        notifyObservers(observer -> observer.onRebuildStart(), "rebuild start");
    }

    /**
     * Notify all observers about rebuild completion
     */
    public void notifyObserversOnRebuildComplete() {
        notifyObservers(observer -> observer.onRebuildComplete(), "rebuild complete");
    }
    
    private void notifyObservers(java.util.function.Consumer<RebuildObserver> action, String event) {
        for (RebuildObserver observer : observers) {
            try {
                action.accept(observer);
            } catch (Exception e) {
                log.error("Observer {} failed on {}", observer.getClass().getSimpleName(), event, e);
            }
        }
    }

    /**
     * Get trie statistics
     */
    public Map<String, Object> getStats() {
        Trie trie = getActiveTrie();
        return trie.getStats();
    }

    /**
     * Observer interface for rebuild events
     */
    public interface RebuildObserver {
        void onRebuildStart();
        void onRebuildComplete();
    }
}

