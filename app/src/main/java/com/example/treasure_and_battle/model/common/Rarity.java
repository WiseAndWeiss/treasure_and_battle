package com.example.treasure_and_battle.model.common;

/**
 * 通用稀有度体系（全模块复用）
 * 包含：普通(白), 稀有(绿), 罕见(蓝), 史诗(紫), 传说(橙)
 * 依据《功能清单v1.md》定义。同时留出尚未说明具体数值的TODO项以便后续配置。
 */
public enum Rarity {

    // 格式：COMMON(id, 显示名称, 颜色ARGB, 全局出现概率, 怪物血量倍率, 怪物攻防倍率, 怪物全属性倍率, 词条数, 怪物金币&经验倍率, 战利品稀有度加成,
    // [TODO项]: 装备基础属性倍率, 宝石槽位数, 出售价格倍率)
    COMMON   (0, "普通", 0xFFFFFFFF, 0.60f, 1.0f, 1.0f,  1.0f,  1, 1.0f,  0,   
              1.0f, 1, 1.0f, 0),
    UNCOMMON (1, "稀有", 0xFF4CAF50, 0.25f, 1.5f, 1.25f, 1.0f,  2, 1.5f,  5,   
              1.25f, 2, 1.5f, 5),
    RARE     (2, "罕见", 0xFF2196F3, 0.10f, 2.0f, 1.25f, 1.0f,  3, 3.0f,  20,  
              1.5f, 3, 3.0f, 20),
    EPIC     (3, "史诗", 0xFF9C27B0, 0.045f, 2.5f, 1.5f,  1.25f, 4, 5.0f,  50,  
              1.75f, 4, 5.0f, 50),
    LEGENDARY(4, "传说", 0xFFFF9800, 0.005f, 3.0f, 2.0f,  1.5f,  5, 10.0f, 100, 
              2.0f, 5, 10.0f, 100);

    // ====================== Md文档明确要求的属性 ======================
    private final int id;
    private final String displayName;
    private final int color;                    // 代表颜色
    private final float globalProbability;      // 全局出现概率 (普通60%, 稀有25%...)

    // 怪物/战斗与掉落相关倍率
    private final float hpMultiplier;           // 怪物血量倍率
    private final float atkDefMultiplier;       // 怪物基础攻防倍率
    private final float allStatsMultiplier;     // 怪物全属性倍率
    private final int affixCount;               // 固定词条数
    private final float goldExpMultiplier;      // 怪物金币&经验倍率
    private final int lootRarityBonus;          // 战利品稀有度幸运加成（如 +5, +20）

    private final float equipmentStatMultiplier;// 装备基础属性倍率
    private final int gemSlotCount;             // 该稀有度开启的宝石槽数量
    private final float sellPriceMultiplier;    // 该稀有度装备物品的出售价格倍率
    private final int affixRarityBonus;       // 词条稀有度加成

    // 构造函数
    Rarity(int id, String displayName, int color, float globalProbability,
           float hpMultiplier, float atkDefMultiplier, float allStatsMultiplier,
           int affixCount, float goldExpMultiplier, int lootRarityBonus,
           float equipmentStatMultiplier, int gemSlotCount, float sellPriceMultiplier, int affixRarityBonus) {
        
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.globalProbability = globalProbability;
        
        this.hpMultiplier = hpMultiplier;
        this.atkDefMultiplier = atkDefMultiplier;
        this.allStatsMultiplier = allStatsMultiplier;
        this.affixCount = affixCount;
        this.goldExpMultiplier = goldExpMultiplier;
        this.lootRarityBonus = lootRarityBonus;

        this.equipmentStatMultiplier = equipmentStatMultiplier;
        this.gemSlotCount = gemSlotCount;
        this.sellPriceMultiplier = sellPriceMultiplier;
        this.affixRarityBonus = affixRarityBonus;
    }

    public static Rarity fromId(int id) {
        for (Rarity r : values()) {
            if (r.id == id) return r;
        }
        return null; // 或抛异常，根据需求调整
    }

    // ====================== Getters ======================
    public int getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }
    public float getGlobalProbability() { return globalProbability; }

    public float getHpMultiplier() { return hpMultiplier; }
    public float getAtkDefMultiplier() { return atkDefMultiplier; }
    public float getAllStatsMultiplier() { return allStatsMultiplier; }
    public int getAffixCount() { return affixCount; }
    public float getGoldExpMultiplier() { return goldExpMultiplier; }
    public int getLootRarityBonus() { return lootRarityBonus; }

    public float getEquipmentStatMultiplier() { return equipmentStatMultiplier; }
    public int getGemSlotCount() { return gemSlotCount; }
    public float getSellPriceMultiplier() { return sellPriceMultiplier; }
    public int getAffixRarityBonus() { return affixRarityBonus; }
}