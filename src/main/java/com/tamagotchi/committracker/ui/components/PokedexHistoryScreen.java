package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.pokemon.XPSystem;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Compact history screen for the Pokedex UI.
 * Displays commit history in a retro Pokedex-themed container
 * optimized for the small screen area (170x180 pixels).
 * 
 * Requirements: 6.6
 */
public class PokedexHistoryScreen extends VBox {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    
    // Screen dimensions from PokedexTheme
    private static final int SCREEN_WIDTH = PokedexTheme.SCREEN_WIDTH;
    private static final int SCREEN_HEIGHT = PokedexTheme.SCREEN_HEIGHT;
    
    // Compact font sizes for small screen
    private static final int TITLE_FONT_SIZE = 9;
    private static final int CONTENT_FONT_SIZE = 7;
    private static final int DETAIL_FONT_SIZE = 6;
    
    private HistoryTab historyTab;
    private Label titleLabel;
    private VBox commitListContainer;
    private ScrollPane scrollPane;
    private XPSystem xpSystem;
    
    /**
     * Creates a new PokedexHistoryScreen with Pokedex styling.
     */
    public PokedexHistoryScreen() {
        this.xpSystem = new XPSystem();
        initializeComponent();
    }
    
    /**
     * Initializes the JavaFX components with Pokedex styling.
     */
    private void initializeComponent() {
        // Fill the entire screen area
        this.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setMinSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setMaxSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        
        this.setSpacing(4);
        this.setPadding(new Insets(6));
        this.setAlignment(Pos.TOP_CENTER);
        this.setStyle(getContainerStyle());
        
        // Title label with pixel font - compact
        titleLabel = new Label("HISTORY");
        titleLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(TITLE_FONT_SIZE),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        titleLabel.setTextFill(Color.web(PokedexTheme.TEXT_WHITE));
        
        // Create compact commit list
        commitListContainer = new VBox(3);
        commitListContainer.setPadding(new Insets(2));
        commitListContainer.setStyle("-fx-background-color: transparent;");
        
        // Wrap in ScrollPane for scrolling
        scrollPane = new ScrollPane(commitListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Add placeholder message
        Label emptyLabel = new Label("No commits yet");
        emptyLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(CONTENT_FONT_SIZE),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        emptyLabel.setTextFill(Color.web("#AAAAAA"));
        commitListContainer.getChildren().add(emptyLabel);
        
        this.getChildren().addAll(titleLabel, scrollPane);
    }
    
    /**
     * Gets the CSS style for the container.
     * 
     * @return CSS style string
     */
    private String getContainerStyle() {
        return "-fx-background-color: transparent;";
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
            emptyLabel.setStyle(PokedexTheme.combineStyles(
                PokedexTheme.getPixelFontStyle(CONTENT_FONT_SIZE),
                PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
            ));
            emptyLabel.setTextFill(Color.web("#AAAAAA"));
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
        
        // Display up to 20 most recent commits (compact view)
        int displayCount = Math.min(commits.size(), 20);
        for (int i = 0; i < displayCount; i++) {
            Commit commit = commits.get(i);
            VBox commitEntry = createCompactCommitEntry(commit);
            commitListContainer.getChildren().add(commitEntry);
        }
    }
    
    /**
     * Creates a compact visual entry for a single commit.
     * 
     * @param commit The commit to display
     * @return VBox containing the compact commit entry
     */
    private VBox createCompactCommitEntry(Commit commit) {
        VBox entry = new VBox(1);
        entry.setPadding(new Insets(3, 4, 3, 4));
        entry.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 3;");
        
        // First line: message (truncated) + XP
        HBox topLine = new HBox(4);
        topLine.setAlignment(Pos.CENTER_LEFT);
        
        String message = commit.getMessage() != null ? commit.getMessage() : "No message";
        if (message.length() > 25) {
            message = message.substring(0, 22) + "...";
        }
        Label messageLabel = new Label(message);
        messageLabel.setStyle(PokedexTheme.getPixelFontStyle(CONTENT_FONT_SIZE));
        messageLabel.setTextFill(Color.web(PokedexTheme.TEXT_WHITE));
        HBox.setHgrow(messageLabel, Priority.ALWAYS);
        
        int xpGained = xpSystem.calculateXPFromCommit(commit);
        Label xpLabel = new Label("+" + xpGained);
        xpLabel.setStyle(PokedexTheme.getPixelFontStyle(CONTENT_FONT_SIZE));
        xpLabel.setTextFill(Color.web("#4ADE80")); // Green for XP
        
        topLine.getChildren().addAll(messageLabel, xpLabel);
        
        // Second line: time + repo
        String timeStr = formatTimestamp(commit.getTimestamp());
        String repoName = commit.getRepositoryName() != null ? commit.getRepositoryName() : "";
        if (repoName.length() > 12) {
            repoName = repoName.substring(0, 9) + "...";
        }
        
        Label detailsLabel = new Label(timeStr + " • " + repoName);
        detailsLabel.setStyle(PokedexTheme.getPixelFontStyle(DETAIL_FONT_SIZE));
        detailsLabel.setTextFill(Color.web("#AAAAAA"));
        
        entry.getChildren().addAll(topLine, detailsLabel);
        return entry;
    }
    
    /**
     * Formats a timestamp for compact display.
     * 
     * @param timestamp The timestamp to format
     * @return Formatted timestamp string
     */
    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = ChronoUnit.MINUTES.between(timestamp, now);
        long hoursAgo = ChronoUnit.HOURS.between(timestamp, now);
        
        if (minutesAgo < 1) {
            return "Now";
        } else if (minutesAgo < 60) {
            return minutesAgo + "m";
        } else if (hoursAgo < 24) {
            return hoursAgo + "h";
        } else {
            return timestamp.format(TIME_FORMATTER);
        }
    }
    
    /**
     * Updates the Pokemon status in the history display.
     * 
     * @param species The current Pokemon species
     * @param stage The current evolution stage
     * @param xp Current XP
     * @param streak Current commit streak in days
     */
    public void updatePokemonStatus(PokemonSpecies species, EvolutionStage stage, int xp, int streak) {
        // Status is shown in the main display, not needed here for compact view
    }
    
    /**
     * Gets the underlying HistoryTab component.
     * 
     * @return The HistoryTab instance (null for compact implementation)
     */
    public HistoryTab getHistoryTab() {
        return historyTab;
    }
    
    /**
     * Refreshes the theme styling.
     */
    public void refreshTheme() {
        this.setStyle(getContainerStyle());
        if (titleLabel != null) {
            titleLabel.setStyle(PokedexTheme.combineStyles(
                PokedexTheme.getPixelFontStyle(TITLE_FONT_SIZE),
                PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
            ));
        }
    }
}
