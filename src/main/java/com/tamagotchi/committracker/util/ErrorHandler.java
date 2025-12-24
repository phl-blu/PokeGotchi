package com.tamagotchi.committracker.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized error handling utility for the Tamagotchi Commit Tracker.
 * Provides graceful error handling, user-friendly messages, and error logging.
 * 
 * Implements Requirements 2.5 (repository access failure handling) and 8.3 (authentication error handling).
 */
public class ErrorHandler {
    private static final Logger logger = Logger.getLogger(ErrorHandler.class.getName());
    
    // Error categories for classification
    public enum ErrorCategory {
        GIT_AUTHENTICATION("Git Authentication Error"),
        NETWORK_CONNECTIVITY("Network Connectivity Error"),
        REPOSITORY_ACCESS("Repository Access Error"),
        SPRITE_LOADING("Sprite Loading Error"),
        FILE_SYSTEM("File System Error"),
        CONFIGURATION("Configuration Error"),
        UNKNOWN("Unknown Error");
        
        private final String displayName;
        
        ErrorCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Error severity levels
    public enum ErrorSeverity {
        INFO,      // Informational, no action needed
        WARNING,   // Warning, operation continues with degraded functionality
        ERROR,     // Error, operation failed but app continues
        CRITICAL   // Critical, may require user intervention
    }
    
    // Track error occurrences to prevent spam
    private static final ConcurrentHashMap<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();
    private static final int MAX_REPEATED_ERRORS = 3;
    
    // Error listeners for UI updates
    private static Consumer<ErrorEvent> errorListener;
    
    /**
     * Represents an error event with context information.
     */
    public static class ErrorEvent {
        private final ErrorCategory category;
        private final ErrorSeverity severity;
        private final String message;
        private final String userFriendlyMessage;
        private final Throwable cause;
        private final long timestamp;
        
        public ErrorEvent(ErrorCategory category, ErrorSeverity severity, 
                         String message, String userFriendlyMessage, Throwable cause) {
            this.category = category;
            this.severity = severity;
            this.message = message;
            this.userFriendlyMessage = userFriendlyMessage;
            this.cause = cause;
            this.timestamp = System.currentTimeMillis();
        }
        
        public ErrorCategory getCategory() { return category; }
        public ErrorSeverity getSeverity() { return severity; }
        public String getMessage() { return message; }
        public String getUserFriendlyMessage() { return userFriendlyMessage; }
        public Throwable getCause() { return cause; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Sets the error listener for UI notifications.
     */
    public static void setErrorListener(Consumer<ErrorEvent> listener) {
        errorListener = listener;
    }
    
    /**
     * Handles Git authentication failures gracefully.
     * Logs the error and provides user-friendly feedback.
     * 
     * @param repositoryName The name of the repository
     * @param cause The underlying exception
     */
    public static void handleGitAuthenticationError(String repositoryName, Throwable cause) {
        String errorKey = "git_auth_" + repositoryName;
        
        if (shouldSuppressError(errorKey)) {
            logger.fine("Suppressing repeated Git authentication error for: " + repositoryName);
            return;
        }
        
        String technicalMessage = "Git authentication failed for repository: " + repositoryName;
        String userMessage = String.format(
            "Unable to access repository '%s'. Please check your Git credentials.\n" +
            "The application will continue monitoring other repositories.",
            repositoryName
        );
        
        logError(ErrorCategory.GIT_AUTHENTICATION, ErrorSeverity.WARNING, technicalMessage, cause);
        notifyError(new ErrorEvent(ErrorCategory.GIT_AUTHENTICATION, ErrorSeverity.WARNING,
                                   technicalMessage, userMessage, cause));
    }
    
    /**
     * Handles network connectivity issues with retry information.
     * 
     * @param operation The operation that failed
     * @param cause The underlying exception
     * @param willRetry Whether the operation will be retried
     */
    public static void handleNetworkError(String operation, Throwable cause, boolean willRetry) {
        String errorKey = "network_" + operation;
        
        if (shouldSuppressError(errorKey)) {
            logger.fine("Suppressing repeated network error for: " + operation);
            return;
        }
        
        String technicalMessage = "Network error during: " + operation;
        String userMessage;
        
        if (willRetry) {
            userMessage = String.format(
                "Network connection issue detected during %s.\n" +
                "The operation will be retried automatically.",
                operation
            );
        } else {
            userMessage = String.format(
                "Network connection issue detected during %s.\n" +
                "Please check your internet connection.",
                operation
            );
        }
        
        logError(ErrorCategory.NETWORK_CONNECTIVITY, ErrorSeverity.WARNING, technicalMessage, cause);
        notifyError(new ErrorEvent(ErrorCategory.NETWORK_CONNECTIVITY, ErrorSeverity.WARNING,
                                   technicalMessage, userMessage, cause));
    }
    
    /**
     * Handles repository access failures.
     * 
     * @param repositoryPath The path to the repository
     * @param cause The underlying exception
     */
    public static void handleRepositoryAccessError(String repositoryPath, Throwable cause) {
        String errorKey = "repo_access_" + repositoryPath;
        
        if (shouldSuppressError(errorKey)) {
            logger.fine("Suppressing repeated repository access error for: " + repositoryPath);
            return;
        }
        
        String technicalMessage = "Failed to access repository: " + repositoryPath;
        String userMessage = String.format(
            "Unable to access repository at '%s'.\n" +
            "The repository may have been moved or deleted.\n" +
            "Monitoring will continue for other repositories.",
            repositoryPath
        );
        
        logError(ErrorCategory.REPOSITORY_ACCESS, ErrorSeverity.WARNING, technicalMessage, cause);
        notifyError(new ErrorEvent(ErrorCategory.REPOSITORY_ACCESS, ErrorSeverity.WARNING,
                                   technicalMessage, userMessage, cause));
    }
    
    /**
     * Handles sprite loading failures with fallback notification.
     * 
     * @param spritePath The path to the sprite that failed to load
     * @param usingFallback Whether a fallback sprite is being used
     */
    public static void handleSpriteLoadingError(String spritePath, boolean usingFallback) {
        String errorKey = "sprite_" + spritePath;
        
        if (shouldSuppressError(errorKey)) {
            return; // Silently suppress repeated sprite errors
        }
        
        String technicalMessage = "Failed to load sprite: " + spritePath;
        
        if (usingFallback) {
            logger.fine(technicalMessage + " - using fallback sprite");
        } else {
            logger.warning(technicalMessage + " - no fallback available");
        }
        
        // Sprite errors are typically not shown to users unless critical
        if (!usingFallback) {
            String userMessage = "Some Pokemon sprites could not be loaded. Display may be affected.";
            notifyError(new ErrorEvent(ErrorCategory.SPRITE_LOADING, ErrorSeverity.INFO,
                                       technicalMessage, userMessage, null));
        }
    }
    
    /**
     * Handles configuration errors.
     * 
     * @param configKey The configuration key that failed
     * @param defaultValue The default value being used
     * @param cause The underlying exception
     */
    public static void handleConfigurationError(String configKey, String defaultValue, Throwable cause) {
        String technicalMessage = "Configuration error for key: " + configKey + ", using default: " + defaultValue;
        
        logError(ErrorCategory.CONFIGURATION, ErrorSeverity.INFO, technicalMessage, cause);
        // Configuration errors typically don't need user notification
    }
    
    /**
     * Shows a user-friendly error dialog on the JavaFX thread.
     * 
     * @param title The dialog title
     * @param message The error message
     * @param severity The error severity
     */
    public static void showErrorDialog(String title, String message, ErrorSeverity severity) {
        Platform.runLater(() -> {
            AlertType alertType;
            switch (severity) {
                case INFO:
                    alertType = AlertType.INFORMATION;
                    break;
                case WARNING:
                    alertType = AlertType.WARNING;
                    break;
                case ERROR:
                case CRITICAL:
                    alertType = AlertType.ERROR;
                    break;
                default:
                    alertType = AlertType.INFORMATION;
            }
            
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Shows a confirmation dialog for retry operations.
     * 
     * @param title The dialog title
     * @param message The message
     * @param onRetry Callback if user chooses to retry
     * @param onCancel Callback if user cancels
     */
    public static void showRetryDialog(String title, String message, 
                                       Runnable onRetry, Runnable onCancel) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            ButtonType retryButton = new ButtonType("Retry");
            ButtonType cancelButton = new ButtonType("Cancel");
            alert.getButtonTypes().setAll(retryButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == retryButton) {
                if (onRetry != null) onRetry.run();
            } else {
                if (onCancel != null) onCancel.run();
            }
        });
    }
    
    /**
     * Logs an error with appropriate level based on severity.
     */
    private static void logError(ErrorCategory category, ErrorSeverity severity, 
                                 String message, Throwable cause) {
        Level logLevel;
        switch (severity) {
            case INFO:
                logLevel = Level.INFO;
                break;
            case WARNING:
                logLevel = Level.WARNING;
                break;
            case ERROR:
            case CRITICAL:
                logLevel = Level.SEVERE;
                break;
            default:
                logLevel = Level.INFO;
        }
        
        String fullMessage = "[" + category.getDisplayName() + "] " + message;
        
        if (cause != null) {
            logger.log(logLevel, fullMessage, cause);
        } else {
            logger.log(logLevel, fullMessage);
        }
    }
    
    /**
     * Notifies the error listener if set.
     */
    private static void notifyError(ErrorEvent event) {
        if (errorListener != null) {
            try {
                errorListener.accept(event);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error listener threw exception", e);
            }
        }
    }
    
    /**
     * Checks if an error should be suppressed to prevent spam.
     * Returns true if the error has been reported too many times recently.
     */
    private static boolean shouldSuppressError(String errorKey) {
        AtomicInteger count = errorCounts.computeIfAbsent(errorKey, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        // Reset count periodically (every 10 minutes)
        if (currentCount == 1) {
            scheduleErrorCountReset(errorKey);
        }
        
        return currentCount > MAX_REPEATED_ERRORS;
    }
    
    /**
     * Schedules a reset of the error count for a specific key.
     */
    private static void scheduleErrorCountReset(String errorKey) {
        Thread resetThread = new Thread(() -> {
            try {
                Thread.sleep(600000); // 10 minutes
                errorCounts.remove(errorKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        resetThread.setDaemon(true);
        resetThread.start();
    }
    
    /**
     * Clears all error counts. Useful for testing.
     */
    public static void clearErrorCounts() {
        errorCounts.clear();
    }
    
    /**
     * Gets the current error count for a specific key. Useful for testing.
     */
    public static int getErrorCount(String errorKey) {
        AtomicInteger count = errorCounts.get(errorKey);
        return count != null ? count.get() : 0;
    }
}
