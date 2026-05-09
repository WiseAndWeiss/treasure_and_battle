package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_AcidSprayTest extends MonsterSkillTestBase {

    @Test
    public void testAcidSprayLevel1_DefReduceDebuff() {
        ActiveSkill skill = createMonsterSkill("monster_acid_spray", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        assertTrue("酸液喷射L1造成魔法伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【酸液喷射】");

        boolean hasDefDown = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff.getBuffId().contains("def_down")) {
                hasDefDown = true;
            }
        }
        assertTrue("应附加物防降低debuff", hasDefDown);
    }

    @Test
    public void testAcidSprayLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_acid_spray", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 155% * 50魔攻 = 77 - 20魔防 = 57
        assertTrue("L4造成更高伤害", damage >= 35);
    }
}
