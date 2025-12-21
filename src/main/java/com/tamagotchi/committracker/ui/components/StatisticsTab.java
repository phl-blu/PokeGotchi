package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.service.StatisticsService;
import com.tamagotchi.committracker.service.StatisticsService.DailyStats;
import com.tamagotchi.committracker.service.StatisticsService.WeeklyStats;
import com.tamagotchi.committracker.service.StatisticsService.ProductivityMetrics;
import com.tamagotchi.committracker.service.StatisticsService.EvolutionEntry;
import com.tamagotchi.committracker.ui.theme.UITheme;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * UI component for displaying commit statistics and productivity metrics.
 * Shows charts, graphs, and analytics for commit activity.
 * 
 * Requirements: 5.5
 */
public class StatisticsTab extends VBox {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd");
    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("MMM dd");
    
    private StatisticsService statisticsService;
    private TabPane tabPane;
    
    // Charts
    private LineChart<String, Number> dailyCommitChart;
    private BarChart<String, Number> weeklyCommitChart;
    private LineChart<String, Number> xpProgressChart;
    
    // Metrics display
    private VBox metricsContainer;
    private VBox evolutionContainer;
    
    public StatisticsTab() {
        this.statisticsService = new StatisticsService();
        initializeComponent();
    }
    
    /**
     * Initializes the JavaFX components for the statistics tab.
     */
    private void initializeComponent() {
        this.setSpacing(UITheme.MEDIUM_SPACING);
        this.setPadding(new Insets(UITheme.LARGE_PADDING));
        this.setAlignment(Pos.TOP_CENTER);
        this.setStyle(UITheme.getTransparentStyle());
        
        // Create tab pane for different statistics views
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        // Create tabs
        Tab chartsTab = createChartsTab();
        Tab metricsTab = createMetricsTab();
        Tab evolutionTab = createEvolutionTab();
        
        tabPane.getTabs().addAll(chartsTab, metricsTab, evolutionTab);
        
        this.getChildren().add(tabPane);
    }
    
    /**
     * Creates the charts tab with commit frequency graphs.
     */
    private Tab createChartsTab() {
        Tab tab = new Tab("Charts");
        
        VBox content = new VBox(UITheme.LARGE_SPACING);
        content.setPadding(new Insets(UITheme.MEDIUM_PADDING));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Daily commits chart
        CategoryAxis dailyXAxis = new CategoryAxis();
        NumberAxis dailyYAxis = new NumberAxis();
        dailyXAxis.setLabel("Date");
        dailyYAxis.setLabel("Commits");
        
        dailyCommitChart = new LineChart<>(dailyXAxis, dailyYAxis);
        dailyCommitChart.setTitle("Daily Commit Activity (Last 30 Days)");
        dailyCommitChart.setPrefHeight(200);
        dailyCommitChart.setCreateSymbols(true);
        dailyCommitChart.setLegendVisible(false);
        
        // Weekly commits chart
        CategoryAxis weeklyXAxis = new CategoryAxis();
        NumberAxis weeklyYAxis = new NumberAxis();
        weeklyXAxis.setLabel("Week");
        weeklyYAxis.setLabel("Commits");
        
        weeklyCommitChart = new BarChart<>(weeklyXAxis, weeklyYAxis);
        weeklyCommitChart.setTitle("Weekly Commit Summary (Last 12 Weeks)");
        weeklyCommitChart.setPrefHeight(200);
        weeklyCommitChart.setLegendVisible(false);
        
        // XP progress chart
        CategoryAxis xpXAxis = new CategoryAxis();
        NumberAxis xpYAxis = new NumberAxis();
        xpXAxis.setLabel("Date");
        xpYAxis.setLabel("XP Gained");
        
        xpProgressChart = new LineChart<>(xpXAxis, xpYAxis);
        xpProgressChart.setTitle("XP Progress (Last 30 Days)");
        xpProgressChart.setPrefHeight(200);
        xpProgressChart.setCreateSymbols(true);
        xpProgressChart.setLegendVisible(false);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        content.getChildren().addAll(dailyCommitChart, weeklyCommitChart, xpProgressChart);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    /**
     * Creates the metrics tab with productivity statistics.
     */
    private Tab createMetricsTab() {
        Tab tab = new Tab("Metrics");
        
        metricsContainer = new VBox(UITheme.LARGE_SPACING);
        metricsContainer.setPadding(new Insets(UITheme.MEDIUM_PADDING));
        metricsContainer.setAlignment(Pos.TOP_CENTER);
        
        ScrollPane scrollPane = new ScrollPane(metricsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }
    
    /**
     * Creates the evolution tab with Pokemon growth history.
     */
    private Tab createEvolutionTab() {
        Tab tab = new Tab("Evolution");
        
        evolutionContainer = new VBox(UITheme.LARGE_SPACING);
        evolutionContainer.setPadding(new Insets(UITheme.MEDIUM_PADDING));
        evolutionContainer.setAlignment(Pos.TOP_CENTER);
        
        ScrollPane scrollPane = new ScrollPane(evolutionContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        tab.setContent(scrollPane);
        return tab;
    }
    
    /**
     * Updates all statistics displays with new commit data.
     * 
     * @param commits List of commits to analyze
     * @param species Current Pokemon species
     * @param stage Current evolution stage
     * @param currentStreak Current commit streak
     */
    public void updateStatistics(List<Commit> commits, PokemonSpecies species, EvolutionStage stage, int currentStreak) {
        updateCharts(commits);
        updateMetrics(commits, currentStreak);
        updateEvolutionHistory(species, stage);
    }
    
    /**
     * Updates the commit frequency charts.
     * 
     * @param commits List of commits to analyze
     */
    private void updateCharts(List<Commit> commits) {
        // If no commits, create sample data for demonstration
        if (commits == null || commits.isEmpty()) {
            commits = createSampleCommits();
        }
        
        // Update daily chart
        List<DailyStats> dailyStats = statisticsService.calculateDailyStats(commits, 30);
        XYChart.Series<String, Number> dailySeries = new XYChart.Series<>();
        dailySeries.setName("Daily Commits");
        
        for (DailyStats stats : dailyStats) {
            String dateStr = stats.getDate().format(DATE_FORMATTER);
            dailySeries.getData().add(new XYChart.Data<>(dateStr, stats.getCommitCount()));
        }
        
        dailyCommitChart.getData().clear();
        dailyCommitChart.getData().add(dailySeries);
        
        // Update weekly chart
        List<WeeklyStats> weeklyStats = statisticsService.calculateWeeklyStats(commits, 12);
        XYChart.Series<String, Number> weeklySeries = new XYChart.Series<>();
        weeklySeries.setName("Weekly Commits");
        
        for (WeeklyStats stats : weeklyStats) {
            String weekStr = stats.getWeekStart().format(WEEK_FORMATTER);
            weeklySeries.getData().add(new XYChart.Data<>(weekStr, stats.getCommitCount()));
        }
        
        weeklyCommitChart.getData().clear();
        weeklyCommitChart.getData().add(weeklySeries);
        
        // Update XP chart
        XYChart.Series<String, Number> xpSeries = new XYChart.Series<>();
        xpSeries.setName("Daily XP");
        
        for (DailyStats stats : dailyStats) {
            String dateStr = stats.getDate().format(DATE_FORMATTER);
            xpSeries.getData().add(new XYChart.Data<>(dateStr, stats.getXpGained()));
        }
        
        xpProgressChart.getData().clear();
        xpProgressChart.getData().add(xpSeries);
    }
    
    /**
     * Creates sample commits for demonstration when no real commits exist.
     * 
     * @return List of sample commits
     */
    private List<Commit> createSampleCommits() {
        List<Commit> sampleCommits = new ArrayList<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // Create sample commits over the last 14 days
        for (int i = 0; i < 14; i++) {
            java.time.LocalDateTime commitTime = now.minusDays(i);
            
            // Create 1-4 commits per day with varying patterns
            int commitsPerDay = 1 + (i % 4);
            for (int j = 0; j < commitsPerDay; j++) {
                Commit commit = new Commit();
                commit.setMessage("Sample commit " + i + "-" + j + ": " + getSampleCommitMessage(i, j));
                commit.setAuthor("Developer " + (j % 3 == 0 ? "Alice" : j % 3 == 1 ? "Bob" : "Charlie"));
                commit.setRepositoryName("sample-project-" + (i % 3 == 0 ? "frontend" : i % 3 == 1 ? "backend" : "mobile"));
                commit.setTimestamp(commitTime.minusHours(j * 2));
                sampleCommits.add(commit);
            }
        }
        
        return sampleCommits;
    }
    
    /**
     * Gets a sample commit message for demonstration.
     * 
     * @param day Day index
     * @param commitIndex Commit index for the day
     * @return Sample commit message
     */
    private String getSampleCommitMessage(int day, int commitIndex) {
        String[] messages = {
            "Add new feature", "Fix bug in authentication", "Update documentation",
            "Refactor code structure", "Improve performance", "Add unit tests",
            "Update dependencies", "Fix styling issues", "Add error handling",
            "Optimize database queries", "Add logging", "Update README"
        };
        return messages[(day * 3 + commitIndex) % messages.length];
    }
    
    /**
     * Updates the productivity metrics display.
     * 
     * @param commits List of commits to analyze
     * @param currentStreak Current commit streak
     */
    private void updateMetrics(List<Commit> commits, int currentStreak) {
        metricsContainer.getChildren().clear();
        
        ProductivityMetrics metrics = statisticsService.calculateProductivityMetrics(commits, currentStreak);
        
        // Title
        Label titleLabel = new Label("Productivity Metrics");
        titleLabel.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.BOLD, UITheme.HEADER_FONT_SIZE));
        titleLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
        
        // Create metrics grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(UITheme.LARGE_SPACING);
        metricsGrid.setVgap(UITheme.MEDIUM_SPACING);
        metricsGrid.setAlignment(Pos.CENTER);
        
        // Add metrics
        addMetricRow(metricsGrid, 0, "Total Commits", String.valueOf(metrics.getTotalCommits()));
        addMetricRow(metricsGrid, 1, "Total XP", String.valueOf(metrics.getTotalXp()));
        addMetricRow(metricsGrid, 2, "Total Repositories", String.valueOf(metrics.getTotalRepositories()));
        addMetricRow(metricsGrid, 3, "Active Days", String.valueOf(metrics.getActiveDays()));
        addMetricRow(metricsGrid, 4, "Current Streak", metrics.getCurrentStreak() + " days");
        addMetricRow(metricsGrid, 5, "Longest Streak", metrics.getLongestStreak() + " days");
        addMetricRow(metricsGrid, 6, "Avg Commits/Day", String.format("%.1f", metrics.getAverageCommitsPerDay()));
        addMetricRow(metricsGrid, 7, "Avg XP/Day", String.format("%.1f", metrics.getAverageXpPerDay()));
        
        if (metrics.getFirstCommitDate() != null) {
            addMetricRow(metricsGrid, 8, "First Commit", metrics.getFirstCommitDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        }
        if (metrics.getLastCommitDate() != null) {
            addMetricRow(metricsGrid, 9, "Last Commit", metrics.getLastCommitDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        }
        
        metricsContainer.getChildren().addAll(titleLabel, metricsGrid);
    }
    
    /**
     * Adds a metric row to the grid.
     * 
     * @param grid The grid pane
     * @param row Row index
     * @param label Metric label
     * @param value Metric value
     */
    private void addMetricRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label + ":");
        labelNode.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.MEDIUM, UITheme.NORMAL_FONT_SIZE));
        labelNode.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
        
        Label valueNode = new Label(value);
        valueNode.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.BOLD, UITheme.NORMAL_FONT_SIZE));
        valueNode.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
        
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }
    
    /**
     * Updates the evolution history display.
     * 
     * @param species Current Pokemon species
     * @param stage Current evolution stage
     */
    private void updateEvolutionHistory(PokemonSpecies species, EvolutionStage stage) {
        evolutionContainer.getChildren().clear();
        
        // Title
        Label titleLabel = new Label("Evolution History");
        titleLabel.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.BOLD, UITheme.HEADER_FONT_SIZE));
        titleLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
        
        // Get evolution history
        List<EvolutionEntry> history = statisticsService.getEvolutionHistory(species, stage);
        
        if (history.isEmpty()) {
            Label emptyLabel = new Label("No evolutions yet - keep committing to grow your Pokemon!");
            emptyLabel.setFont(Font.font(UITheme.PRIMARY_FONT, UITheme.NORMAL_FONT_SIZE));
            emptyLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
            evolutionContainer.getChildren().addAll(titleLabel, emptyLabel);
            return;
        }
        
        VBox evolutionList = new VBox(UITheme.MEDIUM_SPACING);
        
        for (EvolutionEntry entry : history) {
            HBox evolutionEntry = createEvolutionEntry(entry);
            evolutionList.getChildren().add(evolutionEntry);
        }
        
        evolutionContainer.getChildren().addAll(titleLabel, evolutionList);
    }
    
    /**
     * Creates a visual entry for an evolution event.
     * 
     * @param entry The evolution entry
     * @return HBox containing the evolution entry
     */
    private HBox createEvolutionEntry(EvolutionEntry entry) {
        HBox entryBox = new HBox(UITheme.LARGE_SPACING);
        entryBox.setAlignment(Pos.CENTER_LEFT);
        entryBox.setPadding(new Insets(UITheme.MEDIUM_PADDING));
        entryBox.setStyle(UITheme.getCommitEntryStyle());
        
        // Evolution info
        VBox infoBox = new VBox(UITheme.SMALL_SPACING);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        // Evolution description
        String fromStage = formatStageName(entry.getFromStage());
        String toStage = formatStageName(entry.getToStage());
        String species = formatSpeciesName(entry.getSpecies());
        
        Label evolutionLabel = new Label(species + " evolved from " + fromStage + " to " + toStage);
        evolutionLabel.setFont(Font.font(UITheme.PRIMARY_FONT, FontWeight.MEDIUM, UITheme.NORMAL_FONT_SIZE));
        evolutionLabel.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
        
        // Evolution details
        String timeStr = entry.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        Label detailsLabel = new Label(timeStr + " • " + entry.getXpAtEvolution() + " XP • " + entry.getStreakAtEvolution() + " day streak");
        detailsLabel.setFont(Font.font(UITheme.PRIMARY_FONT, UITheme.SMALL_FONT_SIZE));
        detailsLabel.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
        
        infoBox.getChildren().addAll(evolutionLabel, detailsLabel);
        
        // Evolution icon/indicator
        Label iconLabel = new Label("🎉");
        iconLabel.setFont(Font.font(UITheme.HEADER_FONT_SIZE));
        
        entryBox.getChildren().addAll(iconLabel, infoBox);
        return entryBox;
    }
    
    /**
     * Formats a Pokemon species name for display.
     * 
     * @param species The species to format
     * @return Formatted species name
     */
    private String formatSpeciesName(PokemonSpecies species) {
        if (species == null) return "Unknown";
        String name = species.name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
    
    /**
     * Formats an evolution stage name for display.
     * 
     * @param stage The stage to format
     * @return Formatted stage name
     */
    private String formatStageName(EvolutionStage stage) {
        if (stage == null) return "Unknown";
        switch (stage) {
            case EGG: return "Egg";
            case BASIC: return "Basic";
            case STAGE_1: return "Stage 1";
            case STAGE_2: return "Stage 2";
            default: return "Unknown";
        }
    }
    
    /**
     * Refreshes the UI theme for all components when Windows theme changes.
     */
    public void refreshTheme() {
        try {
            // Update main container style
            this.setStyle(UITheme.getTransparentStyle());
            
            // Update tab pane styling
            if (tabPane != null) {
                tabPane.setStyle(UITheme.getTransparentStyle());
            }
            
            // Update chart colors
            updateChartTheme();
            
            // Update metrics container
            if (metricsContainer != null) {
                updateContainerTheme(metricsContainer);
            }
            
            // Update evolution container
            if (evolutionContainer != null) {
                updateContainerTheme(evolutionContainer);
            }
            
            System.out.println("🎨 StatisticsTab theme refreshed");
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to refresh StatisticsTab theme: " + e.getMessage());
        }
    }
    
    /**
     * Updates chart theme colors.
     */
    private void updateChartTheme() {
        // Charts automatically adapt to system theme in most cases
        // Additional customization can be added here if needed
    }
    
    /**
     * Updates container theme colors.
     * 
     * @param container The container to update
     */
    private void updateContainerTheme(VBox container) {
        for (var node : container.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                // Update based on font weight to determine color
                if (label.getFont().getName().contains("Bold")) {
                    label.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
                } else {
                    label.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
                }
            } else if (node instanceof GridPane) {
                updateGridTheme((GridPane) node);
            } else if (node instanceof VBox) {
                updateContainerTheme((VBox) node);
            } else if (node instanceof HBox) {
                updateHBoxTheme((HBox) node);
            }
        }
    }
    
    /**
     * Updates grid theme colors.
     * 
     * @param grid The grid to update
     */
    private void updateGridTheme(GridPane grid) {
        for (var node : grid.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                if (label.getFont().getName().contains("Bold")) {
                    label.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
                } else {
                    label.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
                }
            }
        }
    }
    
    /**
     * Updates HBox theme colors.
     * 
     * @param hbox The HBox to update
     */
    private void updateHBoxTheme(HBox hbox) {
        hbox.setStyle(UITheme.getCommitEntryStyle());
        for (var node : hbox.getChildren()) {
            if (node instanceof VBox) {
                updateContainerTheme((VBox) node);
            } else if (node instanceof Label) {
                Label label = (Label) node;
                if (label.getFont().getName().contains("Bold")) {
                    label.setTextFill(Color.web(UITheme.PRIMARY_TEXT_COLOR));
                } else {
                    label.setTextFill(Color.web(UITheme.SECONDARY_TEXT_COLOR));
                }
            }
        }
    }
}