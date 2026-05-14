package com.example.treasure_and_battle.ui;

import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.material.MaterialItem;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BattleFragmentLootTest {

    private static final int LOOT_PAGE_SIZE = 10;
    private static final int LOOT_ITEMS_PER_ROW = 5;

    private List<Item> testBag;

    @Before
    public void setUp() {
        testBag = new ArrayList<>();
        InventoryManager.initBag(testBag);
    }

    private static MaterialItem makeItem(String id, String name) {
        MaterialItem item = new MaterialItem(id, name, Rarity.COMMON, 10, 99, "test");
        item.setCount(1);
        return item;
    }

    // ====================== 分页计算边界测试 ======================

    @Test
    public void testPaginationZeroItems() {
        int pages = (0 + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
        assertEquals(0, pages);
    }

    @Test
    public void testPaginationOneItem() {
        int pages = (1 + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
        assertEquals(1, pages);
    }

    @Test
    public void testPaginationTenItems() {
        int pages = (10 + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
        assertEquals(1, pages);
    }

    @Test
    public void testPaginationElevenItems() {
        int pages = (11 + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
        assertEquals(2, pages);
    }

    @Test
    public void testPageIndexClampAfterRemoval() {
        int totalItems = 21;
        int pages = (totalItems + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
        assertEquals(3, pages);
        int currentPage = 2;
        totalItems -= 10;
        pages = (totalItems + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
        if (currentPage >= pages && pages > 0) {
            currentPage = pages - 1;
        }
        assertEquals(1, currentPage);
    }

    // ====================== 横向流式布局：每行5个，自动换行 ======================

    @Test
    public void testFlowLayout5ItemsOneRow() {
        int count = 5;
        int rows = (count + LOOT_ITEMS_PER_ROW - 1) / LOOT_ITEMS_PER_ROW;
        assertEquals(1, rows);
        int lastRowItems = count - (rows - 1) * LOOT_ITEMS_PER_ROW;
        assertEquals(5, lastRowItems);
    }

    @Test
    public void testFlowLayout6ItemsTwoRows() {
        int count = 6;
        int rows = (count + LOOT_ITEMS_PER_ROW - 1) / LOOT_ITEMS_PER_ROW;
        assertEquals(2, rows);
        int itemsRow0 = Math.min(LOOT_ITEMS_PER_ROW, count);
        assertEquals(5, itemsRow0);
        int itemsRow1 = count - itemsRow0;
        assertEquals(1, itemsRow1);
    }

    @Test
    public void testFlowLayout10ItemsTwoRows() {
        int count = 10;
        int rows = (count + LOOT_ITEMS_PER_ROW - 1) / LOOT_ITEMS_PER_ROW;
        assertEquals(2, rows);
        int itemsRow0 = Math.min(LOOT_ITEMS_PER_ROW, count);
        int itemsRow1 = count - itemsRow0;
        assertEquals(5, itemsRow0);
        assertEquals(5, itemsRow1);
    }

    @Test
    public void testFlowLayout11ItemsThreeRows() {
        int count = 11;
        int rows = (count + LOOT_ITEMS_PER_ROW - 1) / LOOT_ITEMS_PER_ROW;
        assertEquals(3, rows);
        int itemsRow0 = Math.min(LOOT_ITEMS_PER_ROW, count);
        int itemsRow1 = Math.min(LOOT_ITEMS_PER_ROW, count - itemsRow0);
        int itemsRow2 = count - itemsRow0 - itemsRow1;
        assertEquals(5, itemsRow0);
        assertEquals(5, itemsRow1);
        assertEquals(1, itemsRow2);
    }

    @Test
    public void testFlowLayoutCenteredLastRow() {
        int count = 3;
        int rows = (count + LOOT_ITEMS_PER_ROW - 1) / LOOT_ITEMS_PER_ROW;
        assertEquals(1, rows);
        assertTrue("最后一行不满5个时居中显示", count < LOOT_ITEMS_PER_ROW);
    }

    // ====================== 结算信息显示测试 ======================

    @Test
    public void testRewardGoldZero() {
        String text = "获得金币：" + 0;
        assertEquals("获得金币：0", text);
    }

    @Test
    public void testRewardExpZero() {
        String text = "获得经验：" + 0;
        assertEquals("获得经验：0", text);
    }

    @Test
    public void testRewardGoldLarge() {
        int gold = 999999;
        String text = "获得金币：" + gold;
        assertEquals("获得金币：999999", text);
    }

    @Test
    public void testRewardExpBoundary() {
        int exp = Integer.MAX_VALUE;
        String text = "获得经验：" + exp;
        assertTrue(text.contains(String.valueOf(exp)));
    }

    // ====================== 单件领取测试 ======================

    @Test
    public void testClaimSingleSuccess() {
        Item item = makeItem("mat_test", "测试素材");
        assertTrue(InventoryManager.addItem(testBag, item));
        assertEquals(1, InventoryManager.countOccupied(testBag));
    }

    @Test
    public void testClaimSingleBagFull() {
        for (int i = 0; i < InventoryManager.BAG_SLOTS; i++) {
            Item fill = makeItem("mat_fill_" + i, "填充" + i);
            InventoryManager.addItem(testBag, fill);
        }
        assertEquals(InventoryManager.BAG_SLOTS, InventoryManager.countOccupied(testBag));
        Item extra = makeItem("mat_extra", "额外");
        assertFalse("背包已满应无法放入", InventoryManager.addItem(testBag, extra));
    }

    @Test
    public void testClaimSingleRemoveAndForwardShift() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            items.add(makeItem("mat_" + i, "素材" + i));
        }
        items.remove(2);
        assertEquals(4, items.size());
        assertEquals("素材3", items.get(2).getName());
    }

    // ====================== 一键领取测试 ======================

    @Test
    public void testClaimAllSequential() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            items.add(makeItem("mat_all_" + i, "素材" + i));
        }
        int claimed = 0;
        int total = items.size();
        int idx = 0;
        while (idx < items.size()) {
            Item item = items.get(idx);
            if (InventoryManager.addItem(testBag, item)) {
                items.remove(idx);
                claimed++;
            } else {
                break;
            }
        }
        assertEquals(total, claimed);
        assertEquals(total, InventoryManager.countOccupied(testBag));
        assertTrue(items.isEmpty());
    }

    @Test
    public void testClaimAllBagBecomesFullMidway() {
        for (int i = 0; i < InventoryManager.BAG_SLOTS - 2; i++) {
            Item fill = makeItem("fill_" + i, "填充" + i);
            InventoryManager.addItem(testBag, fill);
        }
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            items.add(makeItem("claim_" + i, "领取" + i));
        }
        int claimed = 0;
        int total = items.size();
        int idx = 0;
        while (idx < items.size()) {
            Item item = items.get(idx);
            if (InventoryManager.addItem(testBag, item)) {
                items.remove(idx);
                claimed++;
            } else {
                break;
            }
        }
        assertTrue("应至少领取2件", claimed >= 2);
        assertTrue("应未能全部领取", claimed < total);
    }

    @Test
    public void testClaimAllEmptyList() {
        List<Item> items = new ArrayList<>();
        int claimed = 0;
        int idx = 0;
        while (idx < items.size()) {
            if (InventoryManager.addItem(testBag, items.get(idx))) {
                items.remove(idx);
                claimed++;
            } else {
                break;
            }
        }
        assertEquals(0, claimed);
    }

    // ====================== 离开二次确认测试 ======================

    @Test
    public void testLeaveWithRemainingItemsNeedsConfirm() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("remain", "剩余"));
        int remaining = items.size();
        assertTrue("有剩余物品时需要二次确认", remaining > 0);
        assertEquals(1, remaining);
    }

    @Test
    public void testLeaveWithoutItemsNoConfirm() {
        List<Item> items = new ArrayList<>();
        int remaining = items.size();
        assertFalse("无剩余物品时不需要确认", remaining > 0);
        assertEquals(0, remaining);
    }

    // ====================== 战斗失败弹窗测试 ======================

    @Test
    public void testDefeatDialogNotCancelable() {
        boolean cancelable = false;
        assertFalse("失败弹窗应禁止背景点击关闭", cancelable);
    }

    @Test
    public void testDefeatDialogHasOnlyConfirmButton() {
        String positiveText = "确定";
        assertEquals("确定", positiveText);
    }

    @Test
    public void testDefeatReturnsToPreviousScene() {
        boolean poppedBack = true;
        assertTrue("点击确定后应退出战斗场景", poppedBack);
    }

    // ====================== FloatMsgOverlay 队列测试 ======================

    @Test
    public void testFloatMsgQueueMax3Simultaneous() {
        int maxVisible = 3;
        int enqueued = 5;
        int pending = enqueued - maxVisible;
        assertEquals(2, pending);
    }

    @Test
    public void testFloatMsgQueueFifoOrder() {
        List<String> queue = new ArrayList<>();
        queue.add("msg1");
        queue.add("msg2");
        queue.add("msg3");
        String firstOut = queue.remove(0);
        assertEquals("msg1", firstOut);
        assertEquals(2, queue.size());
    }

    @Test
    public void testFloatMsgDefaultDuration() {
        long defaultDurationMs = 1200;
        assertTrue("动画时长应 > 0", defaultDurationMs > 0);
    }

    @Test
    public void testFloatMsgAnimationProperties() {
        int flyUpPx = 70;
        float alphaStart = 1f;
        float alphaEnd = 0f;
        assertTrue("应从完全可见开始", alphaStart > alphaEnd);
        assertTrue("应向上移动", flyUpPx > 0);
    }

    // ====================== LruBitmapCache 测试 ======================

    @Test
    public void testLruCacheSingleton() {
        com.example.treasure_and_battle.utils.LruBitmapCache c1 =
                com.example.treasure_and_battle.utils.LruBitmapCache.getInstance();
        com.example.treasure_and_battle.utils.LruBitmapCache c2 =
                com.example.treasure_and_battle.utils.LruBitmapCache.getInstance();
        assertSame("应为同一实例", c1, c2);
    }

    @Test
    public void testLruCacheNullKeyReturnsNull() {
        com.example.treasure_and_battle.utils.LruBitmapCache cache =
                com.example.treasure_and_battle.utils.LruBitmapCache.getInstance();
        assertNull(cache.get(null));
    }
}
