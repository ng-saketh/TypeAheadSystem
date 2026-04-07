# Project Summary - TypeAhead System

## Overview

A production-ready TypeAhead search system built  using a **Trie data structure** for efficient prefix-based searching.

**Key Technologies**:
- Spring Boot 3.1.5
- Trie Data Structure
- Concurrent HashMap (Thread-safe)
- Scheduled Tasks (every 15 minutes)
- REST APIs
- Lombok (for reducing boilerplate)
- Maven (build management)

---

## Project Structure

```
TypeAheadSystem/
│
├── pom.xml                                  # Maven dependencies and build configuration
├── .gitignore                               # Git ignore rules
│
├── README.md                                # Comprehensive documentation
├── QUICK_START.md                           # Quick start guide
├── DATABASE_INTEGRATION_GUIDE.md            # How to switch to database backends
├── API_EXAMPLES.md                          # Detailed API examples and use cases
├── PROJECT_SUMMARY.md                       # This file
│
└── src/
    ├── main/
    │   ├── java/com/typeahead/
    │   │   │
    │   │   ├── TypeAheadApplication.java    # Main Spring Boot application entry point
    │   │   │                                # ⚙️ Enables @EnableScheduling
    │   │   │
    │   │   ├── controller/
    │   │   │   └── TypeAheadController.java # REST API endpoints
    │   │   │                                # Endpoints: POST /ingest, GET /complete
    │   │   │                                # Also: GET /stats, POST /rebuild
    │   │   │
    │   │   ├── service/
    │   │   │   └── TypeAheadService.java    # Business logic orchestration
    │   │   │                                # Manages: Trie operations, ingestion
    │   │   │                                # Scheduled: Trie rebuild every 15 mins
    │   │   │                                # Initializes: Sample data (30 phrases)
    │   │   │
    │   │   ├── trie/
    │   │   │   └── Trie.java                # Core Trie implementation
    │   │   │                                # Operations: insert, search, rebuild, clear
    │   │   │                                # Time Complexity: O(m) for both insert/search
    │   │   │                                # Top-K results maintained at each node
    │   │   │
    │   │   ├── model/
    │   │   │   ├── SearchData.java          # Entity: phrase + frequency
    │   │   │   │                            # Represents search data
    │   │   │   └── TrieNode.java            # Trie node structure
    │   │   │                                # Contains: char map, top searches, EOW flag
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── IngestRequest.java       # Request DTO: { phrase }
    │   │   │   └── CompleteResponse.java    # Response DTO: { prefix, suggestions }
    │   │   │
    │   │   └── repository/
    │   │       ├── SearchDataRepository.java     # Interface (abstraction)
    │   │       │                                 # 8 methods for data access
    │   │       │                                 # Enables easy DB migration
    │   │       │
    │   │       └── impl/
    │   │           └── InMemorySearchDataRepository.java
    │   │                                         # Current: ConcurrentHashMap
    │   │                                         # Thread-safe implementation
    │   │                                         # Auto-increment frequency
    │   │
    │   └── resources/
    │       └── application.properties       # Configuration
    │                                        # Port: 8080
    │                                        # Max suggestions: 5
    │                                        # Rebuild interval: 15 minutes
    │
    └── test/
        └── java/com/typeahead/
            └── TrieTest.java                # Unit tests (10+ test cases)
                                             # Tests: insert, search, rebuild, frequency
                                             # Tests: case sensitivity, top-k limiting
```

---

## Component Details

### 1. Core Data Structures

#### `SearchData` (model/SearchData.java)
- Represents a searchable phrase
- Properties: `phrase` (String), `frequency` (Long)
- Used throughout the system

#### `TrieNode` (model/TrieNode.java)
- Represents a node in the Trie
- Properties:
    - `children`: Map<Character, TrieNode> - branch map
    - `topSearches`: List<SearchData> - top K results
    - `isEndOfWord`: boolean - marks complete phrase

### 2. Trie Implementation

#### `Trie` (trie/Trie.java)
```
insert(SearchData)     - O(m) where m = phrase length
search(prefix)         - O(m + k) where k = suggestions count
rebuild(data)          - O(n*m) where n = total phrases
clear()                - O(1)
getAllPhrases()        - O(n*m)
getStats()             - O(1) metadata
```

**Key Features**:
- Maintains top K results at each node
- Automatic sorting by frequency
- Prefix-based search
- Case-insensitive matching

### 3. Repository Pattern

#### `SearchDataRepository` (repository/SearchDataRepository.java)
Interface with 6 methods:
- `save(SearchData)` - Create or increment frequency
- `findByPhrase(String)` - Find existing data
- `findAll()` - Get all data
- `updateFrequency(String, long)` - Update frequency
- `clear()` - Clear all data
- `count()` - Get data count

**Benefit**: Zero dependency on storage implementation!

#### `InMemorySearchDataRepository` (repository/impl/InMemorySearchDataRepository.java)
- Current implementation using ConcurrentHashMap
- Thread-safe operations
- Auto-increment frequency
- Easy to test and debug

### 4. Services & Controllers

#### `TypeAheadService` (service/TypeAheadService.java)
```java
@Service
public class TypeAheadService {
    @PostConstruct
    void initializeSampleData()      // Load 30 sample phrases on startup
    
    SearchData ingestPhrase()        // Add/update a phrase
    List<SearchData> getSuggestions()// Get top K for prefix
    
    @Scheduled(fixedRateString)
    void rebuildTrie()               // Auto-rebuild every 15 minutes
    
    Object getStatistics()           // Return system stats
    void manualRebuild()             // Manual rebuild trigger
}
```

#### `TypeAheadController` (controller/TypeAheadController.java)
4 REST endpoints:
1. `POST /api/typeahead/ingest` - Ingest phrase
2. `GET /api/typeahead/complete?prefix=<p>` - Get suggestions
3. `GET /api/typeahead/stats` - Get statistics
4. `POST /api/typeahead/rebuild` - Manual rebuild (testing)

---

## API Specifications

### 1. Ingest API

**Endpoint**: `POST /api/typeahead/ingest`

**Request**:
```json
{
  "phrase": "machine learning"
}
```

**Response** (201):
```json
{
  "message": "Phrase ingested successfully",
  "data": {
    "phrase": "machine learning",
    "frequency": 1
  },
  "timestamp": 1712239200000
}
```

**Features**:
- Auto-increments frequency if phrase exists
- Case-insensitive (converts to lowercase)
- Validation: phrase cannot be null/empty

### 2. Complete API

**Endpoint**: `GET /api/typeahead/complete?prefix={prefix}`

**Response** (200):
```json
{
  "prefix": "mach",
  "suggestions": [
    {
      "phrase": "machine learning",
      "frequency": 3
    },
    {
      "phrase": "machine learning algorithms",
      "frequency": 2
    }
  ],
  "timestamp": 1712239200000
}
```

**Features**:
- Prefix-based search
- Sorted by frequency (descending)
- Max 5 results
- O(m) complexity where m = prefix length

### 3. Statistics API

**Endpoint**: `GET /api/typeahead/stats`

**Response**:
```json
{
  "totalPhrasesInStore": 30,
  "trieStats": {
    "totalPhrases": 30,
    "maxSuggestions": 5,
    "rootChildren": 15
  }
}
```

### 4. Rebuild API (Testing)

**Endpoint**: `POST /api/typeahead/rebuild`

**Response**:
```json
{
  "message": "Trie rebuild triggered successfully",
  "timestamp": 1712239200000
}
```
---

## Scheduling

### Auto-Rebuild Schedule

**How it works**:
1. Runs every 15 minutes automatically
2. Collects all data from repository
3. Clears existing Trie
4. Rebuilds with fresh data
5. Results re-sorted by frequency

**Benefits**:
- Keeps suggestions fresh
- Frequency-based ordering maintained
- Automatic background process
- Configurable interval

---

## Thread Safety

- `ConcurrentHashMap` for repository - thread-safe
- Trie operations - synchronized externally if needed
- REST endpoints - handled by Spring

---

## Key Design Patterns

1. **Repository Pattern** - Abstract data access
2. **Dependency Injection** - Spring IoC container
3. **Service Layer** - Business logic separation
4. **Scheduled Tasks** - Auto-rebuild mechanism
5. **DTO Pattern** - Request/Response objects
6. **Singleton Pattern** - Service & Controller beans

---

## Summary

✅ **Production-ready TypeAhead system**
✅ **Trie-based efficient search** (O(m) time complexity)
✅ **Top-K suggestions** with frequency tracking
✅ **Auto-rebuild every 15 minutes**
✅ **30 sample phrases** pre-loaded
✅ **Repository pattern** for easy DB migration
✅ **Thread-safe in-memory storage**
✅ **REST APIs** for easy integration
✅ **Comprehensive documentation**
✅ **Unit tests included**








# API Examples & Use Cases

## Table of Contents
1. [Basic Operations](#basic-operations)
2. [Advanced Usage](#advanced-usage)
3. [Integration Examples](#integration-examples)
4. [Error Handling](#error-handling)
5. [Testing Scenarios](#testing-scenarios)

## Basic Operations

### 1. Ingest a Single Phrase

**Scenario**: User searches for "machine learning" for the first time

```bash
curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{"phrase":"machine learning"}'
```

**Response**:
```json
{
  "message": "Phrase ingested successfully",
  "data": {
    "phrase": "machine learning",
    "frequency": 1
  },
  "timestamp": 1712239200000
}
```

### 2. Search for Suggestions

**Scenario**: User types "mach" and wants autocomplete suggestions

```bash
curl "http://localhost:8080/api/typeahead/complete?prefix=mach"
```

**Response**:
```json
{
  "prefix": "mach",
  "suggestions": [
    {
      "phrase": "machine learning",
      "frequency": 3
    },
    {
      "phrase": "machine learning algorithms",
      "frequency": 2
    }
  ],
  "timestamp": 1712239200000
}
```

### 3. Increment Frequency

**Scenario**: Same phrase searched again

```bash
# First search
curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{"phrase":"machine learning"}'

# Second search
curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{"phrase":"machine learning"}'

# Third search
curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{"phrase":"machine learning"}'

# Search for results with updated frequency
curl "http://localhost:8080/api/typeahead/complete?prefix=mach"
```

**Response**: Frequency will be 3

---

## Advanced Usage

### 1. Prefix Matching with Different Lengths

**Single character prefix**:
```bash
curl "http://localhost:8080/api/typeahead/complete?prefix=j"
```

**Two character prefix**:
```bash
curl "http://localhost:8080/api/typeahead/complete?prefix=ja"
```

**Full phrase matching**:
```bash
curl "http://localhost:8080/api/typeahead/complete?prefix=java+spring+boot"
```

### 2. Case Insensitivity

All searches are case-insensitive:

```bash
# These return the same results:
curl "http://localhost:8080/api/typeahead/complete?prefix=PYTHON"
curl "http://localhost:8080/api/typeahead/complete?prefix=python"
curl "http://localhost:8080/api/typeahead/complete?prefix=PyThOn"
```

### 3. Top-K Suggestions

Configured to return max 5 suggestions. You'll never get more than 5 results per query:

```bash
curl "http://localhost:8080/api/typeahead/complete?prefix=p"
```

Response will have max 5 suggestions, sorted by frequency (highest first).

### 4. Batch Ingestion

Ingest multiple phrases:

```bash
for phrase in "python tutorial" "javascript basics" "rust programming"
do
  curl -X POST http://localhost:8080/api/typeahead/ingest \
    -H "Content-Type: application/json" \
    -d "{\"phrase\":\"$phrase\"}"
done
```

---

## Integration Examples

### Integration with Frontend (JavaScript)

```javascript
// Autocomplete with debouncing
const searchInput = document.getElementById('search');
let debounceTimeout;

searchInput.addEventListener('input', (e) => {
  clearTimeout(debounceTimeout);
  
  debounceTimeout = setTimeout(async () => {
    const prefix = e.target.value;
    
    if (prefix.length < 2) return;
    
    const response = await fetch(
      `/api/typeahead/complete?prefix=${encodeURIComponent(prefix)}`
    );
    
    const data = await response.json();
    
    // Display suggestions
    displaySuggestions(data.suggestions);
  }, 300); // 300ms debounce
});

function displaySuggestions(suggestions) {
  const list = document.getElementById('suggestions');
  list.innerHTML = '';
  
  suggestions.forEach(item => {
    const li = document.createElement('li');
    li.textContent = item.phrase;
    li.onclick = () => selectSuggestion(item.phrase);
    list.appendChild(li);
  });
}

function selectSuggestion(phrase) {
  document.getElementById('search').value = phrase;
  
  // Log the search to increment frequency
  fetch('/api/typeahead/ingest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phrase })
  });
}
```

### Integration with React

```jsx
import React, { useState, useEffect, useRef } from 'react';

function TypeAheadSearch() {
  const [prefix, setPrefix] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const debounceRef = useRef(null);

  useEffect(() => {
    clearTimeout(debounceRef.current);
    
    if (prefix.length < 2) {
      setSuggestions([]);
      return;
    }

    debounceRef.current = setTimeout(async () => {
      const response = await fetch(
        `/api/typeahead/complete?prefix=${encodeURIComponent(prefix)}`
      );
      const data = await response.json();
      setSuggestions(data.suggestions);
    }, 300);
  }, [prefix]);

  const handleSelect = (phrase) => {
    setPrefix(phrase);
    
    // Ingest to update frequency
    fetch('/api/typeahead/ingest', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phrase })
    });
  };

  return (
    <div>
      <input
        type="text"
        value={prefix}
        onChange={(e) => setPrefix(e.target.value)}
        placeholder="Search..."
      />
      <ul>
        {suggestions.map((item, idx) => (
          <li
            key={idx}
            onClick={() => handleSelect(item.phrase)}
            style={{ cursor: 'pointer' }}
          >
            {item.phrase} (freq: {item.frequency})
          </li>
        ))}
      </ul>
    </div>
  );
}

export default TypeAheadSearch;
```

### Integration with Python Client

```python
import requests
import time

BASE_URL = "http://localhost:8080/api/typeahead"

class TypeAheadClient:
    def __init__(self, base_url=BASE_URL):
        self.base_url = base_url
    
    def ingest(self, phrase):
        """Ingest a new phrase"""
        response = requests.post(
            f"{self.base_url}/ingest",
            json={"phrase": phrase}
        )
        return response.json()
    
    def complete(self, prefix):
        """Get suggestions for prefix"""
        response = requests.get(
            f"{self.base_url}/complete",
            params={"prefix": prefix}
        )
        return response.json()
    
    def get_stats(self):
        """Get system statistics"""
        response = requests.get(f"{self.base_url}/stats")
        return response.json()

# Usage
client = TypeAheadClient()

# Ingest phrases
client.ingest("machine learning")
client.ingest("deep learning")
client.ingest("machine learning algorithms")

# Get suggestions
results = client.complete("mach")
print(results["suggestions"])

# Get stats
stats = client.get_stats()
print(f"Total phrases: {stats['totalPhrasesInStore']}")
```

---

## Error Handling

### 1. Empty Prefix Error

**Request**:
```bash
curl "http://localhost:8080/api/typeahead/complete?prefix="
```

**Response** (400 Bad Request):
```json
{
  "error": "Prefix cannot be null or empty",
  "timestamp": 1712239200000
}
```

### 2. Missing Phrase Error

**Request**:
```bash
curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response** (400 Bad Request):
```json
{
  "error": "Phrase cannot be null or empty",
  "timestamp": 1712239200000
}
```

### 3. No Results Found

**Request**:
```bash
curl "http://localhost:8080/api/typeahead/complete?prefix=xyz123notfound"
```

**Response** (200 OK):
```json
{
  "prefix": "xyz123notfound",
  "suggestions": [],
  "timestamp": 1712239200000
}
```

### 4. Server Error Handling

**Request** (when server has issue):
```bash
curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{"phrase":"test", "extra_field": "invalid"}'
```

**Response** (500 Internal Server Error):
```json
{
  "error": "Error ingesting phrase: JSON parsing error",
  "timestamp": 1712239200000
}
```

---

## Testing Scenarios

### Scenario 1: E-Commerce Search

```bash
# Ingest product searches
phrases=("laptop" "laptop dell" "laptop gaming" "laptop gaming asus" \
         "monitor" "monitor 4k" "mouse" "mouse wireless" "keyboard")

for phrase in "${phrases[@]}"
do
  curl -X POST http://localhost:8080/api/typeahead/ingest \
    -H "Content-Type: application/json" \
    -d "{\"phrase\":\"$phrase\"}"
done

# Test searches
curl "http://localhost:8080/api/typeahead/complete?prefix=laptop"
curl "http://localhost:8080/api/typeahead/complete?prefix=monitor"
curl "http://localhost:8080/api/typeahead/complete?prefix=l"
```

### Scenario 2: Documentation Search

```bash
# Ingest documentation topics
phrases=("python documentation" "python tutorial" "python datetime" \
         "java documentation" "java spring" "java lambda" \
         "javascript arrow functions" "javascript async await")

for phrase in "${phrases[@]}"
do
  curl -X POST http://localhost:8080/api/typeahead/ingest \
    -H "Content-Type: application/json" \
    -d "{\"phrase\":\"$phrase\"}"
done

# Simulate multiple searches (same phrase increases frequency)
for i in {1..5}; do curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{"phrase":"python tutorial"}'; done

# Search and verify frequency-based ordering
curl "http://localhost:8080/api/typeahead/complete?prefix=python"
```

### Scenario 3: Load Testing

```bash
#!/bin/bash
# Simulate 100 searches with random phrases

phrases=("python" "java" "javascript" "typescript" "golang" \
         "machine learning" "deep learning" "data science" \
         "cloud computing" "kubernetes")

for i in {1..100}
do
  phrase=${phrases[$((RANDOM % ${#phrases[@]}))]}
  echo "Request $i: $phrase"
  
  curl -X POST http://localhost:8080/api/typeahead/ingest \
    -H "Content-Type: application/json" \
    -d "{\"phrase\":\"$phrase\"}" \
    -s -w "\nStatus: %{http_code}\n"
done

# Check stats after load testing
curl http://localhost:8080/api/typeahead/stats
```

### Scenario 4: Verify Trie Rebuild

```bash
# Get initial stats
echo "Initial stats:"
curl http://localhost:8080/api/typeahead/stats

# Ingest many phrases
for i in {1..20}
do
  curl -X POST http://localhost:8080/api/typeahead/ingest \
    -H "Content-Type: application/json" \
    -d "{\"phrase\":\"phrase_$i\"}" -s > /dev/null
done

# Manually trigger rebuild
echo "Triggering rebuild..."
curl -X POST http://localhost:8080/api/typeahead/rebuild

# Check stats after rebuild
echo "Stats after rebuild:"
curl http://localhost:8080/api/typeahead/stats

# Verify suggestions are ordered by frequency
curl "http://localhost:8080/api/typeahead/complete?prefix=p"
```

### Scenario 5: Frequency Tracking Over Time

```bash
# Ingest initial phrase
curl -X POST http://localhost:8080/api/typeahead/ingest \
  -H "Content-Type: application/json" \
  -d '{"phrase":"popular query"}' | jq '.data.frequency'

# Simulate multiple searches
for i in {1..10}
do
  curl -X POST http://localhost:8080/api/typeahead/ingest \
    -H "Content-Type: application/json" \
    -d '{"phrase":"popular query"}' -s > /dev/null
done

# Trigger rebuild to update ordering
curl -X POST http://localhost:8080/api/typeahead/rebuild -s > /dev/null

# Check if it appears with higher frequency
curl "http://localhost:8080/api/typeahead/complete?prefix=popular" | jq '.suggestions[0].frequency'
```

## Performance Testing

### Monitor Response Time

```bash
# Single request timing
time curl "http://localhost:8080/api/typeahead/complete?prefix=java"

# Average response time (10 requests)
for i in {1..10}; do 
  /usr/bin/time -f "%E" curl "http://localhost:8080/api/typeahead/complete?prefix=java" -s -o /dev/null
done | awk '{sum+=$1; count++} END {print "Average:", sum/count}'
```

---

## Summary

The TypeAhead system supports:
- ✅ Single and batch ingestion
- ✅ Prefix-based searching with top-K results
- ✅ Automatic frequency tracking
- ✅ Case-insensitive matching
- ✅ Periodic Trie rebuilding
- ✅ Comprehensive error handling
- ✅ Easy frontend integration
