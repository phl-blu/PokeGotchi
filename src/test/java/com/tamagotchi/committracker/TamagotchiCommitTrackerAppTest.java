package com.tamagotchi.committracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test to verify the application setup and dependencies.
 */
class TamagotchiCommitTrackerAppTest {

    @Test
    void testApplicationClassExists() {
        // Verify the main application class can be instantiated
        assertDoesNotThrow(() -> {
            TamagotchiCommitTrackerApp app = new TamagotchiCommitTrackerApp();
            assertNotNull(app);
        });
    }
}