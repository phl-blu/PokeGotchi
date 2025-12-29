package com.tamagotchi.committracker.ui.components;

/**
 * Enum representing the different screen modes in the Pokedex UI.
 * Used for navigation between screens via D-pad and button controls.
 * 
 * Screen cycle order (D-pad right): POKEMON → STATISTICS → HISTORY → POKEMON
 * Screen cycle order (D-pad left): POKEMON → HISTORY → STATISTICS → POKEMON
 * Button B always returns to POKEMON from any other screen.
 * 
 * Requirements: 6.1, 6.2, 6.4, 6.5
 */
public enum PokedexScreenMode {
    /**
     * Pokemon selection grid screen (first-time only).
     * Shows available Pokemon for the user to choose from.
     */
    SELECTION("SELECT"),
    
    /**
     * Main Pokemon display screen.
     * Shows the selected Pokemon with stats and name.
     */
    POKEMON("POKEMON"),
    
    /**
     * Commit history screen.
     * Shows the commit log from the existing HistoryTab component.
     */
    HISTORY("HISTORY"),
    
    /**
     * Statistics screen.
     * Shows detailed statistics from the existing StatisticsTab component.
     */
    STATISTICS("STATS");
    
    private final String displayName;
    
    PokedexScreenMode(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the display name for the screen indicator.
     * 
     * @return Short display name for the screen
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the next screen in the right navigation cycle.
     * Cycle: POKEMON → STATISTICS → HISTORY → POKEMON
     * SELECTION is not part of the navigation cycle.
     * 
     * @return The next screen mode when navigating right
     */
    public PokedexScreenMode navigateRight() {
        switch (this) {
            case POKEMON:
                return STATISTICS;
            case STATISTICS:
                return HISTORY;
            case HISTORY:
                return POKEMON;
            case SELECTION:
                return SELECTION; // Selection screen doesn't navigate
            default:
                return POKEMON;
        }
    }
    
    /**
     * Gets the previous screen in the left navigation cycle.
     * Cycle: POKEMON → HISTORY → STATISTICS → POKEMON
     * SELECTION is not part of the navigation cycle.
     * 
     * @return The previous screen mode when navigating left
     */
    public PokedexScreenMode navigateLeft() {
        switch (this) {
            case POKEMON:
                return HISTORY;
            case HISTORY:
                return STATISTICS;
            case STATISTICS:
                return POKEMON;
            case SELECTION:
                return SELECTION; // Selection screen doesn't navigate
            default:
                return POKEMON;
        }
    }
    
    /**
     * Checks if this screen mode is part of the navigation cycle.
     * SELECTION is not navigable via D-pad.
     * 
     * @return true if this screen can be navigated to/from via D-pad
     */
    public boolean isNavigable() {
        return this != SELECTION;
    }
}
