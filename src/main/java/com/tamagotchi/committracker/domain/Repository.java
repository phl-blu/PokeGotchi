package com.tamagotchi.committracker.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

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
    
    public Repository() {}
    
    public Repository(String name, Path path, String remoteUrl, LocalDateTime lastScanned, 
                      boolean isAccessible, AuthenticationType authType) {
        this.name = name;
        this.path = path;
        this.remoteUrl = remoteUrl;
        this.lastScanned = lastScanned;
        this.isAccessible = isAccessible;
        this.authType = authType;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Path getPath() {
        return path;
    }
    
    public void setPath(Path path) {
        this.path = path;
    }
    
    public String getRemoteUrl() {
        return remoteUrl;
    }
    
    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }
    
    public LocalDateTime getLastScanned() {
        return lastScanned;
    }
    
    public void setLastScanned(LocalDateTime lastScanned) {
        this.lastScanned = lastScanned;
    }
    
    public boolean isAccessible() {
        return isAccessible;
    }
    
    public void setAccessible(boolean accessible) {
        isAccessible = accessible;
    }
    
    public AuthenticationType getAuthType() {
        return authType;
    }
    
    public void setAuthType(AuthenticationType authType) {
        this.authType = authType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Repository that = (Repository) o;
        return Objects.equals(path, that.path);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
    
    @Override
    public String toString() {
        return "Repository{" +
               "name='" + name + '\'' +
               ", path=" + path +
               ", remoteUrl='" + remoteUrl + '\'' +
               ", lastScanned=" + lastScanned +
               ", isAccessible=" + isAccessible +
               ", authType=" + authType +
               '}';
    }
}