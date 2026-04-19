package com.example.treasure_and_battle.model.entity;

public class IntentTemplate {
    private String intentId;
    private String name;
    private String description;
    private MonsterIntent.IntentType type;
    private int apCost;
    private int mpCost;
    private double powerMultiplier;

    public String getIntentId() { return intentId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public MonsterIntent.IntentType getType() { return type; }
    public int getApCost() { return apCost; }
    public int getMpCost() { return mpCost; }
    public double getPowerMultiplier() { return powerMultiplier; }
}
