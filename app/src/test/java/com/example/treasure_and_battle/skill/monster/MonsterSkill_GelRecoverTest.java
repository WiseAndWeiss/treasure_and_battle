package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 凝胶再生技能测试 (史莱姆回复)
 * 效果: 恢复最大HP的{x}%
 */
public class MonsterSkill_GelRecoverTest extends MonsterSkillTestBase {

    @Test
    public void testGelRecoverLevel1() {
        testPlayer.setCurrentHp(50);
        ActiveSkill gelRecover = createMonsterSkill("monster_gel_recover", 1);
        int hpBefore = testPlayer.getCurrentHp();

        // 测试怪物对自己用，但我们要测的是caster=testPlayer
        battleManager.executeSkill(testPlayer, gelRecover,
                java.util.Arrays.asList(testMonster), battleContext);

        int hpAfter = testPlayer.getCurrentHp();
        assertTrue("L1应该恢复生命值", hpAfter > hpBefore);
        assertLogContains(LogType.HEAL, "【凝胶再生】");
    }

    @Test
    public void testGelRecoverLevel4_HigherHeal() {
        testPlayer.setCurrentHp(10);
        ActiveSkill l1 = createMonsterSkill("monster_gel_recover", 1);
        ActiveSkill l4 = createMonsterSkill("monster_gel_recover", 4);

        int hpBefore = testPlayer.getCurrentHp();
        battleManager.executeSkill(testPlayer, l1, java.util.Arrays.asList(testMonster), battleContext);
        int heal1 = testPlayer.getCurrentHp() - hpBefore;

        resetEntityStates();
        testPlayer.setCurrentHp(10);
        hpBefore = testPlayer.getCurrentHp();
        battleManager.executeSkill(testPlayer, l4, java.util.Arrays.asList(testMonster), battleContext);
        int heal4 = testPlayer.getCurrentHp() - hpBefore;

        assertTrue("L4恢复量 >= L1", heal4 >= heal1);
    }

    @Test
    public void testGelRecover_NotExceedMaxHp() {
        testPlayer.setCurrentHp(testPlayer.getBaseAttributes().maxHp - 1);
        ActiveSkill gelRecover = createMonsterSkill("monster_gel_recover", 4);

        battleManager.executeSkill(testPlayer, gelRecover,
                java.util.Arrays.asList(testMonster), battleContext);

        assertTrue("HP不应超过最大值",
                testPlayer.getCurrentHp() <= testPlayer.getBaseAttributes().maxHp);
    }
}
