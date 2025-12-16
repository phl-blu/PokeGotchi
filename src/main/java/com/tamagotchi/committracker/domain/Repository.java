package com.tamagotchi.committracker.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Domain model representing a Git repository being monitored.
 */
public class Repository {
    private String name;
    private Path path;
    private String remoteUrl;
    private LocalDateTime lastScanned;
    private boolean isAccessible;
    private AuthenticationType authType;
    
    // TODO: Implement constructors, getters, setters in task 2
}