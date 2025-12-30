package com.tamagotchi.committracker.github;

import java.time.Instant;

/**
 * Represents a GitHub repository fetched from the API.
 * 
 * Requirements: 3.1, 3.2, 3.3
 */
public record GitHubRepository(
    long id,
    String name,
    String fullName,
    String owner,
    boolean isPrivate,
    String defaultBranch,
    Instant pushedAt,
    String etag  // For conditional requests
) {
    /**
     * Creates a GitHubRepository with a new ETag.
     */
    public GitHubRepository withEtag(String newEtag) {
        return new GitHubRepository(id, name, fullName, owner, isPrivate, defaultBranch, pushedAt, newEtag);
    }
    
    /**
     * Gets the owner from the fullName (format: owner/repo).
     */
    public static String extractOwner(String fullName) {
        if (fullName == null || !fullName.contains("/")) {
            return null;
        }
        return fullName.split("/")[0];
    }
    
    /**
     * Gets the repo name from the fullName (format: owner/repo).
     */
    public static String extractRepoName(String fullName) {
        if (fullName == null || !fullName.contains("/")) {
            return fullName;
        }
        String[] parts = fullName.split("/");
        return parts.length > 1 ? parts[1] : fullName;
    }
}
