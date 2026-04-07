package com.typeahead.scheduler;

import com.typeahead.manager.TrieManager;
import com.typeahead.model.SearchData;
import com.typeahead.repository.SearchDataRepository;
import com.typeahead.strategy.RankingStrategy;
import com.typeahead.trie.Trie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Background scheduler for periodic Trie rebuilds
 * Runs every 15 minutes in a separate thread
 * Performs atomic swap with zero downtime
 */
@Slf4j
@Component
public class TrieRebuildScheduler {

    private final SearchDataRepository repository;
    private final TrieManager trieManager;
    private final RankingStrategy rankingStrategy;

    public TrieRebuildScheduler(
        SearchDataRepository repository,
        TrieManager trieManager,
        RankingStrategy rankingStrategy) {
        this.repository = repository;
        this.trieManager = trieManager;
        this.rankingStrategy = rankingStrategy;
    }

    /**
     * Scheduled rebuild task - runs every 15 minutes
     * Configured via application.properties: typeahead.rebuild.intervalMinutes
     */
    @Scheduled(fixedRateString = "${typeahead.rebuild.intervalMinutes:900000}", timeUnit = TimeUnit.MILLISECONDS)
    public void rebuildTrieTask() {
        long startTime = System.currentTimeMillis();
        log.info("Starting scheduled Trie rebuild...");

        try {
            // Notify observers that rebuild is starting
            trieManager.notifyObserversOnRebuildStart();

            // Read all accumulated data from repository
            List<SearchData> allData = repository.findAll();
            log.info("Read {} records from repository for rebuild", allData.size());

            if (allData.isEmpty()) {
                log.warn("No data to rebuild trie, skipping rebuild");
                trieManager.notifyObserversOnRebuildComplete();
                return;
            }

            // Build shadow trie (doesn't affect active trie or readers)
            Trie shadowTrie = new Trie(rankingStrategy);
            shadowTrie.rebuild(allData);

            // Atomic swap (zero-downtime update)
            trieManager.rebuild(shadowTrie);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Scheduled Trie rebuild completed successfully in {} ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Trie rebuild failed after {} ms. Keeping old trie active for graceful degradation", duration, e);
            // OLD TRIE REMAINS ACTIVE - GRACEFUL DEGRADATION
        }
    }
}


