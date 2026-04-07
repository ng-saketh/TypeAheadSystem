package com.typeahead.model;

import java.util.*;

/**
 * Represents a single node in the Trie data structure
 * 
 * Properties:
 * - children: Map of character to child nodes for efficient O(1) lookups
 * - isEndOfWord: Marks if this node represents the end of a complete phrase
 * - frequency: Search count for the phrase represented by this node
 * - lastUpdatedAt: Timestamp of last search (milliseconds)
 * - topSuggestions: Pre-computed top-K suggestions reachable from this prefix
 * 
 * The topSuggestions cache enables O(1) retrieval of best matches during search
 */
public class TrieNode {
    
    // Child nodes mapped by character for O(1) lookup
    private final Map<Character, TrieNode> children;
    
    // Flag indicating if this node marks the end of a complete word/phrase
    private boolean isEndOfWord;
    
    // Search frequency for the phrase ending at this node
    private long frequency;
    
    // Timestamp when this phrase was last searched (milliseconds since epoch)
    private long lastUpdatedAt;
    
    // Pre-computed top-K suggestions cached at this node for O(1) retrieval
    private List<ScoredSuggestion> topSuggestions;
    
    // The actual query string for this node (set when isEndOfWord is true)
    private String query;
    
    /**
     * Default constructor initializes an empty TrieNode
     */
    public TrieNode() {
        this.children = new HashMap<>();
        this.isEndOfWord = false;
        this.frequency = 0L;
        this.lastUpdatedAt = 0L;
        this.topSuggestions = new ArrayList<>();
        this.query = null;
    }
    
    /**
     * Get or create a child node for the given character
     * If child doesn't exist, creates a new TrieNode
     * 
     * @param ch The character
     * @return The child TrieNode (existing or newly created)
     */
    public TrieNode getOrCreateChild(char ch) {
        return children.computeIfAbsent(ch, k -> new TrieNode());
    }
    
    /**
     * Get an existing child node for the given character
     * 
     * @param ch The character
     * @return The child TrieNode or null if not found
     */
    public TrieNode getChild(char ch) {
        return children.get(ch);
    }
    
    /**
     * Get all child nodes
     * 
     * @return Map of character to TrieNode children
     */
    public Map<Character, TrieNode> getChildren() {
        return children;
    }
    
    /**
     * Check if this node marks end of a word
     * 
     * @return true if end of word, false otherwise
     */
    public boolean isEndOfWord() {
        return isEndOfWord;
    }
    
    /**
     * Set whether this node marks end of a word
     * 
     * @param endOfWord true to mark as end of word
     */
    public void setEndOfWord(boolean endOfWord) {
        isEndOfWord = endOfWord;
    }
    
    /**
     * Get the search frequency for this phrase
     * 
     * @return Frequency count
     */
    public long getFrequency() {
        return frequency;
    }
    
    /**
     * Set the search frequency
     * 
     * @param frequency The frequency count
     */
    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }
    
    /**
     * Increment frequency by the given amount
     * 
     * @param amount Amount to increment
     */
    public void incrementFrequency(long amount) {
        this.frequency += amount;
    }
    
    /**
     * Get the last updated timestamp
     * 
     * @return Timestamp in milliseconds
     */
    public long getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    /**
     * Set the last updated timestamp
     * 
     * @param lastUpdatedAt Timestamp in milliseconds
     */
    public void setLastUpdatedAt(long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    /**
     * Get all top suggestions cached at this node
     * 
     * @return List of top-K suggestions, sorted by score descending
     */
    public List<ScoredSuggestion> getTopSuggestions() {
        return new ArrayList<>(topSuggestions);
    }
    
    /**
     * Get top suggestions limited to k
     * 
     * @param k Maximum number of suggestions to return
     * @return List of top-k suggestions
     */
    public List<ScoredSuggestion> getTopSuggestions(int k) {
        return topSuggestions.stream()
            .limit(k)
            .toList();
    }
    
    /**
     * Set the top suggestions for this node
     * Typically called during top-K cache building
     * 
     * @param suggestions List of ScoredSuggestion objects
     */
    public void setTopSuggestions(List<ScoredSuggestion> suggestions) {
        this.topSuggestions = new ArrayList<>(suggestions);
    }
    
    /**
     * Get the number of child nodes
     * 
     * @return Count of children
     */
    public int getChildrenCount() {
        return children.size();
    }
    
    /**
     * Get the query string represented by this node
     * 
     * @return The query string or null if not end of word
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * Set the query string for this node
     * Should be called when marking as end of word
     * 
     * @param query The query string
     */
    public void setQuery(String query) {
        this.query = query;
    }
    
    @Override
    public String toString() {
        return "TrieNode{" +
                "isEndOfWord=" + isEndOfWord +
                ", frequency=" + frequency +
                ", childrenCount=" + children.size() +
                ", topSuggestionsCount=" + topSuggestions.size() +
                '}';
    }
}

