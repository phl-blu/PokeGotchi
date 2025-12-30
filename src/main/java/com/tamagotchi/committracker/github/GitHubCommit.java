package com.tamagotchi.committracker.github;

import java.time.Instant;

/**
 * Represents a commit fetched from GitHub API.
 * 
 * Requirements: 4.1, 4.3, 4.6
 */
public record GitHubCommit(
    String sha,
    String message,
    String authorName,
    String authorEmail,
    Instant committedAt,
    String repositoryFullName,
    String url
) {
    /**
     * Gets a truncated message suitable for display.
     * @param maxLength maximum length of the message
     * @return truncated message with ellipsis if needed
     */
    public String getTruncatedMessage(int maxLength) {
        if (message == null) {
            return "";
        }
        // Get first line only
        String firstLine = message.split("\n")[0];
        if (firstLine.length() <= maxLength) {
            return firstLine;
        }
        return firstLine.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Gets the short SHA (first 7 characters).
     */
    public String getShortSha() {
        if (sha == null || sha.length() < 7) {
            return sha;
        }
        return sha.substring(0, 7);
    }
}
