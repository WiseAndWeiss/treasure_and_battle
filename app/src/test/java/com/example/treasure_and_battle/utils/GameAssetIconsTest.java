package com.example.treasure_and_battle.utils;

import android.content.Context;
import android.widget.ImageView;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class GameAssetIconsTest {

    private Context context;
    private ImageView imageView;

    @Before
    public void setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        imageView = new ImageView(context);
    }

    // ==================== isSafeAssetId 测试 ====================

    @Test
    public void testIsSafeAssetId_Null() {
        assertFalse(GameAssetIcons.isSafeAssetId(null));
    }

    @Test
    public void testIsSafeAssetId_Empty() {
        assertFalse(GameAssetIcons.isSafeAssetId(""));
    }

    @Test
    public void testIsSafeAssetId_ValidId() {
        assertTrue(GameAssetIcons.isSafeAssetId("sword_01"));
        assertTrue(GameAssetIcons.isSafeAssetId("fireball"));
        assertTrue(GameAssetIcons.isSafeAssetId("123"));
    }

    @Test
    public void testIsSafeAssetId_ContainsForwardSlash() {
        assertFalse(GameAssetIcons.isSafeAssetId("path/asset"));
        assertFalse(GameAssetIcons.isSafeAssetId("a/b"));
    }

    @Test
    public void testIsSafeAssetId_ContainsBackslash() {
        assertFalse(GameAssetIcons.isSafeAssetId("path\\asset"));
        assertFalse(GameAssetIcons.isSafeAssetId("a\\b"));
    }

    @Test
    public void testIsSafeAssetId_StartsWithDot() {
        assertFalse(GameAssetIcons.isSafeAssetId(".hidden"));
        assertFalse(GameAssetIcons.isSafeAssetId(".gitignore"));
    }

    @Test
    public void testIsSafeAssetId_EndsWithDot() {
        assertFalse(GameAssetIcons.isSafeAssetId("file."));
        assertFalse(GameAssetIcons.isSafeAssetId("test."));
    }

    @Test
    public void testIsSafeAssetId_ContainsDotInMiddle() {
        assertTrue(GameAssetIcons.isSafeAssetId("file.name"));
        assertTrue(GameAssetIcons.isSafeAssetId("icon.v2.png"));
    }

    @Test
    public void testIsSafeAssetId_WithUnderscore() {
        assertTrue(GameAssetIcons.isSafeAssetId("item_id_123"));
    }

    @Test
    public void testIsSafeAssetId_WithDash() {
        assertTrue(GameAssetIcons.isSafeAssetId("item-id-123"));
    }

    // ==================== assetPath 测试 ====================

    @Test
    public void testAssetPath_ValidId() {
        String path = GameAssetIcons.assetPath("material", "wood");
        assertEquals("icons/material/wood.png", path);
    }

    @Test
    public void testAssetPath_UnsafeId_ReturnsNull() {
        assertNull(GameAssetIcons.assetPath("material", "../etc/passwd"));
        assertNull(GameAssetIcons.assetPath("material", "path/file"));
    }

    @Test
    public void testAssetPath_NullId_ReturnsNull() {
        assertNull(GameAssetIcons.assetPath("material", null));
    }

    @Test
    public void testAssetPath_EmptyId_ReturnsNull() {
        assertNull(GameAssetIcons.assetPath("material", ""));
    }

    @Test
    public void testAssetPath_DifferentFolders() {
        assertEquals("icons/equip/sword.png", GameAssetIcons.assetPath("equip", "sword"));
        assertEquals("icons/skill/fireball.png", GameAssetIcons.assetPath("skill", "fireball"));
        assertEquals("icons/monster/dragon.png", GameAssetIcons.assetPath("monster", "dragon"));
        assertEquals("icons/consumable/potion.png", GameAssetIcons.assetPath("consumable", "potion"));
        assertEquals("icons/gem/ruby.png", GameAssetIcons.assetPath("gem", "ruby"));
    }

    // ==================== bindItem 测试 ====================

    @Test
    public void testBindItem_NullImageView() {
        // 不应该抛出异常
        GameAssetIcons.bindItem(context, null, createMockItem(ItemType.MATERIAL, "wood"));
    }

    @Test
    public void testBindItem_NullItem() {
        // 不应该抛出异常
        GameAssetIcons.bindItem(context, imageView, null);
    }

    @Test
    public void testBindItem_MaterialType() {
        Item item = createMockItem(ItemType.MATERIAL, "wood");
        // 测试不抛异常（资源不存在时会使用fallback）
        GameAssetIcons.bindItem(context, imageView, item);
    }

    @Test
    public void testBindItem_EquipmentType() {
        Item item = createMockItem(ItemType.EQUIPMENT, "sword");
        GameAssetIcons.bindItem(context, imageView, item);
    }

    @Test
    public void testBindItem_ConsumableType() {
        Item item = createMockItem(ItemType.CONSUMABLE, "potion");
        GameAssetIcons.bindItem(context, imageView, item);
    }

    @Test
    public void testBindItem_GemType() {
        Item item = createMockItem(ItemType.GEM, "ruby");
        GameAssetIcons.bindItem(context, imageView, item);
    }

    @Test
    public void testBindItem_NullType() {
        Item item = createMockItem(null, "test");
        GameAssetIcons.bindItem(context, imageView, item);
    }

    @Test
    public void testBindItem_WithFallbackResId() {
        Item item = createMockItem(ItemType.MATERIAL, "wood", 12345);
        GameAssetIcons.bindItem(context, imageView, item);
    }

    // ==================== bindMonster 测试 ====================

    @Test
    public void testBindMonster_NullImageView() {
        // 不应该抛出异常
        GameAssetIcons.bindMonster(context, null, "goblin", 0);
    }

    @Test
    public void testBindMonster_NullEntityId() {
        // 应该使用fallback
        GameAssetIcons.bindMonster(context, imageView, null, 0);
    }

    @Test
    public void testBindMonster_ValidEntityId() {
        GameAssetIcons.bindMonster(context, imageView, "goblin", 0);
    }

    @Test
    public void testBindMonster_WithFallbackResId() {
        GameAssetIcons.bindMonster(context, imageView, "dragon", 12345);
    }

    // ==================== bindSkill 测试 ====================

    @Test
    public void testBindSkill_NullImageView() {
        // 不应该抛出异常
        GameAssetIcons.bindSkill(context, null, "fireball", 0);
    }

    @Test
    public void testBindSkill_NullSkillId() {
        // 应该使用fallback
        GameAssetIcons.bindSkill(context, imageView, null, 0);
    }

    @Test
    public void testBindSkill_ValidSkillId() {
        GameAssetIcons.bindSkill(context, imageView, "fireball", 0);
    }

    @Test
    public void testBindSkill_WithFallbackResId() {
        GameAssetIcons.bindSkill(context, imageView, "heal", 54321);
    }

    // ==================== bindPng 测试 ====================

    @Test
    public void testBindPng_ValidParameters() {
        // 不抛异常即可
        GameAssetIcons.bindPng(context, imageView, "material", "wood", 0);
    }

    @Test
    public void testBindPng_UnsafeId() {
        // 应该使用fallback
        GameAssetIcons.bindPng(context, imageView, "material", "../unsafe", 0);
    }

    @Test
    public void testBindPng_NullId() {
        GameAssetIcons.bindPng(context, imageView, "material", null, 0);
    }

    @Test
    public void testBindPng_WithFallbackResId() {
        GameAssetIcons.bindPng(context, imageView, "skill", "nonexistent", 11111);
    }

    // ==================== 常量测试 ====================

    @Test
    public void testConstants() {
        assertEquals("icons", GameAssetIcons.ROOT);
        assertEquals("material", GameAssetIcons.FOLDER_MATERIAL);
        assertEquals("consumable", GameAssetIcons.FOLDER_CONSUMABLE);
        assertEquals("gem", GameAssetIcons.FOLDER_GEM);
        assertEquals("equip", GameAssetIcons.FOLDER_EQUIP);
        assertEquals("monster", GameAssetIcons.FOLDER_MONSTER);
        assertEquals("skill", GameAssetIcons.FOLDER_SKILL);
    }

    // ==================== 辅助方法 ====================

    private Item createMockItem(ItemType type, String id) {
        return createMockItem(type, id, 0);
    }

    private Item createMockItem(ItemType type, String id, int iconResId) {
        return new Item(id, "Test Item", Rarity.COMMON, 10, type, 1, 99) {
            @Override
            public ItemType getType() {
                return type;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public int getIconResId() {
                return iconResId;
            }
        };
    }
}
