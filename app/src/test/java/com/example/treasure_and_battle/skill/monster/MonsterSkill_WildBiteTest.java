package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 野性撕咬技能测试
 * 效果: 物理伤害 + 流血
 */
public class MonsterSkill_WildBiteTest extends MonsterSkillTestBase {

    @Test
    public void testWildBiteLevel1_DamagePlusBleeding() {
        ActiveSkill wildBite = createMonsterSkill("monster_wild_bite", 1);
        int hpBefore = testMonster.getCurrentHp();

        int damage = executeSkillAndDamage(wildBite, testPlayer, testMonster);

        // L1: 130% * 50物理攻击 = 65 - 20物防 = 45
        assertTrue("野性撕咬L1造成伤害", damage > 0);
        assertEquals(hpBefore - damage, testMonster.getCurrentHp());

        boolean hasBleeding = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof BleedingDebuff) {
                hasBleeding = true;
                assertEquals("L1应附加2层流血", 2, buff.getStackCount());
            }
        }
        assertTrue("应该附加流血debuff", hasBleeding);
        assertLogContains(LogType.ACTION, "【野性撕咬】");
    }

    @Test
    public void testWildBiteLevel4_HigherStacks() {
        ActiveSkill wildBite = createMonsterSkill("monster_wild_bite", 4);

        executeSkillAndDamage(wildBite, testPlayer, testMonster);

        boolean hasBleeding = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof BleedingDebuff) {
                hasBleeding = true;
                assertEquals("L4应附加4层流血", 4, buff.getStackCount());
            }
        }
        assertTrue("应该附加流血debuff", hasBleeding);
    }
}
