package com.tamagotchi.committracker.domain;

import java.time.LocalDateTime;

/**
 * Domain model representing a Git commit with metadata.
 */
public class Commit {
    private String hash;
    private String message;
    private String author;
    private LocalDateTime timestamp;
    private String repositoryName;
    private String repositoryPath;
    
    // TODO: Implement constructors, getters, setters in task 2
}