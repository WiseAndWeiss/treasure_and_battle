package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_IceBarrierTest extends MonsterSkillTestBase {

    @Test
    public void testIceBarrier_ShieldAndMagDefBuff() {
        ActiveSkill skill = createMonsterSkill("monster_ice_barrier", 1);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        assertLogContains(LogType.BUFF, "【冰霜屏障】");

        boolean hasShield = false;
        boolean hasMagDef = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof ShieldBuff) hasShield = true;
            if (buff.getBuffId().contains("ice_barrier_mdef")) hasMagDef = true;
        }
        assertTrue("应生成护盾", hasShield);
        assertTrue("应有法防BUFF", hasMagDef);
    }

    @Test
    public void testIceBarrierLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_ice_barrier", 4);
        battleManager.executeSkill(testPlayer, skill,
                java.util.Arrays.asList(testMonster), battleContext);

        boolean hasShield = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof ShieldBuff) hasShield = true;
        }
        assertTrue("L4应生成护盾", hasShield);
    }

    @Test
    public void testIceBarrierCooldown() {
        ActiveSkill skill = createMonsterSkill("monster_ice_barrier", 1);
        assertEquals("冰霜屏障冷却为2", 2, skill.getCooldown());
    }
}
