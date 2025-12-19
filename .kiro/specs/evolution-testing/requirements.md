# Evolution Testing Requirements

## Problem Statement
The current Pokemon evolution system requires real commit streaks (4/11/22 days) which makes testing difficult during development. Developers need a way to test evolution mechanics without waiting for real commit streaks.

## User Stories

### As a Developer
- **I want** to test Pokemon evolution without waiting for real commit streaks
- **So that** I can verify the evolution animation and sprite loading works correctly
- **Acceptance Criteria:**
  - Evolution can be triggered manually during development
  - Testing mode doesn't affect production evolution requirements
  - Clear separation between testing and production code

### As a User
- **I want** Pokemon to evolve based on my real coding commitment
- **So that** the evolution feels earned and meaningful
- **Acceptance Criteria:**
  - EGG → BASIC requires 4+ day streak OR 50 XP (whichever comes first)
  - BASIC → STAGE_1 requires 11+ day streak (XP not required)
  - STAGE_1 → STAGE_2 requires 22+ day streak (XP not required)
  - No shortcuts available in production

## Current Implementation Status

### ✅ Completed
- Evolution requirements properly implemented in `PokemonStateManager.java`
- Testing method `forceEvolutionForTesting()` available in `PokemonDisplayComponent.java`
- Animation system supports variable frame counts (1-8 frames)
- Individual Pokemon animation speeds implemented

### 🔄 In Progress
- Frame animations currently use 2 frames, system ready for 4+ frames
- Testing code marked for removal before production (see TODO.md)

### ❌ Needs Implementation
- Production build configuration to remove testing code
- Clear documentation of testing vs production modes

## Technical Requirements

### Evolution System
- Must use real Git commit data for streak calculation
- XP system integrated with commit frequency and quality
- Evolution stages: EGG → BASIC → STAGE_1 → STAGE_2

### Animation System
- Support 1-8 frames per Pokemon state
- Individual animation speeds per Pokemon species
- Smooth transitions between states and evolutions

### Testing Framework
- Manual evolution triggers for development
- Keyboard shortcuts for testing (currently E, H, S, I keys)
- Console logging for debugging animation and evolution

## Definition of Done
- [ ] Evolution works with real commit streaks in production
- [ ] Testing shortcuts available during development
- [ ] Clear build process to remove testing code
- [ ] All 9 Pokemon lines properly configured
- [ ] Animation system handles variable frame counts
- [ ] Documentation updated for testing procedures