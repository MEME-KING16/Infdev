package net.infdev.block;

import net.infdev.api.Registries;
import net.infdev.api.Registry;
import net.infdev.api.Block;

import java.util.Iterator;
import java.util.function.ToIntFunction;

public class Blocks {
	public static final Block STONE = new Block("Stone", 0.5f, 0.5f, 0.5f); 
	public static final Block DIRT = new Block("Dirt", 0.588f, 0.294f, 0f);
	public static final Block GRASS = new Block("Grass", 0F, 1F, 0F);
	  
	public static void registerBlocks() {
		Registries.BLOCK.register("stone", STONE);
		Registries.BLOCK.register("dirt", DIRT);
		Registries.BLOCK.register("grass", GRASS);
	}
	

}