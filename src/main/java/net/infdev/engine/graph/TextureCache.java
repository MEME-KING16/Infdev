package net.infdev.engine.graph;

import java.util.*;

public class TextureCache {
    public static final String DEFAULT_TEXTURE = "models/block/stone.png";
    private Map<String, Texture> textureMap;

    public TextureCache() {
        textureMap = new HashMap<>();
    }

    public void cleanup() {
        textureMap.values().forEach(Texture::cleanup);
    }

    public Texture createTexture(String texturePath) {
        return textureMap.computeIfAbsent(texturePath, Texture::new);
    }

    public Texture getTexture(String texturePath) {
        Texture texture = null;
        if (texturePath != null) {
            texture = textureMap.get(texturePath);
        }
        if (texture == null) {
            texture = textureMap.get(DEFAULT_TEXTURE);
            System.err.println("Texture not found in cache: " + texturePath + ", using default texture.");
        }
        return texture;
    }

    public void addTexture(String texturePath) {
        textureMap.put(texturePath, new Texture(texturePath));
    }
}