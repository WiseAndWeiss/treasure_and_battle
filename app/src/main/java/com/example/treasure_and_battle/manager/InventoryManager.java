package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.model.item.Item;
import java.util.ArrayList;
import java.util.List;

public class InventoryManager {
    private static InventoryManager instance;
    private List<Item> items;
    private int maxCapacity = 50;

    private InventoryManager() {
        items = new ArrayList<>();
    }

    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    public boolean addItem(Item newItem) {
        if (newItem.canStack()) {
            for (Item item : items) {
                if (item.getId().equals(newItem.getId()) && item.getCount() < item.getMaxStack()) {
                    int availableSpace = item.getMaxStack() - item.getCount();
                    if (newItem.getCount() <= availableSpace) {
                        item.setCount(item.getCount() + newItem.getCount());
                        return true;
                    } else {
                        item.setCount(item.getMaxStack());
                        newItem.setCount(newItem.getCount() - availableSpace);
                    }
                }
            }
        }

        if (items.size() < maxCapacity) {
            items.add(newItem);
            return true;
        }
        return false;
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public void removeByIndex(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public List<Item> getItems() {
        return items;
    }

    public Item getItemByIndex(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    public List<EquipItem> getEquipItems() {
        List<EquipItem> result = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof EquipItem) {
                result.add((EquipItem) item);
            }
        }
        return result;
    }

    public int getItemIndex(Item item) {
        return items.indexOf(item);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
