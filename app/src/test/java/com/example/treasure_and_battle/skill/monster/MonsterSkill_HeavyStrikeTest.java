package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_HeavyStrikeTest extends MonsterSkillTestBase {

    @Test
    public void testHeavyStrikeLevel1() {
        ActiveSkill skill = createMonsterSkill("monster_heavy_strike", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        assertTrue("重击L1造成伤害", damage > 0);
        assertEquals(hpBefore - damage, testMonster.getCurrentHp());
        assertLogContains(LogType.ACTION, "【重击】");
    }

    @Test
    public void testHeavyStrikeLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_heavy_strike", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 290% * 50 = 145 - 20防 = 125
        assertTrue("重击L4造成高伤害", damage >= 80);
    }

    @Test
    public void testHeavyStrikeCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_heavy_strike", 2);
        assertEquals("重击冷却为2", 2, skill.getCooldown());
    }

    @Test
    public void testHeavyStrike_NotCooldownReadyAfterCast() {
        ActiveSkill skill = createMonsterSkill("monster_heavy_strike", 1);
        assertTrue("初始冷却就绪", skill.isCooldownReady());

        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertFalse("释放后应进入冷却", skill.isCooldownReady());
    }

    @Test
    public void testHeavyStrike_CooldownRecovers() {
        ActiveSkill skill = createMonsterSkill("monster_heavy_strike", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertFalse("释放后冷却中", skill.isCooldownReady());

        skill.decreaseCooldown();
        assertFalse("CD=2，减1后仍为1", skill.isCooldownReady());

        skill.decreaseCooldown();
        assertTrue("CD=2，减2次后应该就绪", skill.isCooldownReady());
    }
}
