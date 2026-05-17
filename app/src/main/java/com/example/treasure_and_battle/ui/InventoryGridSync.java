package com.example.treasure_and_battle.ui;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.material.MaterialItem;

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

    /** 管理器为空时写入综合测试物品集：装备(8部位) + 材料(4种含堆叠) + 宝石(2种) + 消耗品(4种含堆叠) */
    public static void seedDemoIfEmpty(@Nullable Context context) {
        if (context == null) {
            return;
        }
        Character ch = PlayerCharacterHolder.getOrCreate(context);
        if (ch == null || !InventoryManager.isEmpty(ch.getBagItems())) {
            return;
        }
        List<Item> bag = ch.getBagItems();

        EquipmentManager em = EquipmentManager.getInstance(context);
        addEquipOrLog(bag, em.generateEquip(1001, 5, Rarity.LEGENDARY));
        addEquipOrLog(bag, em.generateEquip(2001, 5, Rarity.LEGENDARY));
        addEquipOrLog(bag, em.generateEquip(4001, 5, Rarity.LEGENDARY));
        addEquipOrLog(bag, em.generateEquip(5001, 5, Rarity.LEGENDARY));
        addEquipOrLog(bag, em.generateEquip(6001, 5, Rarity.LEGENDARY));
        addEquipOrLog(bag, em.generateEquip(7001, 5, Rarity.LEGENDARY));
        addEquipOrLog(bag, em.generateEquip(8001, 5, Rarity.RARE));
        addEquipOrLog(bag, em.generateEquip(10001, 5, Rarity.UNCOMMON));
        EquipItem ring = em.generateEquip(9001, 8, Rarity.EPIC);
        if (ring != null) InventoryManager.addItem(bag, ring);
        EquipItem ring2 = em.generateEquip(9002, 8, Rarity.LEGENDARY);
        if (ring2 != null) InventoryManager.addItem(bag, ring2);

        ItemManager im = ItemManager.getInstance(context);
        addWithCount(bag, im.createMaterial("slime_gel_common"), 50);
        addWithCount(bag, im.createMaterial("wolf_fang"), 40);
        addWithCount(bag, im.createMaterial("slime_crystal"), 30);
        addWithCount(bag, im.createMaterial("wolf_alpha_fang"), 1);

        addWithCount(bag, im.createGem("ruby_common"), 1);
        addWithCount(bag, im.createGem("emerald_common"), 1);

        addWithCount(bag, im.createConsumable("potion_hp_small"), 30);
        addWithCount(bag, im.createConsumable("potion_hp_small"), 20);
        addWithCount(bag, im.createConsumable("potion_mp_small"), 25);
        addWithCount(bag, im.createConsumable("potion_mp_small"), 20);
        addWithCount(bag, im.createConsumable("potion_hp_medium"), 15);
        addWithCount(bag, im.createConsumable("potion_hp_large"), 10);
        addWithCount(bag, im.createConsumable("diamond_drill"), 3);
    }

    private static void addEquipOrLog(List<Item> bag, EquipItem item) {
        if (item != null) {
            InventoryManager.addItem(bag, item);
        }
    }

    private static void addWithCount(List<Item> bag, Item item, int count) {
        if (item == null) return;
        item.setCount(Math.min(count, item.getMaxStack()));
        InventoryManager.addItem(bag, item);
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
