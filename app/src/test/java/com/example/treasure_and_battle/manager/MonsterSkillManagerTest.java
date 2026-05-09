package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.manager.skill.MonsterSkillManager;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.monster.MonsterActiveSkill;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class MonsterSkillManagerTest {
    private Context context;
    private MonsterSkillManager skillManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        skillManager = MonsterSkillManager.getInstance(context);
    }

    @Test
    public void testHasSkill_ExistingSkill() {
        assertTrue("冲撞应该存在", skillManager.hasSkill("monster_charge"));
        assertTrue("重击应该存在", skillManager.hasSkill("monster_heavy_strike"));
        assertTrue("野性撕咬应该存在", skillManager.hasSkill("monster_wild_bite"));
    }

    @Test
    public void testHasSkill_NonExistentSkill() {
        assertFalse("slash不应该在怪物技能中", skillManager.hasSkill("slash"));
    }

    @Test
    public void testCreateSkillBySkillId_Charge() {
        Skill skill = skillManager.createSkillBySkillId("monster_charge", 1);
        assertNotNull("冲撞技能不应该为空", skill);
        assertTrue("应该是ActiveSkill子类", skill instanceof ActiveSkill);
        assertEquals("技能名应为冲撞", "冲撞", skill.getSkillName());
        assertEquals("等级为1", 1, skill.getLevel());
    }

    @Test
    public void testCreateSkillBySkillId_WildBite() {
        Skill skill = skillManager.createSkillBySkillId("monster_wild_bite", 3);
        assertNotNull("野性撕咬技能不应该为空", skill);
        assertEquals("等级为3", 3, skill.getLevel());
    }

    @Test
    public void testCreateSkillBySkillId_DefaultLevel() {
        Skill skill = skillManager.createSkillBySkillId("monster_charge");
        assertNotNull(skill);
        assertEquals("默认等级为1", 1, skill.getLevel());
    }

    @Test
    public void testCreateSkillBySkillId_NonExistent() {
        Skill skill = skillManager.createSkillBySkillId("non_existent_skill", 1);
        assertNull("不存在的技能应返回null", skill);
    }

    @Test
    public void testCreateSkillHeavyStrike_CoolDown() {
        Skill skill = skillManager.createSkillBySkillId("monster_heavy_strike", 2);
        assertNotNull(skill);
        assertEquals("重击冷却为2", 2, skill.getCooldown());
    }

    @Test
    public void testCreateSkillCharge_NoCooldown() {
        Skill skill = skillManager.createSkillBySkillId("monster_charge", 1);
        assertNotNull(skill);
        assertEquals("冲撞冷却为0", 0, skill.getCooldown());
    }

    @Test
    public void testCreateSkillDragonBreath_MagicalDamage() {
        Skill skill = skillManager.createSkillBySkillId("monster_dragon_breath", 2);
        assertNotNull(skill);
        assertEquals("龙息等级2参数x=150", 150, skill.getEffectParams().x);
    }

    @Test
    public void testCreateSkillFrenzy_MaxLevel() {
        Skill skill = skillManager.createSkillBySkillId("monster_frenzy", 4);
        assertNotNull(skill);
        assertEquals("狂化max=4", 4, skill.getMaxLevel());
    }

    @Test
    public void testAllSkillsLoadable() {
        String[] allSkills = {
            "monster_charge", "monster_heavy_strike", "monster_wild_bite",
            "monster_sweep", "monster_acid_spray", "monster_gel_recover",
            "monster_slime_armor", "monster_howl", "monster_frenzy",
            "monster_backstab", "monster_intimidate", "monster_throw_sand",
            "monster_dragon_breath", "monster_tail_sweep", "monster_dragon_roar",
            "monster_dragon_scales", "monster_fire_burst", "monster_inferno",
            "monster_flame_shield", "monster_frost_bolt", "monster_blizzard",
            "monster_ice_barrier"
        };
        for (String skillId : allSkills) {
            Skill skill = skillManager.createSkillBySkillId(skillId, 1);
            assertNotNull("技能 " + skillId + " 应该可加载", skill);
            assertTrue("应该是MonsterActiveSkill子类: " + skillId, skill instanceof MonsterActiveSkill);
        }
    }
}
