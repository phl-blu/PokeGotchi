package com.tamagotchi.committracker.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the commit baseline date for GitHub commit tracking.
 * The baseline is the date from which commits are tracked (typically the authentication date).
 * 
 * Requirements: 4.2, 4.3
 * - 4.2: Set authentication date as commit baseline
 * - 4.3: Only retrieve commits made after the baseline date
 */
public class CommitBaselineManager {
    
    private static final Logger LOGGER = Logger.getLogger(CommitBaselineManager.class.getName());
    private static final String BASELINE_FILE_NAME = "commit_baseline.txt";
    
    private final Path storageDirectory;
    
    private volatile Instant baseline;
    
    /**
     * Creates a CommitBaselineManager with the default storage directory.
     */
    public CommitBaselineManager() {
        this(getDefaultStorageDirectory());
    }
    
    /**
     * Creates a CommitBaselineManager with a custom storage directory.
     * 
     * @param storageDirectory the directory to store baseline data
     */
    public CommitBaselineManager(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        
        // Load baseline from disk on initialization
        loadFromDisk();
    }
    
    /**
     * Gets the default storage directory.
     */
    private static Path getDefaultStorageDirectory() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".pokemon-commit-tracker", "config");
    }
    
    /**
     * Sets the commit baseline date.
     * This is typically called when the user first authenticates with GitHub.
     * 
     * Requirement 4.2: Set authentication date as baseline
     * 
     * @param baseline the baseline instant
     */
    public void setBaseline(Instant baseline) {
        if (baseline == null) {
            throw new IllegalArgumentException("Baseline cannot be null");
        }
        
        this.baseline = baseline;
        saveToDisk();
        LOGGER.info("Commit baseline set to: " + baseline);
    }
    
    /**
     * Gets the current commit baseline.
     * 
     * @return Optional containing the baseline, or empty if not set
     */
    public Optional<Instant> getBaseline() {
        return Optional.ofNullable(baseline);
    }
    
    /**
     * Checks if a baseline has been set.
     * 
     * @return true if baseline is set
     */
    public boolean hasBaseline() {
        return baseline != null;
    }
    
    /**
     * Clears the baseline.
     * This should be called when the user logs out or re-authenticates.
     */
    public void clearBaseline() {
        this.baseline = null;
        
        // Delete baseline file
        Path baselineFile = storageDirectory.resolve(BASELINE_FILE_NAME);
        try {
            Files.deleteIfExists(baselineFile);
            LOGGER.info("Commit baseline cleared");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete baseline file", e);
        }
    }
    
    /**
     * Checks if a commit timestamp is after the baseline.
     * 
     * Requirement 4.3: Filter commits by baseline date
     * 
     * @param commitTime the commit timestamp
     * @return true if the commit is after the baseline (or no baseline is set)
     */
    public boolean isAfterBaseline(Instant commitTime) {
        if (baseline == null || commitTime == null) {
            return true; // No baseline means accept all commits
        }
        return !commitTime.isBefore(baseline);
    }
    
    /**
     * Saves the baseline to disk.
     */
    private void saveToDisk() {
        try {
            // Ensure storage directory exists
            Files.createDirectories(storageDirectory);
            
            Path baselineFile = storageDirectory.resolve(BASELINE_FILE_NAME);
            
            // Use simple string format instead of Gson to avoid module access issues
            String content = baseline != null ? baseline.toString() : "";
            
            Files.writeString(baselineFile, content);
            LOGGER.fine("Saved commit baseline to disk");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save commit baseline", e);
        }
    }
    
    /**
     * Loads the baseline from disk.
     */
    private void loadFromDisk() {
        Path baselineFile = storageDirectory.resolve(BASELINE_FILE_NAME);
        
        if (!Files.exists(baselineFile)) {
            LOGGER.fine("No commit baseline file found");
            return;
        }
        
        try {
            String content = Files.readString(baselineFile).trim();
            
            if (!content.isEmpty()) {
                this.baseline = Instant.parse(content);
                LOGGER.info("Loaded commit baseline from disk: " + baseline);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load commit baseline", e);
            this.baseline = null;
        }
    }
}
