package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.tamagotchi.committracker.pokemon.EvolutionStage;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

/**
 * Stats corner component for the Pokedex UI.
 * Displays Pokemon stats (XP, streak, evolution stage) in the top-left corner
 * of the main display screen.
 * 
 * IMPORTANT: This component does NOT display the Pokemon name.
 * The Pokemon name is displayed separately in the PokedexNameLabel component.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
public class PokedexStatsCorner extends VBox {
    
    // Styling constants
    private static final int LABEL_SPACING = 2;
    private static final int FONT_SIZE = 9;
    
    /** Number of labels in this component (XP, streak, stage - NO name) */
    public static final int LABEL_COUNT = 3;
    
    // Labels for stats display
    private Label xpLabel;
    private Label streakLabel;
    private Label stageLabel;
    
    // Current text values (for testing without JavaFX)
    private String xpText = "XP: 0/100";
    private String streakText = "Streak: 0 days";
    private String stageText = "Stage: EGG";
    
    /**
     * Creates a new PokedexStatsCorner component.
     * Initializes with default placeholder values.
     */
    public PokedexStatsCorner() {
        initializeComponent();
    }
    
    /**
     * Initializes the stats corner layout and styling.
     */
    private void initializeComponent() {
        // Apply container styling
        this.setStyle(PokedexTheme.getStatsCornerStyle());
        this.setSpacing(LABEL_SPACING);
        this.setPadding(new Insets(PokedexTheme.STATS_PADDING));
        
        // Create XP label
        xpLabel = createStatsLabel(xpText);
        
        // Create streak label
        streakLabel = createStatsLabel(streakText);
        
        // Create stage label
        stageLabel = createStatsLabel(stageText);
        
        // Add labels to container (NO Pokemon name!)
        this.getChildren().addAll(xpLabel, streakLabel, stageLabel);
    }
    
    /**
     * Creates a styled label for stats display.
     * 
     * @param text The initial text for the label
     * @return A styled Label instance
     */
    private Label createStatsLabel(String text) {
        Label label = new Label(text);
        label.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(FONT_SIZE),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        return label;
    }
    
    /**
     * Updates the XP display with current and threshold values.
     * 
     * Requirements: 4.2
     * 
     * @param currentXP The current XP amount
     * @param nextThreshold The XP needed for next evolution
     */
    public void updateXP(int currentXP, int nextThreshold) {
        xpText = String.format("XP: %d/%d", currentXP, nextThreshold);
        if (xpLabel != null) {
            xpLabel.setText(xpText);
        }
    }
    
    /**
     * Updates the streak display with the current streak count.
     * 
     * Requirements: 4.3
     * 
     * @param days The number of consecutive days with commits
     */
    public void updateStreak(int days) {
        String dayText = days == 1 ? "day" : "days";
        streakText = String.format("Streak: %d %s", days, dayText);
        if (streakLabel != null) {
            streakLabel.setText(streakText);
        }
    }
    
    /**
     * Updates the evolution stage display.
     * 
     * Requirements: 4.4
     * 
     * @param stage The current evolution stage
     */
    public void updateStage(EvolutionStage stage) {
        String stageName = formatStageName(stage);
        stageText = String.format("Stage: %s", stageName);
        if (stageLabel != null) {
            stageLabel.setText(stageText);
        }
    }
    
    /**
     * Formats the evolution stage name for display.
     * This method is package-private for testing.
     * 
     * @param stage The evolution stage to format
     * @return A human-readable stage name
     */
    static String formatStageName(EvolutionStage stage) {
        if (stage == null) {
            return "UNKNOWN";
        }
        switch (stage) {
            case EGG:
                return "EGG";
            case BASIC:
                return "BASIC";
            case STAGE_1:
                return "STAGE 1";
            case STAGE_2:
                return "STAGE 2";
            default:
                return stage.name();
        }
    }
    
    /**
     * Formats the XP text for display.
     * This static method can be used for testing without JavaFX.
     * 
     * @param currentXP The current XP amount
     * @param nextThreshold The XP needed for next evolution
     * @return The formatted XP text
     */
    public static String formatXPText(int currentXP, int nextThreshold) {
        return String.format("XP: %d/%d", currentXP, nextThreshold);
    }
    
    /**
     * Formats the streak text for display.
     * This static method can be used for testing without JavaFX.
     * 
     * @param days The number of consecutive days with commits
     * @return The formatted streak text
     */
    public static String formatStreakText(int days) {
        String dayText = days == 1 ? "day" : "days";
        return String.format("Streak: %d %s", days, dayText);
    }
    
    /**
     * Formats the stage text for display.
     * This static method can be used for testing without JavaFX.
     * 
     * @param stage The current evolution stage
     * @return The formatted stage text
     */
    public static String formatStageText(EvolutionStage stage) {
        return String.format("Stage: %s", formatStageName(stage));
    }
    
    /**
     * Gets all displayed text content for testing purposes.
     * This is useful for verifying that the Pokemon name is NOT included.
     * This static method can be used for testing without JavaFX.
     * 
     * Requirements: 4.5
     * 
     * @param currentXP The current XP amount
     * @param nextThreshold The XP needed for next evolution
     * @param streak The number of consecutive days with commits
     * @param stage The current evolution stage
     * @return A string containing all displayed text
     */
    public static String getAllDisplayedTextStatic(int currentXP, int nextThreshold, int streak, EvolutionStage stage) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatXPText(currentXP, nextThreshold)).append(" ");
        sb.append(formatStreakText(streak)).append(" ");
        sb.append(formatStageText(stage));
        return sb.toString();
    }
    
    /**
     * Gets the XP label for testing purposes.
     * 
     * @return The XP Label
     */
    public Label getXpLabel() {
        return xpLabel;
    }
    
    /**
     * Gets the streak label for testing purposes.
     * 
     * @return The streak Label
     */
    public Label getStreakLabel() {
        return streakLabel;
    }
    
    /**
     * Gets the stage label for testing purposes.
     * 
     * @return The stage Label
     */
    public Label getStageLabel() {
        return stageLabel;
    }
    
    /**
     * Gets all displayed text content for testing purposes.
     * This is useful for verifying that the Pokemon name is NOT included.
     * 
     * Requirements: 4.5
     * 
     * @return A string containing all displayed text
     */
    public String getAllDisplayedText() {
        return xpText + " " + streakText + " " + stageText;
    }
    
    /**
     * Gets the number of labels in this component.
     * Should always be exactly 3 (XP, streak, stage - NO name).
     * 
     * @return The count of labels
     */
    public int getLabelCount() {
        return LABEL_COUNT; // XP, streak, stage - explicitly NOT including name
    }
}
