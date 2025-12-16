package com.tamagotchi.committracker.domain;

import java.time.LocalDateTime;
import java.util.Objects;

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
    
    public Commit() {}
    
    public Commit(String hash, String message, String author, LocalDateTime timestamp, 
                  String repositoryName, String repositoryPath) {
        this.hash = hash;
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
        this.repositoryName = repositoryName;
        this.repositoryPath = repositoryPath;
    }
    
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getRepositoryName() {
        return repositoryName;
    }
    
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }
    
    public String getRepositoryPath() {
        return repositoryPath;
    }
    
    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return Objects.equals(hash, commit.hash) &&
               Objects.equals(repositoryPath, commit.repositoryPath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(hash, repositoryPath);
    }
    
    @Override
    public String toString() {
        return "Commit{" +
               "hash='" + hash + '\'' +
               ", message='" + message + '\'' +
               ", author='" + author + '\'' +
               ", timestamp=" + timestamp +
               ", repositoryName='" + repositoryName + '\'' +
               ", repositoryPath='" + repositoryPath + '\'' +
               '}';
    }
}