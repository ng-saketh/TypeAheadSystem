package com.typeahead.trie;

import com.typeahead.model.SearchData;
import com.typeahead.model.ScoredSuggestion;
import com.typeahead.model.TrieNode;
import com.typeahead.strategy.RankingStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Trie (Prefix Tree) data structure for efficient prefix-based searching.
 * 
 * Key Features:
 * - O(m) time complexity for search where m = prefix length
 * - Maintains top-K suggestions at each node for instant retrieval
 * - Case-insensitive matching
 * - Frequency-based and recency-weighted ranking via strategy pattern
 * - Zero-downtime rebuild capability
 * 
 * Time Complexity:
 * - rebuild(): O(n*m*k*log(k)) where n = phrases, m = avg length, k = top-k
 * - complete(): O(m) to find node + O(1) to get cached results
 * - insert(): O(m) where m = phrase length
 * 
 * Space Complexity: O(n*m) for trie structure + O(k) top suggestions per node
 */
@Slf4j
public class Trie {
    private static final int MAX_K = 10; // Maximum suggestions per node
    
    private TrieNode root;
    private final RankingStrategy rankingStrategy;
    private int totalPhrases = 0;
    
    /**
     * Initialize a Trie with a specific ranking strategy
     * 
     * @param rankingStrategy Strategy for scoring suggestions (Frequency, RecencyWeighted, etc)
     */
    public Trie(RankingStrategy rankingStrategy) {
        this.root = new TrieNode();
        this.rankingStrategy = rankingStrategy;
        log.debug("Initialized Trie with ranking strategy: {}", rankingStrategy.getName());
    }
    
    /**
     * Rebuild the entire Trie from a list of search data
     * Clears existing trie and rebuilds with fresh data
     * Builds top-K cache at each node for O(1) suggestion retrieval
     * 
     * Time Complexity: O(n*m*k*log(k))
     * - n = number of phrases
     * - m = average phrase length
     * - k*log(k) = heap operations for top-K selection
     * 
     * @param dataList List of SearchData containing all phrases, frequencies, and timestamps
     */
    public void rebuild(List<SearchData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.warn("Empty data list provided for rebuild");
            return;
        }
        
        log.info("Starting Trie rebuild with {} records", dataList.size());
        long startTime = System.currentTimeMillis();
        
        // Clear existing trie
        this.root = new TrieNode();
        this.totalPhrases = 0;
        
        // Insert all phrases into trie
        for (SearchData data : dataList) {
            insert(data);
        }
        
        // Build top-K cache at each node (post-processing for efficiency)
        buildTopKCacheRecursive(root);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Trie rebuild completed: {} phrases, {} nodes, {} ms", 
                 totalPhrases, countNodes(root), duration);
    }
    
    /**
     * Insert a single phrase into the Trie
     * 
     * Time Complexity: O(m) where m = phrase length
     * 
     * @param data SearchData containing query, frequency, and timestamp
     */
    private void insert(SearchData data) {
        if (data == null || data.getQuery() == null || data.getQuery().isEmpty()) {
            return;
        }
        
        String query = data.getQuery().toLowerCase();
        insertRecursive(root, query, 0, data.getFrequency(), data.getLastUpdatedAt());
        totalPhrases++;
    }
    
    /**
     * Recursive helper for insert operation
     * Traverses/creates path for each character in the phrase
     * 
     * @param node Current node
     * @param query The query string (normalized to lowercase)
     * @param index Current character index
     * @param frequency Search frequency for this phrase
     * @param timestamp Last updated timestamp
     */
    private void insertRecursive(TrieNode node, String query, int index, long frequency, long timestamp) {
        if (index == query.length()) {
            // Mark as end of word and store metadata
            node.setEndOfWord(true);
            node.setQuery(query);
            node.setFrequency(frequency);
            node.setLastUpdatedAt(timestamp);
            return;
        }
        
        char ch = query.charAt(index);
        TrieNode child = node.getOrCreateChild(ch);
        insertRecursive(child, query, index + 1, frequency, timestamp);
    }
    
    /**
     * Search for phrases matching a given prefix
     * Returns pre-computed top-K suggestions from the prefix node
     * 
     * Time Complexity: O(m + k)
     * - O(m) to find the prefix node
     * - O(1) to retrieve cached suggestions
     * - k ≤ MAX_K suggestions returned
     * 
     * @param prefix The search prefix (case-insensitive, will be normalized)
     * @param k Maximum number of suggestions to return
     * @return List of top-k ScoredSuggestion objects, sorted by score descending
     */
    public List<ScoredSuggestion> complete(String prefix, int k) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }
        
        String normalizedPrefix = prefix.toLowerCase();
        TrieNode prefixNode = findNode(root, normalizedPrefix, 0);
        
        if (prefixNode == null) {
            log.debug("No results found for prefix: '{}'", prefix);
            return Collections.emptyList();
        }
        
        // Get cached top suggestions and limit to k
        List<ScoredSuggestion> results = prefixNode.getTopSuggestions(Math.min(k, MAX_K));
        log.debug("Found {} suggestions for prefix: '{}' (requested k={})", results.size(), prefix, k);
        
        return results;
    }
    
    /**
     * Find the TrieNode corresponding to a given prefix
     * Returns null if prefix doesn't exist
     * 
     * Time Complexity: O(m) where m = prefix length
     * 
     * @param node Current node
     * @param prefix The prefix to search
     * @param index Current character index in prefix
     * @return TrieNode for the prefix or null if not found
     */
    private TrieNode findNode(TrieNode node, String prefix, int index) {
        if (index == prefix.length()) {
            return node;
        }
        
        char ch = prefix.charAt(index);
        TrieNode child = node.getChild(ch);
        
        if (child == null) {
            return null;
        }
        
        return findNode(child, prefix, index + 1);
    }
    
    /**
     * Build top-K cache recursively for all nodes in the trie
     * This is called after all phrases are inserted
     * Each node caches the top-K suggestions reachable from that prefix
     * 
     * Time Complexity: O(n*k*log(k))
     * - n = total nodes
     * - k*log(k) = heap operations for each node
     * 
     * @param node Current node being processed
     */
    private void buildTopKCacheRecursive(TrieNode node) {
        if (node == null) {
            return;
        }
        
        // Recursively build cache for all children first
        for (TrieNode child : node.getChildren().values()) {
            buildTopKCacheRecursive(child);
        }
        
        // Collect all suggestions reachable from this node
        List<ScoredSuggestion> allSuggestions = new ArrayList<>();
        
        // If this node is end of word, add it as a suggestion
        if (node.isEndOfWord()) {
            String query = reconstructQuery(node);
            if (query != null) {
                ScoredSuggestion scored = ScoredSuggestion.builder()
                    .query(query)
                    .frequency(node.getFrequency())
                    .lastUpdatedAt(node.getLastUpdatedAt())
                    .score(rankingStrategy.score(node.getFrequency(), node.getLastUpdatedAt()))
                    .build();
                allSuggestions.add(scored);
            }
        }
        
        // Collect top suggestions from all children
        for (TrieNode child : node.getChildren().values()) {
            allSuggestions.addAll(child.getTopSuggestions());
        }
        
        // Use min-heap to efficiently select top-K
        PriorityQueue<ScoredSuggestion> minHeap = new PriorityQueue<>(
            Comparator.comparingDouble(ScoredSuggestion::getScore)
        );
        
        for (ScoredSuggestion suggestion : allSuggestions) {
            minHeap.offer(suggestion);
            if (minHeap.size() > MAX_K) {
                minHeap.poll();
            }
        }
        
        // Extract from heap and sort by score descending
        List<ScoredSuggestion> topK = new ArrayList<>(minHeap);
        topK.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        node.setTopSuggestions(topK);
    }
    
    /**
     * Reconstruct query string from a node
     * Returns the stored query for nodes marked as end of word
     * 
     * @param node The node to reconstruct from
     * @return The query string or null if node is not end of word
     */
    private String reconstructQuery(TrieNode node) {
        return node.getQuery();
    }
    
    /**
     * Get statistics about the Trie
     * Used for monitoring and debugging
     * 
     * @return Map containing trie statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPhrases", totalPhrases);
        stats.put("totalNodes", countNodes(root));
        stats.put("maxSuggestions", MAX_K);
        stats.put("rankingStrategy", rankingStrategy.getName());
        stats.put("rootChildren", root.getChildrenCount());
        return stats;
    }
    
    /**
     * Count total nodes in the Trie (for statistics)
     * 
     * @param node Current node
     * @return Total number of nodes in subtree
     */
    private int countNodes(TrieNode node) {
        if (node == null) {
            return 0;
        }
        
        int count = 1;
        for (TrieNode child : node.getChildren().values()) {
            count += countNodes(child);
        }
        return count;
    }
    
    /**
     * Get the root node of the Trie
     * Used by tests and debugging
     * 
     * @return The root TrieNode
     */
    public TrieNode getRoot() {
        return root;
    }
    
    /**
     * Check if a phrase exists in the Trie
     * 
     * @param query The query to check
     * @return true if phrase exists and is marked as end-of-word
     */
    public boolean contains(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        
        String normalized = query.toLowerCase();
        TrieNode node = findNode(root, normalized, 0);
        return node != null && node.isEndOfWord();
    }
}
