package com.example.treasure_and_battle.manager.item;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {

    public static final int BAG_SLOTS = 125;

    private InventoryManager() {}

    public static void initBag(List<Item> bag) {
        bag.clear();
        for (int i = 0; i < BAG_SLOTS; i++) {
            bag.add(null);
        }
    }

    public static boolean addItem(List<Item> bag, Item newItem) {
        if (newItem == null) {
            return false;
        }

        if (newItem.canStack()) {
            for (Item item : bag) {
                if (item == null) {
                    continue;
                }
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

        for (int i = 0; i < bag.size(); i++) {
            if (bag.get(i) == null) {
                bag.set(i, newItem);
                return true;
            }
        }
        return false;
    }

    public static void removeItem(List<Item> bag, Item item) {
        if (item == null) {
            return;
        }
        for (int i = 0; i < bag.size(); i++) {
            if (bag.get(i) == item) {
                bag.set(i, null);
                return;
            }
        }
    }

    public static List<ConsumableItem> getBattleUsableConsumables(List<Item> bag) {
        List<ConsumableItem> result = new ArrayList<>();
        for (Item item : bag) {
            if (item instanceof ConsumableItem) {
                ConsumableItem c = (ConsumableItem) item;
                if (c.isUsableInBattle() && c.getCount() > 0) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    public static boolean consumeOne(List<Item> bag, String consumableId) {
        if (consumableId == null || consumableId.isEmpty()) {
            return false;
        }
        for (int i = 0; i < bag.size(); i++) {
            Item item = bag.get(i);
            if (item != null
                && item instanceof ConsumableItem
                && consumableId.equals(item.getId())) {

                if (item.getCount() <= 1) {
                    bag.set(i, null);
                } else {
                    item.setCount(item.getCount() - 1);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(List<Item> bag) {
        for (Item item : bag) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    public static int countOccupied(List<Item> bag) {
        int n = 0;
        for (Item item : bag) {
            if (item != null) {
                n++;
            }
        }
        return n;
    }

    public static void clearAll(List<Item> bag) {
        for (int i = 0; i < bag.size(); i++) {
            bag.set(i, null);
        }
    }
}
