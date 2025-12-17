# TODO - Tamagotchi Commit Tracker

## 🚨 IMPORTANT: Remove Testing Code Before Production

### Evolution Testing Code to Remove:
1. **PokemonDisplayComponent.java**:
   - Remove `forceEvolutionForTesting()` method
   - This method bypasses normal evolution requirements for testing

2. **WidgetWindow.java**:
   - Remove keyboard shortcuts for testing (E, H, S, I keys)
   - Remove testing-related console output messages

3. **Restore Normal Evolution Requirements**:
   - Currently using proper requirements: 4/11/22 day streaks + XP thresholds
   - Testing shortcuts allow bypassing these requirements
   - Make sure evolution only works with real commit streaks

### Current Evolution Requirements (CORRECT):
- **EGG → BASIC**: 4+ day streak + 200 XP
- **BASIC → STAGE_1**: 11+ day streak + 800 XP  
- **STAGE_1 → STAGE_2**: 22+ day streak + 2000 XP

### Frame Animation Status:
✅ **Individual Pokemon Animation Speeds**:
- AnimationUtils loads frames 1-8 for each Pokemon state (flexible)
- Each Pokemon species has its own unique animation speed for personality
- Fallback system works correctly for missing frames
- Animation system handles different frame counts per Pokemon automatically

### Pokemon Animation Speeds:
**All Pokemon Lines (Consistent):**
- **All 9 Pokemon lines**: 2.0 FPS (500ms) - Consistent idle animation speed
- **Frame Count**: Exactly 2 frames per Pokemon state (frame1.png, frame2.png)
- **Animation Style**: Smooth, consistent idle animations across all species

### Updated Pokemon List:
1. **Charmander** → Charmeleon → Charizard (Kanto Fire)
2. **Cyndaquil** → Quilava → Typhlosion (Johto Fire)
3. **Mudkip** → Marshtomp → Swampert (Hoenn Water)
4. **Piplup** → Prinplup → Empoleon (Sinnoh Water)
5. **Snivy** → Servine → Serperior (Unova Grass)
6. **Froakie** → Frogadier → Greninja (Kalos Water)
7. **Rowlet** → Dartrix → Decidueye (Alola Grass)
8. **Grookey** → Thwackey → Rillaboom (Galar Grass)
9. **Fuecoco** → Crocalor → Skeledirge (Paldea Fire)

### Testing Notes:
- Testing shortcuts work correctly for immediate evolution testing
- Real evolution system is intact and will work with actual commit streaks
- Remove testing code when ready for production use

---
**Created**: December 17, 2025  
**Status**: Testing phase - remove testing code before final release