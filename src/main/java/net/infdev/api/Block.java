package net.infdev.api;

import net.infdev.engine.graph.Model;

public class Block {
	private final String name;
	private float[] color;
	private byte id;
	private Model model;
	private static byte globID = 0;

	/**
	 * @param name The name of the block thats being added
	 * @param red The red value
	 */
	public Block(String name, float[] color) {
		this.name = name;
		this.color = color;
		this.id = globID;
		globID+=1;
	}

	public float[] getColor() {
		return this.color;
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
}