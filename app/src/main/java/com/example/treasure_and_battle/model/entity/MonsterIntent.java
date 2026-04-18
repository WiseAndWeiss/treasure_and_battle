package com.example.treasure_and_battle.model.entity;

/**
 * 怪物意图类：代表怪物的攻击、技能、防御或施防等行为
 */
public class MonsterIntent {
    public enum IntentType {
        ATTACK,        // 造成伤害
        DEFEND,        // 增加护盾/防御
        BUFF,          // 增加自身状态
        DEBUFF,        // 给玩家挂负面效果
        HEAL,           // 恢复生命值
        ESCAPE          // 逃跑
    }

    private String name;           // 意图名称，如 "猛击", "防御态势"
    private String description;    // 意图描述
    private IntentType type;       // 意图类型
    private int apCost;            // 行动点消耗 (Action Points)
    private int mpCost;            // 魔法点消耗 (Mana Points)
    private double powerMultiplier;// 威力乘数（比如 1.5倍物理攻击 等）
    private int weight;            // 随机权重（权重越大，越容易被选中）

    public MonsterIntent(String name, String description, IntentType type, int apCost, int mpCost, double powerMultiplier, int weight) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.apCost = apCost;
        this.mpCost = mpCost;
        this.powerMultiplier = powerMultiplier;
        this.weight = weight;
    }

    // ================= Getters =================
    public String getName() { return name; }
    public String getDescription() { return description; }
    public IntentType getType() { return type; }
    public int getApCost() { return apCost; }
    public int getMpCost() { return mpCost; }
    public double getPowerMultiplier() { return powerMultiplier; }
    public int getWeight() { return weight; }
}