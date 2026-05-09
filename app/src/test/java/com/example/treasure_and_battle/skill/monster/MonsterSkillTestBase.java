package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.manager.MonsterSkillManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Before;

import static org.junit.Assert.*;

/**
 * 怪物技能测试基类
 * 继承 ActiveSkillTestBase，通过 MonsterSkillManager 加载技能
 */
public abstract class MonsterSkillTestBase extends ActiveSkillTestBase {

    @Override
    @Before
    public void setUp() {
        MonsterSkillManager.releaseInstance();
        MonsterManager.releaseInstance();
        super.setUp();
    }

    /**
     * 通过 skillId 创建怪物技能并设置等级
     */
    protected ActiveSkill createMonsterSkill(String skillId, int level) {
        Skill skill = MonsterSkillManager.getInstance(context).createSkillBySkillId(skillId, level);
        assertNotNull("怪物技能 " + skillId + " 不存在", skill);
        assertTrue("应该是ActiveSkill类型", skill instanceof ActiveSkill);
        return (ActiveSkill) skill;
    }

    /**
     * 创建怪物技能（默认等级1）
     */
    protected ActiveSkill createMonsterSkill(String skillId) {
        return createMonsterSkill(skillId, 1);
    }

    /**
     * 重置实体状态（复制自 PassiveSkillTestBase）
     */
    protected void resetEntityStates() {
        AttributeSet playerAttr = testPlayer.getBaseAttributes();
        testPlayer.setCurrentHp(playerAttr.maxHp);
        testPlayer.setCurrentMp(playerAttr.maxMp);
        testPlayer.setDead(false);

        AttributeSet monsterAttr = testMonster.getBaseAttributes();
        testMonster.setCurrentHp(monsterAttr.maxHp);
        testMonster.setCurrentMp(monsterAttr.maxMp);
        testMonster.setDead(false);

        testPlayer.getActiveBuffList().clear();
        testMonster.getActiveBuffList().clear();
        testPlayer.getPassiveSkillList().clear();
        testMonster.getPassiveSkillList().clear();

        testPlayer.markAttributeCacheDirty();
        testMonster.markAttributeCacheDirty();
        testPlayer.getFinalAttributes();
        testMonster.getFinalAttributes();

        battleContext.battleLogs.clear();
    }
}
