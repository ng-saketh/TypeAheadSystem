package com.typeahead.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates and normalizes all user inputs
 */
@Slf4j
@Component
public class InputValidator {
    private static final int MAX_K_HARD_CAP = 10;
    private static final String ALLOWED_PATTERN = "^[a-z0-9\\s\\-]+$";

    private final int maxPrefixLength;
    private final long maxFrequency;

    public InputValidator(
        @Value("${typeahead.validation.maxPrefixLength:100}") int maxPrefixLength,
        @Value("${typeahead.validation.maxFrequency:1000000}") long maxFrequency) {
        this.maxPrefixLength = maxPrefixLength;
        this.maxFrequency = maxFrequency;
    }

    /**
     * Validate and normalize search query
     */
    public String validateAndNormalizeQuery(String query) throws ValidationException {
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("Query cannot be null or empty");
        }

        String normalized = query.trim().toLowerCase();

        if (normalized.length() > maxPrefixLength) {
            throw new ValidationException("Query length exceeds maximum of " + maxPrefixLength + " characters");
        }

        if (!isValidInput(normalized)) {
            throw new ValidationException("Query contains invalid characters. Only alphanumeric, spaces, and hyphens allowed");
        }

        return normalized;
    }

    /**
     * Validate search prefix
     */
    public String validateAndNormalizePrefix(String prefix) throws ValidationException {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new ValidationException("Prefix cannot be null or empty");
        }

        String normalized = prefix.trim().toLowerCase();

        if (normalized.length() > maxPrefixLength) {
            throw new ValidationException("Prefix length exceeds maximum of " + maxPrefixLength + " characters");
        }

        if (!isValidInput(normalized)) {
            throw new ValidationException("Prefix contains invalid characters");
        }

        return normalized;
    }

    /**
     * Validate frequency value
     */
    public void validateFrequency(long frequency) throws ValidationException {
        if (frequency <= 0) {
            throw new ValidationException("Frequency must be positive");
        }

        if (frequency > maxFrequency) {
            throw new ValidationException("Frequency exceeds maximum of " + maxFrequency);
        }
    }

    /**
     * Validate K (number of suggestions)
     */
    public int validateK(int k) throws ValidationException {
        if (k <= 0) {
            throw new ValidationException("K must be positive");
        }

        // Hard cap at 10 suggestions
        if (k > MAX_K_HARD_CAP) {
            log.warn("K value {} exceeds hard cap, capping at {}", k, MAX_K_HARD_CAP);
            return MAX_K_HARD_CAP;
        }

        return k;
    }
    
    /**
     * Check if input matches allowed character pattern
     */
    private boolean isValidInput(String input) {
        return input.matches(ALLOWED_PATTERN);
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}

