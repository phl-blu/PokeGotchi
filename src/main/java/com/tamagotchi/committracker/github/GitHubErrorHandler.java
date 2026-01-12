package com.tamagotchi.committracker.github;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles error classification and recovery for GitHub API operations.
 * Implements retry logic with exponential backoff and error-specific handling.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 * 
 * Error handling strategy:
 * - 401 Unauthorized: Attempt token refresh, then re-authentication
 * - 403 Forbidden (rate limit): Pause requests, wait for reset
 * - 403 Forbidden (permission): Log error, skip resource
 * - 404 Not Found: Remove from tracking, log warning
 * - 5xx Server Error: Exponential backoff, max 3 retries
 * - Network Error: Exponential backoff, switch to offline mode
 */
public class GitHubErrorHandler {
    
    private static final Logger logger = Logger.getLogger(GitHubErrorHandler.class.getName());
    
    private static final int MAX_RETRIES = 3;
    
    private final ExponentialBackoff backoff;
    private final GitHubOAuthService oAuthService;
    private final SecureTokenStorage tokenStorage;
    private final RateLimitManager rateLimitManager;
    
    private volatile boolean offlineMode = false;
    private volatile Instant lastNetworkError = null;
    
    /**
     * Listener interface for error events.
     */
    public interface ErrorListener {
        void onAuthenticationRequired();
        void onRateLimitExhausted(Instant resetTime);
        void onOfflineModeEntered();
        void onOfflineModeExited();
        void onPersistentError(GitHubApiException error);
    }
    
    private ErrorListener errorListener;

    
    /**
     * Creates a GitHubErrorHandler with required dependencies.
     */
    public GitHubErrorHandler(
            GitHubOAuthService oAuthService,
            SecureTokenStorage tokenStorage,
            RateLimitManager rateLimitManager) {
        this.backoff = new ExponentialBackoff();
        this.oAuthService = oAuthService;
        this.tokenStorage = tokenStorage;
        this.rateLimitManager = rateLimitManager;
    }
    
    /**
     * Creates a GitHubErrorHandler with custom backoff configuration.
     */
    public GitHubErrorHandler(
            GitHubOAuthService oAuthService,
            SecureTokenStorage tokenStorage,
            RateLimitManager rateLimitManager,
            ExponentialBackoff backoff) {
        this.backoff = backoff;
        this.oAuthService = oAuthService;
        this.tokenStorage = tokenStorage;
        this.rateLimitManager = rateLimitManager;
    }
    
    /**
     * Sets the error listener for receiving error notifications.
     */
    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }
    
    /**
     * Executes an operation with automatic error handling and retry logic.
     * 
     * @param operation The operation to execute
     * @param operationName Name for logging purposes
     * @param <T> Return type of the operation
     * @return CompletableFuture with the result or error
     */
    public <T> CompletableFuture<T> executeWithErrorHandling(
            Supplier<CompletableFuture<T>> operation,
            String operationName) {
        
        return executeWithRetry(operation, operationName, 0);
    }
    
    private <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int attemptNumber) {
        
        if (attemptNumber >= MAX_RETRIES) {
            return CompletableFuture.failedFuture(
                new GitHubApiException("Max retries exceeded for " + operationName, -1));
        }
        
        return operation.get()
            .thenApply(result -> {
                // Success - reset backoff and exit offline mode if needed
                backoff.reset();
                if (offlineMode) {
                    exitOfflineMode();
                }
                return result;
            })
            .exceptionallyCompose(throwable -> {
                Throwable cause = unwrapException(throwable);
                return handleError(cause, operation, operationName, attemptNumber);
            });
    }
    
    private <T> CompletableFuture<T> handleError(
            Throwable error,
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int attemptNumber) {
        
        ErrorClassification classification = classifyError(error);
        
        logger.log(Level.WARNING, 
            "Error in " + operationName + " (attempt " + (attemptNumber + 1) + "): " + 
            classification.getType() + " - " + error.getMessage());
        
        return switch (classification.getType()) {
            case UNAUTHORIZED -> handleUnauthorizedError(operation, operationName, attemptNumber);
            case RATE_LIMITED -> handleRateLimitError(operation, operationName, attemptNumber);
            case FORBIDDEN -> handleForbiddenError(error, operationName);
            case NOT_FOUND -> handleNotFoundError(error, operationName);
            case SERVER_ERROR -> handleServerError(operation, operationName, attemptNumber);
            case NETWORK_ERROR -> handleNetworkError(operation, operationName, attemptNumber);
            case CLIENT_ERROR -> handleClientError(error, operationName);
        };
    }

    
    /**
     * Handles 401 Unauthorized errors.
     * Attempts token refresh first, then prompts for re-authentication.
     */
    private <T> CompletableFuture<T> handleUnauthorizedError(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int attemptNumber) {
        
        logger.info("Handling 401 Unauthorized - attempting token refresh");
        
        Optional<String> refreshToken = tokenStorage.getRefreshToken();
        
        if (refreshToken.isEmpty()) {
            logger.warning("No refresh token available - authentication required");
            notifyAuthenticationRequired();
            return CompletableFuture.failedFuture(
                new GitHubApiException("Authentication required - no refresh token", 401));
        }
        
        return oAuthService.refreshAccessToken(refreshToken.get())
            .thenCompose(tokenResponse -> {
                // Store new tokens
                tokenStorage.storeAccessToken(tokenResponse.accessToken());
                if (tokenResponse.refreshToken() != null) {
                    tokenStorage.storeRefreshToken(tokenResponse.refreshToken());
                }
                logger.info("Token refreshed successfully - retrying operation");
                // Retry the operation with new token
                return executeWithRetry(operation, operationName, attemptNumber + 1);
            })
            .exceptionallyCompose(refreshError -> {
                logger.warning("Token refresh failed - authentication required: " + refreshError.getMessage());
                notifyAuthenticationRequired();
                return CompletableFuture.failedFuture(
                    new GitHubApiException("Authentication required - token refresh failed", 401, refreshError));
            });
    }
    
    /**
     * Handles 403 Rate Limited errors.
     * Waits for rate limit reset before retrying.
     */
    private <T> CompletableFuture<T> handleRateLimitError(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int attemptNumber) {
        
        RateLimitStatus status = rateLimitManager.getRateLimitStatus();
        Duration waitTime = rateLimitManager.getRecommendedWaitTime();
        
        logger.warning("Rate limit exhausted. Reset at: " + status.resetTime() + 
                      ", waiting: " + waitTime.toSeconds() + "s");
        
        notifyRateLimitExhausted(status.resetTime());
        
        // Wait for rate limit reset, then retry
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(waitTime.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for rate limit reset", e);
            }
            return null;
        }).thenCompose(ignored -> executeWithRetry(operation, operationName, attemptNumber + 1));
    }
    
    /**
     * Handles 403 Forbidden errors (non-rate-limit).
     * These are permission errors and should not be retried.
     */
    private <T> CompletableFuture<T> handleForbiddenError(
            Throwable error,
            String operationName) {
        
        logger.warning("Permission denied for " + operationName + ": " + error.getMessage());
        
        GitHubApiException apiError = error instanceof GitHubApiException 
            ? (GitHubApiException) error 
            : new GitHubApiException("Permission denied: " + error.getMessage(), 403, error);
        
        notifyPersistentError(apiError);
        return CompletableFuture.failedFuture(apiError);
    }
    
    /**
     * Handles 404 Not Found errors.
     * Resource doesn't exist - should not be retried.
     */
    private <T> CompletableFuture<T> handleNotFoundError(
            Throwable error,
            String operationName) {
        
        logger.warning("Resource not found for " + operationName + ": " + error.getMessage());
        
        GitHubApiException apiError = error instanceof GitHubApiException 
            ? (GitHubApiException) error 
            : new GitHubApiException("Resource not found: " + error.getMessage(), 404, error);
        
        return CompletableFuture.failedFuture(apiError);
    }

    
    /**
     * Handles 5xx Server errors.
     * Retries with exponential backoff up to MAX_RETRIES.
     */
    private <T> CompletableFuture<T> handleServerError(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int attemptNumber) {
        
        if (attemptNumber >= MAX_RETRIES - 1) {
            logger.severe("Server error persists after " + MAX_RETRIES + " attempts for " + operationName);
            GitHubApiException error = new GitHubApiException(
                "Server error persists after max retries", 500);
            notifyPersistentError(error);
            return CompletableFuture.failedFuture(error);
        }
        
        Duration delay = backoff.calculateDelay(attemptNumber);
        logger.info("Server error - retrying " + operationName + " in " + delay.toMillis() + "ms");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during backoff", e);
            }
            return null;
        }).thenCompose(ignored -> executeWithRetry(operation, operationName, attemptNumber + 1));
    }
    
    /**
     * Handles network errors (connection failures, timeouts, etc.).
     * Retries with exponential backoff and may switch to offline mode.
     */
    private <T> CompletableFuture<T> handleNetworkError(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int attemptNumber) {
        
        lastNetworkError = Instant.now();
        
        if (attemptNumber >= MAX_RETRIES - 1) {
            logger.warning("Network error persists - entering offline mode");
            enterOfflineMode();
            return CompletableFuture.failedFuture(
                new GitHubApiException("Network unavailable - offline mode", -1));
        }
        
        Duration delay = backoff.calculateDelay(attemptNumber);
        logger.info("Network error - retrying " + operationName + " in " + delay.toMillis() + "ms");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during backoff", e);
            }
            return null;
        }).thenCompose(ignored -> executeWithRetry(operation, operationName, attemptNumber + 1));
    }
    
    /**
     * Handles other client errors (4xx).
     * These are typically not retryable.
     */
    private <T> CompletableFuture<T> handleClientError(
            Throwable error,
            String operationName) {
        
        logger.warning("Client error for " + operationName + ": " + error.getMessage());
        
        GitHubApiException apiError = error instanceof GitHubApiException 
            ? (GitHubApiException) error 
            : new GitHubApiException("Client error: " + error.getMessage(), 400, error);
        
        return CompletableFuture.failedFuture(apiError);
    }
    
    /**
     * Classifies an error into a specific type for handling.
     */
    public ErrorClassification classifyError(Throwable error) {
        if (error instanceof GitHubApiException apiError) {
            return classifyApiException(apiError);
        }
        
        // Check for network-related exceptions
        if (isNetworkError(error)) {
            return new ErrorClassification(ErrorType.NETWORK_ERROR, false);
        }
        
        // Default to client error for unknown exceptions
        return new ErrorClassification(ErrorType.CLIENT_ERROR, false);
    }
    
    private ErrorClassification classifyApiException(GitHubApiException error) {
        int statusCode = error.getStatusCode();
        
        if (statusCode == 401) {
            return new ErrorClassification(ErrorType.UNAUTHORIZED, false);
        }
        
        if (statusCode == 403) {
            if (error.isRateLimited()) {
                return new ErrorClassification(ErrorType.RATE_LIMITED, true);
            }
            return new ErrorClassification(ErrorType.FORBIDDEN, false);
        }
        
        if (statusCode == 404) {
            return new ErrorClassification(ErrorType.NOT_FOUND, false);
        }
        
        if (statusCode >= 500) {
            return new ErrorClassification(ErrorType.SERVER_ERROR, true);
        }
        
        if (statusCode == -1) {
            return new ErrorClassification(ErrorType.NETWORK_ERROR, true);
        }
        
        return new ErrorClassification(ErrorType.CLIENT_ERROR, false);
    }

    
    /**
     * Checks if an exception is network-related.
     */
    private boolean isNetworkError(Throwable error) {
        if (error == null) return false;
        
        String className = error.getClass().getName().toLowerCase();
        String message = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        
        // Check class name for network-related exceptions
        if (className.contains("socket") ||
            className.contains("connect") ||
            className.contains("timeout") ||
            className.contains("network") ||
            className.contains("unknownhost")) {
            return true;
        }
        
        // Check message for network-related keywords
        if (message.contains("connection") ||
            message.contains("timeout") ||
            message.contains("network") ||
            message.contains("unreachable") ||
            message.contains("refused") ||
            message.contains("reset")) {
            return true;
        }
        
        // Check cause recursively
        if (error.getCause() != null && error.getCause() != error) {
            return isNetworkError(error.getCause());
        }
        
        return false;
    }
    
    /**
     * Unwraps CompletionException and ExecutionException to get the actual cause.
     */
    private Throwable unwrapException(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof java.util.concurrent.CompletionException ||
                current instanceof java.util.concurrent.ExecutionException) &&
               current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
    
    // Offline mode management
    
    private void enterOfflineMode() {
        if (!offlineMode) {
            offlineMode = true;
            logger.info("Entering offline mode");
            if (errorListener != null) {
                errorListener.onOfflineModeEntered();
            }
        }
    }
    
    private void exitOfflineMode() {
        if (offlineMode) {
            offlineMode = false;
            logger.info("Exiting offline mode");
            if (errorListener != null) {
                errorListener.onOfflineModeExited();
            }
        }
    }
    
    /**
     * Checks if the handler is currently in offline mode.
     */
    public boolean isOfflineMode() {
        return offlineMode;
    }
    
    /**
     * Gets the timestamp of the last network error.
     */
    public Optional<Instant> getLastNetworkError() {
        return Optional.ofNullable(lastNetworkError);
    }
    
    /**
     * Manually exits offline mode (e.g., when network is detected).
     */
    public void tryExitOfflineMode() {
        exitOfflineMode();
    }
    
    // Notification helpers
    
    private void notifyAuthenticationRequired() {
        if (errorListener != null) {
            errorListener.onAuthenticationRequired();
        }
    }
    
    private void notifyRateLimitExhausted(Instant resetTime) {
        if (errorListener != null) {
            errorListener.onRateLimitExhausted(resetTime);
        }
    }
    
    private void notifyPersistentError(GitHubApiException error) {
        if (errorListener != null) {
            errorListener.onPersistentError(error);
        }
    }
    
    /**
     * Error type classification.
     */
    public enum ErrorType {
        UNAUTHORIZED,    // 401 - needs re-authentication
        RATE_LIMITED,    // 403 with rate limit - wait and retry
        FORBIDDEN,       // 403 without rate limit - permission denied
        NOT_FOUND,       // 404 - resource doesn't exist
        SERVER_ERROR,    // 5xx - retry with backoff
        NETWORK_ERROR,   // Connection issues - retry with backoff
        CLIENT_ERROR     // Other 4xx - not retryable
    }
    
    /**
     * Result of error classification.
     */
    public record ErrorClassification(ErrorType type, boolean isRetryable) {
        public ErrorType getType() {
            return type;
        }
        
        public boolean isRetryable() {
            return isRetryable;
        }
    }
}
