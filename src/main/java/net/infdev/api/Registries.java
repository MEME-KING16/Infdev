package net.infdev.api;

import net.infdev.api.world.block.Block;
import net.infdev.api.world.entity.Entity;
import net.infdev.api.world.item.Item;

public class Registries {
    public static final Registry<Block> BLOCK = new Registry<>();
    public static final Registry<Item> ITEM = new Registry<>();
    public static final Registry<Entity> ENTITY = new Registry<>();
}

