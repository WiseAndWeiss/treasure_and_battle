package com.example.treasure_and_battle.item;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.manager.item.ConsumableManager;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.BuffEntry;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.Target;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class ConsumableManagerTest {

    private Context context;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("TestPlayer", context);
        player.setCurrentHp(200);
        player.getFinalAttributes().maxHp = 200;
        player.getFinalAttributes().maxMp = 100;
        player.setCurrentMp(50);
        player.getFinalAttributes().physicalAtk = 30;
        player.getFinalAttributes().magicalAtk = 30;
    }

    // ====================== 空值/异常 ======================

    @Test
    public void testNullPlayer() {
        assertFalse(ConsumableManager.execute(null, null, createHealItem(), context));
    }

    @Test
    public void testNullItem() {
        assertFalse(ConsumableManager.execute(player, null, null, context));
    }

    @Test
    public void testNullEffects() {
        ConsumableItem item = new ConsumableItem("c", "测试",
                Rarity.COMMON, 10, 10, true, true, null, "");
        assertFalse(ConsumableManager.execute(player, null, item, context));
    }

    @Test
    public void testEmptyEffects() {
        ConsumableItem item = new ConsumableItem("c", "测试",
                Rarity.COMMON, 10, 10, true, true, new ArrayList<>(), "");
        assertTrue(ConsumableManager.execute(player, null, item, context));
    }

    @Test
    public void testUnknownEffectType() {
        ConsumableItem.Effect e = new ConsumableItem.Effect();
        e.type = null;
        ConsumableItem item = new ConsumableItem("c", "测试",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertFalse(ConsumableManager.execute(player, null, item, context));
    }

    // ====================== HEAL_HP ======================

    @Test
    public void testHealHP_Percent() {
        player.setCurrentHp(100);
        ConsumableItem item = createSimpleHealHP(10, true);
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(120, player.getCurrentHp());
    }

    @Test
    public void testHealHP_Flat() {
        player.setCurrentHp(100);
        ConsumableItem item = createSimpleHealHP(25, false);
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(125, player.getCurrentHp());
    }

    @Test
    public void testHealHP_Overflow() {
        player.setCurrentHp(190);
        ConsumableItem item = createSimpleHealHP(50, true);
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(200, player.getCurrentHp());
    }

    @Test
    public void testHealHP_MinOne() {
        player.setCurrentHp(100);
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e.value = 0;
        e.isPercent = true;
        ConsumableItem item = new ConsumableItem("c", "测试",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(101, player.getCurrentHp());
    }

    @Test
    public void testHealHP_WithShieldDuration() {
        player.setCurrentHp(100);
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e.value = 50;
        e.isPercent = true;
        e.shieldDuration = 3;
        ConsumableItem item = new ConsumableItem("c", "圣愈",
                Rarity.LEGENDARY, 800, 5, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, new BattleContext(player, new ArrayList<>(), false), item, context));
    }

    // ====================== HEAL_MP ======================

    @Test
    public void testHealMP_Percent() {
        player.setCurrentMp(30);
        ConsumableItem item = createSimpleHealMP(20, true);
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(50, player.getCurrentMp());
    }

    @Test
    public void testHealMP_Flat() {
        player.setCurrentMp(10);
        ConsumableItem item = createSimpleHealMP(30, false);
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(40, player.getCurrentMp());
    }

    @Test
    public void testHealMP_MinOne() {
        player.setCurrentMp(10);
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_MP);
        e.value = 0;
        e.isPercent = true;
        ConsumableItem item = new ConsumableItem("c", "测试",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(11, player.getCurrentMp());
    }

    // ====================== DAMAGE ======================

    @Test
    public void testDamageSingleEnemy() {
        Monster m = createMonster("m1", "怪物", 10);
        m.setCurrentHp(100);
        m.getFinalAttributes().maxHp = 100;

        BattleContext ctx = new BattleContext(player, Collections.singletonList(m), false);

        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.DAMAGE);
        e.target = Target.SINGLE_ENEMY;
        e.value = 20;
        e.isPercent = false;

        ConsumableItem item = new ConsumableItem("bomb", "炸弹",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, ctx, item, context));
        assertTrue(m.getCurrentHp() < 100);
    }

    @Test
    public void testDamageAllEnemies() {
        Monster m1 = createMonster("m1", "怪物A", 10);
        m1.setCurrentHp(100);
        m1.getFinalAttributes().maxHp = 100;
        Monster m2 = createMonster("m2", "怪物B", 10);
        m2.setCurrentHp(100);
        m2.getFinalAttributes().maxHp = 100;

        BattleContext ctx = new BattleContext(player, Arrays.asList(m1, m2), false);

        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.DAMAGE);
        e.target = Target.ALL_ENEMIES;
        e.value = 20;
        e.isPercent = false;

        ConsumableItem item = new ConsumableItem("bomb", "炸弹",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, ctx, item, context));
        assertTrue(m1.getCurrentHp() < 100);
        assertTrue(m2.getCurrentHp() < 100);
    }

    @Test
    public void testDamageNullContext() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.DAMAGE);
        e.value = 10;
        e.isPercent = false;
        ConsumableItem item = new ConsumableItem("bomb", "炸弹",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertFalse(ConsumableManager.execute(player, null, item, context));
    }

    @Test
    public void testDamagePercentBased() {
        Monster m = createMonster("m1", "怪物", 10);
        m.setCurrentHp(100);
        m.getFinalAttributes().maxHp = 100;

        BattleContext ctx = new BattleContext(player, Collections.singletonList(m), false);

        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.DAMAGE);
        e.target = Target.SINGLE_ENEMY;
        e.value = 50;
        e.isPercent = true;

        ConsumableItem item = new ConsumableItem("bomb", "炸弹",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, ctx, item, context));
        int dmg = (int)(Math.max(player.getFinalAttributes().physicalAtk,
                player.getFinalAttributes().magicalAtk) * 0.5f);
        assertEquals(100 - dmg, m.getCurrentHp());
    }

    // ====================== BUFF ======================

    @Test
    public void testBuffEffect() {
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);

        BuffEntry be = new BuffEntry();
        be.buffTemplateId = 4001;
        be.stacks = 5;
        be.duration = 3;

        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.BUFF);
        e.buffs = Arrays.asList(be);

        ConsumableItem item = new ConsumableItem("buff", "增益",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, ctx, item, context));
    }

    @Test
    public void testBuffNullContext() {
        BuffEntry be = new BuffEntry();
        be.buffTemplateId = 4001;

        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.BUFF);
        e.buffs = Arrays.asList(be);

        ConsumableItem item = new ConsumableItem("buff", "增益",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertFalse(ConsumableManager.execute(player, null, item, context));
    }

    // ====================== CLEANSE ======================

    @Test
    public void testCleanseEffect() {
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.CLEANSE);

        ConsumableItem item = new ConsumableItem("cleanse", "净化",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, ctx, item, context));
    }

    @Test
    public void testCleanseNullContext() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.CLEANSE);

        ConsumableItem item = new ConsumableItem("cleanse", "净化",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertFalse(ConsumableManager.execute(player, null, item, context));
    }

    // ====================== ESCAPE ======================

    @Test
    public void testEscapeEffect() {
        BattleContext ctx = new BattleContext(player, new ArrayList<>(), false);
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.ESCAPE);

        ConsumableItem item = new ConsumableItem("escape", "逃跑",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, ctx, item, context));
        assertTrue(ctx.isBattleEnded);
    }

    @Test
    public void testEscapeNullContext() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.ESCAPE);

        ConsumableItem item = new ConsumableItem("escape", "逃跑",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertFalse(ConsumableManager.execute(player, null, item, context));
    }

    // ====================== UTILITY ======================

    @Test
    public void testUtilityEffect() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.UTILITY);
        e.utilityId = "KEY_COPPER";

        ConsumableItem item = new ConsumableItem("key", "钥匙",
                Rarity.COMMON, 10, 10, false, false, Arrays.asList(e), "");
        assertTrue(ConsumableManager.execute(player, null, item, context));
    }

    // ====================== 组合效果 ======================

    @Test
    public void testMultipleEffects() {
        ConsumableItem.Effect e1 = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e1.value = 10;
        e1.isPercent = false;
        ConsumableItem.Effect e2 = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_MP);
        e2.value = 5;
        e2.isPercent = false;

        player.setCurrentHp(100);
        player.setCurrentMp(20);
        ConsumableItem item = new ConsumableItem("combo", "组合药",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e1, e2), "");
        assertTrue(ConsumableManager.execute(player, null, item, context));
        assertEquals(110, player.getCurrentHp());
        assertEquals(25, player.getCurrentMp());
    }

    @Test
    public void testMultipleEffectsFailOnSecond() {
        ConsumableItem.Effect e1 = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e1.value = 10;
        e1.isPercent = false;
        ConsumableItem.Effect e2 = new ConsumableItem.Effect(ConsumableItem.EffectType.DAMAGE);
        e2.value = 10;
        e2.isPercent = false;

        player.setCurrentHp(100);
        ConsumableItem item = new ConsumableItem("combo", "组合",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e1, e2), "");
        assertFalse(ConsumableManager.execute(player, null, item, context));
        assertEquals(110, player.getCurrentHp());
    }

    // ====================== helpers ======================

    private ConsumableItem createHealItem() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e.value = 10;
        e.isPercent = false;
        return new ConsumableItem("heal", "回复", Rarity.COMMON, 10, 10,
                true, true, Arrays.asList(e), "");
    }

    private ConsumableItem createSimpleHealHP(float value, boolean isPercent) {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e.value = value;
        e.isPercent = isPercent;
        return new ConsumableItem("heal", "回复", Rarity.COMMON, 10, 10,
                true, true, Arrays.asList(e), "");
    }

    private ConsumableItem createSimpleHealMP(float value, boolean isPercent) {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_MP);
        e.value = value;
        e.isPercent = isPercent;
        return new ConsumableItem("heal_mp", "回蓝", Rarity.COMMON, 10, 10,
                true, true, Arrays.asList(e), "");
    }

    private Monster createMonster(String id, String name, int level) {
        return new Monster(id, name, level, Rarity.COMMON,
                0, 0, 0, 0, 0, 0,
                10, 10, 1.0f, 1.0f, 1.0f, 1.0f, context);
    }
}
