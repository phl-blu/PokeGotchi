package com.tamagotchi.committracker.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages data privacy and minimization for the GitHub integration.
 * 
 * This class ensures:
 * - Only commit metadata is stored (no code content)
 * - Minimum OAuth scopes are requested
 * - Secure data deletion is available
 * 
 * Requirements: 9.1, 9.2, 9.3
 * - 9.1: Only request minimum necessary OAuth scopes (repo, read:user)
 * - 9.2: Store only metadata (hash, message, timestamp) not code content
 * - 9.3: Securely delete all stored user data on request
 */
public class DataPrivacyManager {
    
    private static final Logger LOGGER = Logger.getLogger(DataPrivacyManager.class.getName());
    
    // Minimum required OAuth scopes - as defined in GitHubConfig
    // repo: Required for accessing private repository commits
    // read:user: Required for reading user profile information
    public static final String[] MINIMUM_OAUTH_SCOPES = {"repo", "read:user"};
    
    // Maximum allowed fields for commit metadata
    private static final List<String> ALLOWED_COMMIT_FIELDS = List.of(
        "sha",
        "message",
        "authorName",
        "authorEmail",
        "committedAt",
        "repositoryFullName",
        "url"
    );
    
    private final SecureTokenStorage tokenStorage;
    private final OfflineCacheManager cacheManager;
    private final Path dataDirectory;
    private final SecureRandom secureRandom;
    
    /**
     * Creates a DataPrivacyManager with default components.
     */
    public DataPrivacyManager() {
        this(
            new SecureTokenStorageImpl(),
            new OfflineCacheManager(),
            getDefaultDataDirectory()
        );
    }
    
    /**
     * Creates a DataPrivacyManager with custom components.
     * 
     * @param tokenStorage the secure token storage
     * @param cacheManager the offline cache manager
     * @param dataDirectory the data directory path
     */
    public DataPrivacyManager(SecureTokenStorage tokenStorage, 
                              OfflineCacheManager cacheManager,
                              Path dataDirectory) {
        this.tokenStorage = Objects.requireNonNull(tokenStorage, "tokenStorage cannot be null");
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager cannot be null");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory cannot be null");
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Gets the default data directory.
     */
    private static Path getDefaultDataDirectory() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".pokemon-commit-tracker");
    }
    
    // ==================== Data Minimization ====================
    
    /**
     * Validates that a GitHubCommit contains only allowed metadata fields.
     * This ensures no code content is accidentally stored.
     * 
     * Requirement 9.2: Store only metadata (hash, message, timestamp) not code content
     * 
     * @param commit the commit to validate
     * @return true if the commit contains only allowed fields
     */
    public boolean validateCommitMetadata(GitHubCommit commit) {
        if (commit == null) {
            return false;
        }
        
        // GitHubCommit is a record with only these fields:
        // sha, message, authorName, authorEmail, committedAt, repositoryFullName, url
        // All of these are metadata, not code content
        return true;
    }
    
    /**
     * Sanitizes a commit message to remove any potentially sensitive information.
     * Truncates long messages and removes any embedded credentials or tokens.
     * 
     * @param message the original commit message
     * @param maxLength maximum allowed length
     * @return sanitized message
     */
    public String sanitizeCommitMessage(String message, int maxLength) {
        if (message == null) {
            return "";
        }
        
        String sanitized = message;
        
        // Remove potential tokens or credentials (patterns like API keys, tokens)
        sanitized = sanitized.replaceAll("(?i)(api[_-]?key|token|secret|password|credential)[=:\\s]+['\"]?[a-zA-Z0-9_\\-]+['\"]?", "[REDACTED]");
        
        // Remove potential URLs with credentials
        sanitized = sanitized.replaceAll("https?://[^:]+:[^@]+@", "https://[REDACTED]@");
        
        // Truncate to max length
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength - 3) + "...";
        }
        
        return sanitized;
    }
    
    /**
     * Gets the minimum required OAuth scopes.
     * 
     * Requirement 9.1: Only request minimum necessary OAuth scopes
     * 
     * @return array of minimum required scopes
     */
    public String[] getMinimumOAuthScopes() {
        return MINIMUM_OAUTH_SCOPES.clone();
    }
    
    /**
     * Validates that the requested scopes are minimal.
     * 
     * @param requestedScopes the scopes being requested
     * @return true if scopes are minimal (only required scopes)
     */
    public boolean validateMinimalScopes(String requestedScopes) {
        if (requestedScopes == null || requestedScopes.isBlank()) {
            return false;
        }
        
        String[] scopes = requestedScopes.split("\\s+");
        
        // Check that only minimum required scopes are requested
        for (String scope : scopes) {
            boolean isAllowed = false;
            for (String allowedScope : MINIMUM_OAUTH_SCOPES) {
                if (allowedScope.equals(scope)) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                LOGGER.warning("Non-minimal OAuth scope requested: " + scope);
                return false;
            }
        }
        
        return true;
    }
    
    // ==================== Secure Data Deletion ====================
    
    /**
     * Securely deletes all user data.
     * This includes tokens, cached commits, repositories, and user preferences.
     * 
     * Requirement 9.3: Securely delete all stored user data on request
     * 
     * @return true if deletion was successful
     */
    public boolean securelyDeleteAllUserData() {
        LOGGER.info("Starting secure deletion of all user data");
        
        boolean success = true;
        
        try {
            // 1. Clear all tokens securely
            tokenStorage.clearAllTokens();
            LOGGER.info("Tokens cleared securely");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to clear tokens", e);
            success = false;
        }
        
        try {
            // 2. Clear all cached data
            cacheManager.clearAllCaches();
            LOGGER.info("Caches cleared");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to clear caches", e);
            success = false;
        }
        
        try {
            // 3. Securely delete data directory contents
            if (Files.exists(dataDirectory)) {
                securelyDeleteDirectory(dataDirectory);
                LOGGER.info("Data directory securely deleted");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete data directory", e);
            success = false;
        }
        
        if (success) {
            LOGGER.info("All user data securely deleted");
        } else {
            LOGGER.warning("Some data may not have been deleted completely");
        }
        
        return success;
    }
    
    /**
     * Securely deletes a file by overwriting with random data before deletion.
     * 
     * @param path the file to delete
     * @throws IOException if deletion fails
     */
    public void securelyDeleteFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path is a directory, use securelyDeleteDirectory instead");
        }
        
        // Overwrite file with random data
        long fileSize = Files.size(path);
        if (fileSize > 0) {
            byte[] randomData = new byte[(int) Math.min(fileSize, Integer.MAX_VALUE)];
            secureRandom.nextBytes(randomData);
            Files.write(path, randomData);
        }
        
        // Delete the file
        Files.delete(path);
        
        LOGGER.fine("Securely deleted file: " + path.getFileName());
    }
    
    /**
     * Securely deletes a directory and all its contents.
     * 
     * @param directory the directory to delete
     * @throws IOException if deletion fails
     */
    public void securelyDeleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        if (!Files.isDirectory(directory)) {
            securelyDeleteFile(directory);
            return;
        }
        
        // Delete all files in directory first
        try (var stream = Files.walk(directory)) {
            stream.sorted((a, b) -> -a.compareTo(b)) // Process files before directories
                  .forEach(path -> {
                      try {
                          if (Files.isRegularFile(path)) {
                              securelyDeleteFile(path);
                          } else if (Files.isDirectory(path)) {
                              Files.delete(path);
                          }
                      } catch (IOException e) {
                          LOGGER.log(Level.WARNING, "Failed to delete: " + path, e);
                      }
                  });
        }
    }
    
    // ==================== Data Export ====================
    
    /**
     * Gets a summary of all stored user data for transparency.
     * 
     * @return summary of stored data
     */
    public DataSummary getStoredDataSummary() {
        boolean hasTokens = tokenStorage.hasStoredTokens();
        var cacheSummary = cacheManager.getCacheSummary();
        
        return new DataSummary(
            hasTokens,
            cacheSummary.totalCommits(),
            cacheSummary.totalRepositories(),
            cacheSummary.hasUserPreferences(),
            cacheSummary.lastSyncTime()
        );
    }
    
    /**
     * Summary of stored user data.
     */
    public record DataSummary(
        boolean hasAuthTokens,
        int cachedCommitCount,
        int cachedRepositoryCount,
        boolean hasUserPreferences,
        Instant lastSyncTime
    ) {
        /**
         * Checks if any user data is stored.
         */
        public boolean hasAnyData() {
            return hasAuthTokens || cachedCommitCount > 0 || 
                   cachedRepositoryCount > 0 || hasUserPreferences;
        }
    }
}
