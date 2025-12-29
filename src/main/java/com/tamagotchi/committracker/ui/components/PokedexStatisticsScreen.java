package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.tamagotchi.committracker.domain.Commit;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

import java.util.List;

/**
 * Wrapper component for the existing StatisticsTab with Pokedex styling.
 * Displays statistics and productivity metrics in a retro Pokedex-themed container.
 * 
 * Requirements: 6.7
 */
public class PokedexStatisticsScreen extends VBox {
    
    private StatisticsTab statisticsTab;
    private Label titleLabel;
    
    /**
     * Creates a new PokedexStatisticsScreen with Pokedex styling.
     */
    public PokedexStatisticsScreen() {
        initializeComponent();
    }
    
    /**
     * Initializes the JavaFX components with Pokedex styling.
     */
    private void initializeComponent() {
        this.setSpacing(8);
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.TOP_CENTER);
        this.setStyle(getContainerStyle());
        
        // Title label with pixel font
        titleLabel = new Label("STATISTICS");
        titleLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(12),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        titleLabel.setTextFill(Color.web(PokedexTheme.TEXT_WHITE));
        
        // Create and integrate the existing StatisticsTab
        statisticsTab = new StatisticsTab();
        applyPokedexStyling(statisticsTab);
        
        // Wrap in ScrollPane for scrolling
        ScrollPane scrollPane = new ScrollPane(statisticsTab);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        this.getChildren().addAll(titleLabel, scrollPane);
    }
    
    /**
     * Gets the CSS style for the container.
     * 
     * @return CSS style string
     */
    private String getContainerStyle() {
        return String.format(
            "-fx-background-color: %s; -fx-background-radius: 8;",
            PokedexTheme.SCREEN_BG
        );
    }
    
    /**
     * Applies Pokedex styling to the StatisticsTab component.
     * 
     * @param tab The StatisticsTab to style
     */
    private void applyPokedexStyling(StatisticsTab tab) {
        // Apply transparent background to let container style show through
        tab.setStyle(PokedexTheme.combineStyles(
            "-fx-background-color: transparent;",
            PokedexTheme.getPixelFontStyle(PokedexTheme.PIXEL_FONT_SIZE)
        ));
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
        if (statisticsTab != null) {
            statisticsTab.updateStatistics(commits, species, stage, currentStreak);
        }
    }
    
    /**
     * Gets the underlying StatisticsTab component.
     * 
     * @return The StatisticsTab instance
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
                PokedexTheme.getPixelFontStyle(12),
                PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
            ));
        }
        if (statisticsTab != null) {
            statisticsTab.refreshTheme();
        }
    }
}
