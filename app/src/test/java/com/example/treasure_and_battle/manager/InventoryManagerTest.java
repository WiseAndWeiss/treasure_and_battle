package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.MaterialItem;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryManagerTest {

    private InventoryManager inventory;

    @Before
    public void setUp() {
        inventory = InventoryManager.getInstance();
        for (Item item : new java.util.ArrayList<>(inventory.getItems())) {
            inventory.removeItem(item);
        }
    }

    @Test
    public void testGetItemsInitiallyEmpty() {
        assertNotNull(inventory.getItems());
        assertEquals(0, inventory.getItems().size());
    }

    @Test
    public void testAddItemSuccess() {
        MaterialItem item = createMaterial("mat_1", 1);
        assertTrue(inventory.addItem(item));
        assertEquals(1, inventory.getItems().size());
    }

    @Test
    public void testAddItemStack() {
        MaterialItem item1 = createMaterial("mat_stack", 5);
        MaterialItem item2 = createMaterial("mat_stack", 3);

        assertTrue(inventory.addItem(item1));
        assertTrue(inventory.addItem(item2));
        assertEquals(1, inventory.getItems().size());
        assertEquals(8, inventory.getItems().get(0).getCount());
    }

    @Test
    public void testAddItemStackToMax() {
        MaterialItem item1 = createMaterial("mat_full", 95);
        MaterialItem item2 = createMaterial("mat_full", 10);

        assertTrue(inventory.addItem(item1));
        assertTrue(inventory.addItem(item2));
        assertEquals(2, inventory.getItems().size());
        assertEquals(99, inventory.getItems().get(0).getCount());
        assertEquals(6, inventory.getItems().get(1).getCount());
    }

    @Test
    public void testAddItemStackOverflow() {
        MaterialItem item1 = createMaterial("mat_over", 95);
        MaterialItem item2 = createMaterial("mat_over", 10);

        assertTrue(inventory.addItem(item1));
        assertTrue(inventory.addItem(item2));
        assertEquals(2, inventory.getItems().size());
        assertEquals(99, inventory.getItems().get(0).getCount());
        assertEquals(6, inventory.getItems().get(1).getCount());
    }

    @Test
    public void testAddItemNonStackable() {
        inventory.addItem(createMaterial("mat_ns1", 5));
        inventory.addItem(createMaterial("mat_ns2", 3));
        assertEquals(2, inventory.getItems().size());
    }

    @Test
    public void testAddItemMultipleStacks() {
        inventory.addItem(createMaterial("mat_a", 50));
        inventory.addItem(createMaterial("mat_a", 40));
        inventory.addItem(createMaterial("mat_a", 20));
        assertEquals(2, inventory.getItems().size());
        assertEquals(99, inventory.getItems().get(0).getCount());
        assertEquals(11, inventory.getItems().get(1).getCount());
    }

    @Test
    public void testRemoveItem() {
        MaterialItem item = createMaterial("mat_del", 1);
        inventory.addItem(item);
        assertEquals(1, inventory.getItems().size());

        inventory.removeItem(item);
        assertEquals(0, inventory.getItems().size());
    }

    @Test
    public void testRemoveItemNonExistent() {
        MaterialItem item = createMaterial("mat_ghost", 1);
        inventory.removeItem(item);
        assertEquals(0, inventory.getItems().size());
    }

    @Test
    public void testAddItemUntilFull() {
        for (int i = 0; i < 50; i++) {
            MaterialItem item = new MaterialItem("mat_" + i, "材料" + i,
                    Rarity.COMMON, 5, 99, "来源");
            assertTrue(inventory.addItem(item));
        }
        assertEquals(50, inventory.getItems().size());

        MaterialItem extra = createMaterial("mat_extra", 1);
        assertFalse(inventory.addItem(extra));
        assertEquals(50, inventory.getItems().size());
    }

    @Test
    public void testAddItemAtBoundary49() {
        for (int i = 0; i < 49; i++) {
            inventory.addItem(new MaterialItem("slot_" + i, "材料" + i,
                    Rarity.COMMON, 5, 99, "来源"));
        }
        assertTrue(inventory.addItem(createMaterial("slot_49", 1)));
        assertEquals(50, inventory.getItems().size());
    }

    @Test
    public void testGetItemsReturnsList() {
        inventory.addItem(createMaterial("m1", 1));
        inventory.addItem(createMaterial("m2", 1));
        assertEquals(2, inventory.getItems().size());
        assertEquals("m1", inventory.getItems().get(0).getId());
        assertEquals("m2", inventory.getItems().get(1).getId());
    }

    private MaterialItem createMaterial(String id, int count) {
        MaterialItem item = new MaterialItem(id, "测试材料",
                Rarity.COMMON, 5, 99, "来源");
        item.setCount(count);
        return item;
    }
}
