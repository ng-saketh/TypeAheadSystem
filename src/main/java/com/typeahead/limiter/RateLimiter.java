package com.typeahead.limiter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket based rate limiter
 * Limits each user to a maximum number of requests per second
 */
@Slf4j
@Component
public class RateLimiter {
    private static final String ANONYMOUS_USER = "anonymous";
    private static final long MILLISECONDS_PER_SECOND = 1000L;

    private final long maxRequestsPerSecond;
    private final Map<String, UserBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(@Value("${typeahead.ratelimit.maxRequestsPerSecond:100}") long maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    /**
     * Check if user is allowed to proceed
     * Throws exception if rate limit exceeded
     */
    public void checkOrThrow(String userId) throws RateLimitExceededException {
        String normalizedUserId = userId == null || userId.trim().isEmpty() ? ANONYMOUS_USER : userId;
        UserBucket bucket = buckets.computeIfAbsent(normalizedUserId, k -> new UserBucket(maxRequestsPerSecond));
        
        if (!bucket.allowRequest()) {
            log.warn("Rate limit exceeded for user: {}", normalizedUserId);
            throw new RateLimitExceededException("Rate limit exceeded: " + maxRequestsPerSecond + " requests/second");
        }
    }

    /**
     * Token bucket implementation for a single user
     */
    private static class UserBucket {
        private final long maxTokens;
        private final AtomicLong tokens;
        private final AtomicLong lastRefillTime;

        UserBucket(long maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = new AtomicLong(maxTokens);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
        }

        synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime.get();

            // Add tokens based on time passed (tokens per second = timePassed * maxTokens / 1000)
            long tokensToAdd = (timePassed * maxTokens) / MILLISECONDS_PER_SECOND;
            
            if (tokensToAdd > 0) {
                tokens.accumulateAndGet(tokensToAdd, (current, add) -> Math.min(current + add, maxTokens));
                lastRefillTime.set(now);
            }

            // Check if we have tokens available
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }

            return false;
        }
    }

    public static class RateLimitExceededException extends Exception {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}


