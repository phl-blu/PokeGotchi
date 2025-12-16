package com.tamagotchi.committracker.domain;

/**
 * Represents the type of authentication used for Git repositories.
 */
public enum AuthenticationType {
    NONE,           // Public repositories
    SSH_KEY,        // SSH key authentication
    HTTPS_TOKEN,    // Personal access token
    HTTPS_PASSWORD  // Username/password
}