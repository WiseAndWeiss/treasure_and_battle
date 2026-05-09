package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_BlizzardTest extends MonsterSkillTestBase {

    @Test
    public void testBlizzardLevel1_MagicAOEAndSlow() {
        ActiveSkill skill = createMonsterSkill("monster_blizzard", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        assertTrue("暴风雪L1造成魔法伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【暴风雪】");

        boolean hasSlow = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof SlowDebuff) hasSlow = true;
        }
        assertTrue("应附加减速", hasSlow);
    }

    @Test
    public void testBlizzardLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_blizzard", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 150% * 50魔攻 = 75 - 20魔防 = 55
        assertTrue("暴风雪L4高伤害", damage >= 25);
    }

    @Test
    public void testBlizzardCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_blizzard", 1);
        assertEquals("暴风雪冷却为3", 3, skill.getCooldown());
    }
}
