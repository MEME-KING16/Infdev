package net.infdev.block;

import net.infdev.api.Registries;
import net.infdev.api.world.block.Block;
import net.infdev.engine.graph.Material;
import net.infdev.engine.graph.Model;
import net.infdev.engine.scene.ModelLoader;
import net.infdev.engine.scene.Scene;

public class Blocks {


	public static final Block AIR = new Block("Air");
	public static final Block STONE = new Block("Stone");
	public static final Block DIRT = new Block("Dirt");
	public static final Block GRASS = new Block("Grass");
	public static final Block WATER = new Block("Water");
	public static final Block SAND = new Block("Sand");

	public static void registerBlocks(Scene scene) {
		scene.getTextureCache().addTexture("models/block/stone.png");
		scene.getTextureCache().addTexture("models/block/dirt.png");
		scene.getTextureCache().addTexture("models/block/grass_block.png");
		scene.getTextureCache().addTexture("models/block/water.png");
		scene.getTextureCache().addTexture("models/block/sand.png");

		STONE.setModel(ModelLoader.loadModel("stone", "models/block/stone.obj", scene.getTextureCache()));
        DIRT.setModel(ModelLoader.loadModel("dirt", "models/block/dirt.obj", scene.getTextureCache()));
        GRASS.setModel(ModelLoader.loadModel("grass_block", "models/block/grass_block.obj", scene.getTextureCache()));
        WATER.setModel(ModelLoader.loadModel("water", "models/block/water.obj", scene.getTextureCache()));
        SAND.setModel(ModelLoader.loadModel("sand", "models/block/sand.obj", scene.getTextureCache()));

		setModelTexture(STONE.getModel(), "models/block/stone.png");
		setModelTexture(DIRT.getModel(), "models/block/dirt.png");
		setModelTexture(GRASS.getModel(), "models/block/grass_block.png");
		setModelTexture(WATER.getModel(), "models/block/water.png");
		setModelTexture(SAND.getModel(), "models/block/sand.png");
		
		scene.addModel(STONE.getModel());
		scene.addModel(DIRT.getModel());
		scene.addModel(GRASS.getModel());
		scene.addModel(WATER.getModel());
		scene.addModel(SAND.getModel());

		Registries.BLOCK.register("infdev", "air", AIR);
		Registries.BLOCK.register("infdev", "stone", STONE);
		Registries.BLOCK.register("infdev", "dirt", DIRT);
		Registries.BLOCK.register("infdev", "grass_block", GRASS);
		Registries.BLOCK.register("infdev", "water", WATER);
		Registries.BLOCK.register("infdev", "sand", SAND);
	}

	private static void setModelTexture(Model model, String texturePath) {
		for (Material material : model.getMaterialList()) {
			material.setTexturePath(texturePath);
		}
	}
}