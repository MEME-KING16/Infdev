package net.infdev.util.math;

public class Vec3d implements Position {
	public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);
	public final double x;
	public final double y;
	public final double z;

	/**
	 * Creates a Vector of type Double pointing to given coords
	 * 
	 * @param x The x coord 
	 * @param y The y coord 
	 * @param z The z coord 
	 */
	public Vec3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * 
	 */
	public Vec3d relativize(Vec3d vec) {
		return new Vec3d(vec.x - this.x, vec.y - this.y, vec.z - this.z);
	}

	/**
	 * Gets the path from the origin to the Vector's coords of smallest distance
	 * 
	 * @return The path of smallest distance
	 */
	public Vec3d normalize() {
		double normalized = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);

		return new Vec3d(this.x / normalized, this.y / normalized, this.z / normalized);
	}

	public double dotProduct(Vec3d vec) {
		return this.x * vec.x + this.y * vec.y + this.z * vec.z;
	}

	public Vec3d crossProduct(Vec3d vec) {
		return new Vec3d(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
	}

	/**
	 * Subtracts a Vector from another, making the Vector shorter 
	 * 
	 * @param vec The Vector that will be removed to the other Vector
	 * 
	 * @return The new Vector with the un-extension
	 */
	public Vec3d subtract(Vec3d vec) {
		return this.subtract(vec.x, vec.y, vec.z);
	}

	/**
	 * Subtracts the given values from the Vector, making the Vector shorter 
	 * 
	 * @param x How much more you want to subtract to the X-Axis
	 * @param y How much more you want to subtract to the Y-Axis
	 * @param z How much more you want to subtract to the Z-Axis
	 * 
	 * @return The new Vector with the un-extension
	 */
	public Vec3d subtract(double x, double y, double z) {
		return this.add(-x, -y, -z);
	}

	/**
	 * Adds a Vector to another, making the Vector longer 
	 * 
	 * @param vec The Vector that will be added to the other Vector
	 * 
	 * @return The new Vector with the extension
	 */
	public Vec3d add(Vec3d vec) {
		return this.add(vec.x, vec.y, vec.z);
	}

	/**
	 * Adds the given values to the Vector, making the Vector longer 
	 * 
	 * @param x How much more you want to add to the X-Axis
	 * @param y How much more you want to add to the Y-Axis
	 * @param z How much more you want to add to the Z-Axis
	 * 
	 * @return The new Vector with the extension
	 */
	public Vec3d add(double x, double y, double z) {
		return new Vec3d(this.x + x, this.y + y, this.z + z);
	}

	@Override
	public double getX() {
		return this.x;
	}

	@Override
	public double getY() {
		return this.y;
	}

	@Override
	public double getZ() {
		return this.z;
	}
}