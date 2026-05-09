package com.example.treasure_and_battle.battle.action;

/**
 * 通用动作意图（用于玩家/怪物共享的动作决策建模）。
 */
public class ActionIntent {
    public enum IntentType {
        ATTACK,
        SKILL,
        ESCAPE
    }

    private String name;
    private String description;
    private IntentType type;
    private int apCost;
    private int mpCost;
    private double powerMultiplier;
    private int weight;
    private int priority;
    private float minSelfHpRate;
    private float maxSelfHpRate;
    private String actionRefId; // 对于 SKILL 可承载 skillId

    public ActionIntent(String name, String description, IntentType type, int apCost, int mpCost, double powerMultiplier, int weight) {
        this(name, description, type, apCost, mpCost, powerMultiplier, weight, 0, -1f, -1f, null);
    }

    public ActionIntent(String name, String description, IntentType type, int apCost, int mpCost,
                        double powerMultiplier, int weight, int priority,
                        float minSelfHpRate, float maxSelfHpRate, String actionRefId) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.apCost = Math.max(0, apCost);
        this.mpCost = Math.max(0, mpCost);
        this.powerMultiplier = powerMultiplier <= 0 ? 1.0 : powerMultiplier;
        this.weight = Math.max(1, weight);
        this.priority = priority;
        this.minSelfHpRate = minSelfHpRate;
        this.maxSelfHpRate = maxSelfHpRate;
        this.actionRefId = actionRefId;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public IntentType getType() { return type; }
    public int getApCost() { return apCost; }
    public int getMpCost() { return mpCost; }
    public double getPowerMultiplier() { return powerMultiplier; }
    public int getWeight() { return weight; }
    public int getPriority() { return priority; }
    public float getMinSelfHpRate() { return minSelfHpRate; }
    public float getMaxSelfHpRate() { return maxSelfHpRate; }
    public String getActionRefId() { return actionRefId; }
}
