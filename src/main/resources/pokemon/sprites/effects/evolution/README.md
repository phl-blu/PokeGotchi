# Evolution Effect Sprites

Generic evolution animation frames used for ALL Pokemon evolutions.

## Frame Sequence (Classic Pokemon Style)

| Frame | Duration | Pokemon Layer | Effect Layer | Description |
|-------|----------|---------------|--------------|-------------|
| **1** | 300ms | Old form visible | Small sparkles | Build-up begins |
| **2** | 300ms | Old form visible | Glow intensifying | Energy gathering |
| **3** | 200ms | White silhouette | Bright glow | Flash begins, form obscured |
| **4** | 150ms | Hidden (pure white) | Peak brightness | Transformation moment |
| **5** | 200ms | White silhouette | Glow fading | New form emerging |
| **6** | 400ms | New form visible | Final sparkles | Evolution complete |

**Total Duration:** ~1550ms

## File Format
- **frame1.png** - Small sparkles around Pokemon (overlay)
- **frame2.png** - More sparkles, glow intensifying (overlay)
- **frame3.png** - Bright glow effect (Pokemon becomes silhouette)
- **frame4.png** - Peak white flash (Pokemon hidden completely)
- **frame5.png** - Glow fading (new form as silhouette)
- **frame6.png** - Final sparkles settling (overlay)

## Specifications
- Size: 64x64 pixels
- Format: PNG with transparency
- Background: Transparent (effects overlay on Pokemon)

## Layering Guide (Important!)

To avoid the "hologram morphing" effect:

1. **Frames 1-2:** Pokemon sprite visible at 100% opacity, effect frames overlay on top
2. **Frame 3:** Apply white color tint to Pokemon sprite (~80% white overlay)
3. **Frame 4:** Hide Pokemon sprite completely, show only white flash effect
4. **Frame 5:** Show NEW Pokemon sprite with white tint (~80% white overlay)
5. **Frame 6:** New Pokemon at 100% opacity, sparkle effects overlay

The key is the **hard cut** at frame 4 - never morph between sprites!

## Optional: Dramatic Pulse Effect

For extra drama (like classic games), loop frames 2→3→2→3 before the final flash:
```
1 → 2 → 3 → 2 → 3 → 4 → 5 → 6
```
This creates the "pulsing glow" effect seen in Gen 3-5 games.

## Notes
- These are GENERIC effects used for ALL Pokemon evolutions
- The Pokemon sprite swap happens during frame 4 (peak flash)
- Effect frames should be semi-transparent to overlay properly
- The animation code handles sprite visibility/tinting automatically
