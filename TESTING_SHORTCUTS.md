# Testing Shortcuts for Development

⚠️ **WARNING**: This file and all testing functionality MUST be removed before production release!

## Keyboard Shortcuts

When the application is running, you can use these keyboard shortcuts for testing:

### Evolution Testing
- **E** - Force evolution to the next stage (Egg → Basic → Stage1 → Stage2)
- **R** - Reset Pokemon back to egg stage (de-evolution)

### Animation Testing
- **C** - Simulate a single commit (7-12 XP) to trigger egg shake/Pokemon animation

### State Testing  
- **H** - Change Pokemon state to HAPPY (temporary animation)
- **S** - Change Pokemon state to SAD
- **T** - Change Pokemon state to THRIVING (temporary animation)

### Information
- **I** - Show current Pokemon info in console (species, stage, state, evolution status)

## Usage Instructions

1. Start the application: `mvn javafx:run`
2. Wait for the Pokemon to appear (will auto-evolve if you have commit history)
3. Click on the widget window to focus it
4. Press any of the shortcut keys listed above
5. Check the console output for testing feedback

## Testing Scenarios

### Test Egg Animation (Key Behavior)
1. Press **R** to reset Pokemon to egg stage
2. Press **C** to simulate a single commit (7-12 XP randomly)
3. **IMPORTANT**: Watch the egg shake/animate once then return to static
4. Press **C** multiple times to simulate multiple commits
5. **Each individual commit should make the egg shake** - this is the core behavior
6. The egg stage (visual appearance) may advance occasionally for variety
7. The egg should animate on EVERY commit, not just when changing stages

### Test Evolution Cycle
1. Press **R** to reset to egg
2. Press **E** to evolve to basic
3. Press **E** again to evolve to stage 1
4. Press **E** again to evolve to stage 2
5. Press **R** to reset back to egg

### Test State Changes
1. Press **H** to see happy animation
2. Press **S** to see sad state
3. Press **T** to see thriving animation
4. Press **I** to check current state in console

## Console Output

All testing actions produce console output like:
```
🧪 TESTING: 'R' pressed - Resetting Pokemon to egg stage
🧪 TESTING: Forcing de-evolution from BASIC back to EGG stage
🥚 TESTING: Pokemon reset to egg stage for testing

🧪 TESTING: 'C' pressed - Simulating single commit
🥚 TESTING: Commit gives +9 XP (total: 34 XP)
🥚 TESTING: Egg should shake/animate for this single commit
```

## Production Cleanup

Before releasing the app, remove:
- All keyboard shortcut handlers in `WidgetWindow.java`
- All `*ForTesting()` methods in Pokemon classes
- All testing utility classes
- All "🧪 TESTING:" console messages
- This entire file

See TODO.md for complete removal checklist.