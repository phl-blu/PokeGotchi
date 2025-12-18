# Testing Shortcuts for Development

⚠️ **WARNING**: This file and all testing functionality MUST be removed before production release!

## Keyboard Shortcuts

When the application is running, you can use these keyboard shortcuts for testing:

### Evolution Testing
- **E** - Force evolution to the next stage (Egg → Basic → Stage1 → Stage2)
- **R** - Reset Pokemon back to egg stage (de-evolution)

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

### Test Egg Animation
1. Press **R** to reset Pokemon to egg stage
2. Make a commit to your repository
3. Watch the egg animate once then return to static

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
```

## Production Cleanup

Before releasing the app, remove:
- All keyboard shortcut handlers in `WidgetWindow.java`
- All `*ForTesting()` methods in Pokemon classes
- All testing utility classes
- All "🧪 TESTING:" console messages
- This entire file

See TODO.md for complete removal checklist.