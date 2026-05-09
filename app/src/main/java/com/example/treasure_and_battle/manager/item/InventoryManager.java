package com.example.treasure_and_battle.manager.item;

import com.example.treasure_and_battle.model.item.Item;
import java.util.ArrayList;
import java.util.List;

public class InventoryManager {
    /** 与 UI 背包总格数一致：5 页 × 5×5，见 InventoryGridSync.BAG_SLOT_COUNT */
    public static final int BAG_GRID_SLOTS = 125;

    private static InventoryManager instance;
    private List<Item> items;
    /** 最多占用的非空槽位数 */
    private int maxCapacity = BAG_GRID_SLOTS;

    private InventoryManager() {
        items = new ArrayList<>(BAG_GRID_SLOTS);
        for (int i = 0; i < BAG_GRID_SLOTS; i++) {
            items.add(null);
        }
    }

    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    private int countOccupiedSlots() {
        int n = 0;
        for (Item item : items) {
            if (item != null) {
                n++;
            }
        }
        return n;
    }

    public boolean addItem(Item newItem) {
        if (newItem.canStack()) {
            for (Item item : items) {
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

        if (countOccupiedSlots() >= maxCapacity) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == null) {
                items.set(i, newItem);
                return true;
            }
        }
        return false;
    }

    public void removeItem(Item item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == item) {
                items.set(i, null);
                return;
            }
        }
    }

    /** 固定长度 {@link #BAG_GRID_SLOTS}，与背包格子一一对应，元素可为 null */
    public List<Item> getItems() {
        return items;
    }

    public boolean isCompletelyEmpty() {
        for (Item item : items) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    public int getOccupiedSlotCount() {
        return countOccupiedSlots();
    }

    /** 测试或重置用：所有槽位置空 */
    public void clearAllSlots() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, null);
        }
    }
}
