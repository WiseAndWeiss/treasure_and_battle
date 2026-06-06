package com.example.treasure_and_battle.manager.item;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.item.gem.GemItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // ====================== 背包物品查询/操作 ======================

    /**
     * 在背包中按物品 ID 查找首个匹配的索引。
     *
     * @return 匹配索引，未找到返回 -1
     */
    public static int findItemIndex(List<Item> bag, String itemId) {
        if (itemId == null) return -1;
        for (int i = 0; i < bag.size(); i++) {
            Item existing = bag.get(i);
            if (existing != null && itemId.equals(existing.getId())) {
                return i;
            }
        }
        return -1;
    }

    /** 找到第一个空格子索引 */
    public static int findEmptySlot(List<Item> bag) {
        for (int i = 0; i < bag.size(); i++) {
            if (bag.get(i) == null) return i;
        }
        return -1;
    }

    /** 将物品放入第一个空格（不堆叠），返回是否成功 */
    public static boolean putItemIntoEmptySlot(List<Item> bag, Item item) {
        if (item == null) return false;
        int idx = findEmptySlot(bag);
        if (idx >= 0) {
            bag.set(idx, item);
            return true;
        }
        return false;
    }

    /** 丢弃指定索引的物品（置 null） */
    public static void discardAt(List<Item> bag, int index) {
        if (index >= 0 && index < bag.size()) {
            bag.set(index, null);
        }
    }

    /**
     * 按装备槽位筛选并前移匹配物品（原地重排）。
     * 匹配指定槽位的 EquipItem 移到前面，其余物品后置。
     */
    public static void compactByEquipSlot(List<Item> bag, EquipSlot slot) {
        if (bag == null || slot == null) return;
        List<Item> matches = new ArrayList<>();
        List<Item> others = new ArrayList<>();
        for (Item item : bag) {
            if (item instanceof EquipItem && ((EquipItem) item).getSlot() == slot) {
                matches.add(item);
            } else {
                others.add(item);
            }
        }
        int writeIndex = 0;
        for (Item item : matches) {
            bag.set(writeIndex++, item);
        }
        for (Item item : others) {
            bag.set(writeIndex++, item);
        }
    }

    // ====================== 消耗品 / 特定物品查询与消耗 ======================

    /** 检查 Character 背包中是否存在指定 ID 的物品 */
    public static boolean hasItem(Character ch, String itemId) {
        if (ch == null || itemId == null) return false;
        List<Item> bag = ch.getBagItems();
        for (Item it : bag) {
            if (it != null && itemId.equals(it.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 Character 背包中消耗 1 个指定 ID 的物品。
     * 可堆叠物品优先减数量，否则移除整格。
     *
     * @return 是否成功消耗
     */
    public static boolean consumeItem(Character ch, String itemId) {
        if (ch == null || itemId == null) return false;
        List<Item> bag = ch.getBagItems();
        for (int i = 0; i < bag.size(); i++) {
            Item it = bag.get(i);
            if (it != null && itemId.equals(it.getId())) {
                if (it.getCount() > 1) {
                    it.setCount(it.getCount() - 1);
                } else {
                    bag.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    /** 检查角色背包中是否有金刚钻 */
    public static boolean hasDiamondDrill(Character ch) {
        return hasItem(ch, "diamond_drill");
    }

    /** 从角色背包消耗 1 个金刚钻 */
    public static boolean consumeDiamondDrill(Character ch) {
        return consumeItem(ch, "diamond_drill");
    }

    /**
     * 使用消耗品（委托 {@link ConsumableManager#executeOutBattle}），
     * 使用成功后自动从背包扣除对应数量。
     *
     * @param ch    角色
     * @param bag   当前背包网格列表
     * @param item  消耗品实例
     * @return 是否使用成功
     */
    public static boolean useConsumable(Character ch, List<Item> bag, ConsumableItem item, Context context) {
        boolean success = ConsumableManager.executeOutBattle(ch, item, context);
        if (!success) return false;
        int index = findItemIndex(bag, item.getId());
        if (index < 0) return false;
        Item bagItem = bag.get(index);
        if (bagItem != null && bagItem.getCount() > 1) {
            bagItem.setCount(bagItem.getCount() - 1);
        } else {
            bag.set(index, null);
        }
        return true;
    }

    // ====================== 装备管理 ======================

    /**
     * 判断装备类型与目标槽位是否兼容。
     */
    public static boolean canEquipToSlot(EquipSlot itemSlot, EquipSlot targetSlot) {
        if (itemSlot == null || targetSlot == null) return false;
        return itemSlot == targetSlot;
    }

    /**
     * 将装备从背包装备到角色指定槽位，返回被换下的旧装备（可能为 null）。
     *
     * @param ch        角色
     * @param bag       背包网格列表
     * @param bagIndex  背包格子索引
     * @param slot      目标装备槽位
     * @param isLeftRing 仅当 slot==RING 时用于区分左右（true=左戒）
     * @return 被换下的旧装备
     */
    @Nullable
    public static EquipItem equipToCharacter(Character ch, List<Item> bag, int bagIndex,
                                             EquipSlot slot, boolean isLeftRing) {
        if (ch == null || bag == null || bagIndex < 0 || bagIndex >= bag.size()) return null;
        Item bagItem = bag.get(bagIndex);
        if (!(bagItem instanceof EquipItem)) return null;

        EquipItem newEquip = (EquipItem) bagItem;
        EquipItem oldEquip = null;

        if (slot == EquipSlot.RING) {
            oldEquip = isLeftRing ? ch.getLeftRing() : ch.getRightRing();
            ch.equipRing(newEquip, isLeftRing);
        } else {
            // 找到当前槽位的旧装备
            for (EquipItem eq : ch.getEquippedItems()) {
                if (eq != null && eq.getSlot() == slot) {
                    oldEquip = eq;
                    break;
                }
            }
            ch.equip(newEquip);
        }

        bag.set(bagIndex, oldEquip != null ? oldEquip : null);
        return oldEquip;
    }

    /**
     * 从角色卸下装备到背包。
     *
     * @return 是否成功卸下并放入背包
     */
    public static boolean unequipFromCharacter(Character ch, List<Item> bag,
                                                EquipSlot slot, boolean isLeftRing) {
        if (ch == null || bag == null) return false;
        EquipItem removed;
        if (slot == EquipSlot.RING) {
            removed = isLeftRing ? ch.getLeftRing() : ch.getRightRing();
            if (removed == null) return false;
            ch.unequipRing(isLeftRing);
        } else {
            removed = null;
            for (EquipItem eq : ch.getEquippedItems()) {
                if (eq != null && eq.getSlot() == slot) {
                    removed = eq;
                    break;
                }
            }
            if (removed == null) return false;
            ch.unequip(slot);
        }
        return putItemIntoEmptySlot(bag, removed);
    }

    /**
     * 自动选择合适槽位装备（含双戒指优先级策略）。
     * 优先使用空槽位；戒指优先左戒后右戒。
     *
     * @return 是否成功装备
     */
    public static boolean autoEquipToCharacter(Character ch, List<Item> bag, int bagIndex,
                                                EquipItem equip) {
        if (ch == null || bag == null || equip == null) return false;
        EquipSlot slot = equip.getSlot();
        if (slot == null) return false;

        if (slot == EquipSlot.RING) {
            boolean leftEmpty = ch.getLeftRing() == null;
            boolean rightEmpty = ch.getRightRing() == null;
            if (leftEmpty) {
                return equipToCharacter(ch, bag, bagIndex, EquipSlot.RING, true) != null || bag.get(bagIndex) == null;
            } else if (rightEmpty) {
                return equipToCharacter(ch, bag, bagIndex, EquipSlot.RING, false) != null || bag.get(bagIndex) == null;
            } else {
                // 双戒指都已满，默认顶替左戒
                return equipToCharacter(ch, bag, bagIndex, EquipSlot.RING, true) != null || bag.get(bagIndex) == null;
            }
        } else {
            return equipToCharacter(ch, bag, bagIndex, slot, false) != null || bag.get(bagIndex) == null;
        }
    }

    /**
     * 从角色读取已装备物品，返回 slot→EquipItem 的映射。
     * Key 使用 EquipSlot.name() 作为标识；戒指额外使用 "RING_LEFT"/"RING_RIGHT"。
     */
    public static Map<String, EquipItem> getEquippedItemsMap(Character ch) {
        Map<String, EquipItem> map = new LinkedHashMap<>();
        if (ch == null) return map;
        for (EquipItem item : ch.getEquippedItems()) {
            if (item == null) continue;
            if (item.getSlot() == EquipSlot.RING) continue;
            map.put(item.getSlot().name(), item);
        }
        EquipItem leftRing = ch.getLeftRing();
        if (leftRing != null) map.put("RING_LEFT", leftRing);
        EquipItem rightRing = ch.getRightRing();
        if (rightRing != null) map.put("RING_RIGHT", rightRing);
        return map;
    }

    // ====================== 宝石管理 ======================

    /**
     * 将宝石镶嵌到装备上，自动扣除背包并同步角色装备状态。
     *
     * @param ch              角色（用于同步装备状态）
     * @param bag             背包网格列表
     * @param gemBagIndex     宝石在背包中的索引
     * @param gem             宝石实例
     * @param equip           目标装备
     * @param equipSlotKey    装备所在槽位标识（EquipSlot.name() 或 "RING_LEFT"/"RING_RIGHT"）
     * @return 是否镶嵌成功
     */
    public static boolean socketGemToEquip(Character ch, List<Item> bag, int gemBagIndex,
                                            GemItem gem, EquipItem equip, String equipSlotKey) {
        if (!equip.socketGem(gem)) return false;

        // 扣除宝石
        Item bagItem = bag.get(gemBagIndex);
        if (bagItem != null && bagItem.getCount() > 1) {
            bagItem.setCount(bagItem.getCount() - 1);
        } else {
            bag.set(gemBagIndex, null);
        }

        // 同步角色装备状态
        syncEquippedToCharacter(ch, equip, equipSlotKey);
        return true;
    }

    /**
     * 从装备拆卸指定位置的宝石，自动消耗金刚钻并放入背包。
     *
     * @param ch        角色
     * @param bag       背包网格列表
     * @param equip     目标装备
     * @param gemIndex  宝石在装备中的索引
     * @param equipSlotKey 装备所在槽位标识
     * @return 拆卸出的宝石（失败返回 null）
     */
    @Nullable
    public static GemItem unsocketGemFromEquip(Character ch, List<Item> bag, EquipItem equip,
                                                int gemIndex, String equipSlotKey) {
        GemItem removed = equip.unsocketGem(gemIndex);
        if (removed == null) return null;

        consumeDiamondDrill(ch);
        putItemIntoEmptySlot(bag, removed);

        // 同步角色装备状态
        syncEquippedToCharacter(ch, equip, equipSlotKey);
        return removed;
    }

    /**
     * 将装备状态同步回角色。
     */
    private static void syncEquippedToCharacter(Character ch, EquipItem equip, String equipSlotKey) {
        if (ch == null || equip == null || equipSlotKey == null) return;
        if ("RING_LEFT".equals(equipSlotKey)) {
            ch.equipRing(equip, true);
        } else if ("RING_RIGHT".equals(equipSlotKey)) {
            ch.equipRing(equip, false);
        } else {
            ch.equip(equip);
        }
    }
}
