# Tamagotchi Commit Tracker

A Pokemon-themed desktop widget that tracks Git commits across all user repositories and displays a virtual pet whose state reflects coding activity.

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   ├── module-info.java                    # Java module configuration
│   │   └── com/tamagotchi/committracker/
│   │       ├── TamagotchiCommitTrackerApp.java # Main JavaFX application
│   │       ├── config/                         # Configuration management
│   │       │   └── AppConfig.java
│   │       ├── domain/                         # Core domain models
│   │       │   ├── Commit.java
│   │       │   ├── Repository.java
│   │       │   ├── CommitHistory.java
│   │       │   └── AuthenticationType.java
│   │       ├── pokemon/                        # Pokemon-specific logic
│   │       │   ├── PokemonState.java
│   │       │   ├── PokemonSpecies.java
│   │       │   ├── EvolutionStage.java
│   │       │   ├── XPSystem.java
│   │       │   └── PokemonStateManager.java
│   │       ├── git/                            # Git operations
│   │       │   ├── CommitService.java
│   │       │   └── RepositoryScanner.java
│   │       ├── ui/                             # User interface
│   │       │   ├── widget/
│   │       │   │   └── WidgetWindow.java
│   │       │   └── components/
│   │       │       ├── PokemonDisplayComponent.java
│   │       │       └── HistoryTab.java
│   │       └── util/                           # Utility classes
│   │           ├── FileUtils.java
│   │           └── AnimationUtils.java
│   └── resources/
│       ├── config/
│       │   └── default.properties             # Default configuration
│       ├── pokemon/
│       │   ├── sprites/                       # Pokemon sprite images
│       │   └── species/
│       │       └── kanto.properties           # Pokemon species data
│       └── ui/
│           └── styles/
│               └── widget.css                 # UI styling
└── test/
    └── java/
        └── com/tamagotchi/committracker/
            ├── TamagotchiCommitTrackerAppTest.java
            ├── config/
            │   └── AppConfigTest.java
            ├── domain/
            │   └── CommitTest.java
            └── pokemon/
                └── EvolutionStageProperties.java
```

## Architecture

The project follows a clean, modular architecture with clear separation of concerns:

### Package Organization

- **`config/`** - Configuration management and application settings
- **`domain/`** - Core business domain models (Commit, Repository, etc.)
- **`pokemon/`** - Pokemon-specific logic, evolution, and XP systems
- **`git/`** - Git repository operations and commit monitoring
- **`ui/`** - User interface components organized by function
  - `widget/` - Main widget window and container components
  - `components/` - Reusable UI components
- **`util/`** - Utility classes for common operations

### Design Principles

- **Domain-Driven Design**: Core business logic separated from infrastructure
- **Single Responsibility**: Each package has a clear, focused purpose
- **Dependency Inversion**: High-level modules don't depend on low-level details
- **Testability**: Clean separation enables comprehensive testing

## Dependencies

- **JavaFX 21.0.1**: UI framework for desktop widget
- **JGit 6.8.0**: Git repository operations
- **JUnit 5**: Unit testing framework
- **jqwik 1.8.2**: Property-based testing framework
- **TestFX**: JavaFX testing support

## Build & Run

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Commands

```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Run the application
mvn javafx:run

# Package the application
mvn clean package
```

## Features (Planned)

- **9 Starter Pokemon**: Choose from Kanto, Johto, and Hoenn starters
- **Evolution System**: Pokemon evolve based on commit streaks and XP
- **Automatic Repository Discovery**: Finds all Git repos on your system
- **5-Minute Polling**: Continuously monitors for new commits
- **Compact Widget Mode**: 80x80px transparent desktop widget
- **Expanded History View**: 320x450px detailed commit history
- **Windows Integration**: Native taskbar integration and theming

## Development Status

This project is currently in the setup phase. The basic project structure and dependencies have been configured. Implementation will proceed according to the task list in `.kiro/specs/tamagotchi-commit-tracker/tasks.md`.

## Testing

The project uses a dual testing approach:
- **Unit Tests**: Verify specific functionality and edge cases
- **Property-Based Tests**: Verify universal properties across all inputs

Run tests with: `mvn test`