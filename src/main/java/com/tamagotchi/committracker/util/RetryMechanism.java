package com.tamagotchi.committracker.util;

import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Retry mechanism for handling transient failures in network operations.
 * Implements exponential backoff with configurable retry limits.
 * 
 * Supports Requirements 3.4 (network connectivity retry) and 2.5 (repository access retry).
 */
public class RetryMechanism {
    private static final Logger logger = Logger.getLogger(RetryMechanism.class.getName());
    
    // Default retry configuration
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_DELAY_MS = 1000; // 1 second
    private static final long DEFAULT_MAX_DELAY_MS = 30000; // 30 seconds
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    
    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double backoffMultiplier;
    private final ScheduledExecutorService scheduler;
    
    /**
     * Creates a RetryMechanism with default configuration.
     */
    public RetryMechanism() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS, DEFAULT_MAX_DELAY_MS, DEFAULT_BACKOFF_MULTIPLIER);
    }
    
    /**
     * Creates a RetryMechanism with custom configuration.
     * 
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay before first retry in milliseconds
     * @param maxDelayMs Maximum delay between retries in milliseconds
     * @param backoffMultiplier Multiplier for exponential backoff
     */
    public RetryMechanism(int maxRetries, long initialDelayMs, long maxDelayMs, double backoffMultiplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "RetryMechanism-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Result of a retry operation.
     */
    public static class RetryResult<T> {
        private final T result;
        private final boolean success;
        private final int attemptsMade;
        private final Throwable lastError;
        
        private RetryResult(T result, boolean success, int attemptsMade, Throwable lastError) {
            this.result = result;
            this.success = success;
            this.attemptsMade = attemptsMade;
            this.lastError = lastError;
        }
        
        public static <T> RetryResult<T> success(T result, int attempts) {
            return new RetryResult<>(result, true, attempts, null);
        }
        
        public static <T> RetryResult<T> failure(int attempts, Throwable error) {
            return new RetryResult<>(null, false, attempts, error);
        }
        
        public T getResult() { return result; }
        public boolean isSuccess() { return success; }
        public int getAttemptsMade() { return attemptsMade; }
        public Throwable getLastError() { return lastError; }
    }
    
    /**
     * Executes an operation with retry logic using exponential backoff.
     * 
     * @param operation The operation to execute
     * @param operationName Name of the operation for logging
     * @param <T> The return type of the operation
     * @return RetryResult containing the result or error information
     */
    public <T> RetryResult<T> executeWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, operationName, e -> isRetryableException(e));
    }
    
    /**
     * Executes an operation with retry logic and custom retry condition.
     * 
     * @param operation The operation to execute
     * @param operationName Name of the operation for logging
     * @param shouldRetry Predicate to determine if exception is retryable
     * @param <T> The return type of the operation
     * @return RetryResult containing the result or error information
     */
    public <T> RetryResult<T> executeWithRetry(Supplier<T> operation, String operationName,
                                                java.util.function.Predicate<Throwable> shouldRetry) {
        int attempt = 0;
        Throwable lastError = null;
        long currentDelay = initialDelayMs;
        
        while (attempt <= maxRetries) {
            attempt++;
            
            try {
                logger.fine("Executing " + operationName + " (attempt " + attempt + "/" + (maxRetries + 1) + ")");
                T result = operation.get();
                
                if (attempt > 1) {
                    logger.info(operationName + " succeeded after " + attempt + " attempts");
                }
                
                return RetryResult.success(result, attempt);
                
            } catch (Exception e) {
                lastError = e;
                
                if (attempt > maxRetries || !shouldRetry.test(e)) {
                    logger.log(Level.WARNING, operationName + " failed after " + attempt + " attempts", e);
                    ErrorHandler.handleNetworkError(operationName, e, false);
                    return RetryResult.failure(attempt, e);
                }
                
                logger.log(Level.INFO, operationName + " failed (attempt " + attempt + "), retrying in " + 
                          currentDelay + "ms: " + e.getMessage());
                ErrorHandler.handleNetworkError(operationName, e, true);
                
                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return RetryResult.failure(attempt, e);
                }
                
                // Calculate next delay with exponential backoff
                currentDelay = Math.min((long) (currentDelay * backoffMultiplier), maxDelayMs);
            }
        }
        
        return RetryResult.failure(attempt, lastError);
    }
    
    /**
     * Executes an operation asynchronously with retry logic.
     * 
     * @param operation The operation to execute
     * @param operationName Name of the operation for logging
     * @param <T> The return type of the operation
     * @return CompletableFuture containing the RetryResult
     */
    public <T> CompletableFuture<RetryResult<T>> executeWithRetryAsync(
            Supplier<T> operation, String operationName) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(operation, operationName));
    }
    
    /**
     * Schedules a retry operation after a delay.
     * 
     * @param operation The operation to execute
     * @param delayMs Delay before execution in milliseconds
     * @param <T> The return type of the operation
     * @return ScheduledFuture for the operation
     */
    public <T> ScheduledFuture<T> scheduleRetry(Callable<T> operation, long delayMs) {
        return scheduler.schedule(operation, delayMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Determines if an exception is retryable (typically network-related).
     * 
     * @param e The exception to check
     * @return true if the operation should be retried
     */
    public static boolean isRetryableException(Throwable e) {
        if (e == null) return false;
        
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String className = e.getClass().getName().toLowerCase();
        
        // Network-related exceptions
        if (className.contains("socket") || 
            className.contains("connect") ||
            className.contains("timeout") ||
            className.contains("network") ||
            className.contains("io")) {
            return true;
        }
        
        // Check message for network-related keywords
        if (message.contains("connection") ||
            message.contains("timeout") ||
            message.contains("network") ||
            message.contains("unreachable") ||
            message.contains("refused") ||
            message.contains("reset") ||
            message.contains("temporary")) {
            return true;
        }
        
        // Check cause recursively
        if (e.getCause() != null && e.getCause() != e) {
            return isRetryableException(e.getCause());
        }
        
        return false;
    }
    
    /**
     * Determines if an exception is an authentication error (not retryable).
     * 
     * @param e The exception to check
     * @return true if the exception is authentication-related
     */
    public static boolean isAuthenticationException(Throwable e) {
        if (e == null) return false;
        
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String className = e.getClass().getName().toLowerCase();
        
        return message.contains("auth") ||
               message.contains("credential") ||
               message.contains("permission") ||
               message.contains("denied") ||
               message.contains("unauthorized") ||
               message.contains("forbidden") ||
               className.contains("auth");
    }
    
    /**
     * Shuts down the retry scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Creates a builder for custom RetryMechanism configuration.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for RetryMechanism configuration.
     */
    public static class Builder {
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private long initialDelayMs = DEFAULT_INITIAL_DELAY_MS;
        private long maxDelayMs = DEFAULT_MAX_DELAY_MS;
        private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder initialDelay(long delayMs) {
            this.initialDelayMs = delayMs;
            return this;
        }
        
        public Builder maxDelay(long delayMs) {
            this.maxDelayMs = delayMs;
            return this;
        }
        
        public Builder backoffMultiplier(double multiplier) {
            this.backoffMultiplier = multiplier;
            return this;
        }
        
        public RetryMechanism build() {
            return new RetryMechanism(maxRetries, initialDelayMs, maxDelayMs, backoffMultiplier);
        }
    }
}
