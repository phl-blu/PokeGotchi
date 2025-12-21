package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.XPSystem;
import com.tamagotchi.committracker.ui.theme.UITheme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * UI component for displaying commit history in expanded mode.
 * Shows a scrollable list of commits with Pokemon status information.
 * 
 * Requirements: 1.3, 5.1, 5.2, 5.3, 5.4
 */
public class HistoryTab extends VBox {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    
    private VBox commitListContainer;
    private ScrollPane scrollPane;
    private Label statusLabel;
    private Label xpProgressLabel;
    private Label streakLabel;
    private Label levelLabel;
    
    // Current state
    private PokemonSpecies currentSpecies;
    private EvolutionStage currentStage;
    private int currentXP;
    private int currentStreak;
    private int currentLevel;
    
    private XPSystem xpSystem;
    
    public HistoryTab() {
        this.xpSystem = new XPSystem();
        initializeComponent();
    }
    
    /**
     * Initializes the JavaFX components for the history tab.
     */
    private void initializeComponent() {
        this.setSpacing(UITheme.LARGE_SPACING);
        this.setPadding(new Insets(UITheme.LARGE_PADDING));
        this.setAlignment(Pos.TOP_CENTER);
        this.setStyle(UITheme.getTransparentStyle());
        
        // Pokemon status section
        VBox statusSection = createStatusSection();
        
        // Commit history section
        VBox historySection = createHistorySection();
        
        this.getChildren().addAll(statusSection, historySection);
    }
    
    /**
     * Creates the Pokemon status display section.
     */
    private VBox createStatusSection() {
        VBox statusBox = new VBox(UITheme.MEDIUM_SPACING);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(UITheme.LARGE_PADDING));
        statusBox.setStyle(UITheme.getStatusSectionStyle());
        
        // Pokemon name and stage
        statusLabel = new Label("Pokemon Status");
        statusLabel.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.BOLD, UITheme.HEADER_FONT_SIZE));
        statusLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
        
        // Level display
        levelLabel = new Label("Level: 1");
        levelLabel.setFont(Font.font(UITheme.PRIMARY_FONT, UITheme.NORMAL_FONT_SIZE));
        levelLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
        
        // XP progress
        xpProgressLabel = new Label("XP: 0 / 200");
        xpProgressLabel.setFont(Font.font(UITheme.PRIMARY_FONT, UITheme.NORMAL_FONT_SIZE));
        xpProgressLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
        
        // Streak counter
        streakLabel = new Label("Streak: 0 days");
        streakLabel.setFont(Font.font(UITheme.PRIMARY_FONT, UITheme.NORMAL_FONT_SIZE));
        streakLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
        
        statusBox.getChildren().addAll(statusLabel, levelLabel, xpProgressLabel, streakLabel);
        return statusBox;
    }
    
    /**
     * Creates the commit history section with scrollable list.
     */
    private VBox createHistorySection() {
        VBox historyBox = new VBox(UITheme.MEDIUM_SPACING);
        historyBox.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(historyBox, Priority.ALWAYS);
        
        // Header
        Label headerLabel = new Label("Recent Commits");
        headerLabel.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.BOLD, UITheme.NORMAL_FONT_SIZE));
        headerLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
        
        // Scrollable commit list
        commitListContainer = new VBox(UITheme.MEDIUM_SPACING);
        commitListContainer.setPadding(new Insets(UITheme.MEDIUM_SPACING));
        
        scrollPane = new ScrollPane(commitListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        historyBox.getChildren().addAll(headerLabel, scrollPane);
        return historyBox;
    }
    
    /**
     * Updates the Pokemon status display.
     * 
     * @param species The current Pokemon species
     * @param stage The current evolution stage
     * @param xp Current XP
     * @param streak Current commit streak in days
     */
    public void updatePokemonStatus(PokemonSpecies species, EvolutionStage stage, int xp, int streak) {
        this.currentSpecies = species;
        this.currentStage = stage;
        this.currentXP = xp;
        this.currentStreak = streak;
        
        // Update XP system
        xpSystem.setCurrentXP(xp);
        this.currentLevel = xpSystem.getLevel() + 1; // Level is 0-indexed
        
        // Update labels
        String speciesName = species != null ? formatSpeciesName(species) : "Unknown";
        String stageName = stage != null ? formatStageName(stage) : "Unknown";
        statusLabel.setText(speciesName + " (" + stageName + ")");
        
        levelLabel.setText("Level: " + currentLevel);
        
        int[] thresholds = XPSystem.getEvolutionXPThresholds();
        int nextThreshold = currentLevel < thresholds.length ? thresholds[currentLevel] : thresholds[thresholds.length - 1];
        xpProgressLabel.setText("XP: " + xp + " / " + nextThreshold);
        
        streakLabel.setText("Streak: " + streak + " days");
    }
    
    /**
     * Updates the commit history display.
     * 
     * @param history The commit history to display
     */
    public void updateCommitHistory(CommitHistory history) {
        commitListContainer.getChildren().clear();
        
        if (history == null || history.getRecentCommits().isEmpty()) {
            Label emptyLabel = new Label("No commits yet");
            emptyLabel.setTextFill(Color.GRAY);
            commitListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        List<Commit> commits = history.getRecentCommits();
        // Sort by timestamp descending (most recent first)
        commits.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        
        // Display up to 50 most recent commits
        int displayCount = Math.min(commits.size(), 50);
        for (int i = 0; i < displayCount; i++) {
            Commit commit = commits.get(i);
            HBox commitEntry = createCommitEntry(commit);
            commitListContainer.getChildren().add(commitEntry);
        }
    }
    
    /**
     * Updates the commit history display from a list of commits.
     * 
     * @param commits The list of commits to display
     */
    public void updateCommitList(List<Commit> commits) {
        commitListContainer.getChildren().clear();
        
        if (commits == null || commits.isEmpty()) {
            Label emptyLabel = new Label("No commits yet");
            emptyLabel.setTextFill(Color.GRAY);
            commitListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        // Sort by timestamp descending (most recent first)
        commits.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        
        // Display up to 50 most recent commits
        int displayCount = Math.min(commits.size(), 50);
        for (int i = 0; i < displayCount; i++) {
            Commit commit = commits.get(i);
            HBox commitEntry = createCommitEntry(commit);
            commitListContainer.getChildren().add(commitEntry);
        }
    }
    
    /**
     * Creates a visual entry for a single commit.
     * Displays: message, timestamp, repository name, author, and XP gained.
     * 
     * @param commit The commit to display
     * @return HBox containing the commit entry
     */
    private HBox createCommitEntry(Commit commit) {
        HBox entry = new HBox(UITheme.LARGE_SPACING);
        entry.setAlignment(Pos.CENTER_LEFT);
        entry.setPadding(new Insets(UITheme.MEDIUM_PADDING));
        entry.setStyle(UITheme.getCommitEntryStyle());
        
        // Left side: commit info
        VBox infoBox = new VBox(UITheme.SMALL_SPACING);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        // Commit message (truncated if too long)
        String message = commit.getMessage() != null ? commit.getMessage() : "No message";
        if (message.length() > 40) {
            message = message.substring(0, 37) + "...";
        }
        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.MEDIUM, UITheme.SMALL_FONT_SIZE));
        messageLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
        messageLabel.setWrapText(true);
        
        // Repository and timestamp
        String repoName = commit.getRepositoryName() != null ? commit.getRepositoryName() : "Unknown";
        String timeStr = formatTimestamp(commit.getTimestamp());
        Label detailsLabel = new Label(repoName + " • " + timeStr);
        detailsLabel.setFont(Font.font(UITheme.PRIMARY_FONT, UITheme.TINY_FONT_SIZE));
        detailsLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
        
        // Author
        String author = commit.getAuthor() != null ? commit.getAuthor() : "Unknown";
        Label authorLabel = new Label("by " + author);
        authorLabel.setFont(Font.font(UITheme.PRIMARY_FONT, UITheme.MICRO_FONT_SIZE));
        authorLabel.setTextFill(Color.web(UITheme.TERTIARY_TEXT_COLOR));
        
        infoBox.getChildren().addAll(messageLabel, detailsLabel, authorLabel);
        
        // Right side: XP gained
        int xpGained = xpSystem.calculateXPFromCommit(commit);
        Label xpLabel = new Label("+" + xpGained + " XP");
        xpLabel.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.BOLD, UITheme.SMALL_FONT_SIZE));
        xpLabel.setTextFill(Color.web(UITheme.XP_COLOR));
        xpLabel.setMinWidth(50);
        xpLabel.setAlignment(Pos.CENTER_RIGHT);
        
        entry.getChildren().addAll(infoBox, xpLabel);
        return entry;
    }
    
    /**
     * Formats a timestamp for display.
     * Shows relative time for recent commits, absolute time for older ones.
     * 
     * @param timestamp The timestamp to format
     * @return Formatted timestamp string
     */
    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "Unknown time";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = ChronoUnit.MINUTES.between(timestamp, now);
        long hoursAgo = ChronoUnit.HOURS.between(timestamp, now);
        long daysAgo = ChronoUnit.DAYS.between(timestamp, now);
        
        if (minutesAgo < 1) {
            return "Just now";
        } else if (minutesAgo < 60) {
            return minutesAgo + " min ago";
        } else if (hoursAgo < 24) {
            return hoursAgo + " hour" + (hoursAgo == 1 ? "" : "s") + " ago";
        } else if (daysAgo < 7) {
            return daysAgo + " day" + (daysAgo == 1 ? "" : "s") + " ago";
        } else {
            return timestamp.format(TIME_FORMATTER);
        }
    }
    
    /**
     * Formats a Pokemon species name for display.
     * 
     * @param species The species to format
     * @return Formatted species name
     */
    private String formatSpeciesName(PokemonSpecies species) {
        String name = species.name();
        // Capitalize first letter, lowercase rest
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
    
    /**
     * Formats an evolution stage name for display.
     * 
     * @param stage The stage to format
     * @return Formatted stage name
     */
    private String formatStageName(EvolutionStage stage) {
        switch (stage) {
            case EGG:
                return "Egg";
            case BASIC:
                return "Basic";
            case STAGE_1:
                return "Stage 1";
            case STAGE_2:
                return "Stage 2";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Formats a commit entry as a string for testing purposes.
     * Contains all required information: message, timestamp, repository, author, XP.
     * 
     * @param commit The commit to format
     * @return Formatted string containing all commit information
     */
    public static String formatCommitForDisplay(Commit commit) {
        if (commit == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Message
        String message = commit.getMessage() != null ? commit.getMessage() : "";
        sb.append("message:").append(message).append("|");
        
        // Timestamp
        String timestamp = commit.getTimestamp() != null ? commit.getTimestamp().toString() : "";
        sb.append("timestamp:").append(timestamp).append("|");
        
        // Repository
        String repo = commit.getRepositoryName() != null ? commit.getRepositoryName() : "";
        sb.append("repository:").append(repo).append("|");
        
        // Author
        String author = commit.getAuthor() != null ? commit.getAuthor() : "";
        sb.append("author:").append(author).append("|");
        
        // XP (calculated)
        XPSystem xpCalc = new XPSystem();
        int xp = xpCalc.calculateXPFromCommit(commit);
        sb.append("xp:").append(xp);
        
        return sb.toString();
    }
    
    /**
     * Checks if a formatted commit display string contains all required information.
     * Required: message, timestamp, repository, author, XP.
     * 
     * @param formattedCommit The formatted commit string
     * @param commit The original commit
     * @return true if all required information is present
     */
    public static boolean containsAllRequiredInfo(String formattedCommit, Commit commit) {
        if (formattedCommit == null || commit == null) {
            return false;
        }
        
        // Check message is present (if commit has one)
        if (commit.getMessage() != null && !commit.getMessage().isEmpty()) {
            if (!formattedCommit.contains("message:" + commit.getMessage())) {
                return false;
            }
        }
        
        // Check timestamp is present (if commit has one)
        if (commit.getTimestamp() != null) {
            if (!formattedCommit.contains("timestamp:" + commit.getTimestamp().toString())) {
                return false;
            }
        }
        
        // Check repository is present (if commit has one)
        if (commit.getRepositoryName() != null && !commit.getRepositoryName().isEmpty()) {
            if (!formattedCommit.contains("repository:" + commit.getRepositoryName())) {
                return false;
            }
        }
        
        // Check author is present (if commit has one)
        if (commit.getAuthor() != null && !commit.getAuthor().isEmpty()) {
            if (!formattedCommit.contains("author:" + commit.getAuthor())) {
                return false;
            }
        }
        
        // Check XP is present
        if (!formattedCommit.contains("xp:")) {
            return false;
        }
        
        return true;
    }
    
    // Getters for testing
    public PokemonSpecies getCurrentSpecies() {
        return currentSpecies;
    }
    
    public EvolutionStage getCurrentStage() {
        return currentStage;
    }
    
    public int getCurrentXP() {
        return currentXP;
    }
    
    public int getCurrentStreak() {
        return currentStreak;
    }
    
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    /**
     * Refreshes the UI theme for all components when Windows theme changes.
     * Updates colors, backgrounds, and text styling to match the current Windows theme.
     */
    public void refreshTheme() {
        try {
            // Update main container style
            this.setStyle(UITheme.getTransparentStyle());
            
            // Update status section styling
            VBox statusSection = (VBox) this.getChildren().get(0);
            if (statusSection != null) {
                statusSection.setStyle(UITheme.getStatusSectionStyle());
            }
            
            // Update all labels with current theme colors
            if (statusLabel != null) {
                statusLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
            }
            if (levelLabel != null) {
                levelLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
            }
            if (xpProgressLabel != null) {
                xpProgressLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
            }
            if (streakLabel != null) {
                streakLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
            }
            
            // Update commit entries
            if (commitListContainer != null) {
                for (var node : commitListContainer.getChildren()) {
                    if (node instanceof HBox) {
                        HBox commitEntry = (HBox) node;
                        commitEntry.setStyle(UITheme.getCommitEntryStyle());
                        
                        // Update text colors in commit entry
                        updateCommitEntryColors(commitEntry);
                    }
                }
            }
            
            System.out.println("🎨 HistoryTab theme refreshed");
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to refresh HistoryTab theme: " + e.getMessage());
        }
    }
    
    /**
     * Updates text colors in a commit entry to match current theme.
     * 
     * @param commitEntry The commit entry HBox to update
     */
    private void updateCommitEntryColors(HBox commitEntry) {
        try {
            // Find the info VBox (first child)
            if (commitEntry.getChildren().size() > 0 && commitEntry.getChildren().get(0) instanceof VBox) {
                VBox infoBox = (VBox) commitEntry.getChildren().get(0);
                
                // Update message label (first child)
                if (infoBox.getChildren().size() > 0 && infoBox.getChildren().get(0) instanceof Label) {
                    Label messageLabel = (Label) infoBox.getChildren().get(0);
                    messageLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
                }
                
                // Update details label (second child)
                if (infoBox.getChildren().size() > 1 && infoBox.getChildren().get(1) instanceof Label) {
                    Label detailsLabel = (Label) infoBox.getChildren().get(1);
                    detailsLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
                }
                
                // Update author label (third child)
                if (infoBox.getChildren().size() > 2 && infoBox.getChildren().get(2) instanceof Label) {
                    Label authorLabel = (Label) infoBox.getChildren().get(2);
                    authorLabel.setTextFill(Color.web(UITheme.TERTIARY_TEXT_COLOR));
                }
            }
            
            // Update XP label (second child of commit entry)
            if (commitEntry.getChildren().size() > 1 && commitEntry.getChildren().get(1) instanceof Label) {
                Label xpLabel = (Label) commitEntry.getChildren().get(1);
                xpLabel.setTextFill(Color.web(UITheme.XP_COLOR));
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to update commit entry colors: " + e.getMessage());
        }
    }
}
