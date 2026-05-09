package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_DragonRoarTest extends MonsterSkillTestBase {

    @Test
    public void testDragonRoar_DebuffApplied() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_roar", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertLogContains(LogType.ACTION, "【龙吼】");

        int debuffCount = 0;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff.getBuffId().contains("dragon_roar")) debuffCount++;
        }
        assertEquals("应有2个debuff(降攻+降防)", 2, debuffCount);
    }

    @Test
    public void testDragonRoarCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_dragon_roar", 1);
        assertEquals("龙吼冷却为4", 4, skill.getCooldown());
    }
}
