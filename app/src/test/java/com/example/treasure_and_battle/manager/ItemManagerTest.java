package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.ConsumableItem;
import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.model.item.GemItem;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.MaterialItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class ItemManagerTest {

    private Context context;
    private ItemManager itemManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        itemManager = ItemManager.getInstance(context);
    }

    // ====================== createItem 统一入口 ======================

    @Test
    public void testCreateItem_Material() {
        Item item = itemManager.createItem("slime_gel_common");
        assertNotNull(item);
        assertTrue(item instanceof MaterialItem);
        assertEquals("slime_gel_common", item.getId());
    }

    @Test
    public void testCreateItem_Consumable() {
        Item item = itemManager.createItem("potion_hp_small");
        assertNotNull(item);
        assertTrue(item instanceof ConsumableItem);
    }

    @Test
    public void testCreateItem_Gem() {
        Item item = itemManager.createItem("ruby_普通");
        assertNotNull(item);
        assertTrue(item instanceof GemItem);
        assertEquals("ruby_普通", item.getId());
    }

    @Test
    public void testCreateItem_Null() {
        assertNull(itemManager.createItem(null));
    }

    @Test
    public void testCreateItem_NonExistent() {
        assertNull(itemManager.createItem("does_not_exist_at_all"));
    }

    // ====================== createMaterial ======================

    @Test
    public void testCreateMaterial_Valid() {
        MaterialItem item = itemManager.createMaterial("slime_gel_common");
        assertNotNull(item);
        assertEquals("史莱姆凝胶", item.getName());
    }

    @Test
    public void testCreateMaterial_Null() {
        assertNull(itemManager.createMaterial(null));
    }

    @Test
    public void testCreateMaterial_NonExistent() {
        assertNull(itemManager.createMaterial("not_a_material"));
    }

    // ====================== createConsumable ======================

    @Test
    public void testCreateConsumable_Valid() {
        ConsumableItem item = itemManager.createConsumable("potion_hp_small");
        assertNotNull(item);
        assertEquals("小型生命药水", item.getName());
    }

    @Test
    public void testCreateConsumable_Null() {
        assertNull(itemManager.createConsumable(null));
    }

    @Test
    public void testCreateConsumable_NonExistent() {
        assertNull(itemManager.createConsumable("no_such_consumable"));
    }

    // ====================== createGem ======================

    @Test
    public void testCreateGem_Valid() {
        GemItem item = itemManager.createGem("ruby_普通");
        assertNotNull(item);
        assertEquals("普通红宝石", item.getName());
        assertEquals("RUBY", item.getGemType());
    }

    @Test
    public void testCreateGem_Null() {
        assertNull(itemManager.createGem(null));
    }

    @Test
    public void testCreateGem_NonExistent() {
        assertNull(itemManager.createGem("no_gem"));
    }

    // ====================== 按稀有度随机选取 ======================

    @Test
    public void testGetRandomConsumableByRarity_Common() {
        ConsumableItem item = itemManager.getRandomConsumableByRarity(Rarity.COMMON);
        assertNotNull(item);
        assertEquals(Rarity.COMMON, item.getRarity());
    }

    @Test
    public void testGetRandomConsumableByRarity_Legendary() {
        ConsumableItem item = itemManager.getRandomConsumableByRarity(Rarity.LEGENDARY);
        assertNotNull(item);
        assertEquals(Rarity.LEGENDARY, item.getRarity());
    }

    @Test
    public void testGetRandomGemByRarity_Common() {
        GemItem item = itemManager.getRandomGemByRarity(Rarity.COMMON);
        assertNotNull(item);
        assertEquals(Rarity.COMMON, item.getRarity());
    }

    @Test
    public void testGetRandomGemByRarity_Legendary() {
        GemItem item = itemManager.getRandomGemByRarity(Rarity.LEGENDARY);
        assertNotNull(item);
        assertEquals(Rarity.LEGENDARY, item.getRarity());
    }

    // ====================== 查询模板 ======================

    @Test
    public void testGetMaterialTemplate() {
        assertNotNull(itemManager.getMaterialTemplate("slime_gel_common"));
    }

    @Test
    public void testGetConsumableTemplate() {
        assertNotNull(itemManager.getConsumableTemplate("potion_hp_small"));
    }

    @Test
    public void testGetGemTemplate() {
        assertNotNull(itemManager.getGemTemplate("ruby_普通"));
    }

    @Test
    public void testGetMaterialTemplate_Null() {
        assertNull(itemManager.getMaterialTemplate(null));
    }

    @Test
    public void testGetConsumableTemplate_NonExistent() {
        assertNull(itemManager.getConsumableTemplate("no_such_id"));
    }
}
