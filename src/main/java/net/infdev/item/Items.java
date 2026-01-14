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

	public static void registerItems(Scene scene) {
		// scene.getTextureCache().addTexture("models/item/stone.png");
		// scene.getTextureCache().addTexture("models/item/dirt.png");
		// scene.getTextureCache().addTexture("models/item/grass_block.png");
		// scene.getTextureCache().addTexture("models/item/water.png");
		// scene.getTextureCache().addTexture("models/item/sand.png");

		// STONE.setModel(ModelLoader.loadModel("stone", "models/block/stone.obj", scene.getTextureCache()));
        // DIRT.setModel(ModelLoader.loadModel("dirt", "models/block/dirt.obj", scene.getTextureCache()));
        // GRASS.setModel(ModelLoader.loadModel("grass_block", "models/block/grass_block.obj", scene.getTextureCache()));
        // WATER.setModel(ModelLoader.loadModel("water", "models/block/water.obj", scene.getTextureCache()));
        // SAND.setModel(ModelLoader.loadModel("sand", "models/block/sand.obj", scene.getTextureCache()));

		// setModelTexture(STONE.getModel(), "models/block/stone.png");
		// setModelTexture(DIRT.getModel(), "models/block/dirt.png");
		// setModelTexture(GRASS.getModel(), "models/block/grass_block.png");
		// setModelTexture(WATER.getModel(), "models/block/water.png");
		// setModelTexture(SAND.getModel(), "models/block/sand.png");
		
		// scene.addModel(STONE.getModel());
		// scene.addModel(DIRT.getModel());
		// scene.addModel(GRASS.getModel());
		// scene.addModel(WATER.getModel());
		// scene.addModel(SAND.getModel());

		Registries.ITEM.register("infdev", "air", AIR);
		Registries.ITEM.register("infdev", "stone", STONE);
		Registries.ITEM.register("infdev", "dirt", DIRT);
		Registries.ITEM.register("infdev", "grass_block", GRASS_BLOCK);
		Registries.ITEM.register("infdev", "water", WATER);
		Registries.ITEM.register("infdev", "sand", SAND);
	}

	private static void setModelTexture(Model model, String texturePath) {
		for (Material material : model.getMaterialList()) {
			material.setTexturePath(texturePath);
		}
	}
}