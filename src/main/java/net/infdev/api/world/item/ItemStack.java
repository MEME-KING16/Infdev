package net.infdev.api.world.item;

import net.infdev.item.Items;

public class ItemStack {
    private Item item;
    private int count;
    
    public ItemStack() {
        item = Items.AIR;
        this.count = 0;
    }
    
    public ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
    }
    
    public Item getItem() {
        return this.item;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setItem(Item item) {
        this.item = item;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public boolean isEmpty() {
        return count <= 0 || item.getName().equals(Items.AIR.getName());
    }
}
