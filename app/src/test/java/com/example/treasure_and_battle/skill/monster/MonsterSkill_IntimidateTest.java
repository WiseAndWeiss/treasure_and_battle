package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_IntimidateTest extends MonsterSkillTestBase {

    @Test
    public void testIntimidate_DebuffApplied() {
        ActiveSkill skill = createMonsterSkill("monster_intimidate", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertLogContains(LogType.ACTION, "【威吓】");

        int debuffCount = 0;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff.getBuffId().contains("intimidate")) debuffCount++;
        }
        assertEquals("应有2个debuff(降攻+降法攻)", 2, debuffCount);
    }

    @Test
    public void testIntimidateLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_intimidate", 4);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);
        assertLogContains(LogType.ACTION, "【威吓】");
    }

    @Test
    public void testIntimidateCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_intimidate", 2);
        assertEquals("威吓冷却为3", 3, skill.getCooldown());
    }
}
