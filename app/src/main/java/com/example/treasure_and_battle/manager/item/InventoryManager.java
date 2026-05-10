package com.example.treasure_and_battle.manager.item;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import java.util.Iterator;

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

    public static synchronized void releaseInstance() {
        instance = null;
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

    public List<Item> getItems() {
        return items;
    }

    public List<ConsumableItem> getBattleUsableConsumables() {
        List<ConsumableItem> result = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof ConsumableItem) {
                ConsumableItem c = (ConsumableItem) item;
                if (c.isUsableInBattle() && c.getCount() > 0) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    public boolean consumeOne(String consumableId) {
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext()) {
            Item item = iter.next();
            if (item.getId().equals(consumableId) && item instanceof ConsumableItem) {
                if (item.getCount() <= 1) {
                    iter.remove();
                } else {
                    item.setCount(item.getCount() - 1);
                }
                return true;
            }
        }
        return false;
    }
}
