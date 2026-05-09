package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_InfernoTest extends MonsterSkillTestBase {

    @Test
    public void testInfernoLevel1_MagicAOEAndBurning() {
        ActiveSkill skill = createMonsterSkill("monster_inferno", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        assertTrue("烈焰风暴L1造成魔法伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【烈焰风暴】");

        boolean hasBurning = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof BurningDebuff) hasBurning = true;
        }
        assertTrue("应附加灼烧", hasBurning);
    }

    @Test
    public void testInfernoLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_inferno", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 160% * 50魔攻 = 80 - 20魔防 = 60
        assertTrue("L4高伤害", damage >= 30);
    }

    @Test
    public void testInfernoCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_inferno", 1);
        assertEquals("烈焰风暴冷却为3", 3, skill.getCooldown());
    }
}
