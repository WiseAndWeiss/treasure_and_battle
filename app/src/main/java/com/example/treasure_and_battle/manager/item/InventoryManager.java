package com.example.treasure_and_battle.manager.item;

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
        // 如果是可堆叠物品，先尝试堆叠
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
        
        // 装入新格子
        if (items.size() < maxCapacity) {
            items.add(newItem);
            return true;
        }
        return false; // 背包满了
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public List<Item> getItems() {
        return items;
    }
}
