package net.infdev.api.world.block;

import net.infdev.engine.graph.Model;

public class Block {
	private final String name;
	private byte id;
	private Model model;
	private static byte globID = 0;
	private float hardness = 1.0f; // Default hardness (1 second to mine)
	private boolean requiresTool = false; // Whether block requires proper tool

	/**
	 * @param name The name of the block thats being added
	 */
	public Block(String name) {
		this.name = name;
		this.id = globID;
		globID+=1;
	}

	public String getName() { 
		return name; 
	}
	
	public byte getId() {
		return id;
	}
	
	public void setModel(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

	public float getHardness() {
		return hardness;
	}

	public void setHardness(float hardness) {
		this.hardness = hardness;
	}

	public boolean requiresTool() {
		return requiresTool;
	}

	public void setRequiresTool(boolean requiresTool) {
		this.requiresTool = requiresTool;
	}
}