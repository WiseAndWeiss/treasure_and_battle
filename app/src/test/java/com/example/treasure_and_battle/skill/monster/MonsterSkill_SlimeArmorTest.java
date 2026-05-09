package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_SlimeArmorTest extends MonsterSkillTestBase {

    @Test
    public void testSlimeArmorLevel1_DoubleDefenseBuff() {
        ActiveSkill skill = createMonsterSkill("monster_slime_armor", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertLogContains(LogType.BUFF, "【黏液护甲】");

        int buffCount = 0;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff.getBuffId().contains("slime_armor")) buffCount++;
        }
        assertEquals("应有2个buff(物防+法防)", 2, buffCount);
    }

    @Test
    public void testSlimeArmor_DoesNotCrash() {
        ActiveSkill skill = createMonsterSkill("monster_slime_armor", 3);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);
        assertTrue(true);
    }
}
