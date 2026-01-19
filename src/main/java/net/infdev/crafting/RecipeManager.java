package net.infdev.crafting;

import net.infdev.api.world.item.Item;
import net.infdev.api.world.item.ItemStack;
import net.infdev.item.Items;

import java.util.ArrayList;
import java.util.List;

public class RecipeManager {
    private static final List<CraftingRecipe> recipes = new ArrayList<>();

    /**
     * Registers a crafting recipe
     */
    public static void registerRecipe(CraftingRecipe recipe) {
        recipes.add(recipe);
    }

    /**
     * Finds a matching recipe for the given crafting grid
     * @param grid The crafting grid (2x2 or 3x3)
     * @return The matching recipe, or null if no match found
     */
    public static CraftingRecipe findMatchingRecipe(ItemStack[][] grid) {
        for (CraftingRecipe recipe : recipes) {
            // Check if recipe fits in the grid size
            int gridSize = grid.length;
            if (recipe.getWidth() > gridSize || recipe.getHeight() > gridSize) {
                continue;
            }

            if (recipe.matches(grid)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Gets the crafting result for a given grid
     * @param grid The crafting grid
     * @return The output item stack, or empty if no match
     */
    public static ItemStack getCraftingResult(ItemStack[][] grid) {
        CraftingRecipe recipe = findMatchingRecipe(grid);
        if (recipe != null) {
            return recipe.getOutput();
        }
        return new ItemStack();
    }

    /**
     * Initializes all crafting recipes
     */
    public static void initializeRecipes() {
        recipes.clear();

        // Oak Planks from Oak Log (shapeless, 1 log -> 4 planks)
        registerRecipe(CraftingRecipe.shapeless(
            new Item[]{Items.OAK_LOG},
            Items.OAK_PLANKS,
            4
        ));

        // Sticks from Oak Planks (shaped, 2 planks vertically -> 4 sticks)
        registerRecipe(new CraftingRecipe(
            new Item[][]{
                {Items.OAK_PLANKS},
                {Items.OAK_PLANKS}
            },
            Items.STICKS,
            4
        ));

        // Crafting Table from Oak Planks (shaped, 2x2 of planks -> 1 crafting table)
        registerRecipe(new CraftingRecipe(
            new Item[][]{
                {Items.OAK_PLANKS, Items.OAK_PLANKS},
                {Items.OAK_PLANKS, Items.OAK_PLANKS}
            },
            Items.CRAFTING_TABLE,
            1
        ));

        // Example 3x3 recipe: Wooden Pickaxe (requires 3 planks and 2 sticks)
        registerRecipe(new CraftingRecipe(
            new Item[][]{
                {Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS},
                {null, Items.STICKS, null},
                {null, Items.STICKS, null}
            },
            Items.WOODEN_PICKAXE,
            1
        ));

        // Wooden Axe (3x3 recipe)
        registerRecipe(new CraftingRecipe(
            new Item[][]{
                {Items.OAK_PLANKS, Items.OAK_PLANKS, null},
                {Items.OAK_PLANKS, Items.STICKS, null},
                {null, Items.STICKS, null}
            },
            Items.WOODEN_AXE,
            1
        ));

        // Wooden Shovel (3x3 recipe)
        registerRecipe(new CraftingRecipe(
            new Item[][]{
                {null, Items.OAK_PLANKS, null},
                {null, Items.STICKS, null},
                {null, Items.STICKS, null}
            },
            Items.WOODEN_SHOVEL,
            1
        ));

        // Wooden Sword (3x3 recipe)
        registerRecipe(new CraftingRecipe(
            new Item[][]{
                {null, Items.OAK_PLANKS, null},
                {null, Items.OAK_PLANKS, null},
                {null, Items.STICKS, null}
            },
            Items.WOODEN_SWORD,
            1
        ));

        // Custom quotes
        registerRecipe(CraftingRecipe.shapeless(
            new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, Items.STICKS},
            Items.KEY_CONSISTENCY,
            1
        ));

        registerRecipe(CraftingRecipe.shapeless(
            new Item[]{Items.STONE, Items.ROTTEN_FLESH},
            Items.DONT_SUFFER,
            1
        ));

        registerRecipe(CraftingRecipe.shapeless(
            new Item[]{Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS},
            Items.CLASS_BLUEPRINT,
            1
        ));

        // BMW 1000 RR from all three quotes
        registerRecipe(CraftingRecipe.shapeless(
            new Item[]{Items.KEY_CONSISTENCY, Items.DONT_SUFFER, Items.CLASS_BLUEPRINT},
            Items.BMW_1000_RR,
            1
        ));
    }

    /**
     * Clears all registered recipes
     */
    public static void clearRecipes() {
        recipes.clear();
    }

    /**
     * Gets all registered recipes
     */
    public static List<CraftingRecipe> getRecipes() {
        return new ArrayList<>(recipes);
    }
}
