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

    /**
     * 将可堆叠且 id 相同的物品合并到靠前格子里（多轮扫描直到稳定）。
     *
     * @return 是否发生过合并
     */
    public static boolean autoStackInPlace(List<Item> bag) {
        if (bag == null || bag.isEmpty()) {
            return false;
        }
        boolean changed = false;
        boolean mergedThisPass;
        do {
            mergedThisPass = false;
            for (int i = 0; i < bag.size(); i++) {
                Item dst = bag.get(i);
                if (dst == null || !dst.canStack() || dst.getCount() >= dst.getMaxStack()) {
                    continue;
                }
                for (int j = i + 1; j < bag.size(); j++) {
                    Item src = bag.get(j);
                    if (src == null || !dst.getId().equals(src.getId())) {
                        continue;
                    }
                    int move = Math.min(dst.getMaxStack() - dst.getCount(), src.getCount());
                    if (move <= 0) {
                        continue;
                    }
                    dst.setCount(dst.getCount() + move);
                    if (move == src.getCount()) {
                        bag.set(j, null);
                    } else {
                        src.setCount(src.getCount() - move);
                    }
                    mergedThisPass = true;
                    changed = true;
                    if (dst.getCount() >= dst.getMaxStack()) {
                        break;
                    }
                }
            }
        } while (mergedThisPass);
        return changed;
    }

    /** 非空物品前移，尾部补空位 */
    public static void compactForward(List<Item> bag) {
        if (bag == null || bag.isEmpty()) {
            return;
        }
        List<Item> nonEmpty = new ArrayList<>();
        for (Item item : bag) {
            if (item != null) {
                nonEmpty.add(item);
            }
        }
        int write = 0;
        for (Item item : nonEmpty) {
            bag.set(write++, item);
        }
        while (write < bag.size()) {
            bag.set(write++, null);
        }
    }

    /** 先堆叠同类可堆叠物品，再向前紧凑 */
    public static void organizeBag(List<Item> bag) {
        autoStackInPlace(bag);
        compactForward(bag);
    }
}
