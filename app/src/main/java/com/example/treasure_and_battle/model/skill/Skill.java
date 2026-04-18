package com.example.treasure_and_battle.model.skill;

/**
 * 技能基类 (Skill)
 * 用于构建各个职业的主动技能、被动技能与事件技能。
 */
public class Skill {
    public enum SkillType {
        PASSIVE,   // 战斗被动技能
        ACTIVE,    // 战斗主动技能
        EVENT      // 事件专属技能
    }

    private String name;           // 技能名称
    private String description;    // 技能详情描述
    private SkillType type;        // 技能类型
    private int currentLevel;      // 当前技能等级 (默认1, 最大5)
    private int maxLevel;          // 最大等级 (通常为5)
    private int costActionPoints;  // 主动技能消耗的行动点
    private int costMp;            // 主动技能消耗的魔法值
    private int costHp;            // 少数血魔法技能消耗的生命值

    public Skill(String name, String description, SkillType type) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.currentLevel = 0; // 0代表未解锁
        this.maxLevel = 5;
        this.costActionPoints = 0;
        this.costMp = 0;
        this.costHp = 0;
    }

    public Skill setCosts(int actionPoints, int mp, int hp) {
        this.costActionPoints = actionPoints;
        this.costMp = mp;
        this.costHp = hp;
        return this; // Builder 模式方便链式调用
    }

    /**
     * 升级技能
     * 初次加点解锁技能(1级)，后续每加1点，技能效果增强，消耗小幅上升（具体由具体业务逻辑读取等级计算）
     */
    public boolean upgrade() {
        if (currentLevel < maxLevel) {
            currentLevel++;
            // 根据《文档》中每升1级，消耗小幅提升10%的简单逻辑：
            if (currentLevel > 1) {
                costActionPoints = (int) Math.ceil(costActionPoints * 1.1);
                costMp = (int) Math.ceil(costMp * 1.1);
                costHp = (int) Math.ceil(costHp * 1.1);
            }
            return true;
        }
        return false;
    }

    public boolean isUnlocked() {
        return currentLevel > 0;
    }

    // ================= Getters & Setters =================
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SkillType getType() { return type; }
    public int getCurrentLevel() { return currentLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getCostActionPoints() { return costActionPoints; }
    public int getCostMp() { return costMp; }
    public int getCostHp() { return costHp; }
}
