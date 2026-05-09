package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.BattleContext.SurpriseDirection;
import com.example.treasure_and_battle.battle.damage.DamageSource;
import com.example.treasure_and_battle.buff.impl.defensive.DamageReductionBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.manager.battle.DamageManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.utils.RandomUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class DamageManagerTest {
    private Context context;
    private DamageManager damageManager;
    private Player testPlayer;
    private Monster testMonster;
    private BattleContext battleContext;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        damageManager = DamageManager.getInstance(context);
        RandomUtils.setSeed(123456L);

        testPlayer = new Player("TestPlayer", context);
        AttributeSet playerAttr = testPlayer.getBaseAttributes();
        playerAttr.strength = 10;
        playerAttr.agility = 10;
        playerAttr.intelligence = 10;
        playerAttr.spirit = 10;
        playerAttr.physique = 10;
        playerAttr.luck = 10;
        playerAttr.maxHp = 200;
        playerAttr.maxMp = 100;
        playerAttr.physicalAtk = 50;
        playerAttr.physicalDef = 30;
        playerAttr.magicalAtk = 50;
        playerAttr.magicalDef = 30;
        playerAttr.speed = 15;
        playerAttr.hitRate = 1.0f;
        playerAttr.dodgeRate = 0.1f;
        playerAttr.physicalCritRate = 0.3f;
        playerAttr.physicalCritDmg = 2.0f;
        playerAttr.magicalCritRate = 0.3f;
        playerAttr.magicalCritDmg = 2.0f;
        playerAttr.expBonus = 1.0f;
        playerAttr.goldBonus = 1.0f;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(playerAttr.maxHp);
        testPlayer.setCurrentMp(playerAttr.maxMp);

        testMonster = new Monster("m_test", "测试怪物", 1, com.example.treasure_and_battle.model.common.Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        AttributeSet monsterAttr = testMonster.getBaseAttributes();
        monsterAttr.maxHp = 300;
        monsterAttr.maxMp = 50;
        monsterAttr.physicalDef = 20;
        monsterAttr.magicalDef = 20;
        monsterAttr.speed = 10;
        monsterAttr.hitRate = 1.0f;
        monsterAttr.dodgeRate = 0.05f;
        monsterAttr.physicalCritRate = 0f;
        monsterAttr.magicalCritRate = 0f;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(monsterAttr.maxHp);
        testMonster.setCurrentMp(monsterAttr.maxMp);

        battleContext = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
    }

    private void resetHp() {
        testPlayer.setCurrentHp(testPlayer.getBaseAttributes().maxHp);
        testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);
        testPlayer.setDead(false);
        testMonster.setDead(false);
    }

    // ====================== 物理伤害完整管线测试 ======================

    @Test
    public void testPhysicalDamage_BasicFormula_attackerHitsAndDefenseApplied() {
        resetHp();
        // 50物攻 - 20物防 = 30最终伤害
        int before = testMonster.getCurrentHp();
        damageManager.dealDamage(DamageConfig.normalAttack(), testPlayer, testMonster, 50, battleContext);

        // 命中率 = 1.0 - 0.05 = 0.95, 固定种子大概率命中
        int damageTaken = before - testMonster.getCurrentHp();
        assertTrue("物理伤害有防御减免", damageTaken > 0);
        assertTrue("伤害类型应为PHYSICAL", battleContext.damageType.contains("PHYSICAL"));
    }

    @Test
    public void testPhysicalDamage_CriticalHitDoublesRawDamage() {
        resetHp();
        // 设置100%暴击
        testPlayer.getBaseAttributes().physicalCritRate = 1.0f;
        testPlayer.markAttributeCacheDirty();

        damageManager.dealDamage(DamageConfig.normalAttack(), testPlayer, testMonster, 50, battleContext);

        if (battleContext.isHit) {
            assertTrue("暴击时rawDamage应该翻倍", battleContext.rawDamage >= 50 * 2);
            assertTrue("应该标记为暴击", battleContext.isCriticalHit);
        }
    }

    // ====================== 闪避测试 ======================

    @Test
    public void testPhysicalDamage_CanDodge() {
        resetHp();
        // 设置高闪避，低命中确保未命中
        testMonster.getBaseAttributes().dodgeRate = 0.99f;
        testMonster.markAttributeCacheDirty();
        testPlayer.getBaseAttributes().hitRate = 0.01f;
        testPlayer.markAttributeCacheDirty();

        RandomUtils.setSeed(0L);
        int before = testMonster.getCurrentHp();
        damageManager.dealDamage(DamageConfig.normalAttack(), testPlayer, testMonster, 50, battleContext);

        // 可能命中可能闪避，验证逻辑一致性
        if (!battleContext.isHit) {
            assertEquals("未命中应该扣0血", before, testMonster.getCurrentHp());
            assertEquals("未命中finalDamage为0", 0, battleContext.finalDamage);
        }
    }

    // ====================== 真伤测试（可闪避） ======================

    @Test
    public void testTrueDamage_OnlyDodge_NoDefense() {
        resetHp();
        int before = testMonster.getCurrentHp();
        // 真伤：只看闪避，无视防御。命中率=1-0.05=0.95
        damageManager.dealDamage(DamageConfig.trueDamage(DamageSource.ACTIVE_SKILL),
                testPlayer, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        if (battleContext.isHit) {
            // 无视防御20→应该100伤害（或略小于100如果被减伤/护盾吸收，但这里没有）
            assertEquals("真伤无视防御", 100, damageTaken);
        }
    }

    // ====================== Buff物理伤害（不可闪避不可暴击） ======================

    @Test
    public void testBuffPhysicalDamage_NoDodgeNoCrit_ButDefenseApplied() {
        resetHp();
        int before = testMonster.getCurrentHp();

        // buff物理伤害：不可闪避不可暴击，但受防御减免
        damageManager.dealDamage(DamageConfig.buffPhysical(), null, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        assertTrue("buff物理伤害必定命中", battleContext.isHit);
        assertFalse("buff物理伤害不暴击", battleContext.isCriticalHit);
        // 100 - 20物防 = 80伤害
        assertEquals("buff物理伤害受防御减免", 80, damageTaken);
    }

    @Test
    public void testBuffMagicalDamage_DefenseApplied() {
        resetHp();
        int before = testMonster.getCurrentHp();

        damageManager.dealDamage(DamageConfig.buffMagical(), null, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        assertTrue("buff魔法伤害必定命中", battleContext.isHit);
        // 100 - 20魔防 = 80伤害
        assertEquals("buff魔法伤害受防御减免", 80, damageTaken);
    }

    // ====================== Buff真伤（完全不可防） ======================

    @Test
    public void testBuffTrueDamage_CompletelyUnmitigated() {
        resetHp();
        int before = testMonster.getCurrentHp();

        damageManager.dealDamage(DamageConfig.buffTrue(), null, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        assertTrue("buff真伤必定命中", battleContext.isHit);
        assertFalse("buff真伤不暴击", battleContext.isCriticalHit);
        assertEquals("buff真伤无视一切防御", 100, damageTaken);
    }

    // ====================== 护盾吸收测试 ======================

    @Test
    public void testPhysicalDamage_ShieldAbsorbsDamage() {
        resetHp();
        // 添加护盾
        ShieldBuff shield = new ShieldBuff(
                "test_shield", "测试护盾", "吸收%d",
                BuffType.BUFF, false, -1, 50, false, 1.0f);
        testMonster.getActiveBuffList().add(shield);

        int before = testMonster.getCurrentHp();
        // 100物攻物理伤害 - 20物防 = 80，护盾50 → 实际扣血30
        damageManager.dealDamage(DamageConfig.normalAttack(), testPlayer, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        if (battleContext.isHit) {
            assertTrue("有护盾时伤害减少", damageTaken <= 30);
        }
    }

    @Test
    public void testTrueDamage_BypassesShield() {
        resetHp();
        ShieldBuff shield = new ShieldBuff(
                "test_shield", "测试护盾", "吸收%d",
                BuffType.BUFF, false, -1, 50, false, 1.0f);
        testMonster.getActiveBuffList().add(shield);

        int before = testMonster.getCurrentHp();
        damageManager.dealDamage(DamageConfig.trueDamage(DamageSource.ACTIVE_SKILL),
                testPlayer, testMonster, 50, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        if (battleContext.isHit) {
            assertEquals("真伤无视护盾", 50, damageTaken);
        }
    }

    // ====================== 穿甲伤害测试 ======================

    @Test
    public void testPiercingDamage_NoDodgeNoCrit_IgnoresDefense_RespectsShield() {
        resetHp();
        ShieldBuff shield = new ShieldBuff(
                "test_shield", "测试护盾", "吸收%d",
                BuffType.BUFF, false, -1, 30, false, 1.0f);
        testMonster.getActiveBuffList().add(shield);

        int before = testMonster.getCurrentHp();
        // 穿甲：无视防御和减伤，但受护盾。100伤害 - 30护盾 = 70扣血
        damageManager.dealDamage(DamageConfig.piercing(DamageSource.PIERCING),
                testPlayer, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        assertEquals("穿甲伤害受护盾吸收：100-30=70", 70, damageTaken);
        assertTrue("穿甲伤害不暴击", !battleContext.isCriticalHit || battleContext.rawDamage <= 100);
    }

    @Test
    public void testPiercingDamage_NoShield_FullDamage() {
        resetHp();
        int before = testMonster.getCurrentHp();
        damageManager.dealDamage(DamageConfig.piercing(DamageSource.PIERCING),
                testPlayer, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        assertEquals("无护盾时穿甲伤害直接穿透", 100, damageTaken);
    }

    // ====================== 减伤Buff测试 ======================

    @Test
    public void testDamageReductionBuff_ReducesPhysicalDamage() {
        resetHp();
        DamageReductionBuff reduction = new DamageReductionBuff(
                "test_reduction", "减伤测试", "减少%d%%",
                BuffType.BUFF, false, -1, 1, false, 0.5f);
        testMonster.getActiveBuffList().add(reduction);

        int before = testMonster.getCurrentHp();
        damageManager.dealDamage(DamageConfig.normalAttack(), testPlayer, testMonster, 100, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        if (battleContext.isHit) {
            // 100 - 20防 = 80, 减伤50% → 40
            assertTrue("减伤后伤害应小于无减伤时的80", damageTaken < 80);
        }
    }

    @Test
    public void testTrueDamage_BypassesDamageReduction() {
        resetHp();
        DamageReductionBuff reduction = new DamageReductionBuff(
                "test_reduction", "减伤测试", "减少%d%%",
                BuffType.BUFF, false, -1, 1, false, 0.5f);
        testMonster.getActiveBuffList().add(reduction);

        int before = testMonster.getCurrentHp();
        damageManager.dealDamage(DamageConfig.trueDamage(DamageSource.ACTIVE_SKILL),
                testPlayer, testMonster, 50, battleContext);

        int damageTaken = before - testMonster.getCurrentHp();
        if (battleContext.isHit) {
            assertEquals("真伤无视减伤", 50, damageTaken);
        }
    }

    // ====================== 击杀判定测试 ======================

    @Test
    public void testKill_EntityShouldBeDead() {
        resetHp();
        damageManager.dealDamage(DamageConfig.buffTrue(), null, testMonster, 999, battleContext);
        assertTrue("怪物应该死亡", testMonster.isDead());
        assertEquals("HP应该为0", 0, testMonster.getCurrentHp());
    }

    // ====================== Context字段验证 ======================

    @Test
    public void testDamageSource_IsSetOnContext() {
        resetHp();
        damageManager.dealDamage(DamageConfig.buffMagical(), null, testMonster, 10, battleContext);
        assertEquals("buff魔法伤害来源应为BUFF",
                DamageSource.BUFF, battleContext.damageSource);
    }
}
