# Implementation Plan

## Overview
This implementation plan converts the Pokemon Commit Tracker design into a series of incremental development tasks for a JavaFX MVP. Each task builds on previous work, ending with a fully functional desktop widget that tracks Git commits and displays an evolving Pokemon companion.

## Tasks

- [x] 1. Set up project structure and dependencies





  - Create Maven/Gradle project with JavaFX dependencies
  - Add JGit library for Git repository operations
  - Set up jqwik for property-based testing and JUnit 5 for unit testing
  - Configure JavaFX module system and application structure
  - _Requirements: All requirements depend on proper project setup_

- [x] 2. Implement core data models and XP system





  - Create Commit, Repository, and CommitHistory data classes
  - Implement PokemonSpecies and EvolutionStage enums with all 27 Pokemon
  - Build XPSystem class with evolution thresholds (0, 200, 800, 2000 XP)
  - Create PokemonState enum and state calculation logic
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ]* 2.1 Write property test for XP calculation consistency
  - **Property 3: Pokemon State and Evolution Responsiveness**
  - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**

- [x] 3. Build Git repository discovery and monitoring system

  - Implement Repository Scanner to find all Git repositories on system
  - Create Commit Service with JGit integration for reading commit history
  - Add 5-minute polling mechanism with background threading
  - Handle Git authentication using existing system credentials
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3.1 Write property test for repository discovery

  - **Property 1: Repository Discovery and Monitoring**
  - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**

- [ ]* 3.2 Write property test for polling consistency
  - **Property 2: Polling Consistency**
  - **Validates: Requirements 3.1, 3.3, 3.5**

- [x] 4. Create basic JavaFX widget window with transparency





  - Set up transparent JavaFX Stage with 80x80px compact mode
  - Implement window dragging functionality with mouse event handlers
  - Add position persistence (save/restore widget location)
  - Configure always-on-top behavior and taskbar integration
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 1.6, 6.1, 6.2, 6.4, 6.5_

- [ ]* 4.1 Write property test for UI state management
  - **Property 5: UI State Management**
  - **Validates: Requirements 1.2, 1.4**

- [x] 5. Implement Pokemon sprite animation system





  - Create Pokemon Display Component with frame-based animation
  - Load and cycle through 3-4 PNG/JPEG frames for each Pokemon state
  - Implement smooth transitions between animation states (Idle, Happy, Sad, etc.)
  - Add evolution animation sequence for stage transitions
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 7.6_

- [x] 6. Fix egg animation system and implement proper commit-triggered behavior





  - Update AnimationUtils.getEggStageFromStreak() to use XP days instead of streak days
  - Modify egg loading to show only frame1.png when idle (static display)
  - Implement commit-triggered animation: 1 commit = 1 cycle through frames 1-4 at 11 FPS
  - Update egg stage progression logic: 1 day XP = stage1, 2 days XP = stage2, 3 days XP = stage3, 4 days XP = stage4
  - Add triggerCommitAnimation() method that plays animation once then returns to static
  - Remove continuous looping for eggs when no commits detected
  - _Requirements: 8.1, 8.5, 8.6_

- [x] 6.1 Update XP-to-stage calculation logic


  - Modify getEggStageFromStreak() to getEggStageFromXPDays()
  - Calculate XP days from total accumulated XP
  - Map XP days to egg stages (1 day = stage1, 2 days = stage2, etc.)
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 6.2 Implement single-cycle egg animation


  - Create playOnceAnimation() method for eggs
  - Animation plays frames 1-4 once at 11 FPS when commit detected
  - Return to static frame1.png after animation completes
  - _Requirements: 8.5, 8.6_

- [x] 7. Build Pokemon state management and evolution logic





  - Connect commit activity to Pokemon emotional states
  - Implement evolution criteria checking (4-day, 11-day, 22-day streaks + XP)
  - Create Pokemon State Manager to calculate states from commit patterns
  - Add evolution triggers and animation sequences
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 9.2, 9.3, 9.4, 9.5_

- [x] 8. Create Pokemon selection system with 9 starters





  - Implement initial Pokemon selection UI (9 starter options)
  - Create Pokemon-specific egg display system for initial state
  - Add Pokemon species switching functionality
  - Store selected Pokemon preference persistently
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 9. Checkpoint - Ensure all core functionality works






  - Ensure all tests pass, ask the user if questions arise.
  - Verify egg animation triggers correctly on commits
  - Test egg stage progression based on XP accumulation

- [x] 10. Build expanded mode with commit history display





  - Create expandable UI (320x450px) triggered by widget click
  - Implement History Tab with scrollable commit list
  - Display commit information (message, timestamp, repository, author, XP gained)
  - Add Pokemon status display (level, XP progress, streak counter)
  - _Requirements: 1.3, 5.1, 5.2, 5.3, 5.4_

- [x] 10.1 Write property test for commit display completeness



  - Property 4: Commit Display Completeness**
  - Validates: Requirements 5.1, 5.2, 5.3**

- [ ] 11. Add statistics and productivity metrics
  - Implement commit frequency graphs and productivity charts
  - Create evolution log showing Pokemon growth history
  - Add daily/weekly commit statistics display
  - Include XP breakdown and evolution progress tracking
  - _Requirements: 5.5_

<!-- - [ ] 12. Create remaining Pokemon egg sprite folders
  - Create egg folders for remaining 7 Pokemon lines (mudkip, piplup, snivy, froakie, rowlet, grookey, fuecoco)
  - Each Pokemon needs egg/stage{1-4} folders with 4 frames each
  - Document sprite requirements for each Pokemon's unique egg design
  - _Requirements: Individual Pokemon egg sprites_ -->

- [ ] 13. Implement error handling and resilience
  - Add graceful handling of Git authentication failures
  - Implement retry mechanisms for network connectivity issues
  - Create fallback animations for missing sprite files
  - Add error logging and user-friendly error messages
  - _Requirements: 2.5, 8.3_

- [ ]* 11.1 Write property test for error resilience

  - **Property 6: Error Resilience**
  - **Validates: Requirements 2.5, 7.3**

- [x] 12. Enhance Windows system integration









  - Implement Windows theme adaptation for UI styling
  - Add proper z-order and focus behavior for window management
  - Integrate with Windows credential storage for Git authentication
  - Ensure proper taskbar minimization behavior
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ]* 12.1 Write property test for system integration
  - **Property 7: System Integration**
  - **Validates: Requirements 6.2, 6.3, 6.4, 6.5**

- [ ]* 12.2 Write property test for authentication security
  - **Property 8: Authentication Security**
  - **Validates: Requirements 8.1, 8.2, 8.4, 8.5**

- [ ] 13. Polish and optimization
  - Optimize memory usage for large commit histories
  - Implement lazy loading for Pokemon sprite frames
  - Add configuration options for scan intervals and repository limits
  - Create smooth UI transitions and improved visual feedback
  - _Requirements: Performance and usability improvements_

- [ ] 14. Final checkpoint - Complete MVP testing
  - Ensure all tests pass, ask the user if questions arise.
  - Verify all Pokemon evolution mechanics work correctly
  - Test with multiple Git repositories and authentication methods
  - Validate UI responsiveness and Windows integration