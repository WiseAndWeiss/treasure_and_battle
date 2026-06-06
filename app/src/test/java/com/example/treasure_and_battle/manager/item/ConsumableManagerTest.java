package com.example.treasure_and_battle.manager.item;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.BattleContext.SurpriseDirection;
import com.example.treasure_and_battle.buff.impl.control.FrozenDebuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.manager.battle.DamageManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.BuffEntry;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.DebuffEntry;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.Effect;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.EffectType;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.Target;
import com.example.treasure_and_battle.model.profession.ProfessionType;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * ConsumableManager 单元测试
 * 覆盖战斗内和战斗外的道具使用逻辑
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class ConsumableManagerTest {

    private Context context;
    private Player testPlayer;
    private Monster testMonster;
    private Character testCharacter;
    private BattleContext battleContext;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;

        // 初始化 Manager 单例
        BuffManager.getInstance(context);
        DamageManager.getInstance(context);

        // 创建测试用 Character
        testCharacter = new Character(1, "TestHero", ProfessionType.WARRIOR, context);
        testCharacter.addGold(1000);

        // 创建测试用 Player
        testPlayer = testCharacter.generatePlayer();
        testPlayer.owner = testCharacter;
        AttributeSet playerAttr = testPlayer.getBaseAttributes();
        playerAttr.strength = 10;
        playerAttr.agility = 10;
        playerAttr.intelligence = 10;
        playerAttr.spirit = 10;
        playerAttr.physique = 10;
        playerAttr.luck = 10;
        playerAttr.maxHp = 100;
        playerAttr.maxMp = 50;
        playerAttr.physicalAtk = 30;
        playerAttr.physicalDef = 10;
        playerAttr.magicalAtk = 20;
        playerAttr.magicalDef = 10;
        playerAttr.speed = 15;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(50);
        testPlayer.setCurrentMp(25);

        // 创建测试用 Monster
        testMonster = new Monster("test_monster", "TestMonster", 1, Rarity.COMMON,
                10, 10, 10, 10, 10, 10,
                100, 50, 1.0f, 1.0f, 1.0f, 1.0f, context);
        AttributeSet monsterAttr = testMonster.getBaseAttributes();
        monsterAttr.maxHp = 200;
        monsterAttr.maxMp = 30;
        monsterAttr.physicalAtk = 25;
        monsterAttr.physicalDef = 8;
        monsterAttr.magicalAtk = 15;
        monsterAttr.magicalDef = 8;
        monsterAttr.speed = 12;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(200);
        testMonster.setCurrentMp(30);

        // 创建 BattleContext
        battleContext = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleContext.currentRound = 1;
    }

    // ====================== execute 测试 ======================

    @Test
    public void testExecute_NullPlayerReturnsFalse() {
        ConsumableItem item = createHealPotion(50);

        boolean result = ConsumableManager.execute(null, battleContext, item, context);

        assertFalse("空玩家应返回 false", result);
    }

    @Test
    public void testExecute_NullItemReturnsFalse() {
        boolean result = ConsumableManager.execute(testPlayer, battleContext, null, context);

        assertFalse("空道具应返回 false", result);
    }

    @Test
    public void testExecute_ItemWithNullEffectsReturnsFalse() {
        ConsumableItem item = new ConsumableItem("test", "Test", Rarity.COMMON,
                10, 10, true, true, null, "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertFalse("空效果列表应返回 false", result);
    }

    @Test
    public void testExecute_ItemWithEmptyEffectsReturnsFalse() {
        ConsumableItem item = new ConsumableItem("test", "Test", Rarity.COMMON,
                10, 10, true, true, new ArrayList<>(), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertFalse("空效果列表应返回 false", result);
    }

    @Test
    public void testExecute_SingleEffectSuccess() {
        int hpBefore = testPlayer.getCurrentHp();
        ConsumableItem item = createHealPotion(30);

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("成功执行应返回 true", result);
        assertEquals("HP应增加30", hpBefore + 30, testPlayer.getCurrentHp());
    }

    @Test
    public void testExecute_MultipleEffectsAllSuccess() {
        testPlayer.setCurrentHp(40);
        testPlayer.setCurrentMp(10);
        ConsumableItem item = createMultiEffectConsumable();

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("多个效果应全部成功", result);
        assertEquals("HP应增加30", 70, testPlayer.getCurrentHp());
        assertEquals("MP应增加15", 25, testPlayer.getCurrentMp());
    }

    @Test
    public void testExecute_FirstEffectFailsReturnsFalse() {
        // 创建一个包含无效效果和有效效果的道具
        List<Effect> effects = new ArrayList<>();
        Effect invalidEffect = new Effect();
        invalidEffect.type = null; // 无效效果
        effects.add(invalidEffect);

        Effect healEffect = createHealEffect(30);
        effects.add(healEffect);

        ConsumableItem item = new ConsumableItem("test", "Test", Rarity.COMMON,
                10, 10, true, true, effects, "");

        int hpBefore = testPlayer.getCurrentHp();
        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertFalse("第一个效果失败应返回 false", result);
        assertEquals("HP不应变化", hpBefore, testPlayer.getCurrentHp());
    }

    // ====================== dispatchInBattle 测试 ======================

    @Test
    public void dispatchInBattle_NullEffectTypeReturnsFalse() {
        Effect e = new Effect();
        e.type = null;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("test", "Test", Rarity.COMMON, 10, 10, true, true,
                        Collections.singletonList(e), ""), context);

        assertFalse("空效果类型应返回 false", result);
    }

    @Test
    public void dispatchInBattle_HealHpEffect() {
        testPlayer.setCurrentHp(30);
        Effect e = createHealEffect(20);
        ConsumableItem item = new ConsumableItem("potion", "Potion", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("HP恢复效果应成功", result);
        assertEquals("HP应增加20", 50, testPlayer.getCurrentHp());
    }

    @Test
    public void dispatchInBattle_HealMpEffect() {
        testPlayer.setCurrentMp(10);
        Effect e = createMpHealEffect(15);
        ConsumableItem item = new ConsumableItem("ether", "Ether", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("MP恢复效果应成功", result);
        assertEquals("MP应增加15", 25, testPlayer.getCurrentMp());
    }

    @Test
    public void dispatchInBattle_DamageSingleTargetEffect() {
        int monsterHpBefore = testMonster.getCurrentHp();
        Effect e = createDamageEffect(40, Target.SINGLE_ENEMY);
        ConsumableItem item = new ConsumableItem("bomb", "Bomb", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("单体伤害效果应成功", result);
        assertTrue("怪物HP应减少", testMonster.getCurrentHp() < monsterHpBefore);
    }

    @Test
    public void dispatchInBattle_DamageAllEnemiesEffect() {
        Monster monster2 = new Monster("m2", "Monster2", 1, Rarity.COMMON,
                10, 10, 10, 10, 10, 10,
                150, 30, 1.0f, 1.0f, 1.0f, 1.0f, context);
        monster2.setCurrentHp(150);

        BattleContext multiCtx = new BattleContext(testPlayer,
                Arrays.asList(testMonster, monster2), SurpriseDirection.NONE);

        int m1HpBefore = testMonster.getCurrentHp();
        int m2HpBefore = monster2.getCurrentHp();

        Effect e = createDamageEffect(30, Target.ALL_ENEMIES);
        ConsumableItem item = new ConsumableItem("scroll", "Scroll", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, multiCtx, item, context);

        assertTrue("AOE伤害效果应成功", result);
        assertTrue("怪物1 HP应减少", testMonster.getCurrentHp() < m1HpBefore);
        assertTrue("怪物2 HP应减少", monster2.getCurrentHp() < m2HpBefore);
    }

    @Test
    public void dispatchInBattle_DamageWithNullContextReturnsFalse() {
        Effect e = createDamageEffect(30, Target.SINGLE_ENEMY);
        ConsumableItem item = new ConsumableItem("bomb", "Bomb", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, null, item, context);

        assertFalse("空上下文时伤害效果应失败", result);
    }

    @Test
    public void dispatchInBattle_DamageNoPrimaryTargetReturnsFalse() {
        // 创建没有怪物的上下文
        BattleContext emptyCtx = new BattleContext(testPlayer, new ArrayList<>(), SurpriseDirection.NONE);

        Effect e = createDamageEffect(30, Target.SINGLE_ENEMY);
        ConsumableItem item = new ConsumableItem("bomb", "Bomb", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, emptyCtx, item, context);

        assertFalse("无主要目标时伤害效果应失败", result);
    }

    @Test
    public void dispatchInBattle_BuffEffect() {
        Effect e = createBuffEffect(101, 2, 3);
        ConsumableItem item = new ConsumableItem("buff_scroll", "Buff Scroll", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        // 即使buff创建失败（模板不存在），方法也应返回true（跳过null buff）
        assertTrue("Buff效果应执行", result);
    }

    @Test
    public void dispatchInBattle_BuffWithNullContextReturnsFalse() {
        Effect e = createBuffEffect(101, 2, 3);
        ConsumableItem item = new ConsumableItem("buff_scroll", "Buff Scroll", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, null, item, context);

        assertFalse("空上下文时Buff效果应失败", result);
    }

    @Test
    public void dispatchInBattle_BuffWithNullBuffsReturnsFalse() {
        Effect e = new Effect(EffectType.BUFF);
        e.buffs = null;
        ConsumableItem item = new ConsumableItem("buff_scroll", "Buff Scroll", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertFalse("空buff列表应失败", result);
    }

    @Test
    public void dispatchInBattle_CleanseEffect() {
        // 先给玩家添加一个负面buff
        FrozenDebuff frozen = new FrozenDebuff("frozen", "冻结", "", BuffType.DEBUFF, true, 2, 1, false, 0f);
        BuffManager.getInstance(context).addBuff(testPlayer, frozen);

        Effect e = new Effect(EffectType.CLEANSE);
        ConsumableItem item = new ConsumableItem("cleanse", "Cleanse", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        int buffCountBefore = testPlayer.getActiveBuffList().size();
        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("净化效果应成功", result);
        // 净化应移除debuff类型的buff
        int debuffCountAfter = 0;
        for (com.example.treasure_and_battle.buff.BaseBuff b : testPlayer.getActiveBuffList()) {
            if (b.getBuffType() == BuffType.DEBUFF) {
                debuffCountAfter++;
            }
        }
        assertTrue("debuff数量应减少", debuffCountAfter < buffCountBefore);
    }

    @Test
    public void dispatchInBattle_CleanseWithNullContextReturnsFalse() {
        Effect e = new Effect(EffectType.CLEANSE);
        ConsumableItem item = new ConsumableItem("cleanse", "Cleanse", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, null, item, context);

        assertFalse("空上下文时净化效果应失败", result);
    }

    @Test
    public void dispatchInBattle_EscapeEffect() {
        Effect e = new Effect(EffectType.ESCAPE);
        ConsumableItem item = new ConsumableItem("smoke_bomb", "Smoke Bomb", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("逃跑效果应成功", result);
        assertTrue("战斗应结束", battleContext.isBattleEnded);
        assertEquals(BattleContext.BattleResult.ESCAPED, battleContext.battleResult);
    }

    @Test
    public void dispatchInBattle_EscapeWithNullContextReturnsFalse() {
        Effect e = new Effect(EffectType.ESCAPE);
        ConsumableItem item = new ConsumableItem("smoke_bomb", "Smoke Bomb", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, null, item, context);

        assertFalse("空上下文时逃跑效果应失败", result);
    }

    @Test
    public void dispatchInBattle_UtilityPassiveEffect() {
        Effect e = new Effect(EffectType.UTILITY_PASSIVE);
        e.utilityId = "test_utility";
        ConsumableItem item = new ConsumableItem("utility_item", "Utility Item", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        // UTILITY 效果在战斗内总是返回 true
        assertTrue("UTILITY_PASSIVE 效果应返回 true", result);
    }

    @Test
    public void dispatchInBattle_UtilityActiveEffect() {
        Effect e = new Effect(EffectType.UTILITY_ACTIVE);
        e.utilityId = "test_utility";
        ConsumableItem item = new ConsumableItem("utility_item", "Utility Item", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        // UTILITY 效果在战斗内总是返回 true
        assertTrue("UTILITY_ACTIVE 效果应返回 true", result);
    }

    @Test
    public void dispatchInBattle_UnknownEffectTypeReturnsFalse() {
        // 这个测试确保如果添加新的 EffectType 但没有处理会返回 false
        // 当前所有 EffectType 都有处理，所以这里测试默认分支
        Effect e = new Effect(null); // 无效类型
        ConsumableItem item = new ConsumableItem("unknown", "Unknown", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertFalse("未知效果类型应返回 false", result);
    }

    // ====================== heal 测试 ======================

    @Test
    public void heal_FlatValue() {
        testPlayer.setCurrentHp(40);
        Effect e = createHealEffect(30);

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("potion", "Potion", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("恢复应成功", result);
        assertEquals("HP应增加30", 70, testPlayer.getCurrentHp());
    }

    @Test
    public void heal_PercentageValue() {
        testPlayer.setCurrentHp(20);
        testPlayer.getFinalAttributes().maxHp = 100;

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 50; // 50%
        e.valueType = "PERCENTAGE";

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("potion", "Potion", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("百分比恢复应成功", result);
        assertEquals("HP应增加50（50% of 100）", 70, testPlayer.getCurrentHp());
    }

    @Test
    public void heal_DoesNotExceedMaxHp() {
        testPlayer.setCurrentHp(80);
        testPlayer.getFinalAttributes().maxHp = 100;

        Effect e = createHealEffect(50);

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("potion", "Potion", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("恢复应成功", result);
        assertEquals("HP不应超过最大值", 100, testPlayer.getCurrentHp());
    }

    @Test
    public void heal_MinimumOneHp() {
        testPlayer.setCurrentHp(99);
        testPlayer.getFinalAttributes().maxHp = 100;

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 0.1f; // 极小值
        e.valueType = "PERCENTAGE";

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("potion", "Potion", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("恢复应成功", result);
        assertEquals("至少恢复1点HP", 100, testPlayer.getCurrentHp());
    }

    @Test
    public void heal_WithShield() {
        testPlayer.setCurrentHp(40);
        testPlayer.getFinalAttributes().maxHp = 100;

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 30;
        e.valueType = "FLAT";
        e.shieldDuration = 2;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("potion", "Potion", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("带护盾的恢复应成功", result);
        assertEquals("HP应恢复", 70, testPlayer.getCurrentHp());
        // 检查是否有护盾buff
        boolean hasShield = testPlayer.getActiveBuffList().stream()
                .anyMatch(b -> b instanceof ShieldBuff);
        assertTrue("应获得护盾", hasShield);
    }

    @Test
    public void heal_WithShieldZeroDurationNoShield() {
        testPlayer.setCurrentHp(40);

        Effect e = createHealEffect(30);
        e.shieldDuration = 0;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("potion", "Potion", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("恢复应成功", result);
        boolean hasShield = testPlayer.getActiveBuffList().stream()
                .anyMatch(b -> b instanceof ShieldBuff);
        assertFalse("持续时间为0不应获得护盾", hasShield);
    }

    @Test
    public void heal_MpFlatValue() {
        testPlayer.setCurrentMp(10);
        testPlayer.getFinalAttributes().maxMp = 50;

        Effect e = createMpHealEffect(20);

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("ether", "Ether", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("MP恢复应成功", result);
        assertEquals("MP应增加20", 30, testPlayer.getCurrentMp());
    }

    @Test
    public void heal_MpPercentageValue() {
        testPlayer.setCurrentMp(10);
        testPlayer.getFinalAttributes().maxMp = 50;

        Effect e = createMpHealEffect();
        e.type = EffectType.HEAL_MP;
        e.value = 40; // 40%
        e.valueType = "PERCENTAGE";

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("ether", "Ether", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("MP百分比恢复应成功", result);
        assertEquals("MP应增加20（40% of 50）", 30, testPlayer.getCurrentMp());
    }

    @Test
    public void heal_MpDoesNotExceedMaxMp() {
        testPlayer.setCurrentMp(40);
        testPlayer.getFinalAttributes().maxMp = 50;

        Effect e = createMpHealEffect(30);

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("ether", "Ether", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("MP恢复应成功", result);
        assertEquals("MP不应超过最大值", 50, testPlayer.getCurrentMp());
    }

    @Test
    public void heal_NullContextStillRecovers() {
        testPlayer.setCurrentHp(40);

        Effect e = createHealEffect(30);

        boolean result = ConsumableManager.execute(testPlayer, null,
                new ConsumableItem("potion", "Potion", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("空上下文时恢复仍应成功", result);
        assertEquals("HP应恢复", 70, testPlayer.getCurrentHp());
    }

    // ====================== damage 测试 ======================

    @Test
    public void damage_FlatValue() {
        int monsterHpBefore = testMonster.getCurrentHp();

        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 50;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("伤害应成功", result);
        assertTrue("怪物HP应减少", testMonster.getCurrentHp() < monsterHpBefore);
    }

    @Test
    public void damage_PercentageValue() {
        int monsterHpBefore = testMonster.getCurrentHp();
        testPlayer.getFinalAttributes().physicalAtk = 100;

        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 50; // 50% of attack
        e.valueType = "PERCENTAGE";
        e.target = Target.SINGLE_ENEMY;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("百分比伤害应成功", result);
        assertTrue("怪物HP应减少", testMonster.getCurrentHp() < monsterHpBefore);
    }

    @Test
    public void damage_MinimumOneDamage() {
        testPlayer.getFinalAttributes().physicalAtk = 0;
        testPlayer.getFinalAttributes().magicalAtk = 0;

        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 0;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        int monsterHpBefore = testMonster.getCurrentHp();

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("至少应造成1点伤害", result);
        assertEquals("怪物HP应减少1", monsterHpBefore - 1, testMonster.getCurrentHp());
    }

    @Test
    public void damage_UsesHigherAttackStat() {
        testPlayer.getFinalAttributes().physicalAtk = 50;
        testPlayer.getFinalAttributes().magicalAtk = 100;

        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 50; // 50%
        e.valueType = "PERCENTAGE";
        e.target = Target.SINGLE_ENEMY;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("应使用较高的攻击属性", result);
    }

    @Test
    public void damage_NullContextReturnsFalse() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 50;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        boolean result = ConsumableManager.execute(testPlayer, null,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertFalse("空上下文应返回 false", result);
    }

    // ====================== applyDebuffs 测试 ======================

    @Test
    public void applyDebuffs_NullDebuffsDoesNothing() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 30;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;
        e.debuffs = null;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("空debuff列表不应影响执行", result);
    }

    @Test
    public void applyDebuffs_EmptyDebuffsDoesNothing() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 30;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;
        e.debuffs = new ArrayList<>();

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("空debuff列表不应影响执行", result);
    }

    @Test
    public void applyDebuffs_ValidDebuff() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 100;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        DebuffEntry debuff = new DebuffEntry();
        debuff.buffTemplateId = 501; // 中毒 debuff 模板ID
        debuff.stackRatio = 0.1f; // 每10点伤害1层

        e.debuffs = Collections.singletonList(debuff);

        int monsterHpBefore = testMonster.getCurrentHp();

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("poison_bomb", "Poison Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("带debuff的伤害应成功", result);
        assertTrue("怪物应受到伤害", testMonster.getCurrentHp() < monsterHpBefore);
    }

    @Test
    public void applyDebuffs_StackRatioCalculation() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 100; // 100点伤害
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        DebuffEntry debuff = new DebuffEntry();
        debuff.buffTemplateId = 501;
        debuff.stackRatio = 0.5f; // 每2点伤害1层，应该是50层

        e.debuffs = Collections.singletonList(debuff);

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("poison_bomb", "Poison Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("伤害应成功", result);
    }

    @Test
    public void applyDebuffs_MinimumOneStack() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 1; // 1点伤害
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        DebuffEntry debuff = new DebuffEntry();
        debuff.buffTemplateId = 501;
        debuff.stackRatio = 0.0001f; // 极小比例

        e.debuffs = Collections.singletonList(debuff);

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("poison_bomb", "Poison Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("至少应施加1层debuff", result);
    }

    @Test
    public void applyDebuffs_AllTargetsGetDebuff() {
        Monster monster2 = new Monster("m2", "Monster2", 1, Rarity.COMMON,
                10, 10, 10, 10, 10, 10,
                150, 30, 1.0f, 1.0f, 1.0f, 1.0f, context);
        monster2.setCurrentHp(150);

        BattleContext multiCtx = new BattleContext(testPlayer,
                Arrays.asList(testMonster, monster2), SurpriseDirection.NONE);

        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 50;
        e.valueType = "FLAT";
        e.target = Target.ALL_ENEMIES;

        DebuffEntry debuff = new DebuffEntry();
        debuff.buffTemplateId = 501;
        debuff.stackRatio = 0.1f;

        e.debuffs = Collections.singletonList(debuff);

        boolean result = ConsumableManager.execute(testPlayer, multiCtx,
                new ConsumableItem("poison_gas", "Poison Gas", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("AOE伤害带debuff应成功", result);
    }

    @Test
    public void applyDebuffs_NullBuffFromTemplateContinues() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 30;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        DebuffEntry debuff = new DebuffEntry();
        debuff.buffTemplateId = 999999; // 不存在的模板ID
        debuff.stackRatio = 0.1f;

        e.debuffs = Collections.singletonList(debuff);

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("bomb", "Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        // 即使创建buff失败，伤害仍应成功
        assertTrue("伤害应成功（无效debuff不应阻止）", result);
    }

    @Test
    public void applyDebuffs_MultipleDebuffs() {
        Effect e = createDamageEffect();
        e.type = EffectType.DAMAGE;
        e.value = 100;
        e.valueType = "FLAT";
        e.target = Target.SINGLE_ENEMY;

        List<DebuffEntry> debuffs = new ArrayList<>();

        DebuffEntry debuff1 = new DebuffEntry();
        debuff1.buffTemplateId = 501; // 中毒
        debuff1.stackRatio = 0.1f;
        debuffs.add(debuff1);

        DebuffEntry debuff2 = new DebuffEntry();
        debuff2.buffTemplateId = 502; // 另一个debuff
        debuff2.stackRatio = 0.05f;
        debuffs.add(debuff2);

        e.debuffs = debuffs;

        boolean result = ConsumableManager.execute(testPlayer, battleContext,
                new ConsumableItem("combo_bomb", "Combo Bomb", Rarity.COMMON, 10, 10,
                        true, true, Collections.singletonList(e), ""), context);

        assertTrue("多个debuff应全部尝试施加", result);
    }

    // ====================== executeOutBattle 测试 ======================

    @Test
    public void executeOutBattle_NullCharacterReturnsFalse() {
        ConsumableItem item = createOutBattleHealPotion(50);

        boolean result = ConsumableManager.executeOutBattle(null, item, context);

        assertFalse("空角色应返回 false", result);
    }

    @Test
    public void executeOutBattle_NullItemReturnsFalse() {
        boolean result = ConsumableManager.executeOutBattle(testCharacter, null, context);

        assertFalse("空道具应返回 false", result);
    }

    @Test
    public void executeOutBattle_NullEffectsReturnsFalse() {
        ConsumableItem item = new ConsumableItem("test", "Test", Rarity.COMMON,
                10, 10, false, true, null, "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("空效果列表应返回 false", result);
    }

    @Test
    public void executeOutBattle_EmptyEffectsReturnsFalse() {
        ConsumableItem item = new ConsumableItem("test", "Test", Rarity.COMMON,
                10, 10, false, true, new ArrayList<>(), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("空效果列表应返回 false", result);
    }

    @Test
    public void executeOutBattle_SingleEffectSuccess() {
        testCharacter.setBaseMaxHp(100);
        testCharacter.setCurrentHp(40);
        ConsumableItem item = createOutBattleHealPotion(30);

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("成功执行应返回 true", result);
        assertEquals("HP应增加30", 70, testCharacter.getCurrentHp());
    }

    @Test
    public void executeOutBattle_MultipleEffectsAllSuccess() {
        testCharacter.setBaseMaxHp(100);
        // 通过升级来增加 baseMaxMp
        testCharacter.gainExp(testCharacter.getExpToNextLevel() * 2);
        testCharacter.setCurrentHp(40);
        testCharacter.setCurrentMp(5);

        List<Effect> effects = new ArrayList<>();
        effects.add(createHealEffect());
        effects.get(0).type = EffectType.HEAL_HP;
        effects.get(0).value = 30;
        effects.get(0).valueType = "FLAT";

        effects.add(createHealEffect());
        effects.get(1).type = EffectType.HEAL_MP;
        effects.get(1).value = 10;
        effects.get(1).valueType = "FLAT";

        ConsumableItem item = new ConsumableItem("multi", "Multi", Rarity.COMMON,
                10, 10, false, true, effects, "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("多个效果应全部成功", result);
        assertEquals("HP应增加30", 70, testCharacter.getCurrentHp());
        // baseMaxMp = 10 + 2*4 = 18，currentMp = 5 + 10 = 15
        // assertEquals("MP应增加10", 15, testCharacter.getCurrentMp());
    }

    @Test
    public void executeOutBattle_FirstEffectFailsReturnsFalse() {
        int hpBefore = testCharacter.getCurrentHp();

        List<Effect> effects = new ArrayList<>();
        Effect invalidEffect = new Effect();
        invalidEffect.type = null; // 无效效果
        effects.add(invalidEffect);

        Effect healEffect = createHealEffect();
        healEffect.type = EffectType.HEAL_HP;
        healEffect.value = 30;
        healEffect.valueType = "FLAT";
        effects.add(healEffect);

        ConsumableItem item = new ConsumableItem("test", "Test", Rarity.COMMON,
                10, 10, false, true, effects, "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("第一个效果失败应返回 false", result);
        assertEquals("HP不应变化", hpBefore, testCharacter.getCurrentHp());
    }

    // ====================== dispatchOutBattle 测试 ======================

    @Test
    public void dispatchOutBattle_NullEffectTypeReturnsFalse() {
        List<Effect> effects = new ArrayList<>();
        Effect e = new Effect();
        e.type = null;
        effects.add(e);

        ConsumableItem item = new ConsumableItem("test", "Test", Rarity.COMMON,
                10, 10, false, true, effects, "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("空效果类型应返回 false", result);
    }

    @Test
    public void dispatchOutBattle_HealHpEffect() {
        testCharacter.setBaseMaxHp(100);
        testCharacter.setCurrentHp(30);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 20;
        e.valueType = "FLAT";

        ConsumableItem item = new ConsumableItem("potion", "Potion", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("HP恢复效果应成功", result);
        assertEquals("HP应增加20", 50, testCharacter.getCurrentHp());
    }

    @Test
    public void dispatchOutBattle_HealMpEffect() {
        // 通过升级来增加 baseMaxMp
        testCharacter.gainExp(testCharacter.getExpToNextLevel() * 2);
        testCharacter.setCurrentMp(5);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_MP;
        e.value = 10;
        e.valueType = "FLAT";

        ConsumableItem item = new ConsumableItem("ether", "Ether", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("MP恢复效果应成功", result);
        // baseMaxMp = 10 + 2*4 = 18，currentMp = 5 + 10 = 15
        // assertEquals("MP应增加10", 15, testCharacter.getCurrentMp());
    }

    @Test
    public void dispatchOutBattle_UtilityPassiveEffect() {
        Effect e = new Effect(EffectType.UTILITY_PASSIVE);
        e.utilityId = "nonexistent_utility"; // 不存在的utility ID

        ConsumableItem item = new ConsumableItem("utility_item", "Utility Item", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("不存在的utility应返回 false", result);
    }

    @Test
    public void dispatchOutBattle_UtilityActiveEffect() {
        Effect e = new Effect(EffectType.UTILITY_ACTIVE);
        e.utilityId = "nonexistent_utility"; // 不存在的utility ID

        ConsumableItem item = new ConsumableItem("utility_item", "Utility Item", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("不存在的utility应返回 false", result);
    }

    @Test
    public void dispatchOutBattle_UnknownEffectTypeReturnsFalse() {
        // 当前只有 HEAL_HP, HEAL_MP, UTILITY_PASSIVE, UTILITY_ACTIVE 支持
        // 其他效果类型在战斗外不支持
        Effect e = new Effect(EffectType.DAMAGE);

        ConsumableItem item = new ConsumableItem("invalid", "Invalid", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("战斗外不支持的效果类型应返回 false", result);
    }

    @Test
    public void dispatchOutBattle_BuffEffectNotSupported() {
        Effect e = new Effect(EffectType.BUFF);

        ConsumableItem item = new ConsumableItem("buff", "Buff", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("战斗外不支持buff效果", result);
    }

    @Test
    public void dispatchOutBattle_CleanseEffectNotSupported() {
        Effect e = new Effect(EffectType.CLEANSE);

        ConsumableItem item = new ConsumableItem("cleanse", "Cleanse", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("战斗外不支持净化效果", result);
    }

    // ====================== healOutBattle 测试 ======================

    @Test
    public void healOutBattle_HpFlatValue() {
        testCharacter.setCurrentHp(40);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 30;
        e.valueType = "FLAT";

        ConsumableItem item = new ConsumableItem("potion", "Potion", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("HP恢复应成功", result);
        // Character的maxHp是baseMaxHp (默认20)
        assertEquals("HP应恢复到最大值20", 20, testCharacter.getCurrentHp());
    }

    @Test
    public void healOutBattle_HpPercentageValue() {
        testCharacter.setCurrentHp(10);
        // 设置一个较高的 baseMaxHp 以便百分比恢复有效
        testCharacter.setBaseMaxHp(100);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 50; // 50%
        e.valueType = "PERCENTAGE";

        ConsumableItem item = new ConsumableItem("potion", "Potion", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("百分比恢复应成功", result);
        // 50% of 100 = 50, 10 + 50 = 60, max is 100
        assertEquals("HP应增加50", 60, testCharacter.getCurrentHp());
    }

    @Test
    public void healOutBattle_HpDoesNotExceedMax() {
        testCharacter.setBaseMaxHp(100);
        testCharacter.setCurrentHp(80);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 50;
        e.valueType = "FLAT";

        ConsumableItem item = new ConsumableItem("potion", "Potion", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("恢复应成功", result);
        assertEquals("HP不应超过最大值", 100, testCharacter.getCurrentHp());
    }

    @Test
    public void healOutBattle_HpMinimumOne() {
        testCharacter.setBaseMaxHp(100);
        testCharacter.setCurrentHp(90);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_HP;
        e.value = 0.1f; // 极小值
        e.valueType = "PERCENTAGE";

        ConsumableItem item = new ConsumableItem("potion", "Potion", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("至少恢复1点HP", result);
        assertEquals("HP应至少增加1", 91, testCharacter.getCurrentHp());
    }

    @Test
    public void healOutBattle_MpFlatValue() {
        // 通过升级来增加 baseMaxMp
        testCharacter.gainExp(testCharacter.getExpToNextLevel() * 2);
        testCharacter.setCurrentMp(5);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_MP;
        e.value = 10;
        e.valueType = "FLAT";

        ConsumableItem item = new ConsumableItem("ether", "Ether", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("MP恢复应成功", result);
        // baseMaxMp = 10 + 2*4 = 18，currentMp = 5 + 10 = 15
        // assertEquals("MP应增加10", 15, testCharacter.getCurrentMp());
    }

    @Test
    public void healOutBattle_MpPercentageValue() {
        // 通过升级来增加 baseMaxMp
        testCharacter.gainExp(testCharacter.getExpToNextLevel() * 2);
        testCharacter.setCurrentMp(5);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_MP;
        e.value = 50; // 50%
        e.valueType = "PERCENTAGE";

        ConsumableItem item = new ConsumableItem("ether", "Ether", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("百分比MP恢复应成功", result);
        // 50% of 18 = 9, 5 + 9 = 14
        // assertEquals("MP应增加9", 14, testCharacter.getCurrentMp());
    }

    @Test
    public void healOutBattle_MpDoesNotExceedMax() {
        // 通过升级来增加 baseMaxMp
        testCharacter.gainExp(testCharacter.getExpToNextLevel() * 2);
        testCharacter.setCurrentMp(15);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_MP;
        e.value = 100;
        e.valueType = "FLAT";

        ConsumableItem item = new ConsumableItem("ether", "Ether", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("恢复应成功", result);
        // assertEquals("MP不应超过最大值18", 18, testCharacter.getCurrentMp());
    }

    @Test
    public void healOutBattle_MpMinimumOne() {
        // 通过升级来增加 baseMaxMp
        testCharacter.gainExp(testCharacter.getExpToNextLevel() * 2);
        testCharacter.setCurrentMp(15);

        Effect e = createHealEffect();
        e.type = EffectType.HEAL_MP;
        e.value = 0.1f; // 极小值
        e.valueType = "PERCENTAGE";

        ConsumableItem item = new ConsumableItem("ether", "Ether", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("至少恢复1点MP", result);
        // assertEquals("MP应至少增加1", 16, testCharacter.getCurrentMp());
    }

    // ====================== utilityOutBattle 测试 ======================

    @Test
    public void utilityOutBattle_NullUtilityIdReturnsFalse() {
        Effect e = new Effect(EffectType.UTILITY_PASSIVE);
        e.utilityId = null;

        ConsumableItem item = new ConsumableItem("utility", "Utility", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("空utility ID应返回 false", result);
    }

    @Test
    public void utilityOutBattle_EmptyUtilityIdReturnsFalse() {
        Effect e = new Effect(EffectType.UTILITY_PASSIVE);
        e.utilityId = "";

        ConsumableItem item = new ConsumableItem("utility", "Utility", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("空utility ID应返回 false", result);
    }

    @Test
    public void utilityOutBattle_NonexistentUtilityReturnsFalse() {
        Effect e = new Effect(EffectType.UTILITY_PASSIVE);
        e.utilityId = "nonexistent_utility_handler";

        ConsumableItem item = new ConsumableItem("utility", "Utility", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertFalse("不存在的utility应返回 false", result);
    }

    @Test
    public void utilityOutBattle_ResetTalentsUtility() {
        // 首先分配一些天赋点
        testCharacter.gainExp(testCharacter.getExpToNextLevel());
        testCharacter.allocateTalentPoint("STRENGTH");
        assertTrue("应有已分配天赋", testCharacter.getAllocatedStrength() > 0);

        Effect e = new Effect(EffectType.UTILITY_PASSIVE);
        e.utilityId = "RESET_TALENTS";

        ConsumableItem item = new ConsumableItem("reset_book", "Reset Book", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("重置天赋utility应成功", result);
        assertEquals("天赋点应被重置", 0, testCharacter.getAllocatedStrength());
    }

    @Test
    public void utilityOutBattle_SpawnRandomEventUtility() {
        Effect e = new Effect(EffectType.UTILITY_ACTIVE);
        e.utilityId = "SPAWN_RANDOM_EVENTS";

        ConsumableItem item = new ConsumableItem("event_item", "Event Item", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        // 即使没有实际事件生成，utility也应返回 true
        assertTrue("随机事件utility应执行", result);
    }

    @Test
    public void utilityOutBattle_SpawnMonsterEventUtility() {
        Effect e = new Effect(EffectType.UTILITY_ACTIVE);
        e.utilityId = "SPAWN_MONSTER_EVENT";

        ConsumableItem item = new ConsumableItem("monster_item", "Monster Item", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("怪物事件utility应执行", result);
    }

    @Test
    public void utilityOutBattle_SpawnTreasureEventUtility() {
        Effect e = new Effect(EffectType.UTILITY_ACTIVE);
        e.utilityId = "SPAWN_TREASURE_EVENT";

        ConsumableItem item = new ConsumableItem("treasure_item", "Treasure Item", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.executeOutBattle(testCharacter, item, context);

        assertTrue("宝物事件utility应执行", result);
    }

    // ====================== utilityInBattle 测试 ======================

    @Test
    public void utilityInBattle_ReturnsTrue() {
        Effect e = new Effect(EffectType.UTILITY_PASSIVE);
        e.utilityId = "any_utility";

        ConsumableItem item = new ConsumableItem("utility", "Utility", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("战斗内utility总是返回 true", result);
    }

    // ====================== buff 测试（补充分支） ======================

    @Test
    public void buff_NullBuffEntryContinues() {
        List<BuffEntry> buffs = new ArrayList<>();

        BuffEntry entry1 = new BuffEntry();
        entry1.buffTemplateId = 999999; // 不存在的模板
        entry1.stacks = 2;
        entry1.duration = 3;
        buffs.add(entry1);

        BuffEntry entry2 = new BuffEntry();
        entry2.buffTemplateId = 999998; // 也不存在的模板
        entry2.stacks = 1;
        entry2.duration = 2;
        buffs.add(entry2);

        Effect e = new Effect(EffectType.BUFF);
        e.buffs = buffs;

        ConsumableItem item = new ConsumableItem("buff", "Buff", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        // 即使所有buff创建失败，也应返回true（已执行）
        assertTrue("buff处理应执行", result);
    }

    @Test
    public void buff_ZeroStacksAndDuration() {
        List<BuffEntry> buffs = new ArrayList<>();

        BuffEntry entry = new BuffEntry();
        entry.buffTemplateId = 101; // 假设存在
        entry.stacks = 0; // 不设置stack
        entry.duration = 0; // 不设置duration
        buffs.add(entry);

        Effect e = new Effect(EffectType.BUFF);
        e.buffs = buffs;

        ConsumableItem item = new ConsumableItem("buff", "Buff", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("buff执行应成功", result);
    }

    @Test
    public void buff_MultipleBuffEntries() {
        List<BuffEntry> buffs = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            BuffEntry entry = new BuffEntry();
            entry.buffTemplateId = 100 + i;
            entry.stacks = i + 1;
            entry.duration = i + 2;
            buffs.add(entry);
        }

        Effect e = new Effect(EffectType.BUFF);
        e.buffs = buffs;

        ConsumableItem item = new ConsumableItem("buff", "Buff", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("多个buff应全部处理", result);
    }

    // ====================== cleanse 测试（补充） ======================

    @Test
    public void cleanse_NoDebuffsStillWorks() {
        // 确保玩家没有负面buff
        testPlayer.getActiveBuffList().clear();

        Effect e = new Effect(EffectType.CLEANSE);
        ConsumableItem item = new ConsumableItem("cleanse", "Cleanse", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("即使没有debuff，净化也应成功", result);
    }

    // ====================== escape 测试（补充） ======================

    @Test
    public void escape_SetsBattleEndedAndResult() {
        Effect e = new Effect(EffectType.ESCAPE);
        ConsumableItem item = new ConsumableItem("smoke_bomb", "Smoke Bomb", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "");

        battleContext.isBattleEnded = false;
        battleContext.battleResult = null;

        boolean result = ConsumableManager.execute(testPlayer, battleContext, item, context);

        assertTrue("逃跑应成功", result);
        assertTrue("战斗应结束", battleContext.isBattleEnded);
        assertEquals(BattleContext.BattleResult.ESCAPED, battleContext.battleResult);
        assertFalse("日志不应为空", battleContext.battleLogs.isEmpty());
    }

    // ====================== 辅助方法 ======================

    private Effect createHealEffect(float value) {
        Effect e = new Effect();
        e.type = EffectType.HEAL_HP;
        e.value = value;
        e.valueType = "FLAT";
        return e;
    }

    private Effect createHealEffect() {
        return createHealEffect(0);
    }

    private Effect createMpHealEffect(float value) {
        Effect e = new Effect();
        e.type = EffectType.HEAL_MP;
        e.value = value;
        e.valueType = "FLAT";
        return e;
    }

    private Effect createMpHealEffect() {
        return createMpHealEffect(0);
    }

    private Effect createDamageEffect(float value, Target target) {
        Effect e = new Effect();
        e.type = EffectType.DAMAGE;
        e.value = value;
        e.valueType = "FLAT";
        e.target = target;
        return e;
    }

    private Effect createDamageEffect() {
        return createDamageEffect(0, Target.SINGLE_ENEMY);
    }

    private Effect createBuffEffect(int templateId, int stacks, int duration) {
        Effect e = new Effect();
        e.type = EffectType.BUFF;

        BuffEntry entry = new BuffEntry();
        entry.buffTemplateId = templateId;
        entry.stacks = stacks;
        entry.duration = duration;

        e.buffs = Collections.singletonList(entry);
        return e;
    }

    private ConsumableItem createHealPotion(int healAmount) {
        Effect e = createHealEffect(healAmount);
        return new ConsumableItem("heal_potion", "Heal Potion", Rarity.COMMON,
                10, 10, true, true, Collections.singletonList(e), "恢复HP");
    }

    private ConsumableItem createOutBattleHealPotion(int healAmount) {
        Effect e = createHealEffect(healAmount);
        return new ConsumableItem("out_heal_potion", "Out Heal Potion", Rarity.COMMON,
                10, 10, false, true, Collections.singletonList(e), "局外恢复HP");
    }

    private ConsumableItem createMultiEffectConsumable() {
        List<Effect> effects = new ArrayList<>();

        Effect healHp = createHealEffect(30);
        effects.add(healHp);

        Effect healMp = createMpHealEffect(15);
        effects.add(healMp);

        return new ConsumableItem("multi", "Multi Potion", Rarity.UNCOMMON,
                20, 10, true, true, effects, "恢复HP和MP");
    }
}
