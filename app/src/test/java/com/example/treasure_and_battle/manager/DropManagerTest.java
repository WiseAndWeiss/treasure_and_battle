package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.MaterialItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class DropManagerTest {

    private Context context;
    private DropManager dropManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        dropManager = DropManager.getInstance(context);
        InventoryManager.getInstance().getItems().clear();
    }

    // ====================== generateDrops ======================

    @Test
    public void testGenerateDropsNullContext() {
        List<Item> drops = dropManager.generateDrops(null);
        assertNotNull(drops);
        assertTrue(drops.isEmpty());
    }

    @Test
    public void testGenerateDropsNullMonsters() {
        Player player = new Player("p", context);
        BattleContext ctx = new BattleContext(player, (List<Monster>) null, false);
        List<Item> drops = dropManager.generateDrops(ctx);
        assertNotNull(drops);
    }

    @Test
    public void testGenerateDropsEmptyMonsters() {
        Player player = new Player("p", context);
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        List<Item> drops = dropManager.generateDrops(ctx);
        assertNotNull(drops);
    }

    @Test
    public void testGenerateDropsWithMonster() {
        Player player = new Player("p", context);
        Monster m = new Monster("m1", "测试怪物", 5, Rarity.COMMON,
                0, 0, 0, 0, 0, 0,
                30, 10, 1.0f, 1.0f, 1.0f, 1.0f, context);
        m.setTemplateId(1001);

        BattleContext ctx = new BattleContext(player, Collections.singletonList(m), false);
        List<Item> drops = dropManager.generateDrops(ctx);
        assertNotNull(drops);
        assertTrue(drops.size() >= 1);
    }

    @Test
    public void testGenerateDropsMonsterWithoutTemplate() {
        Player player = new Player("p", context);
        Monster m = new Monster("m2", "无模板怪物", 5, Rarity.COMMON,
                0, 0, 0, 0, 0, 0,
                30, 10, 1.0f, 1.0f, 1.0f, 1.0f, context);
        m.setTemplateId(0);

        BattleContext ctx = new BattleContext(player, Collections.singletonList(m), false);
        List<Item> drops = dropManager.generateDrops(ctx);
        assertNotNull(drops);
        assertEquals(1, drops.size());
    }

    // ====================== claimDrop ======================

    @Test
    public void testClaimDropValid() {
        MaterialItem item = new MaterialItem("mat_1", "材料",
                Rarity.COMMON, 5, 99, "来源");
        Player player = new Player("p", context);
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        ctx.pendingLoot = new ArrayList<>(Arrays.asList(item));

        Item claimed = dropManager.claimDrop(ctx, 0);
        assertNotNull(claimed);
        assertEquals("mat_1", claimed.getId());
        assertTrue(ctx.pendingLoot.isEmpty());
    }

    @Test
    public void testClaimDropInvalidIndex() {
        Player player = new Player("p", context);
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        ctx.pendingLoot = new ArrayList<>();

        assertNull(dropManager.claimDrop(ctx, 0));
        assertNull(dropManager.claimDrop(ctx, -1));
        assertNull(dropManager.claimDrop(ctx, 5));
    }

    @Test
    public void testClaimDropNullContext() {
        assertNull(dropManager.claimDrop(null, 0));
    }

    // ====================== claimAll ======================

    @Test
    public void testClaimAllAllCollected() {
        MaterialItem item1 = new MaterialItem("mat_a", "A",
                Rarity.COMMON, 5, 99, "来源");
        MaterialItem item2 = new MaterialItem("mat_b", "B",
                Rarity.COMMON, 5, 99, "来源");

        Player player = new Player("p", context);
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        ctx.pendingLoot = new ArrayList<>(Arrays.asList(item1, item2));

        List<Item> unclaimed = dropManager.claimAll(ctx);
        assertNotNull(unclaimed);
        assertTrue(unclaimed.isEmpty());
        assertTrue(ctx.pendingLoot.isEmpty());
    }

    @Test
    public void testClaimAllPartialFailure() {
        InventoryManager inv = InventoryManager.getInstance();
        for (int i = 0; i < 50; i++) {
            inv.addItem(new MaterialItem("fill_" + i, "填充",
                    Rarity.COMMON, 5, 99, "来源"));
        }

        MaterialItem item1 = new MaterialItem("test_1", "测试1",
                Rarity.COMMON, 5, 99, "来源");
        MaterialItem item2 = new MaterialItem("test_2", "测试2",
                Rarity.COMMON, 5, 99, "来源");

        Player player = new Player("p", context);
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        ctx.pendingLoot = new ArrayList<>(Arrays.asList(item1, item2));

        List<Item> unclaimed = dropManager.claimAll(ctx);
        assertNotNull(unclaimed);
        assertTrue(unclaimed.size() >= 2);
    }

    @Test
    public void testClaimAllNullContext() {
        List<Item> unclaimed = dropManager.claimAll(null);
        assertNotNull(unclaimed);
        assertTrue(unclaimed.isEmpty());
    }

    @Test
    public void testClaimAllEmptyPending() {
        Player player = new Player("p", context);
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        ctx.pendingLoot = new ArrayList<>();

        List<Item> unclaimed = dropManager.claimAll(ctx);
        assertNotNull(unclaimed);
        assertTrue(unclaimed.isEmpty());
    }

    // ====================== pickTypeByWeight ======================

    @Test
    public void testPickTypeByWeightNonNullMonster() {
        Player player = new Player("p", context);
        Monster m = new Monster("m3", "测试怪物", 10, Rarity.UNCOMMON,
                0, 0, 0, 0, 0, 0,
                60, 25, 1.5f, 1.25f, 1.5f, 1.0f, context);
        m.setTemplateId(1002);

        BattleContext ctx = new BattleContext(player, Collections.singletonList(m), false);
        List<Item> drops = dropManager.generateDrops(ctx);
        assertNotNull(drops);
        assertTrue(drops.size() >= 1);
    }
}
