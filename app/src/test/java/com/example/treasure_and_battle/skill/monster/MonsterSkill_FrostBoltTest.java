package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;
import static org.junit.Assert.*;

public class MonsterSkill_FrostBoltTest extends MonsterSkillTestBase {

    @Test
    public void testFrostBoltLevel1_MagicDamageAndSlow() {
        ActiveSkill skill = createMonsterSkill("monster_frost_bolt", 1);
        int hpBefore = testMonster.getCurrentHp();
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        assertTrue("冰霜弹L1造成魔法伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【冰霜弹】");

        boolean hasSlow = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof SlowDebuff) hasSlow = true;
        }
        assertTrue("应附加减速", hasSlow);
    }

    @Test
    public void testFrostBoltLevel4() {
        ActiveSkill skill = createMonsterSkill("monster_frost_bolt", 4);
        int damage = executeSkillAndDamage(skill, testPlayer, testMonster);
        // L4: 180% * 50魔攻 = 90 - 20魔防 = 70
        assertTrue("冰霜弹L4高伤害", damage >= 35);

        boolean hasSlow = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof SlowDebuff) hasSlow = true;
        }
        assertTrue("L4也应减速", hasSlow);
    }
}
