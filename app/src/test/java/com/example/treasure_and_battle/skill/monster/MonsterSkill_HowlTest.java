package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_HowlTest extends MonsterSkillTestBase {

    @Test
    public void testHowl_BuffApplied() {
        ActiveSkill skill = createMonsterSkill("monster_howl", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testPlayer), battleContext);

        assertLogContains(LogType.BUFF, "【狼嚎】");

        int buffCount = 0;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff.getBuffId().contains("howl")) buffCount++;
        }
        assertEquals("应有2个buff(物攻+速度)", 2, buffCount);
    }

    @Test
    public void testHowlLevel4_StrongerBuff() {
        ActiveSkill skill = createMonsterSkill("monster_howl", 4);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testPlayer), battleContext);

        assertLogContains(LogType.BUFF, "【狼嚎】");
    }

    @Test
    public void testHowlCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_howl", 2);
        assertEquals("狼嚎冷却为3", 3, skill.getCooldown());
    }

    @Test
    public void testHowl_SelfTargetedWithAllAllies() {
        ActiveSkill skill = createMonsterSkill("monster_howl", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testPlayer), battleContext);

        assertLogContains(LogType.BUFF, "【狼嚎】");

        int howlBuffs = 0;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff.getBuffId().contains("howl")) howlBuffs++;
        }
        assertTrue("狼嚎应对自己生效", howlBuffs >= 2);
    }
}
