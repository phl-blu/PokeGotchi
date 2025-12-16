# Tamagotchi Commit Tracker - Architecture Overview

## Clean Architecture Implementation

This project follows clean architecture principles with clear separation of concerns and modular design.

## Package Structure

### Core Domain (`domain/`)
Contains the essential business entities and value objects:
- `Commit` - Represents a Git commit with metadata
- `Repository` - Represents a monitored Git repository
- `CommitHistory` - Aggregates commit data for analysis
- `AuthenticationType` - Enum for Git authentication methods

### Pokemon Logic (`pokemon/`)
Encapsulates all Pokemon-related business logic:
- `PokemonState` - Emotional states based on commit activity
- `PokemonSpecies` - 27 available Pokemon (9 starter lines)
- `EvolutionStage` - Evolution progression (Egg → Basic → Stage 1 → Stage 2)
- `XPSystem` - Experience point calculation and thresholds
- `PokemonStateManager` - Determines state and evolution logic

### Git Operations (`git/`)
Handles all Git repository interactions:
- `RepositoryScanner` - Discovers and monitors repositories
- `CommitService` - Orchestrates commit polling and processing

### User Interface (`ui/`)
Organized by component type:
- `widget/WidgetWindow` - Main application window container
- `components/PokemonDisplayComponent` - Pokemon animation display
- `components/HistoryTab` - Commit history visualization

### Configuration (`config/`)
Centralized configuration management:
- `AppConfig` - Application settings and constants

### Utilities (`util/`)
Common utility functions:
- `FileUtils` - File and path operations
- `AnimationUtils` - Animation and sprite management

## Design Benefits

### 1. **Separation of Concerns**
Each package has a single, well-defined responsibility:
- Domain logic is isolated from infrastructure
- Pokemon mechanics are separate from Git operations
- UI components are decoupled from business logic

### 2. **Testability**
Clean boundaries enable comprehensive testing:
- Domain models can be tested in isolation
- Pokemon logic can be verified with property-based tests
- Git operations can be mocked for UI testing

### 3. **Maintainability**
Modular structure makes the codebase easier to:
- Navigate and understand
- Modify without affecting other components
- Extend with new features

### 4. **Dependency Management**
Clear dependency flow:
- UI depends on services
- Services depend on domain models
- No circular dependencies

## Resource Organization

### Configuration (`resources/config/`)
- `default.properties` - Default application settings
- Environment-specific overrides

### Pokemon Data (`resources/pokemon/`)
- `sprites/` - Animation frames (PNG/JPEG)
- `species/` - Pokemon metadata and evolution chains

### UI Assets (`resources/ui/`)
- `styles/` - CSS styling for JavaFX components

## Testing Strategy

### Package-Aligned Tests
Test structure mirrors source structure:
- `domain/` tests verify business logic
- `pokemon/` tests use property-based testing for evolution rules
- `config/` tests validate configuration loading

### Test Types
- **Unit Tests**: Specific functionality verification
- **Property Tests**: Universal rules across all inputs
- **Integration Tests**: Component interaction validation

## Future Extensibility

This architecture supports easy extension:
- **New Pokemon**: Add to species configuration
- **Additional Git Providers**: Extend git package
- **UI Themes**: Add to ui/styles
- **New Metrics**: Extend domain models

The clean separation ensures changes in one area don't cascade through the entire system.