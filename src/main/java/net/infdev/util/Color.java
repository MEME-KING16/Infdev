package net.infdev.util;

public class Color {
	private float[] color;

	public Color(float red, float green, float blue) {
		this.color[0] = red;
		this.color[1] = green;
		this.color[2] = blue;
	}

	public float[] getColor() {
		return this.color;
	}
}