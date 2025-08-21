package net.infdev.block;

import net.infdev.api.Registries;
import net.infdev.api.Registry;
import net.infdev.api.Block;

import java.util.Iterator;
import java.util.function.ToIntFunction;

public class Blocks {
	private static float[] GREY = { 0.5f, 0.5f, 0.5f };
	private static float[] BROWN = { 0.588f, 0.294f, 0f };
	private static float[] GREEN = { 0F, 1F, 0F };

	public static final Block STONE = new Block("Stone", GREY); 
	public static final Block DIRT = new Block("Dirt", BROWN);
	public static final Block GRASS = new Block("Grass", GREEN);
	  
	public static void registerBlocks() {
		Registries.BLOCK.register("stone", STONE);
		Registries.BLOCK.register("dirt", DIRT);
		Registries.BLOCK.register("grass", GRASS);
	}
	

}