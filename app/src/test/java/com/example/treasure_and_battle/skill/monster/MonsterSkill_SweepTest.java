package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_SweepTest extends MonsterSkillTestBase {

    @Test
    public void testSweepLevel1() {
        ActiveSkill skill = createMonsterSkill("monster_sweep", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        assertTrue("横扫L1造成伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【横扫】");
    }

    @Test
    public void testSweepLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_sweep", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 145% * 50 = 72 - 20防 = 52
        assertTrue("横扫L4造成更高伤害", damage >= 40);
    }
}
