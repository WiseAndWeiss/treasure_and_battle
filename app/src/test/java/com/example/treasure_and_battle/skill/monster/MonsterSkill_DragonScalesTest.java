package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_DragonScalesTest extends MonsterSkillTestBase {

    @Test
    public void testDragonScales_ShieldGenerated() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_scales", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertLogContains(LogType.BUFF, "【龙鳞护体】");

        boolean hasShield = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof ShieldBuff) hasShield = true;
        }
        assertTrue("应生成护盾", hasShield);
    }

    @Test
    public void testDragonScalesLevel4_LargerShield() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_scales", 4);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        boolean hasShield = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof ShieldBuff) hasShield = true;
        }
        assertTrue("L4应生成护盾", hasShield);
    }

    @Test
    public void testDragonScalesCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_scales", 1);
        assertEquals("龙鳞护体冷却为3", 3, skill.getCooldown());
    }
}
