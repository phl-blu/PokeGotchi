package com.tamagotchi.committracker.github;

/**
 * Exception thrown when GitHub API requests fail.
 * Contains HTTP status code and error details for proper error handling.
 * 
 * Requirements: 8.1, 8.2, 8.3
 */
public class GitHubApiException extends RuntimeException {
    
    private final int statusCode;
    private final String errorType;
    private final boolean isRateLimited;
    private final boolean isAuthError;
    
    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = classifyError(statusCode);
        this.isRateLimited = statusCode == 403 && message != null && 
                            message.toLowerCase().contains("rate limit");
        this.isAuthError = statusCode == 401;
    }
    
    public GitHubApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorType = classifyError(statusCode);
        this.isRateLimited = statusCode == 403 && message != null && 
                            message.toLowerCase().contains("rate limit");
        this.isAuthError = statusCode == 401;
    }
    
    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorType = "NETWORK_ERROR";
        this.isRateLimited = false;
        this.isAuthError = false;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public boolean isRateLimited() {
        return isRateLimited;
    }
    
    public boolean isAuthError() {
        return isAuthError;
    }
    
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    public boolean isRetryable() {
        // Server errors and rate limits are retryable
        return isServerError() || isRateLimited || statusCode == -1;
    }
    
    private static String classifyError(int statusCode) {
        return switch (statusCode) {
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 422 -> "VALIDATION_ERROR";
            case 429 -> "RATE_LIMITED";
            case -1 -> "NETWORK_ERROR";
            default -> {
                if (statusCode >= 500) yield "SERVER_ERROR";
                if (statusCode >= 400) yield "CLIENT_ERROR";
                yield "UNKNOWN";
            }
        };
    }
}
