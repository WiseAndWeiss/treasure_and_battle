package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_BackstabTest extends MonsterSkillTestBase {

    @Test
    public void testBackstabLevel1() {
        ActiveSkill skill = createMonsterSkill("monster_backstab", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L1: 160% * 50 = 80 - 20防 = 60
        assertTrue("背刺L1造成伤害", damage >= 40);
        assertEquals(hpBefore - damage, testMonster.getCurrentHp());
        assertLogContains(LogType.ACTION, "【背刺】");
    }

    @Test
    public void testBackstabLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_backstab", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 220% * 50 = 110 - 20防 = 90
        assertTrue("背刺L4造成更高伤害", damage >= 60);
    }
}
