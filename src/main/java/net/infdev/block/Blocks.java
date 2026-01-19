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
	public static final Block OAK_LOG = new Block("Oak Log");
	public static final Block OAK_LEAVES = new Block("Oak Leaves");
	public static final Block CRAFTING_TABLE = new Block("Crafting Table");

	public static void registerBlocks(Scene scene) {
		// Set block hardness values (in seconds)
		DIRT.setHardness(0.5f);
		GRASS.setHardness(0.5f);
		STONE.setHardness(3.0f);
		STONE.setRequiresTool(true);
		OAK_LOG.setHardness(2.0f);
		OAK_LEAVES.setHardness(0.2f);
		SAND.setHardness(0.5f);
		WATER.setHardness(100.0f);
		CRAFTING_TABLE.setHardness(2.5f);

		scene.getTextureCache().addTexture("models/block/stone.png");
		scene.getTextureCache().addTexture("models/block/dirt.png");
		scene.getTextureCache().addTexture("models/block/grass_block.png");
		scene.getTextureCache().addTexture("models/block/water.png");
		scene.getTextureCache().addTexture("models/block/sand.png");
		scene.getTextureCache().addTexture("models/block/oak_log.png");
		scene.getTextureCache().addTexture("models/block/oak_leaves.png");
		scene.getTextureCache().addTexture("models/block/crafting_table.png");

		STONE.setModel(ModelLoader.loadModel("stone", "models/block/stone.obj", scene.getTextureCache()));
        DIRT.setModel(ModelLoader.loadModel("dirt", "models/block/dirt.obj", scene.getTextureCache()));
        GRASS.setModel(ModelLoader.loadModel("grass_block", "models/block/grass_block.obj", scene.getTextureCache()));
        WATER.setModel(ModelLoader.loadModel("water", "models/block/water.obj", scene.getTextureCache()));
        SAND.setModel(ModelLoader.loadModel("sand", "models/block/sand.obj", scene.getTextureCache()));
        OAK_LOG.setModel(ModelLoader.loadModel("oak_log", "models/block/oak_log.obj", scene.getTextureCache()));
        OAK_LEAVES.setModel(ModelLoader.loadModel("oak_leaves", "models/block/oak_leaves.obj", scene.getTextureCache()));
        CRAFTING_TABLE.setModel(ModelLoader.loadModel("crafting_table", "models/block/crafting_table.obj", scene.getTextureCache()));

		setModelTexture(STONE.getModel(), "models/block/stone.png");
		setModelTexture(DIRT.getModel(), "models/block/dirt.png");
		setModelTexture(GRASS.getModel(), "models/block/grass_block.png");
		setModelTexture(WATER.getModel(), "models/block/water.png");
		setModelTexture(SAND.getModel(), "models/block/sand.png");
		setModelTexture(OAK_LOG.getModel(), "models/block/oak_log.png");
		setModelTexture(OAK_LEAVES.getModel(), "models/block/oak_leaves.png");
		setModelTexture(CRAFTING_TABLE.getModel(), "models/block/crafting_table.png");

		scene.addModel(STONE.getModel());
		scene.addModel(DIRT.getModel());
		scene.addModel(GRASS.getModel());
		scene.addModel(WATER.getModel());
		scene.addModel(SAND.getModel());
		scene.addModel(OAK_LOG.getModel());
		scene.addModel(OAK_LEAVES.getModel());
		scene.addModel(CRAFTING_TABLE.getModel());

		Registries.BLOCK.register("infdev", "air", AIR);
		Registries.BLOCK.register("infdev", "stone", STONE);
		Registries.BLOCK.register("infdev", "dirt", DIRT);
		Registries.BLOCK.register("infdev", "grass_block", GRASS);
		Registries.BLOCK.register("infdev", "water", WATER);
		Registries.BLOCK.register("infdev", "sand", SAND);
		Registries.BLOCK.register("infdev", "oak_log", OAK_LOG);
		Registries.BLOCK.register("infdev", "oak_leaves", OAK_LEAVES);
		Registries.BLOCK.register("infdev", "crafting_table", CRAFTING_TABLE);
	}

	private static void setModelTexture(Model model, String texturePath) {
		for (Material material : model.getMaterialList()) {
			material.setTexturePath(texturePath);
		}
	}
}