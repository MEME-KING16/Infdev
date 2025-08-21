package net.infdev.api;

public class Block {
	private final String name;
	private float red;
	private float green;
	private float blue;

	/**
	 * @param name The name of the block thats being added
	 * @param red The red value
	 */
	public Block(String name, float red, float green, float blue) {
		this.name = name;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public float[] getColor() {
		float[] color = {this.red, this.green, this.blue};

		return color;
	}

	public String getName() { 
		return name; 
	}
}