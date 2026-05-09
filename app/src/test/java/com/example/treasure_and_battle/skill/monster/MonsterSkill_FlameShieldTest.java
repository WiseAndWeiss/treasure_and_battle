package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_FlameShieldTest extends MonsterSkillTestBase {

    @Test
    public void testFlameShield_ShieldGenerated() {
        ActiveSkill skill = createMonsterSkill("monster_flame_shield", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertLogContains(LogType.BUFF, "【火焰护盾】");

        boolean hasShield = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof ShieldBuff) hasShield = true;
        }
        assertTrue("应生成护盾", hasShield);
    }

    @Test
    public void testFlameShieldLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_flame_shield", 4);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertLogContains(LogType.BUFF, "【火焰护盾】");
    }

    @Test
    public void testFlameShieldCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_flame_shield", 1);
        assertEquals("火焰护盾冷却为2", 2, skill.getCooldown());
    }
}
