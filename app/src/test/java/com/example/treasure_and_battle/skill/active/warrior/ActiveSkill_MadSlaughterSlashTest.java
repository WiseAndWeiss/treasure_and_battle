package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 狂弑千斩技能测试
 * 技能效果：消耗生命，对全体敌方造成x%物理攻击伤害，附加y层流血debuff与z%虚弱减益（持续w回合）
 */
public class ActiveSkill_MadSlaughterSlashTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：75%AOE伤害，2层流血，5%虚弱（2回合），消耗10HP
     */
    @Test
    public void testMadSlaughterSlashLevel1() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 1);
        testPlayer.setCurrentHp(200);

        // 创建3个怪物
        Monster monster1 = testMonster;
        Monster monster2 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        Monster monster3 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);

        // 统一设置所有怪物的HP和防御
        int maxHp = 300;
        int physicalDef = 20;

        monster1.getBaseAttributes().maxHp = maxHp;
        monster1.getBaseAttributes().physicalDef = physicalDef;
        monster1.setCurrentHp(maxHp);
        monster1.markAttributeCacheDirty();

        monster2.getBaseAttributes().maxHp = maxHp;
        monster2.getBaseAttributes().physicalDef = physicalDef;
        monster2.getBaseAttributes().dodgeRate = 0f;
        monster2.setCurrentHp(maxHp);
        monster2.markAttributeCacheDirty();

        monster3.getBaseAttributes().maxHp = maxHp;
        monster3.getBaseAttributes().physicalDef = physicalDef;
        monster3.getBaseAttributes().dodgeRate = 0f;
        monster3.setCurrentHp(maxHp);
        monster3.markAttributeCacheDirty();

        List<BattleEntity> targets = new ArrayList<>();
        targets.add(monster1);
        targets.add(monster2);
        targets.add(monster3);

        // When
        battleManager.executeSkill(testPlayer, madSlaughterSlash, targets, battleContext);

        // Then - 验证所有敌人都受到伤害
        assertTrue("怪物1应该受到伤害", monster1.getCurrentHp() < maxHp);
        assertTrue("怪物2应该受到伤害", monster2.getCurrentHp() < maxHp);
        assertTrue("怪物3应该受到伤害", monster3.getCurrentHp() < maxHp);

        // 验证HP消耗
        assertEquals("应该消耗10HP", 190, testPlayer.getCurrentHp());

        // 验证每个敌人都有流血和虚弱debuff
        for (Monster monster : Arrays.asList(monster1, monster2, monster3)) {
            List<BaseBuff> buffs = monster.getActiveBuffList();

            BleedingDebuff bleedingDebuff = (BleedingDebuff) buffs.stream()
                .filter(buff -> buff instanceof BleedingDebuff)
                .findFirst()
                .orElse(null);

            assertNotNull("怪物应该有流血debuff", bleedingDebuff);
            assertEquals("流血层数应该为2", 2, bleedingDebuff.getStackCount());

            WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
                .filter(buff -> buff instanceof WeaknessDebuff)
                .findFirst()
                .orElse(null);

            assertNotNull("怪物应该有虚弱debuff", weaknessDebuff);
            assertEquals("虚弱应该持续2回合", 2, weaknessDebuff.getRemainingDuration());
        }

        printBattleLogs();
    }

    /**
     * 测试等级5：100%AOE伤害，4层流血，10%虚弱（3回合）
     */
    @Test
    public void testMadSlaughterSlashLevel5() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 5);
        testPlayer.setCurrentHp(200);

        int maxHp = testMonster.getBaseAttributes().maxHp;
        testMonster.setCurrentHp(maxHp);

        // When
        battleManager.executeSkill(testPlayer, madSlaughterSlash,
            Arrays.asList(testMonster), battleContext);

        // Then
        assertTrue("应该造成伤害", testMonster.getCurrentHp() < maxHp);
        assertEquals("应该消耗22HP", 178, testPlayer.getCurrentHp());

        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuff = (BleedingDebuff) buffs.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        assertEquals("流血层数应该为4", 4, bleedingDebuff.getStackCount());

        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertEquals("虚弱应该持续3回合", 3, weaknessDebuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试虚弱debuff效果
     */
    @Test
    public void testWeaknessDebuffEffect() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 3); // 7%虚弱
        AttributeSet monsterAttr = testMonster.getFinalAttributes();

        int baseAtk = monsterAttr.physicalAtk;
        int baseMatk = monsterAttr.magicalAtk;
        int weaknessPercent = 7;

        // When
        battleManager.executeSkill(testPlayer, madSlaughterSlash,
            Arrays.asList(testMonster), battleContext);

        // Then - 验证虚弱效果
        AttributeSet finalAttr = testMonster.getFinalAttributes();
        int expectedAtk = Math.round(baseAtk * (1 - weaknessPercent / 100.0f));
        int expectedMatk = Math.round(baseMatk * (1 - weaknessPercent / 100.0f));

        assertEquals("物理攻击应该降低7%", expectedAtk, finalAttr.physicalAtk);
        assertEquals("法术攻击应该降低7%", expectedMatk, finalAttr.magicalAtk);

        System.out.println("基础物攻: " + baseAtk + ", 预期: " + expectedAtk + ", 实际: " + finalAttr.physicalAtk);
        System.out.println("基础法攻: " + baseMatk + ", 预期: " + expectedMatk + ", 实际: " + finalAttr.magicalAtk);

        printBattleLogs();
    }

    /**
     * 测试流血和虚弱同时存在
     */
    @Test
    public void testBothDebuffsApplied() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 2);

        // When
        battleManager.executeSkill(testPlayer, madSlaughterSlash,
            Arrays.asList(testMonster), battleContext);

        // Then - 验证同时有流血和虚弱
        List<BaseBuff> buffs = testMonster.getActiveBuffList();

        BleedingDebuff bleedingDebuff = (BleedingDebuff) buffs.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有流血debuff", bleedingDebuff);
        assertNotNull("应该有虚弱debuff", weaknessDebuff);

        assertEquals("应该有2个debuff", 2, buffs.size());

        printBattleLogs();
    }

    /**
     * 测试AOE伤害
     */
    @Test
    public void testAOEDamage() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 3);

        Monster monster1 = testMonster;
        Monster monster2 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        Monster monster3 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);

        int maxHp = 300;
        int physicalDef = 20;

        for (Monster monster : Arrays.asList(monster1, monster2, monster3)) {
            monster.getBaseAttributes().maxHp = maxHp;
            monster.getBaseAttributes().physicalDef = physicalDef;
            monster.getBaseAttributes().dodgeRate = 0f;
            monster.setCurrentHp(maxHp);
            monster.markAttributeCacheDirty();
        }

        List<BattleEntity> targets = new ArrayList<>();
        targets.add(monster1);
        targets.add(monster2);
        targets.add(monster3);

        // When
        battleManager.executeSkill(testPlayer, madSlaughterSlash, targets, battleContext);

        // Then - 验证所有敌人都受到伤害
        int totalDamage = 0;
        for (Monster monster : Arrays.asList(monster1, monster2, monster3)) {
            int damage = maxHp - monster.getCurrentHp();
            assertTrue("怪物应该受到伤害", damage > 0);
            totalDamage += damage;
        }

        assertTrue("总伤害应该大于0", totalDamage > 0);

        System.out.println("总伤害: " + totalDamage);

        printBattleLogs();
    }

    /**
     * 测试HP消耗
     */
    @Test
    public void testHpCost() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 1);
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, madSlaughterSlash,
            Arrays.asList(testMonster), battleContext);

        // Then - 等级1应该消耗10HP
        assertEquals("应该消耗10HP", 190, testPlayer.getCurrentHp());

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        testPlayer.setCurrentHp(200);

        // 测试等级1（2层流血，5%虚弱）
        testMonster.getActiveBuffList().clear();
        ActiveSkill skill1 = createSkill("mad_slaughter_slash", 1);
        battleManager.executeSkill(testPlayer, skill1,
            Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs1 = testMonster.getActiveBuffList();
        BleedingDebuff bleeding1 = (BleedingDebuff) buffs1.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);
        int stacks1 = bleeding1.getStackCount();

        // 测试等级5（4层流血，10%虚弱）
        testMonster.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        testPlayer.setCurrentHp(200);
        ActiveSkill skill5 = createSkill("mad_slaughter_slash", 5);
        battleManager.executeSkill(testPlayer, skill5,
            Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs5 = testMonster.getActiveBuffList();
        BleedingDebuff bleeding5 = (BleedingDebuff) buffs5.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);
        int stacks5 = bleeding5.getStackCount();

        // 验证流血层数递增
        assertTrue("等级5流血层数应该大于等级1", stacks5 > stacks1);

        System.out.println("等级1流血层数: " + stacks1);
        System.out.println("等级5流血层数: " + stacks5);

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 3);

        Monster monster2 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        List<BattleEntity> targets = Arrays.asList(testMonster, monster2);

        // When
        battleManager.executeSkill(testPlayer, madSlaughterSlash, targets, battleContext);

        // Then - 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【狂弑千斩】");
        assertLogContains(LogType.ACTION, "个敌人");
        assertLogContains(LogType.ACTION, "层流血");
        assertLogContains(LogType.ACTION, "虚弱");

        printBattleLogs();
    }

    /**
     * 测试虚弱debuff持续时间
     */
    @Test
    public void testWeaknessDuration() {
        // Given
        ActiveSkill madSlaughterSlash = createSkill("mad_slaughter_slash", 1); // 2回合虚弱

        battleManager.executeSkill(testPlayer, madSlaughterSlash,
            Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        // When - 经过1回合
        com.example.treasure_and_battle.manager.BuffManager.getInstance(context).tickBuffs(testMonster);

        // Then
        List<BaseBuff> buffsAfterTick1 = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessAfter1 = (WeaknessDebuff) buffsAfterTick1.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("经过1回合后虚弱debuff应该仍然存在", weaknessAfter1);
        assertEquals("剩余持续时间应该为1", 1, weaknessAfter1.getRemainingDuration());

        // When - 再经过1回合
        com.example.treasure_and_battle.manager.BuffManager.getInstance(context).tickBuffs(testMonster);

        // Then - 虚弱debuff应该消失，但流血debuff应该保留（如果还有层数）
        List<BaseBuff> buffsAfterTick2 = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessAfter2 = (WeaknessDebuff) buffsAfterTick2.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNull("经过2回合后虚弱debuff应该消失", weaknessAfter2);

        printBattleLogs();
    }
}
