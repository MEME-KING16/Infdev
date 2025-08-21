package net.infdev.block;

import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform4f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;


public class Block {
	public float red;
	public float green;
	public float blue;

	public float[] cube;

	/**
	 * Adds a cube to the world
	 * @param x - x cord for the front top left coner
	 * @param y - y cord for the front top left coner
	 * @param z - z cord for the front top left coner
	 * @param red - Red float value
	 * @param green - Green float value
	 * @param blue - Blue float value
	 */
	public void registerCube(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		ArrayList<ArrayList<Float>> cube = new ArrayList<ArrayList<Float>>();

		cube.add(registerPoint(minX, minY, minZ, this.red, this.green, this.blue));		// Bottom Left Front
		cube.add(registerPoint(maxX, minY, minZ, this.red, this.green, this.blue));		// Bottom Right Front
		cube.add(registerPoint(maxX, maxY, minZ, this.red, this.green, this.blue));		// Top Right Front
		cube.add(registerPoint(minX, maxY, minZ, this.red, this.green, this.blue));		// Top Left Front
		cube.add(registerPoint(minX, minY, maxZ, this.red, this.green, this.blue));		// Bottom Left Back
		cube.add(registerPoint(maxX, minY, maxZ, this.red, this.green, this.blue));		// Bottom Right Back
		cube.add(registerPoint(maxX, maxY, maxZ, this.red, this.green, this.blue));		// Top Right Back
		cube.add(registerPoint(minX, maxY, maxZ, this.red, this.green, this.blue));		// Top Left Back

        // double[] vertices = cube.stream().flatMap(List::stream).mapToDouble(Float::floatValue).toArray();
		float[] vertices = mapToFloat(cube);

		this.cube = vertices;
	}

	public void setCubeColor(float[] color) {
		this.red = color[0];
		this.green = color[1];
		this.blue = color[2];
	}

	/**
	 * Render the block
	 * @param projection
	 * @param view
	 * @param mvpLoc
	 * @param indicesCount
	 * @param shaderProgram
	 * @param vao
	 */
    public void render(Matrix4f projection, Matrix4f view, int mvpLoc, int indicesCount, int shaderProgram, int vao) {
		int colorLocation = glGetUniformLocation(shaderProgram, "cubeColor");
		glUniform4f(colorLocation, this.red, this.green, this.blue, 1.0f);

		
        glBindBuffer(GL_ARRAY_BUFFER, vao);
        glBufferData(GL_ARRAY_BUFFER, this.cube, GL_STATIC_DRAW);

        Matrix4f model = new Matrix4f();
			// .translate(0f,0f,0f);
			//.rotate(angle, 0.0f, 1.0f, 0.0f);
		Matrix4f mvp = new Matrix4f();
		projection.mul(view, mvp);
		mvp.mul(model);

		FloatBuffer fb = BufferUtils.createFloatBuffer(16);
		mvp.get(fb);
		glUniformMatrix4fv(mvpLoc, false, fb);
		glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_INT, 0);
    }

	private ArrayList<Float> registerPoint(float x, float y, float z, float red, float green, float blue) {
		ArrayList<Float> point = new ArrayList<Float>();

		point.add(x);
		point.add(y);
		point.add(z);
		point.add(red);
		point.add(green);
		point.add(blue);

		return point;
	}

	private static float[] mapToFloat(ArrayList<ArrayList<Float>> list) {
   		List<Float> flat = list.stream()
                                .flatMap(inner -> inner.stream())
                                .toList();
                                
        float[] result = new float[flat.size()];
        for (int i = 0; i < flat.size(); i++) {
            result[i] = flat.get(i);
        }
        return result;
}
}