package net.infdev.engine.graph;

import java.util.*;

import org.joml.Vector4f;

public class Material {
    public static final Vector4f DEFAULT_COLOR = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
    private Vector4f ambientColor;
    private Vector4f diffuseColor;
	private List<Mesh> meshList;
    private String texturePath;
    private float reflectance;
    private Vector4f specularColor;

    public Material() {
        diffuseColor = DEFAULT_COLOR;
        ambientColor = DEFAULT_COLOR;
		specularColor = DEFAULT_COLOR;
        meshList = new ArrayList<>();
    }

    public void cleanup() {
        meshList.forEach(Mesh::cleanup);
    }

    public List<Mesh> getMeshList() {
        return meshList;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public Vector4f getDiffuseColor() {
        return diffuseColor;
    }

    public Vector4f getAmbientColor() {
        return ambientColor;
    }

    public float getReflectance() {
        return reflectance;
    }

    public Vector4f getSpecularColor() {
        return specularColor;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public void setDiffuseColor(Vector4f diffuseColor) {
        this.diffuseColor = diffuseColor;
    }

    public void setAmbientColor(Vector4f ambientColor) {
        this.ambientColor = ambientColor;
    }
    
    public void setReflectance(float reflectance) {
        this.reflectance = reflectance;
    }

    public void setSpecularColor(Vector4f specularColor) {
        this.specularColor = specularColor;
    }
}