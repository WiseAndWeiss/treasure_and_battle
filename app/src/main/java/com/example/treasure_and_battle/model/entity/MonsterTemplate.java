package com.example.treasure_and_battle.model.entity;

public class MonsterTemplate {
    private int templateId;
    private String entityId;
    private String name;
    private int level;
    private int rarityId;

    private int strength;
    private int agility;
    private int intelligence;
    private int spirit;
    private int physique;
    private int luck;

    private float hpMultiplier;
    private float atkMultiplier;
    private float defMultiplier;
    private float spdMultiplier;

    private int expReward;
    private int goldReward;

    public static class SkillReference {
        private String skillId;
        private int weight;
        private int priority;
        private int apCost;
        private int mpCost;
        private double powerMultiplier;
        private int level;

        public String getSkillId() { return skillId; }
        public int getWeight() { return weight; }
        public int getPriority() { return priority; }
        public int getApCost() { return apCost; }
        public int getMpCost() { return mpCost; }
        public double getPowerMultiplier() { return powerMultiplier; }
        public int getLevel() { return level <= 0 ? 1 : Math.min(level, 4); }
    }

    private java.util.List<SkillReference> skillPool;

    public static class DropEntry {
        private String materialId;
        private float dropRate;

        public String getMaterialId() { return materialId; }
        public float getDropRate() { return dropRate; }
    }

    private java.util.List<DropEntry> dropTable;

    public int getTemplateId() { return templateId; }
    public String getEntityId() { return entityId; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getRarityId() { return rarityId; }
    public int getStrength() { return strength; }
    public int getAgility() { return agility; }
    public int getIntelligence() { return intelligence; }
    public int getSpirit() { return spirit; }
    public int getPhysique() { return physique; }
    public int getLuck() { return luck; }
    public float getHpMultiplier() { return hpMultiplier; }
    public float getAtkMultiplier() { return atkMultiplier; }
    public float getDefMultiplier() { return defMultiplier; }
    public float getSpdMultiplier() { return spdMultiplier; }
    public int getExpReward() { return expReward; }
    public int getGoldReward() { return goldReward; }
    public java.util.List<SkillReference> getSkillPool() { return skillPool; }
    public java.util.List<DropEntry> getDropTable() { return dropTable; }
}
