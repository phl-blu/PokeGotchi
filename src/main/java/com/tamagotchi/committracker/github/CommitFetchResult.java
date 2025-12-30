package com.tamagotchi.committracker.github;

import java.util.List;
import java.util.Collections;

/**
 * Result of fetching commits from GitHub API.
 * Contains commits, ETag for conditional requests, and rate limit info.
 * 
 * Requirements: 4.1, 6.1, 6.2
 */
public record CommitFetchResult(
    List<GitHubCommit> commits,
    String newEtag,
    boolean wasModified,
    int remainingRateLimit
) {
    /**
     * Creates a result for when content was not modified (304 response).
     */
    public static CommitFetchResult notModified(String etag, int remainingRateLimit) {
        return new CommitFetchResult(Collections.emptyList(), etag, false, remainingRateLimit);
    }
    
    /**
     * Creates a result with commits.
     */
    public static CommitFetchResult withCommits(List<GitHubCommit> commits, String etag, int remainingRateLimit) {
        return new CommitFetchResult(
            commits != null ? List.copyOf(commits) : Collections.emptyList(),
            etag,
            true,
            remainingRateLimit
        );
    }
    
    /**
     * Gets the number of commits fetched.
     */
    public int getCommitCount() {
        return commits != null ? commits.size() : 0;
    }
    
    /**
     * Checks if any commits were fetched.
     */
    public boolean hasCommits() {
        return commits != null && !commits.isEmpty();
    }
}
