# PokeGotchi

A Pokémon-themed desktop widget that tracks your Git commits and raises a virtual Pokémon companion whose growth reflects your coding activity. The more you commit, the more your Pokémon thrives — neglect your repos and watch it sulk.

The UI takes visual inspiration from the official Pokémon Pokédex design.

---

## Features

- **9 Starter Lines** — Choose from starters spanning Generations 1–9 (Charmander, Cyndaquil, Mudkip, Piplup, Snivy, Froakie, Rowlet, Grookey, Fuecoco)
- **Egg Hatching** — Your Pokémon starts as an egg and hatches once you begin committing
- **Evolution System** — Pokémon evolve through Egg → Basic → Stage 1 → Stage 2 based on XP earned from commits
- **Automatic Repository Discovery** — Scans your system for all local Git repositories
- **5-Minute Polling** — Continuously monitors for new commits in the background
- **Compact Widget Mode** — 80×80px transparent desktop overlay
- **Expanded History View** — 320×450px detailed commit history panel

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Run

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Launch the app
mvn javafx:run

# Package
mvn clean package
```

---

## Tech Stack

| Dependency | Version | Purpose |
|---|---|---|
| JavaFX | 21.0.1 | Desktop UI framework |
| JGit | 6.8.0 | Git repository operations |
| JUnit 5 | — | Unit testing |
| jqwik | 1.8.2 | Property-based testing |
| TestFX | — | JavaFX UI testing |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/tamagotchi/committracker/
│   │   ├── config/          # App configuration
│   │   ├── domain/          # Core models (Commit, Repository, etc.)
│   │   ├── pokemon/         # Pokémon logic, XP, evolution
│   │   ├── git/             # Repository scanning & commit polling
│   │   ├── ui/              # Widget window & UI components
│   │   └── util/            # File & animation utilities
│   └── resources/
│       ├── config/          # Default settings
│       ├── pokemon/sprites/ # Sprite assets
│       └── ui/styles/       # CSS theming
└── test/                    # Unit & property-based tests
```

---

## Credits & Assets

### Pokémon Sprites

All Pokémon sprites (excluding eggs) are sourced from the **Project Pokémon Sprite Index** and community contributors. Egg sprites were **original artwork drawn by me**.

| Starter Line | Source |
|---|---|
| Charmander line | [Generation 1 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-1-pok%C3%A9mon-r90/) |
| Cyndaquil line | [Generation 2 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-2-pok%C3%A9mon-r91/) |
| Mudkip line | [Generation 3 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-3-pok%C3%A9mon-r92/) |
| Piplup line | [Generation 4 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-4-pok%C3%A9mon-r93/) |
| Snivy line | [Generation 5 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-5-pok%C3%A9mon-r94/) |
| Froakie line | [Generation 6 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-6-pok%C3%A9mon-r95/) |
| Rowlet line | [Generation 7 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-7-pok%C3%A9mon-r96/) |
| Grookey line | [Generation 8 – Project Pokémon](https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-8-pok%C3%A9mon-r123/) |
| Fuecoco line | [Custom 3D Animated Renders by DJTHED](https://www.smogon.com/forums/threads/custom-3d-animated-renders.3526922/page-18) (post #436) |
| Egg sprites | Original artwork by **phl-blu** |

> Pokémon is a trademark of Nintendo / Game Freak / Creatures Inc. This project is fan-made and non-commercial.

---

## Architecture

This project follows clean architecture principles — domain logic, Pokémon mechanics, Git operations, and UI are fully decoupled.
