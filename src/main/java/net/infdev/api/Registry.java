package net.infdev.api;

import java.util.HashMap;
import java.util.Map;

public class Registry<T> {
	private final Map<String, T> entries = new HashMap<>();

    /**
     * Add to registry
     * @param id - id
     * @param value - data
     */
	public void register(String id, T value) {
		if (entries.containsKey(id)) {
			throw new IllegalArgumentException("Duplicate id: " + id);
		}
		entries.put(id, value);
	}
    /**
     * Get from the registy
     * @param id -id
     * @return
     */
	public T get(String id) {
		return entries.get(id);
	}
    /**
     * Get all from registry
     * @return
     */
	public Map<String, T> getAll() {
		return entries;
	}
}