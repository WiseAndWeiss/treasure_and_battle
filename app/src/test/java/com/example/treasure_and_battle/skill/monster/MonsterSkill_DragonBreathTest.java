package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_DragonBreathTest extends MonsterSkillTestBase {

    @Test
    public void testDragonBreathLevel1() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_breath", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L1: 120% * 50魔攻 = 60 - 20魔防 = 40
        assertTrue("龙息L1造成魔法伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【龙息】");
    }

    @Test
    public void testDragonBreathLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_breath", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 210% * 50 = 105 - 20防 = 85
        assertTrue("龙息L4高伤害", damage >= 50);
    }

    @Test
    public void testDragonBreathCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_breath", 1);
        assertEquals("龙息冷却为3", 3, skill.getCooldown());
    }
}
