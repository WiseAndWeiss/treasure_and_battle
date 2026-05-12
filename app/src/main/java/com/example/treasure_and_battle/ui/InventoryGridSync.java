package com.example.treasure_and_battle.ui;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * 背包网格与 {@link Character#getBagItems()} 同步。
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
            sharedBagGrid = newGridFilledFromCharacter(context);
        }
        return sharedBagGrid;
    }

    private static List<Item> newGridFilledFromCharacter(@Nullable Context context) {
        List<Item> grid = new ArrayList<>(BAG_SLOT_COUNT);
        Character ch = context != null ? PlayerCharacterHolder.getOrCreate(context) : null;
        List<Item> bag = ch != null ? ch.getBagItems() : null;
        for (int i = 0; i < BAG_SLOT_COUNT; i++) {
            grid.add(bag != null && i < bag.size() ? bag.get(i) : null);
        }
        return grid;
    }

    /** 管理器为空时写入与原先 UI 一致的演示装备 */
    public static void seedDemoIfEmpty(@Nullable Context context) {
        if (context == null) {
            return;
        }
        Character ch = PlayerCharacterHolder.getOrCreate(context);
        if (ch == null || !InventoryManager.isEmpty(ch.getBagItems())) {
            return;
        }
        EquipmentManager em = EquipmentManager.getInstance(context);
        InventoryManager.addItem(ch.getBagItems(), em.generateEquip(1001, 1, Rarity.COMMON));
        InventoryManager.addItem(ch.getBagItems(), em.generateEquip(3001, 10, Rarity.LEGENDARY));
    }

    /** 将网格槽位按索引写回 Character 背包（保留空格） */
    public static void flushSharedGridToManager(@Nullable Context context) {
        if (sharedBagGrid == null || context == null) {
            return;
        }
        Character ch = PlayerCharacterHolder.getOrCreate(context);
        if (ch == null) return;
        List<Item> bag = ch.getBagItems();
        int cap = Math.min(sharedBagGrid.size(), bag.size());
        for (int i = 0; i < cap; i++) {
            bag.set(i, sharedBagGrid.get(i));
        }
    }

    /** 用 Character 背包覆盖网格（不替换网格 List 实例） */
    public static void reloadSharedGridFromManager(@Nullable Context context) {
        if (sharedBagGrid == null || context == null) {
            return;
        }
        Character ch = PlayerCharacterHolder.getOrCreate(context);
        if (ch == null) return;
        List<Item> bag = ch.getBagItems();
        for (int i = 0; i < sharedBagGrid.size(); i++) {
            sharedBagGrid.set(i, i < bag.size() ? bag.get(i) : null);
        }
    }
}
