package net.infdev.inventory;

import net.infdev.api.world.item.Item;
import net.infdev.api.world.item.ItemStack;

public class Inventory {
    private static final int HOTBAR_SIZE = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final int INVENTORY_SIZE = HOTBAR_SIZE * INVENTORY_ROWS;
    
    private final ItemStack[] hotbar = new ItemStack[HOTBAR_SIZE];
    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    private int selectedSlot = 0;
    
    public Inventory() {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbar[i] = new ItemStack();
        }
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory[i] = new ItemStack();
        }
    }
    
    public ItemStack getSelectedItem() {
        return hotbar[selectedSlot];
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            selectedSlot = slot;
        }
    }
    
    public ItemStack getHotbarSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            return hotbar[slot];
        }
        return null;
    }
    
    public ItemStack getInventorySlot(int slot) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            return inventory[slot];
        }
        return null;
    }
    
    public void setHotbarSlot(int slot, ItemStack item) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            hotbar[slot] = item;
        }
    }
    
    public void setInventorySlot(int slot, ItemStack item) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            inventory[slot] = item;
        }
    }
    
    public boolean addItem(Item item, int count) {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (hotbar[i].getItem().equals(item) && hotbar[i].getCount() < 64) {
                int space = 64 - hotbar[i].getCount();
                int toAdd = Math.min(space, count);
                hotbar[i].setCount(hotbar[i].getCount() + toAdd);
                count -= toAdd;
                if (count == 0) return true;
            }
        }
        
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (inventory[i].getItem().equals(item) && inventory[i].getCount() < 64) {
                int space = 64 - inventory[i].getCount();
                int toAdd = Math.min(space, count);
                inventory[i].setCount(inventory[i].getCount() + toAdd);
                count -= toAdd;
                if (count == 0) return true;
            }
        }
        
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (hotbar[i].isEmpty()) {
                hotbar[i] = new ItemStack(item, Math.min(count, 64));
                count -= Math.min(count, 64);
                if (count == 0) return true;
            }
        }
        
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (inventory[i].isEmpty()) {
                inventory[i] = new ItemStack(item, Math.min(count, 64));
                count -= Math.min(count, 64);
                if (count == 0) return true;
            }
        }
        
        return count == 0;
    }
    
    public boolean removeSelectedItem() {
        ItemStack selected = hotbar[selectedSlot];
        if (!selected.isEmpty()) {
            selected.setCount(selected.getCount() - 1);
            if (selected.getCount() <= 0) {
                hotbar[selectedSlot] = new ItemStack();
            }
            return true;
        }
        return false;
    }

    public boolean isValidHotbarSlot(int slot) {
        return slot >= 0 && slot < HOTBAR_SIZE;
    }

    public boolean isValidInventorySlot(int slot) {
        return slot >= 0 && slot < INVENTORY_SIZE;
    }

    public void swapSlots(int fromSlot, int toSlot, boolean isFromHotbar, boolean isToHotbar) {
        ItemStack fromStack = isFromHotbar ? getHotbarSlot(fromSlot) : getInventorySlot(fromSlot);
        ItemStack toStack = isToHotbar ? getHotbarSlot(toSlot) : getInventorySlot(toSlot);

        if (fromStack == null || toStack == null) return;

        ItemStack temp = new ItemStack(fromStack.getItem(), fromStack.getCount());

        if (isFromHotbar) {
            setHotbarSlot(fromSlot, toStack);
        } else {
            setInventorySlot(fromSlot, toStack);
        }

        if (isToHotbar) {
            setHotbarSlot(toSlot, temp);
        } else {
            setInventorySlot(toSlot, temp);
        }
    }

    public boolean mergeStacks(int fromSlot, int toSlot, boolean isFromHotbar, boolean isToHotbar) {
        ItemStack fromStack = isFromHotbar ? getHotbarSlot(fromSlot) : getInventorySlot(fromSlot);
        ItemStack toStack = isToHotbar ? getHotbarSlot(toSlot) : getInventorySlot(toSlot);

        if (fromStack == null || toStack == null) return false;
        if (fromStack.isEmpty() || toStack.isEmpty()) return false;
        if (!fromStack.getItem().equals(toStack.getItem())) return false;
        if (toStack.getCount() >= 64) return false;

        int space = 64 - toStack.getCount();
        int toTransfer = Math.min(space, fromStack.getCount());

        toStack.setCount(toStack.getCount() + toTransfer);
        fromStack.setCount(fromStack.getCount() - toTransfer);

        if (fromStack.getCount() <= 0) {
            if (isFromHotbar) {
                setHotbarSlot(fromSlot, new ItemStack());
            } else {
                setInventorySlot(fromSlot, new ItemStack());
            }
        }

        return true;
    }

    public ItemStack splitStack(int slot, boolean isHotbar) {
        ItemStack stack = isHotbar ? getHotbarSlot(slot) : getInventorySlot(slot);

        if (stack == null || stack.isEmpty() || stack.getCount() <= 1) return null;

        int halfCount = stack.getCount() / 2;
        int remainingCount = stack.getCount() - halfCount;

        stack.setCount(remainingCount);

        return new ItemStack(stack.getItem(), halfCount);
    }

    public void clearAll() {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbar[i] = new ItemStack();
        }
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory[i] = new ItemStack();
        }
        selectedSlot = 0;
    }

    public ItemStack[] copyHotbar() {
        ItemStack[] copy = new ItemStack[HOTBAR_SIZE];
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack src = hotbar[i];
            copy[i] = new ItemStack(src.getItem(), src.getCount());
        }
        return copy;
    }

    public ItemStack[] copyInventory() {
        ItemStack[] copy = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack src = inventory[i];
            copy[i] = new ItemStack(src.getItem(), src.getCount());
        }
        return copy;
    }

    public void loadState(ItemStack[] hotbarState, ItemStack[] inventoryState) {
        clearAll();
        if (hotbarState != null) {
            for (int i = 0; i < Math.min(hotbarState.length, HOTBAR_SIZE); i++) {
                ItemStack src = hotbarState[i];
                if (src != null) {
                    hotbar[i] = new ItemStack(src.getItem(), src.getCount());
                }
            }
        }
        if (inventoryState != null) {
            for (int i = 0; i < Math.min(inventoryState.length, INVENTORY_SIZE); i++) {
                ItemStack src = inventoryState[i];
                if (src != null) {
                    inventory[i] = new ItemStack(src.getItem(), src.getCount());
                }
            }
        }
    }
}
