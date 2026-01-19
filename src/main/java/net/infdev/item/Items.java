package net.infdev.item;

import net.infdev.api.Registries;
import net.infdev.api.world.item.Item;
import net.infdev.engine.graph.Material;
import net.infdev.engine.graph.Model;
import net.infdev.engine.scene.ModelLoader;
import net.infdev.engine.scene.Scene;

public class Items {


	public static final Item AIR = new Item("Air");
	public static final Item STONE = new Item("Stone");
	public static final Item DIRT = new Item("Dirt");
	public static final Item GRASS_BLOCK = new Item("Grass Block");
	public static final Item WATER = new Item("Water");
	public static final Item SAND = new Item("Sand");
	public static final Item OAK_LOG = new Item("Oak Log");
	public static final Item OAK_LEAVES = new Item("Oak Leaves");
	public static final Item OAK_PLANKS = new Item("Oak Planks");
	public static final Item STICKS = new Item("Sticks");
	public static final Item CRAFTING_TABLE = new Item("Crafting Table");
	public static final Item WOODEN_PICKAXE = new Item("Wooden Pickaxe");
	public static final Item WOODEN_AXE = new Item("Wooden Axe");
	public static final Item WOODEN_SHOVEL = new Item("Wooden Shovel");
	public static final Item WOODEN_SWORD = new Item("Wooden Sword");
	public static final Item COOKED_PORKCHOP = new Item("Cooked Porkchop", true, 8);
	public static final Item COOKED_BEEF = new Item("Cooked Beef", true, 8);
	public static final Item ROTTEN_FLESH = new Item("Rotten Flesh", true, 4);

	public static void registerItems(Scene scene) {
		// Set tool properties
		WOODEN_PICKAXE.setToolType("pickaxe");
		WOODEN_PICKAXE.setMiningSpeedMultiplier(2.0f);

		WOODEN_AXE.setToolType("axe");
		WOODEN_AXE.setMiningSpeedMultiplier(2.0f);

		WOODEN_SHOVEL.setToolType("shovel");
		WOODEN_SHOVEL.setMiningSpeedMultiplier(2.0f);

		// Load item textures (reusing block textures for block items)
		scene.getTextureCache().createTexture("models/block/stone.png");
		scene.getTextureCache().createTexture("models/block/dirt.png");
		scene.getTextureCache().createTexture("models/block/grass_block.png");
		scene.getTextureCache().createTexture("models/block/water.png");
		scene.getTextureCache().createTexture("models/block/sand.png");
		scene.getTextureCache().createTexture("models/block/oak_log.png");
		scene.getTextureCache().createTexture("models/block/oak_leaves.png");
		scene.getTextureCache().createTexture("models/item/oak_planks.png");
		scene.getTextureCache().createTexture("models/item/sticks.png");
		scene.getTextureCache().createTexture("models/block/crafting_table.png");
		scene.getTextureCache().createTexture("models/item/wooden_pickaxe.png");
		scene.getTextureCache().createTexture("models/item/wooden_axe.png");
		scene.getTextureCache().createTexture("models/item/wooden_shovel.png");
		scene.getTextureCache().createTexture("models/item/wooden_sword.png");
		scene.getTextureCache().createTexture("models/item/cooked_porkchop.png");
		scene.getTextureCache().createTexture("models/item/cooked_beef.png");
		scene.getTextureCache().createTexture("models/item/rotten_flesh.png");

		// Assign texture paths to items
		AIR.setTexturePath(null);
		STONE.setTexturePath("models/block/stone.png");
		DIRT.setTexturePath("models/block/dirt.png");
		GRASS_BLOCK.setTexturePath("models/block/grass_block.png");
		WATER.setTexturePath("models/block/water.png");
		SAND.setTexturePath("models/block/sand.png");
		OAK_LOG.setTexturePath("models/block/oak_log.png");
		OAK_LEAVES.setTexturePath("models/block/oak_leaves.png");
		OAK_PLANKS.setTexturePath("models/item/oak_planks.png");
		STICKS.setTexturePath("models/item/sticks.png");
		CRAFTING_TABLE.setTexturePath("models/block/crafting_table.png");
		WOODEN_PICKAXE.setTexturePath("models/item/wooden_pickaxe.png");
		WOODEN_AXE.setTexturePath("models/item/wooden_axe.png");
		WOODEN_SHOVEL.setTexturePath("models/item/wooden_shovel.png");
		WOODEN_SWORD.setTexturePath("models/item/wooden_sword.png");
		COOKED_PORKCHOP.setTexturePath("models/item/cooked_porkchop.png");
		COOKED_BEEF.setTexturePath("models/item/cooked_beef.png");
		ROTTEN_FLESH.setTexturePath("models/item/rotten_flesh.png");

		Registries.ITEM.register("infdev", "air", AIR);
		Registries.ITEM.register("infdev", "stone", STONE);
		Registries.ITEM.register("infdev", "dirt", DIRT);
		Registries.ITEM.register("infdev", "grass_block", GRASS_BLOCK);
		Registries.ITEM.register("infdev", "water", WATER);
		Registries.ITEM.register("infdev", "sand", SAND);
		Registries.ITEM.register("infdev", "oak_log", OAK_LOG);
		Registries.ITEM.register("infdev", "oak_leaves", OAK_LEAVES);
		Registries.ITEM.register("infdev", "oak_planks", OAK_PLANKS);
		Registries.ITEM.register("infdev", "sticks", STICKS);
		Registries.ITEM.register("infdev", "crafting_table", CRAFTING_TABLE);
		Registries.ITEM.register("infdev", "wooden_pickaxe", WOODEN_PICKAXE);
		Registries.ITEM.register("infdev", "wooden_axe", WOODEN_AXE);
		Registries.ITEM.register("infdev", "wooden_shovel", WOODEN_SHOVEL);
		Registries.ITEM.register("infdev", "wooden_sword", WOODEN_SWORD);
		Registries.ITEM.register("infdev", "cooked_porkchop", COOKED_PORKCHOP);
		Registries.ITEM.register("infdev", "cooked_beef", COOKED_BEEF);
		Registries.ITEM.register("infdev", "rotten_flesh", ROTTEN_FLESH);
	}

	private static void setModelTexture(Model model, String texturePath) {
		for (Material material : model.getMaterialList()) {
			material.setTexturePath(texturePath);
		}
	}
}