# CORRECT Pokemon Sprite Structure

Each Pokemon line has its own unique egg sprites and animations.

## Structure for Each Pokemon Line:

### Example: Charmander Line
```
charmander/
├── egg/           # Charmander-specific egg sprites
│   ├── stage1/    # Day 1 - Charmander egg basic
│   │   ├── frame1.png
│   │   ├── frame2.png
│   │   ├── frame3.png
│   │   └── frame4.png
│   ├── stage2/    # Day 2 - Charmander egg barely cracked
│   │   ├── frame1.png
│   │   ├── frame2.png
│   │   ├── frame3.png
│   │   └── frame4.png
│   ├── stage3/    # Day 3 - Charmander egg more cracked
│   │   ├── frame1.png
│   │   ├── frame2.png
│   │   ├── frame3.png
│   │   └── frame4.png
│   └── stage4/    # Day 4 - Charmander egg very cracked
│       ├── frame1.png
│       ├── frame2.png
│       ├── frame3.png
│       └── frame4.png
├── basic/         # Charmander (hatched)
│   ├── content/
│   │   ├── frame1.png
│   │   └── frame2.png
│   └── [other states...]
├── stage_1/       # Charmeleon
└── stage_2/       # Charizard
```

## All 9 Pokemon Lines Need This Structure:

1. **charmander/** - Fire egg (orange/red theme)
2. **cyndaquil/** - Fire egg (blue/yellow theme)  
3. **mudkip/** - Water egg (blue theme)
4. **piplup/** - Water egg (blue/white theme)
5. **snivy/** - Grass egg (green theme)
6. **froakie/** - Water egg (blue/white theme)
7. **rowlet/** - Grass/Flying egg (brown/green theme)
8. **grookey/** - Grass egg (green/brown theme)
9. **fuecoco/** - Fire egg (red/orange theme)

Each Pokemon's egg should have unique colors/patterns that hint at what will hatch!