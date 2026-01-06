package net.infdev.engine;

import java.io.IOException;
import java.io.InputStream;

public class Utils {

	private Utils() {
		// Utility class
	}

	public static String readFile(String filePath) {
		try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(filePath)) {
			if (is == null) {
				throw new RuntimeException("File not found in classpath: " + filePath);
			}
			return new String(is.readAllBytes());
		} catch (IOException e) {
			throw new RuntimeException("Error reading file from classpath [" + filePath + "]", e);
		}
	}
}