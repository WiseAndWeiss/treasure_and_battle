package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.material.MaterialItem;
import com.example.treasure_and_battle.model.item.Item;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryManagerTest {

    private List<Item> bag;

    @Before
    public void setUp() {
        bag = new ArrayList<>();
        InventoryManager.initBag(bag);
    }

    @Test
    public void testGetItemsInitiallyEmpty() {
        assertNotNull(bag);
        assertEquals(InventoryManager.BAG_SLOTS, bag.size());
        assertTrue(InventoryManager.isEmpty(bag));
        assertEquals(0, InventoryManager.countOccupied(bag));
    }

    @Test
    public void testAddItemSuccess() {
        MaterialItem item = createMaterial("mat_1", 1);
        assertTrue(InventoryManager.addItem(bag, item));
        assertEquals(1, InventoryManager.countOccupied(bag));
    }

    @Test
    public void testAddItemStack() {
        MaterialItem item1 = createMaterial("mat_stack", 5);
        MaterialItem item2 = createMaterial("mat_stack", 3);

        assertTrue(InventoryManager.addItem(bag, item1));
        assertTrue(InventoryManager.addItem(bag, item2));
        assertEquals(1, InventoryManager.countOccupied(bag));
        assertEquals(8, bag.get(0).getCount());
    }

    @Test
    public void testAddItemStackToMax() {
        MaterialItem item1 = createMaterial("mat_full", 95);
        MaterialItem item2 = createMaterial("mat_full", 10);

        assertTrue(InventoryManager.addItem(bag, item1));
        assertTrue(InventoryManager.addItem(bag, item2));
        assertEquals(2, InventoryManager.countOccupied(bag));
        assertEquals(99, bag.get(0).getCount());
        assertEquals(6, bag.get(1).getCount());
    }

    @Test
    public void testAddItemStackOverflow() {
        MaterialItem item1 = createMaterial("mat_over", 95);
        MaterialItem item2 = createMaterial("mat_over", 10);

        assertTrue(InventoryManager.addItem(bag, item1));
        assertTrue(InventoryManager.addItem(bag, item2));
        assertEquals(2, InventoryManager.countOccupied(bag));
        assertEquals(99, bag.get(0).getCount());
        assertEquals(6, bag.get(1).getCount());
    }

    @Test
    public void testAddItemNonStackable() {
        InventoryManager.addItem(bag, createMaterial("mat_ns1", 5));
        InventoryManager.addItem(bag, createMaterial("mat_ns2", 3));
        assertEquals(2, InventoryManager.countOccupied(bag));
    }

    @Test
    public void testAddItemMultipleStacks() {
        InventoryManager.addItem(bag, createMaterial("mat_a", 50));
        InventoryManager.addItem(bag, createMaterial("mat_a", 40));
        InventoryManager.addItem(bag, createMaterial("mat_a", 20));
        assertEquals(2, InventoryManager.countOccupied(bag));
        assertEquals(99, bag.get(0).getCount());
        assertEquals(11, bag.get(1).getCount());
    }

    @Test
    public void testRemoveItem() {
        MaterialItem item = createMaterial("mat_del", 1);
        InventoryManager.addItem(bag, item);
        assertEquals(1, InventoryManager.countOccupied(bag));

        InventoryManager.removeItem(bag, item);
        assertEquals(0, InventoryManager.countOccupied(bag));
    }

    @Test
    public void testRemoveItemNonExistent() {
        MaterialItem item = createMaterial("mat_ghost", 1);
        InventoryManager.removeItem(bag, item);
        assertEquals(0, InventoryManager.countOccupied(bag));
    }

    @Test
    public void testAddItemUntilFull() {
        for (int i = 0; i < InventoryManager.BAG_SLOTS; i++) {
            MaterialItem item = new MaterialItem("mat_" + i, "材料" + i,
                    Rarity.COMMON, 5, 99, "来源");
            assertTrue(InventoryManager.addItem(bag, item));
        }
        assertEquals(InventoryManager.BAG_SLOTS, InventoryManager.countOccupied(bag));

        MaterialItem extra = createMaterial("mat_extra", 1);
        assertFalse(InventoryManager.addItem(bag, extra));
        assertEquals(InventoryManager.BAG_SLOTS, InventoryManager.countOccupied(bag));
    }

    @Test
    public void testAddItemAtLastSlot() {
        for (int i = 0; i < InventoryManager.BAG_SLOTS - 1; i++) {
            InventoryManager.addItem(bag, new MaterialItem("slot_" + i, "材料" + i,
                    Rarity.COMMON, 5, 99, "来源"));
        }
        assertTrue(InventoryManager.addItem(bag, createMaterial("slot_last", 1)));
        assertEquals(InventoryManager.BAG_SLOTS, InventoryManager.countOccupied(bag));
    }

    @Test
    public void testGetItemsReturnsList() {
        InventoryManager.addItem(bag, createMaterial("m1", 1));
        InventoryManager.addItem(bag, createMaterial("m2", 1));
        assertEquals(2, InventoryManager.countOccupied(bag));
        assertEquals("m1", bag.get(0).getId());
        assertEquals("m2", bag.get(1).getId());
    }

    @Test
    public void testAutoStackInPlaceMergesScatteredStacks() {
        bag.set(0, createMaterial("mat_a", 20));
        bag.set(5, createMaterial("mat_a", 30));
        bag.set(10, createMaterial("mat_a", 15));
        assertEquals(3, InventoryManager.countOccupied(bag));

        assertTrue(InventoryManager.autoStackInPlace(bag));
        assertEquals(1, InventoryManager.countOccupied(bag));
        assertEquals(65, bag.get(0).getCount());
    }

    @Test
    public void testAutoStackInPlaceRespectsMaxStack() {
        bag.set(0, createMaterial("mat_a", 80));
        bag.set(3, createMaterial("mat_a", 50));
        InventoryManager.autoStackInPlace(bag);
        assertEquals(2, InventoryManager.countOccupied(bag));
        assertEquals(99, bag.get(0).getCount());
        assertEquals(31, bag.get(1).getCount());
    }

    @Test
    public void testOrganizeBagStacksThenCompactsForward() {
        bag.set(4, createMaterial("mat_a", 10));
        bag.set(12, createMaterial("mat_a", 5));
        bag.set(20, createMaterial("mat_b", 3));

        InventoryManager.organizeBag(bag);

        assertEquals(2, InventoryManager.countOccupied(bag));
        assertEquals("mat_a", bag.get(0).getId());
        assertEquals(15, bag.get(0).getCount());
        assertEquals("mat_b", bag.get(1).getId());
        assertEquals(3, bag.get(1).getCount());
        assertEquals(null, bag.get(2));
    }

    private MaterialItem createMaterial(String id, int count) {
        MaterialItem item = new MaterialItem(id, "测试材料",
                Rarity.COMMON, 5, 99, "来源");
        item.setCount(count);
        return item;
    }
}
