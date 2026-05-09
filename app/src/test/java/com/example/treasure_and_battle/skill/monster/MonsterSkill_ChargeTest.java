package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 冲撞技能测试 (通用物理)
 * 效果: 对单体造成{x}%物理攻击伤害
 */
public class MonsterSkill_ChargeTest extends MonsterSkillTestBase {

    @Test
    public void testChargeLevel1() {
        ActiveSkill charge = createMonsterSkill("monster_charge", 1);
        int hpBefore = testMonster.getCurrentHp();

        int damage = executeSkillAndDamage(charge, testPlayer, testMonster);

        // L1: 150% * 50物攻 = 75 - 20物防 = 55
        assertTrue("冲撞L1造成伤害", damage > 0);
        assertEquals(hpBefore - damage, testMonster.getCurrentHp());
        assertLogContains(LogType.ACTION, "【冲撞】");
    }

    @Test
    public void testChargeLevel4() {
        ActiveSkill charge = createMonsterSkill("monster_charge", 4);
        int hpBefore = testMonster.getCurrentHp();

        int damage = executeSkillAndDamage(charge, testPlayer, testMonster);

        // L4: 210% * 50物攻 = 105 - 20物防 = 85
        assertTrue("冲撞L4造成更高伤害", damage >= 60);
        assertEquals(hpBefore - damage, testMonster.getCurrentHp());
    }

    @Test
    public void testChargeLevels_Progression() {
        ActiveSkill l1 = createMonsterSkill("monster_charge", 1);
        ActiveSkill l4 = createMonsterSkill("monster_charge", 4);

        int dmg1 = executeSkillAndDamage(l1, testPlayer, testMonster);
        resetEntityStates();
        int dmg4 = executeSkillAndDamage(l4, testPlayer, testMonster);

        assertTrue("L4伤害应该>=L1", dmg4 >= dmg1);
    }

    @Test
    public void testChargeCost() {
        ActiveSkill charge = createMonsterSkill("monster_charge", 1);
        int apBefore = testPlayer.getCurrentActionPoints();

        battleManager.executeSkill(testPlayer, charge,
                java.util.Arrays.asList(testMonster), battleContext);

        assertEquals("消耗1行动点", apBefore - 1, testPlayer.getCurrentActionPoints());
    }
}
