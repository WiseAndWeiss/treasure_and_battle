package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_TailSweepTest extends MonsterSkillTestBase {

    @Test
    public void testTailSweepLevel1() {
        ActiveSkill skill = createMonsterSkill("monster_tail_sweep", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L1: 180% * 50 = 90 - 20防 = 70
        assertTrue("龙尾扫击L1造成伤害", damage >= 40);
        assertEquals(hpBefore - damage, testMonster.getCurrentHp());
        assertLogContains(LogType.ACTION, "【龙尾扫击】");
    }

    @Test
    public void testTailSweepLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_tail_sweep", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 270% * 50 = 135 - 20防 = 115
        assertTrue("龙尾扫击L4高伤害", damage >= 70);
    }

    @Test
    public void testTailSweepCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_tail_sweep", 1);
        assertEquals("龙尾扫击冷却为2", 2, skill.getCooldown());
    }
}
