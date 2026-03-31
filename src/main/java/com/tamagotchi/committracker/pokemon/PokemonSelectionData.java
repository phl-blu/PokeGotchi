package com.tamagotchi.committracker.pokemon;

import com.tamagotchi.committracker.util.FileUtils;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Manages Pokemon selection persistence.
 * Stores the user's selected starter Pokemon and ensures the selection screen
 * is only shown once (on first application launch).
 * Also persists GitHub account data, commit baseline, and last sync timestamp.
 * 
 * Requirements: 4.2, 10.2
 */
public class PokemonSelectionData {
    
    private static final String SELECTION_FILE = "pokemon-selection.properties";
    private static final String SELECTED_STARTER_KEY = "selected.starter";
    private static final String HAS_SELECTED_KEY = "has.selected";
    private static final String SELECTION_TIMESTAMP_KEY = "selection.timestamp";
    private static final String GITHUB_USER_ID_KEY = "github.user.id";
    private static final String GITHUB_USERNAME_KEY = "github.username";
    private static final String GITHUB_AUTHENTICATED_KEY = "github.authenticated";
    /** ISO-8601 epoch-second string for the commit baseline (authentication date). Requirements: 4.2 */
    private static final String COMMIT_BASELINE_KEY = "github.commit.baseline";
    /** ISO-8601 epoch-second string for the last successful sync. Requirements: 10.2 */
    private static final String LAST_SYNC_KEY = "github.last.sync";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private PokemonSpecies selectedStarter;
    private boolean hasSelectedStarter;
    private LocalDateTime selectionTimestamp;
    private long githubUserId;
    private String githubUsername;
    private boolean githubAuthenticated;
    /** Commit baseline – the Instant from which GitHub commits are tracked. Requirements: 4.2 */
    private Instant commitBaseline;
    /** Timestamp of the last successful GitHub sync. Requirements: 10.2 */
    private Instant lastSyncTimestamp;
    
    /**
     * Creates a new PokemonSelectionData instance and loads any existing selection.
     */
    public PokemonSelectionData() {
        loadSelection();
    }
    
    /**
     * Loads the saved Pokemon selection from persistent storage.
     */
    private void loadSelection() {
        try {
            Properties props = FileUtils.loadProperties(SELECTION_FILE);
            
            String hasSelected = props.getProperty(HAS_SELECTED_KEY, "false");
            this.hasSelectedStarter = Boolean.parseBoolean(hasSelected);
            
            if (hasSelectedStarter) {
                String starterName = props.getProperty(SELECTED_STARTER_KEY);
                if (starterName != null && !starterName.isEmpty()) {
                    try {
                        this.selectedStarter = PokemonSpecies.valueOf(starterName);
                    } catch (IllegalArgumentException e) {
                        // Invalid species name, reset selection
                        this.hasSelectedStarter = false;
                        this.selectedStarter = null;
                    }
                }
                
                String timestamp = props.getProperty(SELECTION_TIMESTAMP_KEY);
                if (timestamp != null && !timestamp.isEmpty()) {
                    try {
                        this.selectionTimestamp = LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT);
                    } catch (Exception e) {
                        this.selectionTimestamp = null;
                    }
                }
            }
            
            // Load GitHub authentication data
            String githubAuth = props.getProperty(GITHUB_AUTHENTICATED_KEY, "false");
            this.githubAuthenticated = Boolean.parseBoolean(githubAuth);
            
            if (githubAuthenticated) {
                String userIdStr = props.getProperty(GITHUB_USER_ID_KEY);
                if (userIdStr != null && !userIdStr.isEmpty()) {
                    try {
                        this.githubUserId = Long.parseLong(userIdStr);
                    } catch (NumberFormatException e) {
                        this.githubUserId = 0;
                    }
                }
                this.githubUsername = props.getProperty(GITHUB_USERNAME_KEY, "");
            }
            
            // Load commit baseline (Requirements: 4.2)
            String baselineStr = props.getProperty(COMMIT_BASELINE_KEY);
            if (baselineStr != null && !baselineStr.isEmpty()) {
                try {
                    this.commitBaseline = Instant.parse(baselineStr);
                } catch (Exception e) {
                    this.commitBaseline = null;
                }
            }
            
            // Load last sync timestamp (Requirements: 10.2)
            String lastSyncStr = props.getProperty(LAST_SYNC_KEY);
            if (lastSyncStr != null && !lastSyncStr.isEmpty()) {
                try {
                    this.lastSyncTimestamp = Instant.parse(lastSyncStr);
                } catch (Exception e) {
                    this.lastSyncTimestamp = null;
                }
            }
        } catch (IOException e) {
            // No saved selection, user is first-time
            this.hasSelectedStarter = false;
            this.selectedStarter = null;
            this.selectionTimestamp = null;
            this.githubAuthenticated = false;
            this.githubUserId = 0;
            this.githubUsername = null;
            this.commitBaseline = null;
            this.lastSyncTimestamp = null;
        }
    }
    
    /**
     * Saves the Pokemon selection to persistent storage.
     * This prevents the selection screen from showing again.
     * 
     * @param starter The selected starter Pokemon
     */
    public void saveSelection(PokemonSpecies starter) {
        if (starter == null) {
            return;
        }
        
        this.selectedStarter = starter;
        this.hasSelectedStarter = true;
        this.selectionTimestamp = LocalDateTime.now();
        
        try {
            Properties props = new Properties();
            props.setProperty(HAS_SELECTED_KEY, "true");
            props.setProperty(SELECTED_STARTER_KEY, starter.name());
            props.setProperty(SELECTION_TIMESTAMP_KEY, selectionTimestamp.format(TIMESTAMP_FORMAT));
            
            // Preserve GitHub authentication data
            props.setProperty(GITHUB_AUTHENTICATED_KEY, String.valueOf(githubAuthenticated));
            if (githubAuthenticated) {
                props.setProperty(GITHUB_USER_ID_KEY, String.valueOf(githubUserId));
                if (githubUsername != null) {
                    props.setProperty(GITHUB_USERNAME_KEY, githubUsername);
                }
            }
            
            FileUtils.saveProperties(props, SELECTION_FILE);
            System.out.println("✅ Pokemon selection saved: " + starter.name());
        } catch (IOException e) {
            System.err.println("Failed to save Pokemon selection: " + e.getMessage());
        }
    }
    
    /**
     * Saves GitHub authentication data.
     * Links the Pokemon progress to the GitHub account.
     * 
     * Requirements: 1.1, 1.6
     * 
     * @param userId The GitHub user ID
     * @param username The GitHub username
     */
    public void saveGitHubAuth(long userId, String username) {
        this.githubUserId = userId;
        this.githubUsername = username;
        this.githubAuthenticated = true;
        
        try {
            Properties props = new Properties();
            
            // Preserve existing Pokemon selection data
            props.setProperty(HAS_SELECTED_KEY, String.valueOf(hasSelectedStarter));
            if (selectedStarter != null) {
                props.setProperty(SELECTED_STARTER_KEY, selectedStarter.name());
            }
            if (selectionTimestamp != null) {
                props.setProperty(SELECTION_TIMESTAMP_KEY, selectionTimestamp.format(TIMESTAMP_FORMAT));
            }
            
            // Save GitHub authentication data
            props.setProperty(GITHUB_AUTHENTICATED_KEY, "true");
            props.setProperty(GITHUB_USER_ID_KEY, String.valueOf(userId));
            if (username != null) {
                props.setProperty(GITHUB_USERNAME_KEY, username);
            }
            
            // Preserve commit baseline and last sync if already set
            if (commitBaseline != null) {
                props.setProperty(COMMIT_BASELINE_KEY, commitBaseline.toString());
            }
            if (lastSyncTimestamp != null) {
                props.setProperty(LAST_SYNC_KEY, lastSyncTimestamp.toString());
            }
            
            FileUtils.saveProperties(props, SELECTION_FILE);
            System.out.println("✅ GitHub authentication saved for user: " + username + " (ID: " + userId + ")");
        } catch (IOException e) {
            System.err.println("Failed to save GitHub authentication: " + e.getMessage());
        }
    }
    
    /**
     * Clears GitHub authentication data.
     * Used when user revokes access or re-authenticates.
     */
    public void clearGitHubAuth() {
        this.githubUserId = 0;
        this.githubUsername = null;
        this.githubAuthenticated = false;
        
        try {
            Properties props = new Properties();
            
            // Preserve existing Pokemon selection data
            props.setProperty(HAS_SELECTED_KEY, String.valueOf(hasSelectedStarter));
            if (selectedStarter != null) {
                props.setProperty(SELECTED_STARTER_KEY, selectedStarter.name());
            }
            if (selectionTimestamp != null) {
                props.setProperty(SELECTION_TIMESTAMP_KEY, selectionTimestamp.format(TIMESTAMP_FORMAT));
            }
            
            // Clear GitHub authentication data
            props.setProperty(GITHUB_AUTHENTICATED_KEY, "false");
            
            FileUtils.saveProperties(props, SELECTION_FILE);
            System.out.println("✅ GitHub authentication cleared");
        } catch (IOException e) {
            System.err.println("Failed to clear GitHub authentication: " + e.getMessage());
        }
    }
    
    /**
     * Checks if GitHub authentication is saved.
     * 
     * @return true if GitHub authentication data is stored
     */
    public boolean isGitHubAuthenticated() {
        return githubAuthenticated;
    }
    
    /**
     * Gets the stored GitHub user ID.
     * 
     * @return The GitHub user ID, or 0 if not authenticated
     */
    public long getGitHubUserId() {
        return githubUserId;
    }
    
    /**
     * Gets the stored GitHub username.
     * 
     * @return The GitHub username, or null if not authenticated
     */
    public String getGitHubUsername() {
        return githubUsername;
    }
    
    /**
     * Checks if this is a first-time user who hasn't selected a Pokemon yet.
     * 
     * @return true if no Pokemon has been selected, false otherwise
     */
    public boolean isFirstTimeUser() {
        return !hasSelectedStarter;
    }
    
    // ==================== Commit Baseline (Requirements: 4.2) ====================
    
    /**
     * Gets the stored commit baseline date.
     * This is the Instant from which GitHub commits are tracked (typically the authentication date).
     * 
     * @return the commit baseline, or null if not set
     */
    public Instant getCommitBaseline() {
        return commitBaseline;
    }
    
    /**
     * Saves the commit baseline date to persistent storage.
     * 
     * Requirements: 4.2
     * 
     * @param baseline the baseline Instant to persist
     */
    public void saveCommitBaseline(Instant baseline) {
        this.commitBaseline = baseline;
        persistCurrentState();
        System.out.println("✅ Commit baseline saved: " + baseline);
    }
    
    // ==================== Last Sync Timestamp (Requirements: 10.2) ====================
    
    /**
     * Gets the timestamp of the last successful GitHub sync.
     * 
     * @return the last sync Instant, or null if never synced
     */
    public Instant getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }
    
    /**
     * Saves the last sync timestamp to persistent storage.
     * 
     * Requirements: 10.2
     * 
     * @param syncTime the Instant when the sync completed
     */
    public void saveLastSyncTimestamp(Instant syncTime) {
        this.lastSyncTimestamp = syncTime;
        persistCurrentState();
        System.out.println("✅ Last sync timestamp saved: " + syncTime);
    }
    
    /**
     * Writes all current in-memory state back to the properties file.
     * Used internally to persist individual field updates without losing other data.
     */
    private void persistCurrentState() {
        try {
            Properties props = new Properties();
            props.setProperty(HAS_SELECTED_KEY, String.valueOf(hasSelectedStarter));
            if (selectedStarter != null) {
                props.setProperty(SELECTED_STARTER_KEY, selectedStarter.name());
            }
            if (selectionTimestamp != null) {
                props.setProperty(SELECTION_TIMESTAMP_KEY, selectionTimestamp.format(TIMESTAMP_FORMAT));
            }
            props.setProperty(GITHUB_AUTHENTICATED_KEY, String.valueOf(githubAuthenticated));
            if (githubAuthenticated) {
                props.setProperty(GITHUB_USER_ID_KEY, String.valueOf(githubUserId));
                if (githubUsername != null) {
                    props.setProperty(GITHUB_USERNAME_KEY, githubUsername);
                }
            }
            if (commitBaseline != null) {
                props.setProperty(COMMIT_BASELINE_KEY, commitBaseline.toString());
            }
            if (lastSyncTimestamp != null) {
                props.setProperty(LAST_SYNC_KEY, lastSyncTimestamp.toString());
            }
            FileUtils.saveProperties(props, SELECTION_FILE);
        } catch (IOException e) {
            System.err.println("Failed to persist preferences: " + e.getMessage());
        }
    }
    
    /**
     * Gets the selected starter Pokemon.
     * 
     * @return The selected Pokemon species, or null if none selected
     */
    public PokemonSpecies getSelectedStarter() {
        return selectedStarter;
    }
    
    /**
     * Checks if a starter has been selected.
     * 
     * @return true if a starter has been selected
     */
    public boolean hasSelectedStarter() {
        return hasSelectedStarter;
    }
    
    /**
     * Gets the timestamp when the selection was made.
     * 
     * @return The selection timestamp, or null if none
     */
    public LocalDateTime getSelectionTimestamp() {
        return selectionTimestamp;
    }
    
    /**
     * Gets the list of all 9 starter Pokemon options.
     * 
     * @return Array of starter Pokemon species
     */
    public static PokemonSpecies[] getStarterOptions() {
        return new PokemonSpecies[] {
            PokemonSpecies.CHARMANDER,  // Kanto Fire
            PokemonSpecies.CYNDAQUIL,   // Johto Fire
            PokemonSpecies.FUECOCO,     // Paldea Fire
            PokemonSpecies.PIPLUP,      // Sinnoh Water
            PokemonSpecies.MUDKIP,      // Hoenn Water
            PokemonSpecies.FROAKIE,     // Kalos Water
            PokemonSpecies.ROWLET,      // Alola Grass
            PokemonSpecies.GROOKEY,     // Galar Grass
            PokemonSpecies.SNIVY        // Unova Grass
        };
    }
    
    /**
     * Gets the Pokemon type for display purposes.
     * 
     * @param species The Pokemon species
     * @return The type string (Fire, Water, or Grass)
     */
    public static String getPokemonType(PokemonSpecies species) {
        switch (species) {
            case CHARMANDER:
            case CYNDAQUIL:
            case FUECOCO:
                return "Fire";
            case MUDKIP:
            case PIPLUP:
            case FROAKIE:
                return "Water";
            case SNIVY:
            case ROWLET:
            case GROOKEY:
                return "Grass";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Gets a formatted display name for the Pokemon.
     * 
     * @param species The Pokemon species
     * @return Capitalized name (e.g., "Charmander")
     */
    public static String getDisplayName(PokemonSpecies species) {
        String name = species.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
