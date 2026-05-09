package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_FireBurstTest extends MonsterSkillTestBase {

    @Test
    public void testFireBurstLevel1_MagicDamageAndBurning() {
        ActiveSkill skill = createMonsterSkill("monster_fire_burst", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        assertTrue("火焰爆裂L1造成魔法伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【火焰爆裂】");

        boolean hasBurning = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof BurningDebuff) {
                hasBurning = true;
                assertEquals("L1附加2层灼烧", 2, buff.getStackCount());
            }
        }
        assertTrue("应附加灼烧", hasBurning);
    }

    @Test
    public void testFireBurstLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_fire_burst", 4);
        executeSkillAndDamage(skill, testPlayer, testMonster);

        boolean hasBurning = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof BurningDebuff) {
                hasBurning = true;
                assertEquals("L4附加4层灼烧", 4, buff.getStackCount());
            }
        }
        assertTrue("L4应附加灼烧", hasBurning);
    }
}
