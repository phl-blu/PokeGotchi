package com.tamagotchi.committracker.github;

import java.time.Instant;

/**
 * Represents a GitHub user profile.
 * 
 * Requirements: 1.1, 9.1
 */
public record GitHubUser(
    long id,
    String login,
    String name,
    String email,
    String avatarUrl,
    Instant createdAt
) {
    /**
     * Gets the display name (name if available, otherwise login).
     */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return login;
    }
}
