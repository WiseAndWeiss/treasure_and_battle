package com.example.treasure_and_battle.ui;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * 背包网格与 {@link InventoryManager#getItems()} 同步。
 * 固定 {@link #BAG_SLOT_COUNT} 格（5 页 × 5×5），与管理器槽位下标一一对应，保留空格不压缩。
 */
public final class InventoryGridSync {

    public static final int BAG_TOTAL_PAGES = 5;
    public static final int BAG_ITEMS_PER_PAGE = 25;
    public static final int BAG_SLOT_COUNT = BAG_TOTAL_PAGES * BAG_ITEMS_PER_PAGE;

    private static List<Item> sharedBagGrid;

    private InventoryGridSync() {}

    /**
     * 获取与 {@link BagFragment} / {@link TradeBagBottomController} 共用的网格列表。
     */
    public static synchronized List<Item> getSharedBagGrid(@Nullable Context context) {
        if (sharedBagGrid == null) {
            seedDemoIfEmpty(context);
            sharedBagGrid = newGridFilledFromManager();
        }
        return sharedBagGrid;
    }

    private static List<Item> newGridFilledFromManager() {
        List<Item> grid = new ArrayList<>(BAG_SLOT_COUNT);
        for (int i = 0; i < BAG_SLOT_COUNT; i++) {
            grid.add(null);
        }
        List<Item> inv = InventoryManager.getInstance().getItems();
        int n = Math.min(BAG_SLOT_COUNT, inv.size());
        for (int i = 0; i < n; i++) {
            grid.set(i, inv.get(i));
        }
        return grid;
    }

    /** 管理器为空时写入与原先 UI 一致的演示装备（通过 {@link InventoryManager#addItem}） */
    public static void seedDemoIfEmpty(@Nullable Context context) {
        if (context == null) {
            return;
        }
        InventoryManager im = InventoryManager.getInstance();
        if (!im.isCompletelyEmpty()) {
            return;
        }
        EquipmentManager em = EquipmentManager.getInstance(context);
        im.addItem(em.generateEquip(3001, 1, Rarity.COMMON));
        im.addItem(em.generateEquip(3003, 10, Rarity.LEGENDARY));
    }

    /** 将网格槽位按索引写回管理器（保留空格） */
    public static void flushSharedGridToManager() {
        if (sharedBagGrid == null) {
            return;
        }
        List<Item> inv = InventoryManager.getInstance().getItems();
        int cap = Math.min(sharedBagGrid.size(), inv.size());
        for (int i = 0; i < cap; i++) {
            inv.set(i, sharedBagGrid.get(i));
        }
    }

    /** 用管理器槽位覆盖网格（不替换网格 List 实例） */
    public static void reloadSharedGridFromManager() {
        if (sharedBagGrid == null) {
            return;
        }
        List<Item> inv = InventoryManager.getInstance().getItems();
        for (int i = 0; i < sharedBagGrid.size(); i++) {
            sharedBagGrid.set(i, i < inv.size() ? inv.get(i) : null);
        }
    }
}
