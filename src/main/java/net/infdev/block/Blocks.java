package net.infdev.block;

import net.infdev.api.Registries;
import net.infdev.api.Registry;
import net.infdev.engine.graph.TextureCache;
import net.infdev.engine.scene.ModelLoader;
import net.infdev.engine.scene.Scene;
import net.infdev.Main;
import net.infdev.api.Block;

import java.util.Iterator;
import java.util.function.ToIntFunction;

public class Blocks {

	private static float[] GREY = { 0.5f, 0.5f, 0.5f };
	private static float[] BROWN = { 0.588f, 0.294f, 0f };
	private static float[] GREEN = { 0F, 1F, 0F };
	private static float[] NOTHING = { 0f, 0f, 0f };

	public static final Block AIR = new Block("Air", NOTHING);
	public static final Block STONE = new Block("Stone", GREY);
	public static final Block DIRT = new Block("Dirt", BROWN);
	public static final Block GRASS = new Block("Grass", GREEN);
	  
	public static void registerBlocks(Scene scene) {
		STONE.setModel(ModelLoader.loadModel("stone", "models/block/stone.obj", scene.getTextureCache()));
        DIRT.setModel(ModelLoader.loadModel("dirt_block", "models/block/dirt_block.obj", scene.getTextureCache()));
        GRASS.setModel(ModelLoader.loadModel("grass_block", "models/block/grass_block.obj", scene.getTextureCache()));

		scene.addModel(STONE.getModel());
		scene.addModel(DIRT.getModel());
		scene.addModel(GRASS.getModel());

		Registries.BLOCK.register("air", AIR);
		Registries.BLOCK.register("stone", STONE);
		Registries.BLOCK.register("dirt_block", DIRT);
		Registries.BLOCK.register("grass_block", GRASS);
	}
}