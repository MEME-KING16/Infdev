package net.infdev.api.world.item;

public class Item {
    private final String name;
    private byte id;
    private static byte globID = 0;
    private String texturePath;

    public Item(String name) {
        this.name = name;
        this.id = globID;
		globID+=1;
    }

    public byte getId() {
		return id;
	}

    public String getName() {
        return name;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public String getTexturePath() {
        return texturePath;
    }
}