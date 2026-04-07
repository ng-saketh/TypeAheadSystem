package com.typeahead.controller;

import com.typeahead.dto.CompleteResponse;
import com.typeahead.dto.IngestRequest;
import com.typeahead.limiter.RateLimiter;
import com.typeahead.model.ScoredSuggestion;
import com.typeahead.service.CompletionService;
import com.typeahead.service.IngestionService;
import com.typeahead.service.TypeAheadService;
import com.typeahead.validator.InputValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/typeahead")
@CrossOrigin(origins = "*")
public class TypeAheadController {

    private final IngestionService ingestionService;
    private final CompletionService completionService;
    private final TypeAheadService typeAheadService;

    public TypeAheadController(
        IngestionService ingestionService,
        CompletionService completionService,
        TypeAheadService typeAheadService) {
        this.ingestionService = ingestionService;
        this.completionService = completionService;
        this.typeAheadService = typeAheadService;
    }

    /**
     * Ingest a new search query (WRITE PATH)
     * POST /api/typeahead/ingest
     */
    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody IngestRequest request) {
        return executeWithErrorHandling(() -> {
            String userId = request.getUserId() != null ? request.getUserId() : "anonymous";
            ingestionService.ingest(request.getQuery(), request.getFrequency(), userId);
            log.info("Successfully ingested query: {} by user: {}", request.getQuery(), userId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new SuccessResponse("Query ingested successfully")
            );
        }, "ingest query");
    }

    /**
     * Get autocomplete suggestions for a prefix (READ PATH)
     * GET /api/typeahead/complete?prefix=<prefix>&k=5
     */
    @GetMapping("/complete")
    public ResponseEntity<?> complete(
        @RequestParam(value = "prefix") String prefix,
        @RequestParam(value = "k", defaultValue = "5") int k,
        @RequestParam(value = "userId", defaultValue = "anonymous") String userId) {
        return executeWithErrorHandling(() -> {
            List<ScoredSuggestion> results = completionService.complete(prefix, k, userId);
            log.debug("Retrieved {} suggestions for prefix: '{}'", results.size(), prefix);
            return ResponseEntity.ok(new CompleteResponse(prefix, results));
        }, "complete suggestions");
    }

    /**
     * Get system statistics
     * GET /api/typeahead/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return executeWithErrorHandling(() -> 
            ResponseEntity.ok(typeAheadService.getStatistics()),
            "retrieve statistics"
        );
    }

    /**
     * Manually trigger Trie rebuild (for admin/testing)
     * POST /api/typeahead/rebuild
     */
    @PostMapping("/rebuild")
    public ResponseEntity<?> rebuild() {
        return executeWithErrorHandling(() -> {
            typeAheadService.rebuildTrie();
            log.info("Manual Trie rebuild triggered");
            return ResponseEntity.ok(new SuccessResponse("Trie rebuild triggered successfully"));
        }, "trigger rebuild");
    }
    
    /**
     * Execute API action with centralized error handling
     */
    @FunctionalInterface
    private interface ApiAction {
        ResponseEntity<?> execute() throws Exception;
    }
    
    private ResponseEntity<?> executeWithErrorHandling(ApiAction action, String operation) {
        try {
            return action.execute();
        } catch (InputValidator.ValidationException e) {
            log.warn("Validation error while {}: {}", operation, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RateLimiter.RateLimitExceededException e) {
            log.warn("Rate limit exceeded while {}: {}", operation, e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error while {}", operation, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse("Error during " + operation + ": " + e.getMessage())
            );
        }
    }

    public static class ErrorResponse {
        public String error;
        public long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }

        public String getError() {
            return error;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class SuccessResponse {
        public String message;
        public long timestamp;

        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
