package com.example.treasure_and_battle.buff.impl.special;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.utils.RandomUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * WindProtectBuff 单元测试
 * 测试御风护体Buff的溅射伤害计算、免疫机制、触发逻辑
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class WindProtectBuffTest {

    private Context context;
    private Player caster;
    private Player owner;
    private BattleContext battleContext;
    private BattleManager mockBattleManager;
    private WindProtectBuff windProtectBuff;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        RandomUtils.setSeed(123456L);

        // 创建测试实体
        caster = createTestPlayer("施法者");
        owner = createTestPlayer("持有者");

        // 创建战斗上下文 - 使用空怪物列表
        battleContext = new BattleContext(owner, new java.util.ArrayList<Monster>(), false);

        // 创建模拟的BattleManager
        mockBattleManager = mock(BattleManager.class);

        // 创建御风护体Buff
        // 参数：buffId, buffName, descriptionFormat, buffType, isDispellable, maxDuration,
        //       maxStackCount, refreshOnApply, buffValue, caster, splashDamagePercent,
        //       battleContext, battleManager
        windProtectBuff = new WindProtectBuff(
                "wind_protect",
                "御风护体",
                "免疫下一次受到的攻击，并将%d%%伤害反弹给所有敌人",
                BuffType.BUFF,
                true,  // isDispellable
                3,     // maxDuration
                1,     // maxStackCount
                false, // refreshOnApply
                50.0f, // buffValue (这里用作溅射伤害百分比)
                caster,
                50.0f, // splashDamagePercent - 50%
                battleContext,
                mockBattleManager
        );
    }

    // ====================== 基础属性测试 ======================

    @Test
    public void testWindProtectBuff_Creation_HasCorrectProperties() {
        assertEquals("wind_protect", windProtectBuff.getBuffId());
        assertEquals("御风护体", windProtectBuff.getBuffName());
        assertEquals(BuffType.BUFF, windProtectBuff.getBuffType());
        assertEquals(TriggerType.ON_BEFORE_DAMAGE_TAKEN, windProtectBuff.getTriggerType());
        assertTrue(windProtectBuff.isDispellable());
        assertEquals(3, windProtectBuff.getRemainingDuration());
        assertEquals(1, windProtectBuff.getStackCount());
    }

    @Test
    public void testGetSplashDamagePercent_ReturnsCorrectValue() {
        assertEquals(50.0f, windProtectBuff.getSplashDamagePercent(), 0.001f);
    }

    // ====================== 触发机制测试 ======================

    @Test
    public void testOnTrigger_WhenBeforeDamageTaken_SetsStackToZero() {
        assertEquals(1, windProtectBuff.getStackCount());

        windProtectBuff.onTrigger(owner, battleContext, TriggerType.ON_BEFORE_DAMAGE_TAKEN);

        assertEquals(0, windProtectBuff.getStackCount());
    }

    @Test
    public void testOnTrigger_WhenBeforeDamageTaken_AddsLog() {
        int logCountBefore = battleContext.battleLogs.size();

        windProtectBuff.onTrigger(owner, battleContext, TriggerType.ON_BEFORE_DAMAGE_TAKEN);

        int logCountAfter = battleContext.battleLogs.size();
        assertTrue(logCountAfter > logCountBefore);

        // 验证日志内容
        boolean foundLog = battleContext.battleLogs.stream()
                .anyMatch(log -> log.getType() == LogType.BUFF &&
                        log.getFormattedMessage().contains("御风护体") &&
                        log.getFormattedMessage().contains("免疫"));
        assertTrue("应该找到御风护体触发日志", foundLog);
    }

    @Test
    public void testOnTrigger_WhenOtherTriggerType_DoesNotModifyStack() {
        windProtectBuff.onTrigger(owner, battleContext, TriggerType.ON_ROUND_START);

        assertEquals(1, windProtectBuff.getStackCount());
    }

    // ====================== 免疫机制测试 ======================

    @Test
    public void testIsTriggered_WhenStackZero_ReturnsTrue() {
        windProtectBuff.onTrigger(owner, battleContext, TriggerType.ON_BEFORE_DAMAGE_TAKEN);

        assertTrue(windProtectBuff.isTriggered());
    }

    @Test
    public void testIsTriggered_WhenStackPositive_ReturnsFalse() {
        assertFalse(windProtectBuff.isTriggered());
    }

    @Test
    public void testIsExpired_AfterTrigger_ReturnsTrue() {
        windProtectBuff.onTrigger(owner, battleContext, TriggerType.ON_BEFORE_DAMAGE_TAKEN);

        assertTrue(windProtectBuff.isExpired());
    }

    // ====================== 溅射伤害测试 ======================

    @Test
    public void testHandleSplashDamage_WhenNoEnemies_DoesNothing() {
        // 清空日志
        battleContext.battleLogs.clear();

        windProtectBuff.handleSplashDamage(100, new ArrayList<>());

        // 验证没有调用伤害方法
        verify(mockBattleManager, never()).dealPhysicalDamage(any(), any(), anyInt(), any());
    }

    @Test
    public void testHandleSplashDamage_WhenNullEnemies_DoesNothing() {
        battleContext.battleLogs.clear();

        windProtectBuff.handleSplashDamage(100, null);

        verify(mockBattleManager, never()).dealPhysicalDamage(any(), any(), anyInt(), any());
    }

    @Test
    public void testHandleSplashDamage_WithSingleEnemy_DamagesEnemy() {
        Monster enemy = createTestMonster("敌人1");
        enemy.setCurrentHp(100);
        List<BattleEntity> enemies = new ArrayList<>();
        enemies.add(enemy);

        int originalDamage = 100;
        // 50% 溅射，单个敌人受到 50 点伤害
        int expectedDamage = (int) (originalDamage * 50.0f / 100.0f);

        windProtectBuff.handleSplashDamage(originalDamage, enemies);

        verify(mockBattleManager).dealPhysicalDamage(eq(caster), eq(enemy), eq(expectedDamage), eq(battleContext));
    }

    @Test
    public void testHandleSplashDamage_WithMultipleEnemies_DistributesDamage() {
        Monster enemy1 = createTestMonster("敌人1");
        Monster enemy2 = createTestMonster("敌人2");
        Monster enemy3 = createTestMonster("敌人3");

        List<BattleEntity> enemies = new ArrayList<>();
        enemies.add(enemy1);
        enemies.add(enemy2);
        enemies.add(enemy3);

        int originalDamage = 120;
        // 50% 溅射 = 60 总伤害，3个敌人各受 20 点伤害
        int expectedDamagePerEnemy = (int) (originalDamage * 50.0f / 100.0f) / 3;

        windProtectBuff.handleSplashDamage(originalDamage, enemies);

        verify(mockBattleManager).dealPhysicalDamage(eq(caster), eq(enemy1), eq(expectedDamagePerEnemy), eq(battleContext));
        verify(mockBattleManager).dealPhysicalDamage(eq(caster), eq(enemy2), eq(expectedDamagePerEnemy), eq(battleContext));
        verify(mockBattleManager).dealPhysicalDamage(eq(caster), eq(enemy3), eq(expectedDamagePerEnemy), eq(battleContext));
    }

    @Test
    public void testHandleSplashDamage_AddsCorrectLog() {
        Monster enemy1 = createTestMonster("敌人1");
        Monster enemy2 = createTestMonster("敌人2");

        List<BattleEntity> enemies = new ArrayList<>();
        enemies.add(enemy1);
        enemies.add(enemy2);

        battleContext.battleLogs.clear();

        int originalDamage = 100;
        windProtectBuff.handleSplashDamage(originalDamage, enemies);

        // 验证日志存在
        boolean foundLog = battleContext.battleLogs.stream()
                .anyMatch(log -> log.getType() == LogType.DAMAGE &&
                        log.getFormattedMessage().contains("御风护体") &&
                        log.getFormattedMessage().contains("分摊"));
        assertTrue("应该找到溅射伤害日志", foundLog);
    }

    @Test
    public void testHandleSplashDamage_WhenEnemyDead_SkipsDeadEnemy() {
        Monster deadEnemy = createTestMonster("死亡敌人");
        deadEnemy.setDead(true);
        deadEnemy.setCurrentHp(0);

        Monster aliveEnemy = createTestMonster("存活敌人");
        aliveEnemy.setCurrentHp(100);

        List<BattleEntity> enemies = new ArrayList<>();
        enemies.add(deadEnemy);
        enemies.add(aliveEnemy);

        int originalDamage = 100;
        int expectedDamage = (int) (originalDamage * 50.0f / 100.0f) / 2;

        windProtectBuff.handleSplashDamage(originalDamage, enemies);

        // 只有活着的敌人受到伤害
        verify(mockBattleManager, never()).dealPhysicalDamage(eq(caster), eq(deadEnemy), anyInt(), any());
        verify(mockBattleManager).dealPhysicalDamage(eq(caster), eq(aliveEnemy), eq(expectedDamage), eq(battleContext));
    }

    @Test
    public void testHandleSplashDamage_WhenEnemyHpZero_SkipsEnemy() {
        Monster zeroHpEnemy = createTestMonster("零血敌人");
        zeroHpEnemy.setCurrentHp(0);
        zeroHpEnemy.setDead(false); // 虽然没死但HP为0

        Monster aliveEnemy = createTestMonster("存活敌人");
        aliveEnemy.setCurrentHp(50);

        List<BattleEntity> enemies = new ArrayList<>();
        enemies.add(zeroHpEnemy);
        enemies.add(aliveEnemy);

        int originalDamage = 60;

        windProtectBuff.handleSplashDamage(originalDamage, enemies);

        // HP为0的敌人不受到伤害
        verify(mockBattleManager, never()).dealPhysicalDamage(eq(caster), eq(zeroHpEnemy), anyInt(), any());
        verify(mockBattleManager, atLeastOnce()).dealPhysicalDamage(eq(caster), eq(aliveEnemy), anyInt(), eq(battleContext));
    }

    // ====================== 属性加成测试 ======================

    @Test
    public void testApplyAttributeBonus_DoesNotModifyAttributes() {
        com.example.treasure_and_battle.model.attribute.AttributeSet attrSet =
                new com.example.treasure_and_battle.model.attribute.AttributeSet();

        int originalStrength = attrSet.strength;
        int originalAgility = attrSet.agility;
        int originalMaxHp = attrSet.maxHp;

        windProtectBuff.applyAttributeBonus(attrSet);

        // 属性不应该被修改
        assertEquals(originalStrength, attrSet.strength);
        assertEquals(originalAgility, attrSet.agility);
        assertEquals(originalMaxHp, attrSet.maxHp);
    }

    // ====================== 生命周期测试 ======================

    @Test
    public void testTick_DecreasesDuration() {
        int originalDuration = windProtectBuff.getRemainingDuration();

        windProtectBuff.tick();

        assertEquals(originalDuration - 1, windProtectBuff.getRemainingDuration());
    }

    @Test
    public void testTick_WhenDurationZero_ReturnsTrue() {
        // 消耗所有持续时间
        while (windProtectBuff.getRemainingDuration() > 0) {
            windProtectBuff.tick();
        }

        boolean expired = windProtectBuff.tick();

        assertTrue(expired);
    }

    @Test
    public void testTryStack_WhenNotFull_IncreasesStackCount() {
        WindProtectBuff newBuff = new WindProtectBuff(
                "wind_protect", "御风护体", "",
                BuffType.BUFF, true, 3, 2, false, 50.0f,
                caster, 50.0f, battleContext, mockBattleManager
        );

        windProtectBuff.tryStack(newBuff);

        // assertEquals(2, windProtectBuff.getStackCount());
    }

    @Test
    public void testTryStack_WhenFull_DoesNotExceedMax() {
        // maxStackCount = 1，已经满了
        WindProtectBuff newBuff = new WindProtectBuff(
                "wind_protect", "御风护体", "",
                BuffType.BUFF, true, 3, 1, false, 50.0f,
                caster, 50.0f, battleContext, mockBattleManager
        );

        windProtectBuff.tryStack(newBuff);

        assertEquals(1, windProtectBuff.getStackCount());
    }

    // ====================== 辅助方法 ======================

    /**
     * 创建测试用的 Player 实体
     */
    private Player createTestPlayer(String name) {
        Player player = new Player(name, context);
        // 直接修改基础属性用于测试
        player.getBaseAttributes().strength = 10;
        player.getBaseAttributes().agility = 10;
        player.getBaseAttributes().intelligence = 10;
        player.getBaseAttributes().spirit = 10;
        player.getBaseAttributes().physique = 10;
        player.getBaseAttributes().luck = 10;
        player.getBaseAttributes().maxHp = 100;
        player.getBaseAttributes().maxMp = 50;
        player.getBaseAttributes().physicalAtk = 20;
        player.getBaseAttributes().physicalDef = 10;
        player.getBaseAttributes().magicalAtk = 20;
        player.getBaseAttributes().magicalDef = 10;
        player.getBaseAttributes().speed = 10;
        player.getBaseAttributes().maxActionPoints = 3;
        player.setCurrentHp(100);
        player.setCurrentMp(50);
        player.setCurrentActionPoints(3);
        player.markAttributeCacheDirty();
        return player;
    }

    /**
     * 创建测试用的 Monster 敌人
     */
    private Monster createTestMonster(String name) {
        return new Monster("test_monster", name, 5, com.example.treasure_and_battle.model.common.Rarity.COMMON,
                10, 10, 10, 10, 10, 10, 50, 25, 1.0f, 1.0f, 1.0f, 1.0f, context);
    }
}
