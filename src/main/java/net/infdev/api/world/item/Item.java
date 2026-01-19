package net.infdev.api.world.item;

public class Item {
    private final String name;
    private byte id;
    private static byte globID = 0;
    private String texturePath;
    private boolean isFood;
    private int foodValue;
    private String toolType = null; // "pickaxe", "axe", "shovel", null for non-tools
    private float miningSpeedMultiplier = 1.0f; // Speed multiplier for mining

    public Item(String name) {
        this.name = name;
        this.id = globID;
		globID+=1;
        this.isFood = false;
        this.foodValue = 0;
    }

    public Item(String name, boolean isFood, int foodValue) {
        this.name = name;
        this.id = globID;
		globID+=1;
        this.isFood = isFood;
        this.foodValue = foodValue;
    }

    public byte getId() {
		return id;
	}

    public String getName() {
        return name;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public boolean isFood() {
        return isFood;
    }

    public int getFoodValue() {
        return foodValue;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public float getMiningSpeedMultiplier() {
        return miningSpeedMultiplier;
    }

    public void setMiningSpeedMultiplier(float miningSpeedMultiplier) {
        this.miningSpeedMultiplier = miningSpeedMultiplier;
    }
}