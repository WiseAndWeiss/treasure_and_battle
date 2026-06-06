package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.manager.battle.MonsterManager;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Test;

import static org.junit.Assert.*;

public class MonsterSkill_LevelTest extends MonsterSkillTestBase {

    @Test
    public void testSkillLevel_ComesFromJson() {
        Monster monster = MonsterManager.getInstance(context).createMonsterWithoutAffixes(2005);

        ActiveSkill bite = monster.getMonsterSkill("monster_wild_bite");
        assertNotNull("芬里尔应有野性撕咬", bite);
        assertEquals("芬里尔的野性撕咬应为L4", 4, bite.getLevel());

        ActiveSkill frenzy = monster.getMonsterSkill("monster_frenzy");
        assertNotNull("芬里尔应有狂化", frenzy);
        assertEquals("芬里尔的狂化应为L4", 4, frenzy.getLevel());
    }

    @Test
    public void testSkillLevel_LowRarityHasLevel1() {
        Monster monster = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);

        ActiveSkill charge = monster.getMonsterSkill("monster_charge");
        assertNotNull("小史莱姆应有冲撞", charge);
        assertEquals("小史莱姆冲撞应为L1", 1, charge.getLevel());
    }

    @Test
    public void testSkillLevel_ClampedToMax4() {
        Monster monster = MonsterManager.getInstance(context).createMonsterWithoutAffixes(2005);
        ActiveSkill bite = monster.getMonsterSkill("monster_wild_bite");
        assertNotNull(bite);

        int actualLevel = bite.getLevel();
        assertTrue("等级不应超过maxLevel(4)", actualLevel <= 4);

        int maxLevel = bite.getMaxLevel();
        assertTrue("等级不应超过maxLevel", actualLevel <= maxLevel);
    }
}
