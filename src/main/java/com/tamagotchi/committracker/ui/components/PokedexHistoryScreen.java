package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import com.tamagotchi.committracker.domain.CommitHistory;
import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

/**
 * Wrapper component for the existing HistoryTab with Pokedex styling.
 * Displays commit history in a retro Pokedex-themed container.
 * 
 * Requirements: 6.6
 */
public class PokedexHistoryScreen extends VBox {
    
    private HistoryTab historyTab;
    private Label titleLabel;
    
    /**
     * Creates a new PokedexHistoryScreen with Pokedex styling.
     */
    public PokedexHistoryScreen() {
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
        titleLabel = new Label("COMMIT HISTORY");
        titleLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(12),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        titleLabel.setTextFill(Color.web(PokedexTheme.TEXT_WHITE));
        
        // Create and integrate the existing HistoryTab
        historyTab = new HistoryTab();
        applyPokedexStyling(historyTab);
        
        // Wrap in ScrollPane for scrolling
        ScrollPane scrollPane = new ScrollPane(historyTab);
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
     * Applies Pokedex styling to the HistoryTab component.
     * 
     * @param tab The HistoryTab to style
     */
    private void applyPokedexStyling(HistoryTab tab) {
        // Apply transparent background to let container style show through
        tab.setStyle("-fx-background-color: transparent;");
        
        // Apply pixel font styling to all labels in the tab
        tab.setStyle(PokedexTheme.combineStyles(
            "-fx-background-color: transparent;",
            PokedexTheme.getPixelFontStyle(PokedexTheme.PIXEL_FONT_SIZE)
        ));
    }
    
    /**
     * Updates the commit history display.
     * 
     * @param history The commit history to display
     */
    public void updateCommitHistory(CommitHistory history) {
        if (historyTab != null) {
            historyTab.updateCommitHistory(history);
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
        if (historyTab != null) {
            historyTab.updatePokemonStatus(species, stage, xp, streak);
        }
    }
    
    /**
     * Gets the underlying HistoryTab component.
     * 
     * @return The HistoryTab instance
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
                PokedexTheme.getPixelFontStyle(12),
                PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
            ));
        }
        if (historyTab != null) {
            historyTab.refreshTheme();
        }
    }
}
