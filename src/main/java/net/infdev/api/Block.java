package net.infdev.api;

public class Block {
	private final String name;
	private float[] color;

	/**
	 * @param name The name of the block thats being added
	 * @param red The red value
	 */
	public Block(String name, float[] color) {
		this.name = name;
		this.color = color;
	}

	public float[] getColor() {
		return this.color;
	}

	public String getName() { 
		return name; 
	}
}