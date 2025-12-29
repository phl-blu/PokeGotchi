package com.tamagotchi.committracker.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import com.tamagotchi.committracker.pokemon.PokemonSelectionData;
import com.tamagotchi.committracker.pokemon.PokemonSpecies;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;
import com.tamagotchi.committracker.util.AnimationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pokedex-styled Pokemon selection screen with dynamic grid layout.
 * Displays available Pokemon in a grid with circular frames and dark backgrounds.
 * Implements hover highlighting and click selection with callback notification.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4
 */
public class PokedexSelectionScreen extends VBox {
    
    private GridPane pokemonGrid;
    private Consumer<PokemonSpecies> onPokemonSelected;
    private PokemonSpecies hoveredPokemon;
    private PokemonSpecies selectedPokemon;
    
    // Map to track cells for styling updates
    private Map<PokemonSpecies, StackPane> cellMap;
    
    // Grid configuration from PokedexTheme
    private static final int CELL_SIZE = PokedexTheme.CELL_SIZE;
    private static final int CELL_SPACING = PokedexTheme.CELL_SPACING;
    private static final int SPRITE_SIZE = 40; // Bigger sprites for better visibility
    
    /**
     * Creates a new Pokedex selection screen.
     * 
     * @param onSelected Callback when a Pokemon is selected
     */
    public PokedexSelectionScreen(Consumer<PokemonSpecies> onSelected) {
        this.onPokemonSelected = onSelected;
        this.cellMap = new HashMap<>();
        this.hoveredPokemon = null;
        this.selectedPokemon = null;
        
        initializeLayout();
        createPokemonGrid();
    }
    
    /**
     * Initializes the layout properties for the selection screen.
     */
    private void initializeLayout() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10));
        setSpacing(10);
    }
    
    /**
     * Creates the Pokemon selection grid with dynamic sizing.
     * Grid dimensions are calculated based on the number of available Pokemon.
     */
    private void createPokemonGrid() {
        pokemonGrid = new GridPane();
        pokemonGrid.setAlignment(Pos.CENTER);
        pokemonGrid.setHgap(CELL_SPACING);
        pokemonGrid.setVgap(CELL_SPACING);
        
        PokemonSpecies[] starters = PokemonSelectionData.getStarterOptions();
        int pokemonCount = starters.length;
        
        // Calculate grid dimensions (aim for roughly square grid)
        int columns = calculateGridColumns(pokemonCount);
        int rows = (int) Math.ceil((double) pokemonCount / columns);
        
        // Populate grid with Pokemon cells
        for (int i = 0; i < starters.length; i++) {
            int row = i / columns;
            int col = i % columns;
            
            StackPane cell = createPokemonCell(starters[i]);
            pokemonGrid.add(cell, col, row);
        }
        
        getChildren().add(pokemonGrid);
    }
    
    /**
     * Calculates the optimal number of columns for the grid.
     * Aims for a roughly square layout.
     * 
     * @param count Number of Pokemon to display
     * @return Number of columns for the grid
     */
    private int calculateGridColumns(int count) {
        // For 9 Pokemon, use 3x3 grid
        // For other counts, calculate square root and round up
        return (int) Math.ceil(Math.sqrt(count));
    }
    
    /**
     * Creates a single Pokemon cell with circular frame and dark background.
     * 
     * @param species The Pokemon species for this cell
     * @return StackPane containing the styled cell
     */
    private StackPane createPokemonCell(PokemonSpecies species) {
        StackPane cell = new StackPane();
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(CELL_SIZE, CELL_SIZE);
        
        // Apply initial cell styling (dark background, circular)
        cell.setStyle(PokedexTheme.getCellStyle(false, false));
        
        // Create circular clip for the cell
        Circle clip = new Circle(CELL_SIZE / 2.0);
        clip.setCenterX(CELL_SIZE / 2.0);
        clip.setCenterY(CELL_SIZE / 2.0);
        cell.setClip(clip);
        
        // Load and display egg sprite
        ImageView spriteView = new ImageView();
        spriteView.setFitWidth(SPRITE_SIZE);
        spriteView.setFitHeight(SPRITE_SIZE);
        spriteView.setPreserveRatio(true);
        
        Image eggSprite = AnimationUtils.loadEggPreviewSprite(species);
        if (eggSprite != null) {
            spriteView.setImage(eggSprite);
        }
        
        cell.getChildren().add(spriteView);
        
        // Store cell reference for later styling updates
        cellMap.put(species, cell);
        
        // Add mouse event handlers for hover and click
        setupCellEventHandlers(cell, species);
        
        return cell;
    }
    
    /**
     * Sets up mouse event handlers for a cell.
     * Handles hover highlighting and click selection.
     * 
     * @param cell The cell StackPane
     * @param species The Pokemon species for this cell
     */
    private void setupCellEventHandlers(StackPane cell, PokemonSpecies species) {
        // Mouse enter - apply hover highlight
        cell.setOnMouseEntered(e -> {
            hoveredPokemon = species;
            updateCellStyle(species, true, species.equals(selectedPokemon));
        });
        
        // Mouse exit - remove hover highlight (unless selected)
        cell.setOnMouseExited(e -> {
            if (hoveredPokemon == species) {
                hoveredPokemon = null;
            }
            updateCellStyle(species, false, species.equals(selectedPokemon));
        });
        
        // Mouse click - select Pokemon and notify callback
        cell.setOnMouseClicked(e -> {
            handlePokemonSelection(species);
        });
        
        // Set cursor to hand to indicate clickable
        cell.setStyle(cell.getStyle() + " -fx-cursor: hand;");
    }
    
    /**
     * Updates the style of a cell based on hover and selection state.
     * 
     * @param species The Pokemon species
     * @param highlighted Whether the cell is hovered
     * @param selected Whether the cell is selected
     */
    private void updateCellStyle(PokemonSpecies species, boolean highlighted, boolean selected) {
        StackPane cell = cellMap.get(species);
        if (cell != null) {
            cell.setStyle(PokedexTheme.getCellStyle(highlighted, selected) + " -fx-cursor: hand;");
        }
    }
    
    /**
     * Handles Pokemon selection when a cell is clicked.
     * Updates selection state and notifies the callback.
     * 
     * @param species The selected Pokemon species
     */
    private void handlePokemonSelection(PokemonSpecies species) {
        // Clear previous selection styling
        if (selectedPokemon != null && !selectedPokemon.equals(species)) {
            updateCellStyle(selectedPokemon, false, false);
        }
        
        // Update selection
        selectedPokemon = species;
        updateCellStyle(species, true, true);
        
        System.out.println("🎮 Selected: " + PokemonSelectionData.getDisplayName(species));
        
        // Notify callback
        if (onPokemonSelected != null) {
            onPokemonSelected.accept(species);
        }
    }
    
    /**
     * Gets the currently selected Pokemon.
     * 
     * @return The selected Pokemon species, or null if none selected
     */
    public PokemonSpecies getSelectedPokemon() {
        return selectedPokemon;
    }
    
    /**
     * Gets the currently hovered Pokemon.
     * 
     * @return The hovered Pokemon species, or null if none hovered
     */
    public PokemonSpecies getHoveredPokemon() {
        return hoveredPokemon;
    }
    
    /**
     * Checks if a Pokemon is currently selected.
     * 
     * @return true if a Pokemon is selected
     */
    public boolean hasSelection() {
        return selectedPokemon != null;
    }
    
    /**
     * Gets the Pokemon grid component.
     * 
     * @return The GridPane containing Pokemon cells
     */
    public GridPane getPokemonGrid() {
        return pokemonGrid;
    }
    
    /**
     * Gets the number of cells in the grid.
     * 
     * @return Number of Pokemon cells
     */
    public int getCellCount() {
        return cellMap.size();
    }
    
    /**
     * Gets all Pokemon species displayed in the grid.
     * 
     * @return Array of Pokemon species in the grid
     */
    public PokemonSpecies[] getDisplayedPokemon() {
        return cellMap.keySet().toArray(new PokemonSpecies[0]);
    }
    
    /**
     * Checks if a specific Pokemon is displayed in the grid.
     * 
     * @param species The Pokemon species to check
     * @return true if the Pokemon is in the grid
     */
    public boolean containsPokemon(PokemonSpecies species) {
        return cellMap.containsKey(species);
    }
    
    /**
     * Gets the cell for a specific Pokemon.
     * 
     * @param species The Pokemon species
     * @return The StackPane cell, or null if not found
     */
    public StackPane getCell(PokemonSpecies species) {
        return cellMap.get(species);
    }
}
