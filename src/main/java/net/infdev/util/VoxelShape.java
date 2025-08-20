package net.infdev.util; 

import java.util.ArrayList;
import java.util.List;

public abstract class VoxelShape {
	public float[] shape(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		ArrayList<ArrayList<Float>> shape = new ArrayList<ArrayList<Float>>();

		shape.add(registerPoint(minX, minY, minZ));		// Bottom Left Front
		shape.add(registerPoint(maxX, minY, minZ));		// Bottom Right Front
		shape.add(registerPoint(maxX, maxY, minZ));		// Top Right Front
		shape.add(registerPoint(minX, maxY, minZ));		// Top Left Front
		shape.add(registerPoint(minX, minY, maxZ));		// Bottom Left Back
		shape.add(registerPoint(maxX, minY, maxZ));		// Bottom Right Back
		shape.add(registerPoint(maxX, maxY, maxZ));		// Top Right Back
		shape.add(registerPoint(minX, maxY, maxZ));		// Top Left Back

		float[] shapes = mapToFloat(shape);

		return shapes;
	}

	private ArrayList<Float> registerPoint(float x, float y, float z) {
		ArrayList<Float> point = new ArrayList<Float>();

		point.add(x);
		point.add(y);
		point.add(z);

		return point;
	}

	private static float[] mapToFloat(ArrayList<ArrayList<Float>> list) {
		List<Float> flat = list.stream().flatMap(inner -> inner.stream()).toList();
								
		float[] result = new float[flat.size()];
		
		for (int i = 0; i < flat.size(); i++) {
			result[i] = flat.get(i);
		}

		return result;
	}
}