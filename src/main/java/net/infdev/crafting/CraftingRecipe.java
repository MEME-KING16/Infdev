package net.infdev.crafting;

import net.infdev.api.world.item.Item;
import net.infdev.api.world.item.ItemStack;
import net.infdev.item.Items;

public class CraftingRecipe {
    private final Item[][] pattern;
    private final Item output;
    private final int outputCount;
    private final boolean shapeless;
    private final int width;
    private final int height;

    /**
     * Creates a shaped crafting recipe
     * @param pattern 2D array representing the crafting pattern (null or AIR for empty slots)
     * @param output The item produced by this recipe
     * @param outputCount The number of items produced
     */
    public CraftingRecipe(Item[][] pattern, Item output, int outputCount) {
        this.pattern = pattern;
        this.output = output;
        this.outputCount = outputCount;
        this.shapeless = false;
        this.height = pattern.length;
        this.width = pattern.length > 0 ? pattern[0].length : 0;
    }

    /**
     * Creates a shapeless crafting recipe
     * @param ingredients Array of required items (order doesn't matter)
     * @param output The item produced by this recipe
     * @param outputCount The number of items produced
     */
    public static CraftingRecipe shapeless(Item[] ingredients, Item output, int outputCount) {
        CraftingRecipe recipe = new CraftingRecipe(new Item[1][ingredients.length], output, outputCount);
        System.arraycopy(ingredients, 0, recipe.pattern[0], 0, ingredients.length);
        return new CraftingRecipe(recipe.pattern, output, outputCount, true);
    }

    private CraftingRecipe(Item[][] pattern, Item output, int outputCount, boolean shapeless) {
        this.pattern = pattern;
        this.output = output;
        this.outputCount = outputCount;
        this.shapeless = shapeless;
        this.height = pattern.length;
        this.width = pattern.length > 0 ? pattern[0].length : 0;
    }

    /**
     * Checks if the given crafting grid matches this recipe
     * @param grid The crafting grid to check (2x2 or 3x3)
     * @return true if the grid matches this recipe
     */
    public boolean matches(ItemStack[][] grid) {
        if (shapeless) {
            return matchesShapeless(grid);
        } else {
            return matchesShaped(grid);
        }
    }

    private boolean matchesShaped(ItemStack[][] grid) {
        int gridHeight = grid.length;
        int gridWidth = grid[0].length;

        if (height > gridHeight || width > gridWidth) {
            return false;
        }

        for (int offsetY = 0; offsetY <= gridHeight - height; offsetY++) {
            for (int offsetX = 0; offsetX <= gridWidth - width; offsetX++) {
                if (matchesAt(grid, offsetX, offsetY)) {
                    if (checkEmptySlots(grid, offsetX, offsetY)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean matchesAt(ItemStack[][] grid, int offsetX, int offsetY) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Item patternItem = pattern[y][x];
                ItemStack gridStack = grid[offsetY + y][offsetX + x];

                if (patternItem != null && !isAir(patternItem)) {
                    if (gridStack.isEmpty() || !gridStack.getItem().equals(patternItem)) {
                        return false;
                    }
                } else {
                    if (!gridStack.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean checkEmptySlots(ItemStack[][] grid, int offsetX, int offsetY) {
        int gridHeight = grid.length;
        int gridWidth = grid[0].length;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (x >= offsetX && x < offsetX + width && y >= offsetY && y < offsetY + height) {
                    continue;
                }

                if (!grid[y][x].isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean matchesShapeless(ItemStack[][] grid) {
        int[] requiredCounts = new int[pattern[0].length];
        for (int i = 0; i < pattern[0].length; i++) {
            requiredCounts[i] = 1;
        }

        for (ItemStack[] row : grid) {
            for (ItemStack stack : row) {
                if (stack.isEmpty()) continue;

                boolean found = false;
                for (int i = 0; i < pattern[0].length; i++) {
                    if (pattern[0][i] != null && stack.getItem().equals(pattern[0][i]) && requiredCounts[i] > 0) {
                        requiredCounts[i]--;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return false;
                }
            }
        }

        for (int count : requiredCounts) {
            if (count > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the output of this recipe
     */
    public ItemStack getOutput() {
        return new ItemStack(output, outputCount);
    }

    /**
     * Consumes ingredients from the crafting grid
     */
    public void consumeIngredients(ItemStack[][] grid) {
        for (ItemStack[] row : grid) {
            for (ItemStack stack : row) {
                if (!stack.isEmpty()) {
                    stack.setCount(stack.getCount() - 1);
                    if (stack.getCount() <= 0) {
                        stack.setItem(Items.AIR);
                        stack.setCount(0);
                    }
                }
            }
        }
    }

    private boolean isAir(Item item) {
        return item == null || item.equals(Items.AIR);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isShapeless() {
        return shapeless;
    }
}
