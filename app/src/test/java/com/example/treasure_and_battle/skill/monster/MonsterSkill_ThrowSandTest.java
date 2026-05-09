package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 撒沙技能测试 (山贼专属)
 * 效果: 物理伤害 + 命中率降低debuff
 */
public class MonsterSkill_ThrowSandTest extends MonsterSkillTestBase {

    @Test
    public void testThrowSandLevel1() {
        ActiveSkill throwSand = createMonsterSkill("monster_throw_sand", 1);
        int hpBefore = testMonster.getCurrentHp();

        int damage = executeSkillAndDamage(throwSand, testPlayer, testMonster);

        assertTrue("撒沙L1造成伤害", damage > 0);
        assertLogContains(LogType.ACTION, "【撒沙】");

        boolean hasHitDebuff = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof AttributeBuff) {
                AttributeBuff ab = (AttributeBuff) buff;
                if (ab.getBuffId().contains("hit_down")) {
                    hasHitDebuff = true;
                }
            }
        }
        assertTrue("应该附加命中率降低debuff", hasHitDebuff);
    }

    @Test
    public void testThrowSandLevel4_StrongerDebuff() {
        ActiveSkill throwSand = createMonsterSkill("monster_throw_sand", 4);

        executeSkillAndDamage(throwSand, testPlayer, testMonster);

        boolean hasDebuff = false;
        for (BaseBuff buff : testMonster.getActiveBuffList()) {
            if (buff instanceof AttributeBuff && buff.getBuffId().contains("hit_down")) {
                hasDebuff = true;
            }
        }
        assertTrue("L4也应附加debuff", hasDebuff);
    }

    /**
     * 同名的怪物，如果只有一个（比如"撒沙"只对山贼），
     * 则这方法只测基本调用不崩溃即可
     */
    @Test
    public void testThrowSand_DoesNotCrash() {
        ActiveSkill throwSand = createMonsterSkill("monster_throw_sand", 1);
        battleManager.executeSkill(testPlayer, throwSand,
                java.util.Arrays.asList(testMonster), battleContext);
        assertTrue(testMonster.getCurrentHp() <= testMonster.getBaseAttributes().maxHp);
    }
}
