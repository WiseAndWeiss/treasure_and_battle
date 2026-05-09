package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_FrenzyTest extends MonsterSkillTestBase {

    @Test
    public void testFrenzy_CostHpAndApplyBuffs() {
        int hpBefore = testPlayer.getCurrentHp();
        ActiveSkill skill = createMonsterSkill("monster_frenzy", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertTrue("狂化应消耗HP", testPlayer.getCurrentHp() < hpBefore);
        assertLogContains(LogType.ACTION, "【狂化】");

        int buffCount = 0;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff.getBuffId().contains("frenzy")) buffCount++;
        }
        assertEquals("应有3个buff(攻+速+减速)", 3, buffCount);
    }

    @Test
    public void testFrenzy_HpNotBelowOne() {
        testPlayer.setCurrentHp(5);
        ActiveSkill skill = createMonsterSkill("monster_frenzy", 4);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertTrue("HP不应低于1", testPlayer.getCurrentHp() >= 1);
    }

    @Test
    public void testFrenzyCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_frenzy", 1);
        assertEquals("狂化冷却为4", 4, skill.getCooldown());
    }
}
