package net.infdev.api;

import java.util.HashMap;
import java.util.Map;

public class Registry<T> {
	private final Map<String, T> entries = new HashMap<>();

    /**
     * Registers a new thing that has an ID and holds data
     * 
     * @param id The ID you want to give to the registered thing
     * @param value The data you want to put in the registered thing
     */
	public void register(String namespace, String id, T value) {
		if (entries.containsKey(namespace + ":" + id)) {
			throw new IllegalArgumentException("Duplicate id: " + id + " in namespace: " + namespace);
		}

		entries.put(namespace + ":" + id, value);
	}

    /**
     * Get from the registry
     * 
     * @param id The ID of the registered thing you want to get
     * 
     * @return The registered thing that has the ID
     */
	public T get(String namespace, String id) {
		return entries.get(namespace + ":" + id);
	}

    /**
     * Get all registered things
     * 
     * @return All registered things
     */
	public Map<String, T> getAll() {
		return entries;
	}
}