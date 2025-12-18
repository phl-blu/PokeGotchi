# Pokemon Sprite Folder Structure

This document outlines the complete folder structure for all Pokemon sprites in the Tamagotchi Commit Tracker.

## Egg Sprites (Universal for all Pokemon)

```
eggs/
├── stage1/     # Day 1 - Basic egg (fresh)
│   ├── frame1.png  (static display)
│   ├── frame2.png  (animation frame 2)
│   ├── frame3.png  (animation frame 3)
│   └── frame4.png  (animation frame 4)
├── stage2/     # Day 2 - Barely cracked
│   ├── frame1.png  (static display)
│   ├── frame2.png  (animation frame 2)
│   ├── frame3.png  (animation frame 3)
│   └── frame4.png  (animation frame 4)
├── stage3/     # Day 3 - More cracked
│   ├── frame1.png  (static display)
│   ├── frame2.png  (animation frame 2)
│   ├── frame3.png  (animation frame 3)
│   └── frame4.png  (animation frame 4)
└── stage4/     # Day 4 - Very cracked (ready to hatch)
    ├── frame1.png  (static display)
    ├── frame2.png  (animation frame 2)
    ├── frame3.png  (animation frame 3)
    └── frame4.png  (animation frame 4)
```

## Pokemon Sprites (9 Evolution Lines)

### 1. Charmander Line (Kanto Fire)
```
charmander/
├── basic/      # Charmander
│   ├── content/
│   │   ├── frame1.png
│   │   └── frame2.png
│   ├── happy/
│   │   ├── frame1.png
│   │   └── frame2.png
│   ├── sad/
│   │   ├── frame1.png
│   │   └── frame2.png
│   ├── thriving/
│   │   ├── frame1.png
│   │   └── frame2.png
│   ├── concerned/
│   │   ├── frame1.png
│   │   └── frame2.png
│   └── neglected/
│       ├── frame1.png
│       └── frame2.png
├── stage_1/    # Charmeleon
│   └── [same states as basic]
└── stage_2/    # Charizard
    └── [same states as basic]
```

### 2. Cyndaquil Line (Johto Fire)
```
cyndaquil/
├── basic/      # Cyndaquil
├── stage_1/    # Quilava
└── stage_2/    # Typhlosion
```

### 3. Mudkip Line (Hoenn Water)
```
mudkip/
├── basic/      # Mudkip
├── stage_1/    # Marshtomp
└── stage_2/    # Swampert
```

### 4. Piplup Line (Sinnoh Water)
```
piplup/
├── basic/      # Piplup
├── stage_1/    # Prinplup
└── stage_2/    # Empoleon
```

### 5. Snivy Line (Unova Grass)
```
snivy/
├── basic/      # Snivy
├── stage_1/    # Servine
└── stage_2/    # Serperior
```

### 6. Froakie Line (Kalos Water)
```
froakie/
├── basic/      # Froakie
├── stage_1/    # Frogadier
└── stage_2/    # Greninja
```

### 7. Rowlet Line (Alola Grass)
```
rowlet/
├── basic/      # Rowlet
├── stage_1/    # Dartrix
└── stage_2/    # Decidueye
```

### 8. Grookey Line (Galar Grass)
```
grookey/
├── basic/      # Grookey
├── stage_1/    # Thwackey
└── stage_2/    # Rillaboom
```

### 9. Fuecoco Line (Paldea Fire)
```
fuecoco/
├── basic/      # Fuecoco
├── stage_1/    # Crocalor
└── stage_2/    # Skeledirge
```

## Animation States

Each Pokemon evolution stage has 6 animation states:

1. **content** - Default neutral state (static display)
2. **happy** - Positive state when commits detected (animated)
3. **sad** - Negative state when no commits for extended periods
4. **thriving** - Very positive state with high commit activity
5. **concerned** - Slightly worried state with declining activity
6. **neglected** - Very negative state with long periods of inactivity

## Animation Behavior

### Eggs (11 FPS):
- **Static**: Shows frame1.png of appropriate stage based on streak days
- **Animated**: Cycles through all 4 frames when commit detected
- **Frame Duration**: ~91ms per frame (11 FPS)

### Pokemon (2 FPS):
- **Static**: Shows frame1.png when no recent commits
- **Animated**: Cycles through 2 frames when commit detected
- **Frame Duration**: 500ms per frame (2 FPS)

## Image Specifications

- **Size**: 64x64 pixels
- **Format**: PNG with transparency
- **Style**: Consistent Pokemon aesthetic
- **Naming**: frame1.png, frame2.png, etc.

## Total File Count

- **Eggs**: 4 stages × 4 frames = 16 files
- **Pokemon**: 9 lines × 3 stages × 6 states × 2 frames = 324 files
- **Total**: 340 sprite files

## Usage Notes

1. Replace placeholder files with actual PNG sprites
2. Maintain consistent 64x64 pixel dimensions
3. Use transparency for backgrounds
4. Ensure frame1.png works well as static display
5. Create smooth animation transitions between frames