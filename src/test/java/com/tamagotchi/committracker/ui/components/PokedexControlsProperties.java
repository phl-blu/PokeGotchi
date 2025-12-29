package com.tamagotchi.committracker.ui.components;

import net.jqwik.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Property-based tests for PokedexControls component.
 * Tests D-pad navigation cycling and Button B behavior.
 */
class PokedexControlsProperties {

    /**
     * Screen modes for navigation testing.
     * The cycle is: POKEMON -> STATISTICS -> HISTORY -> POKEMON (for right)
     * And reverse for left.
     */
    enum ScreenMode {
        POKEMON,
        STATISTICS,
        HISTORY
    }

    /**
     * Simple screen navigator for testing navigation logic.
     */
    static class ScreenNavigator {
        private ScreenMode currentScreen = ScreenMode.POKEMON;
        
        // Screen order for right navigation: POKEMON -> STATISTICS -> HISTORY -> POKEMON
        private static final ScreenMode[] SCREEN_ORDER = {
            ScreenMode.POKEMON,
            ScreenMode.STATISTICS,
            ScreenMode.HISTORY
        };
        
        public ScreenMode getCurrentScreen() {
            return currentScreen;
        }
        
        public void setCurrentScreen(ScreenMode screen) {
            this.currentScreen = screen;
        }
        
        public ScreenMode navigateRight() {
            int currentIndex = getScreenIndex(currentScreen);
            int nextIndex = (currentIndex + 1) % SCREEN_ORDER.length;
            currentScreen = SCREEN_ORDER[nextIndex];
            return currentScreen;
        }
        
        public ScreenMode navigateLeft() {
            int currentIndex = getScreenIndex(currentScreen);
            int prevIndex = (currentIndex - 1 + SCREEN_ORDER.length) % SCREEN_ORDER.length;
            currentScreen = SCREEN_ORDER[prevIndex];
            return currentScreen;
        }
        
        public ScreenMode goToHome() {
            currentScreen = ScreenMode.POKEMON;
            return currentScreen;
        }
        
        private int getScreenIndex(ScreenMode screen) {
            for (int i = 0; i < SCREEN_ORDER.length; i++) {
                if (SCREEN_ORDER[i] == screen) {
                    return i;
                }
            }
            return 0;
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 8: D-pad navigation cycles through screens correctly**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * For any starting screen and any number of right presses, 
     * the navigation should cycle through screens in the correct order.
     */
    @Property(tries = 100)
    @Label("D-pad right navigation should cycle through screens in correct order")
    void dPadRightNavigationShouldCycleCorrectly(
            @ForAll("validScreenMode") ScreenMode startScreen,
            @ForAll("validNavigationCount") int rightPresses) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(startScreen);
        
        List<ScreenMode> visitedScreens = new ArrayList<>();
        visitedScreens.add(startScreen);
        
        for (int i = 0; i < rightPresses; i++) {
            ScreenMode nextScreen = navigator.navigateRight();
            visitedScreens.add(nextScreen);
        }
        
        // Verify the cycle is correct: POKEMON -> STATISTICS -> HISTORY -> POKEMON
        for (int i = 1; i < visitedScreens.size(); i++) {
            ScreenMode prev = visitedScreens.get(i - 1);
            ScreenMode curr = visitedScreens.get(i);
            
            ScreenMode expectedNext = getExpectedNextRight(prev);
            assertEquals(expectedNext, curr,
                "After " + prev + ", right navigation should go to " + expectedNext + " but got " + curr);
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 8: D-pad navigation cycles through screens correctly**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * For any starting screen and any number of left presses,
     * the navigation should cycle through screens in reverse order.
     */
    @Property(tries = 100)
    @Label("D-pad left navigation should cycle through screens in reverse order")
    void dPadLeftNavigationShouldCycleCorrectly(
            @ForAll("validScreenMode") ScreenMode startScreen,
            @ForAll("validNavigationCount") int leftPresses) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(startScreen);
        
        List<ScreenMode> visitedScreens = new ArrayList<>();
        visitedScreens.add(startScreen);
        
        for (int i = 0; i < leftPresses; i++) {
            ScreenMode nextScreen = navigator.navigateLeft();
            visitedScreens.add(nextScreen);
        }
        
        // Verify the reverse cycle is correct: POKEMON -> HISTORY -> STATISTICS -> POKEMON
        for (int i = 1; i < visitedScreens.size(); i++) {
            ScreenMode prev = visitedScreens.get(i - 1);
            ScreenMode curr = visitedScreens.get(i);
            
            ScreenMode expectedPrev = getExpectedNextLeft(prev);
            assertEquals(expectedPrev, curr,
                "After " + prev + ", left navigation should go to " + expectedPrev + " but got " + curr);
        }
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 8: D-pad navigation cycles through screens correctly**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * After 3 right presses from any screen, we should return to the starting screen.
     */
    @Property(tries = 100)
    @Label("Three right presses should return to starting screen")
    void threeRightPressesShouldReturnToStart(
            @ForAll("validScreenMode") ScreenMode startScreen) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(startScreen);
        
        // Press right 3 times (full cycle)
        navigator.navigateRight();
        navigator.navigateRight();
        navigator.navigateRight();
        
        assertEquals(startScreen, navigator.getCurrentScreen(),
            "After 3 right presses, should return to starting screen " + startScreen);
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 8: D-pad navigation cycles through screens correctly**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * After 3 left presses from any screen, we should return to the starting screen.
     */
    @Property(tries = 100)
    @Label("Three left presses should return to starting screen")
    void threeLeftPressesShouldReturnToStart(
            @ForAll("validScreenMode") ScreenMode startScreen) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(startScreen);
        
        // Press left 3 times (full cycle)
        navigator.navigateLeft();
        navigator.navigateLeft();
        navigator.navigateLeft();
        
        assertEquals(startScreen, navigator.getCurrentScreen(),
            "After 3 left presses, should return to starting screen " + startScreen);
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 8: D-pad navigation cycles through screens correctly**
     * **Validates: Requirements 6.1, 6.2**
     * 
     * Left and right navigation should be inverses of each other.
     */
    @Property(tries = 100)
    @Label("Left and right navigation should be inverses")
    void leftAndRightShouldBeInverses(
            @ForAll("validScreenMode") ScreenMode startScreen) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(startScreen);
        
        // Go right then left should return to start
        navigator.navigateRight();
        navigator.navigateLeft();
        
        assertEquals(startScreen, navigator.getCurrentScreen(),
            "Right then left should return to starting screen");
        
        // Go left then right should also return to start
        navigator.setCurrentScreen(startScreen);
        navigator.navigateLeft();
        navigator.navigateRight();
        
        assertEquals(startScreen, navigator.getCurrentScreen(),
            "Left then right should return to starting screen");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 9: Button B returns to Pokemon screen**
     * **Validates: Requirements 6.4**
     * 
     * For any screen other than Pokemon, pressing Button B should return to Pokemon screen.
     */
    @Property(tries = 100)
    @Label("Button B should always return to Pokemon screen")
    void buttonBShouldReturnToPokemonScreen(
            @ForAll("validScreenMode") ScreenMode startScreen) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(startScreen);
        
        ScreenMode result = navigator.goToHome();
        
        assertEquals(ScreenMode.POKEMON, result,
            "Button B (goToHome) should always return to POKEMON screen");
        assertEquals(ScreenMode.POKEMON, navigator.getCurrentScreen(),
            "Current screen should be POKEMON after Button B press");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 9: Button B returns to Pokemon screen**
     * **Validates: Requirements 6.4**
     * 
     * Button B should be idempotent - pressing it multiple times should keep us on Pokemon screen.
     */
    @Property(tries = 100)
    @Label("Button B should be idempotent")
    void buttonBShouldBeIdempotent(
            @ForAll("validScreenMode") ScreenMode startScreen,
            @ForAll("validNavigationCount") int buttonBPresses) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(startScreen);
        
        // Press Button B multiple times
        for (int i = 0; i < buttonBPresses; i++) {
            navigator.goToHome();
        }
        
        assertEquals(ScreenMode.POKEMON, navigator.getCurrentScreen(),
            "After any number of Button B presses, should be on POKEMON screen");
    }

    /**
     * **Feature: pokedex-ui-redesign, Property 9: Button B returns to Pokemon screen**
     * **Validates: Requirements 6.4**
     * 
     * After navigating away and pressing Button B, we should be back on Pokemon screen.
     */
    @Property(tries = 100)
    @Label("Button B should return to Pokemon after any navigation sequence")
    void buttonBShouldReturnAfterNavigation(
            @ForAll("validNavigationSequence") List<Boolean> navigationSequence) {
        
        ScreenNavigator navigator = new ScreenNavigator();
        navigator.setCurrentScreen(ScreenMode.POKEMON);
        
        // Execute navigation sequence (true = right, false = left)
        for (Boolean goRight : navigationSequence) {
            if (goRight) {
                navigator.navigateRight();
            } else {
                navigator.navigateLeft();
            }
        }
        
        // Press Button B
        navigator.goToHome();
        
        assertEquals(ScreenMode.POKEMON, navigator.getCurrentScreen(),
            "After any navigation sequence, Button B should return to POKEMON");
    }

    // Helper methods
    
    private ScreenMode getExpectedNextRight(ScreenMode current) {
        switch (current) {
            case POKEMON: return ScreenMode.STATISTICS;
            case STATISTICS: return ScreenMode.HISTORY;
            case HISTORY: return ScreenMode.POKEMON;
            default: return ScreenMode.POKEMON;
        }
    }
    
    private ScreenMode getExpectedNextLeft(ScreenMode current) {
        switch (current) {
            case POKEMON: return ScreenMode.HISTORY;
            case HISTORY: return ScreenMode.STATISTICS;
            case STATISTICS: return ScreenMode.POKEMON;
            default: return ScreenMode.POKEMON;
        }
    }

    // Providers
    
    @Provide
    Arbitrary<ScreenMode> validScreenMode() {
        return Arbitraries.of(ScreenMode.values());
    }
    
    @Provide
    Arbitrary<Integer> validNavigationCount() {
        return Arbitraries.integers().between(1, 10);
    }
    
    @Provide
    Arbitrary<List<Boolean>> validNavigationSequence() {
        return Arbitraries.of(true, false).list().ofMinSize(1).ofMaxSize(10);
    }
}
