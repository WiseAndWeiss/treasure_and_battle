package com.example.treasure_and_battle.model.profession;

import com.example.treasure_and_battle.model.skill.Skill;
import com.example.treasure_and_battle.model.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 职业抽象基类 (Profession)
 * 定义职业专有的能力（技能树、初始属性成长倾斜、天赋体系）
 */
public abstract class Profession {
    protected String professionName;   // 职业名称
    // 职业独立拥有的技能池
    protected List<Skill> passiveSkills; // 被动技能 (4个)
    protected List<Skill> activeSkills;  // 主动技能 (3-6个)
    protected List<Skill> eventSkills;   // 事件专属技能 (2-4个)

    public Profession(String name) {
        this.professionName = name;
        this.passiveSkills = new ArrayList<>();
        this.activeSkills = new ArrayList<>();
        this.eventSkills = new ArrayList<>();
        
        // 抽象方法调用：让具体职业子类初始化自己的技能列表
        initSkillPool();
    }

    /**
     * 子类必须实现：初始化本职业的技能列表
     */
    protected abstract void initSkillPool();
    
    /**
     * 子类必须实现：为玩家赋予初始的职业偏移体质和基础面板
     * 这个方法将在玩家创建时被调用（对应玩家的1级）
     */
    public abstract void applyInitialStats(Player player);

    /**
     * 玩家每升一级时，职业所赋予的专属属性成长
     * 子类重写：例如战士每级成长更多HP，法师更多MP
     */
    public abstract void applyLevelUpGrowth(Player player);

    public String getProfessionName() {
        return professionName;
    }

    public List<Skill> getPassiveSkills() { return passiveSkills; }
    public List<Skill> getActiveSkills() { return activeSkills; }
    public List<Skill> getEventSkills() { return eventSkills; }
}
