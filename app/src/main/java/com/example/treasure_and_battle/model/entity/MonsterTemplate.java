package com.example.treasure_and_battle.model.entity;

public class MonsterTemplate {
    private int templateId;
    private String entityId;
    private String name;
    private int level;
    private int rarityId; // 1:普通, 2:稀罕 等

    private int maxHp;
    private int maxMp;
    private int patk;
    private int matt;
    private int pdef;
    private int mdef;
    private int speed;

    private int strength;
    private int agility;
    private int intelligence;
    private int spirit;
    private int physique;
    private int luck;

    private int expReward;
    private int goldReward;

    public static class IntentReference {
        private String intentId;
        private int weight;
        public String getIntentId() { return intentId; }
        public int getWeight() { return weight; }
    }

    private java.util.List<IntentReference> intents;

    public int getTemplateId() { return templateId; }
    public String getEntityId() { return entityId; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getRarityId() { return rarityId; }
    public int getMaxHp() { return maxHp; }
    public int getMaxMp() { return maxMp; }
    public int getPatk() { return patk; }
    public int getMatt() { return matt; }
    public int getPdef() { return pdef; }
    public int getMdef() { return mdef; }
    public int getSpeed() { return speed; }
    public int getStrength() { return strength; }
    public int getAgility() { return agility; }
    public int getIntelligence() { return intelligence; }
    public int getSpirit() { return spirit; }
    public int getPhysique() { return physique; }
    public int getLuck() { return luck; }
    public int getExpReward() { return expReward; }
    public int getGoldReward() { return goldReward; }
    public java.util.List<IntentReference> getIntents() { return intents; }
}