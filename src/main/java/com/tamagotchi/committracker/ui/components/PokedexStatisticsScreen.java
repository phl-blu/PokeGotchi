package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.service.StatisticsService;
import com.tamagotchi.committracker.service.StatisticsService.ProductivityMetrics;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

import java.util.List;

/**
 * Compact statistics screen for the Pokedex UI.
 * Displays key metrics in a retro Pokedex-themed container
 * optimized for the small screen area (170x180 pixels).
 * 
 * Requirements: 6.7
 */
public class PokedexStatisticsScreen extends VBox {
    
    // Screen dimensions from PokedexTheme
    private static final int SCREEN_WIDTH = PokedexTheme.SCREEN_WIDTH;
    private static final int SCREEN_HEIGHT = PokedexTheme.SCREEN_HEIGHT;
    
    // Compact font sizes for small screen
    private static final int TITLE_FONT_SIZE = 9;
    private static final int LABEL_FONT_SIZE = 7;
    private static final int VALUE_FONT_SIZE = 8;
    
    private StatisticsTab statisticsTab;
    private StatisticsService statisticsService;
    private Label titleLabel;
    private VBox metricsContainer;
    private ScrollPane scrollPane;
    
    /**
     * Creates a new PokedexStatisticsScreen with Pokedex styling.
     */
    public PokedexStatisticsScreen() {
        this.statisticsService = new StatisticsService();
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
        titleLabel = new Label("STATS");
        titleLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(TITLE_FONT_SIZE),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        titleLabel.setTextFill(Color.web(PokedexTheme.TEXT_WHITE));
        
        // Create compact metrics container
        metricsContainer = new VBox(4);
        metricsContainer.setPadding(new Insets(2));
        metricsContainer.setStyle("-fx-background-color: transparent;");
        metricsContainer.setAlignment(Pos.TOP_CENTER);
        
        // Wrap in ScrollPane for scrolling
        scrollPane = new ScrollPane(metricsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Add placeholder metrics
        updateMetricsDisplay(null, 0);
        
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
     * Updates all statistics displays with new commit data.
     * 
     * @param commits List of commits to analyze
     * @param species Current Pokemon species
     * @param stage Current evolution stage
     * @param currentStreak Current commit streak
     */
    public void updateStatistics(List<Commit> commits, PokemonSpecies species, 
                                  EvolutionStage stage, int currentStreak) {
        updateMetricsDisplay(commits, currentStreak);
    }
    
    /**
     * Updates the metrics display with compact layout.
     * 
     * @param commits List of commits to analyze
     * @param currentStreak Current commit streak
     */
    private void updateMetricsDisplay(List<Commit> commits, int currentStreak) {
        metricsContainer.getChildren().clear();
        
        ProductivityMetrics metrics = statisticsService.calculateProductivityMetrics(commits, currentStreak);
        
        // Create compact metrics grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(8);
        metricsGrid.setVgap(4);
        metricsGrid.setAlignment(Pos.CENTER);
        metricsGrid.setPadding(new Insets(4));
        metricsGrid.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 5;");
        
        // Add key metrics in a compact 2-column layout
        int row = 0;
        addCompactMetricRow(metricsGrid, row++, "Commits", String.valueOf(metrics.getTotalCommits()));
        addCompactMetricRow(metricsGrid, row++, "Total XP", String.valueOf(metrics.getTotalXp()));
        addCompactMetricRow(metricsGrid, row++, "Repos", String.valueOf(metrics.getTotalRepositories()));
        addCompactMetricRow(metricsGrid, row++, "Active Days", String.valueOf(metrics.getActiveDays()));
        addCompactMetricRow(metricsGrid, row++, "Streak", metrics.getCurrentStreak() + " days");
        addCompactMetricRow(metricsGrid, row++, "Best Streak", metrics.getLongestStreak() + " days");
        addCompactMetricRow(metricsGrid, row++, "Avg/Day", String.format("%.1f", metrics.getAverageCommitsPerDay()));
        addCompactMetricRow(metricsGrid, row++, "XP/Day", String.format("%.1f", metrics.getAverageXpPerDay()));
        
        metricsContainer.getChildren().add(metricsGrid);
        
        // Add a simple activity indicator
        VBox activityBox = createActivityIndicator(metrics);
        metricsContainer.getChildren().add(activityBox);
    }
    
    /**
     * Adds a compact metric row to the grid.
     * 
     * @param grid The grid pane
     * @param row Row index
     * @param label Metric label
     * @param value Metric value
     */
    private void addCompactMetricRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label + ":");
        labelNode.setStyle(PokedexTheme.getPixelFontStyle(LABEL_FONT_SIZE));
        labelNode.setTextFill(Color.web("#AAAAAA"));
        
        Label valueNode = new Label(value);
        valueNode.setStyle(PokedexTheme.getPixelFontStyle(VALUE_FONT_SIZE));
        valueNode.setTextFill(Color.web(PokedexTheme.TEXT_WHITE));
        
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
    
    /**
     * Creates a simple activity indicator showing recent activity level.
     * 
     * @param metrics The productivity metrics
     * @return VBox containing the activity indicator
     */
    private VBox createActivityIndicator(ProductivityMetrics metrics) {
        VBox activityBox = new VBox(2);
        activityBox.setAlignment(Pos.CENTER);
        activityBox.setPadding(new Insets(4));
        activityBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 5;");
        
        // Activity level label
        Label activityLabel = new Label("Activity Level");
        activityLabel.setStyle(PokedexTheme.getPixelFontStyle(LABEL_FONT_SIZE));
        activityLabel.setTextFill(Color.web("#AAAAAA"));
        
        // Simple activity bar using text characters
        double avgCommits = metrics.getAverageCommitsPerDay();
        String activityLevel;
        String activityColor;
        
        if (avgCommits >= 5) {
            activityLevel = "★★★★★ AMAZING";
            activityColor = "#FFD700"; // Gold
        } else if (avgCommits >= 3) {
            activityLevel = "★★★★☆ GREAT";
            activityColor = "#4ADE80"; // Green
        } else if (avgCommits >= 2) {
            activityLevel = "★★★☆☆ GOOD";
            activityColor = "#60A5FA"; // Blue
        } else if (avgCommits >= 1) {
            activityLevel = "★★☆☆☆ OK";
            activityColor = "#FBBF24"; // Yellow
        } else if (avgCommits > 0) {
            activityLevel = "★☆☆☆☆ LOW";
            activityColor = "#F87171"; // Red
        } else {
            activityLevel = "☆☆☆☆☆ NONE";
            activityColor = "#6B7280"; // Gray
        }
        
        Label levelLabel = new Label(activityLevel);
        levelLabel.setStyle(PokedexTheme.getPixelFontStyle(VALUE_FONT_SIZE));
        levelLabel.setTextFill(Color.web(activityColor));
        
        activityBox.getChildren().addAll(activityLabel, levelLabel);
        return activityBox;
    }
    
    /**
     * Gets the underlying StatisticsTab component.
     * 
     * @return The StatisticsTab instance (null for compact implementation)
     */
    public StatisticsTab getStatisticsTab() {
        return statisticsTab;
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
