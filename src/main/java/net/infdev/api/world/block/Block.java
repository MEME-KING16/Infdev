package net.infdev.api.world.block;

import net.infdev.engine.graph.Model;

public class Block {
	private final String name;
	private byte id;
	private Model model;
	private static byte globID = 0;

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
}