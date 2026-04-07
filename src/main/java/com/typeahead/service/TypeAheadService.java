package com.typeahead.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typeahead.manager.TrieManager;
import com.typeahead.repository.SearchDataRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
public class TypeAheadService {

    private final SearchDataRepository searchDataRepository;
    private final TrieManager trieManager;
    private final CompletionService completionService;

    public TypeAheadService(
        SearchDataRepository searchDataRepository,
        TrieManager trieManager,
        CompletionService completionService) {
        this.searchDataRepository = searchDataRepository;
        this.trieManager = trieManager;
        this.completionService = completionService;
    }

    /**
     * Initialize system with sample data
     */
    @PostConstruct
    public void initializeSampleData() {
        log.info("Initializing TypeAhead system with sample data...");

        String[] samplePhrases = getData();
        // String[] samplePhrases = getSamplePhrases();

        for (String phrase : samplePhrases) {
            try {
                long timestamp = System.currentTimeMillis();
                searchDataRepository.upsert(phrase, 1L, timestamp);
            } catch (Exception e) {
                log.error("Error ingesting sample phrase: {}", phrase, e);
            }
        }

        log.info("Sample data initialized with {} phrases", samplePhrases.length);
        rebuildTrie();
    }
    
    private String[] getSamplePhrases() {
        return new String[] {
            "python programming", "python tutorial", "python list", "python dictionary",
            "java", "java spring boot", "java collections",
            "javascript", "javascript async", "javascript array methods",
            "typescript", "typescript interfaces",
            "database", "database design", "database optimization",
            "aws", "aws lambda", "aws s3",
            "cloud computing", "cloud native",
            "docker", "docker containers", "kubernetes",
            "machine learning", "machine learning algorithms",
            "data science", "data structures", "algorithms",
            "system design", "microservices"
        };
    }

    public String[] getData(){
        try {
            Map<String, Integer> data = loadJson();
            String[] phrases = data.keySet().toArray(new String[0]);
            return phrases;
        } catch (Exception e) {
            log.error("Error loading JSON data", e);
            return new String[0];
        }
    }

    /**
     * Rebuild Trie from repository data
     */
    public void rebuildTrie() {
        try {
            trieManager.notifyObserversOnRebuildStart();
            
            var allData = searchDataRepository.findAll();
            if (!allData.isEmpty()) {
                var trie = new com.typeahead.trie.Trie(
                    new com.typeahead.strategy.FrequencyRanking());
                trie.rebuild(allData);
                trieManager.rebuild(trie);
            }
            
            trieManager.notifyObserversOnRebuildComplete();
            log.info("Trie rebuild completed");
        } catch (Exception e) {
            log.error("Error during trie rebuild", e);
        }
    }

    /**
     * Get service statistics
     */
    public Object getStatistics() {
        return Map.of(
            "totalPhrasesInStore", searchDataRepository.count(),
            "trieStats", trieManager.getStats(),
            "cacheStats", completionService.getCacheStats()
        );
    }

    public static Map<String, Integer> loadJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            
            String sampleDataPath = "static/words_dictionary.json";

            InputStream inputStream =
                new ClassPathResource(sampleDataPath).getInputStream();

            return mapper.readValue(
                inputStream,
                new TypeReference<Map<String, Integer>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON", e);
        }
    }
}
