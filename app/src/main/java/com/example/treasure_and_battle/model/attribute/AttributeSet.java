package com.example.treasure_and_battle.model.attribute;

import java.util.HashMap;
import java.util.Map;

// 完整属性容器，承载六维属性+战斗属性
public class AttributeSet {
    // ====================== 属性修改来源追踪 ======================
    public enum Source {
        BASE,
        TALENT,
        EQUIPMENT,
        AFFIX,
        BUFF,
        GEM,
        PROFESSION,
        OTHER
    }

    private final Map<String, Map<Source, Float>> modificationLog = new HashMap<>();

    public void recordModification(String attributeName, Source source, float value) {
        modificationLog
            .computeIfAbsent(attributeName, k -> new HashMap<>())
            .merge(source, value, Float::sum);
    }

    public Map<Source, Float> getModificationSources(String attributeName) {
        return modificationLog.getOrDefault(attributeName, new HashMap<>());
    }

    public void resetModificationLog() {
        modificationLog.clear();
    }

    // 六维属性
    public int strength;     // 力量
    public int agility;      // 敏捷
    public int intelligence; // 智力
    public int spirit;       // 精神
    public int physique;     // 体魄
    public int luck;         // 幸运

    // 基础战斗属性
    public int maxHp;
    public int maxMp;
    public int physicalAtk;
    public int physicalDef;
    public int magicalAtk;
    public int magicalDef;
    public int speed;
    public int maxActionPoints; // 最大行动点数

    // 附加战斗属性（百分比）
    public float physicalCritRate;  // 物理暴击率
    public float physicalCritDmg;   // 物理暴击伤害
    public float magicalCritRate;   // 法术暴击率
    public float magicalCritDmg;    // 法术暴击伤害
    public float hitRate;           // 命中率
    public float dodgeRate;         // 闪避率
    public float debuffResist;      // 异常抵抗率
    public float mpCostReduction;   // 蓝耗减免
    public float damageReductionRate; // 伤害减免率（0.2 = 20%）

    // 额外收益属性
    public float lootRarityBonus;    // 战利品稀有度加成
    public float goldBonus;          // 金币获取倍率
    public float expBonus;           // 经验获取倍率

    // 百分比加成修饰池 (用于将所有百分比加成收拢在同一乘区)
    public float percentStrength;
    public float percentAgility;
    public float percentIntelligence;
    public float percentSpirit;
    public float percentPhysique;
    public float percentLuck;

    public float percentMaxHp;
    public float percentMaxMp;
    public float percentPhysicalAtk;
    public float percentPhysicalDef;
    public float percentMagicalAtk;
    public float percentMagicalDef;
    public float percentSpeed;

    // 构造函数：初始化全0属性
    public AttributeSet() {
        // 六维属性默认 0
        this.strength = 0;
        this.agility = 0;
        this.intelligence = 0;
        this.spirit = 0;
        this.physique = 0;
        this.luck = 0;

        // 核心百分比加成池（统一乘区，默认0，如0.2代表20%）
        this.percentStrength = 0f;
        this.percentAgility = 0f;
        this.percentIntelligence = 0f;
        this.percentSpirit = 0f;
        this.percentPhysique = 0f;
        this.percentLuck = 0f;

        this.percentMaxHp = 0f;
        this.percentMaxMp = 0f;
        this.percentPhysicalAtk = 0f;
        this.percentPhysicalDef = 0f;
        this.percentMagicalAtk = 0f;
        this.percentMagicalDef = 0f;
        this.percentSpeed = 0f;
    }

    // ====================== 实用方法 ======================

    /**
     * 将另一个 AttributeSet 的所有字段值加到当前对象上
     * 用于累加多个buff/装备的属性加成
     */
    public void add(AttributeSet other) {
        this.strength += other.strength;
        this.agility += other.agility;
        this.intelligence += other.intelligence;
        this.spirit += other.spirit;
        this.physique += other.physique;
        this.luck += other.luck;

        this.maxHp += other.maxHp;
        this.maxMp += other.maxMp;
        this.physicalAtk += other.physicalAtk;
        this.physicalDef += other.physicalDef;
        this.magicalAtk += other.magicalAtk;
        this.magicalDef += other.magicalDef;
        this.speed += other.speed;
        this.maxActionPoints += other.maxActionPoints;

        this.physicalCritRate += other.physicalCritRate;
        this.physicalCritDmg += other.physicalCritDmg;
        this.magicalCritRate += other.magicalCritRate;
        this.magicalCritDmg += other.magicalCritDmg;
        this.hitRate += other.hitRate;
        this.dodgeRate += other.dodgeRate;
        this.debuffResist += other.debuffResist;
        this.mpCostReduction += other.mpCostReduction;
        this.damageReductionRate += other.damageReductionRate;
        this.lootRarityBonus += other.lootRarityBonus;
        this.goldBonus += other.goldBonus;
        this.expBonus += other.expBonus;

        this.percentStrength += other.percentStrength;
        this.percentAgility += other.percentAgility;
        this.percentIntelligence += other.percentIntelligence;
        this.percentSpirit += other.percentSpirit;
        this.percentPhysique += other.percentPhysique;
        this.percentLuck += other.percentLuck;

        this.percentMaxHp += other.percentMaxHp;
        this.percentMaxMp += other.percentMaxMp;
        this.percentPhysicalAtk += other.percentPhysicalAtk;
        this.percentPhysicalDef += other.percentPhysicalDef;
        this.percentMagicalAtk += other.percentMagicalAtk;
        this.percentMagicalDef += other.percentMagicalDef;
        this.percentSpeed += other.percentSpeed;
    }

    /**
     * 将另一个 AttributeSet 的所有字段值复制到当前对象上
     * 用于从baseAttributes复制到finalAttributes
     */
    public void copyFrom(AttributeSet other) {
        this.strength = other.strength;
        this.agility = other.agility;
        this.intelligence = other.intelligence;
        this.spirit = other.spirit;
        this.physique = other.physique;
        this.luck = other.luck;

        this.maxHp = other.maxHp;
        this.maxMp = other.maxMp;
        this.physicalAtk = other.physicalAtk;
        this.physicalDef = other.physicalDef;
        this.magicalAtk = other.magicalAtk;
        this.magicalDef = other.magicalDef;
        this.speed = other.speed;
        this.maxActionPoints = other.maxActionPoints;

        this.physicalCritRate = other.physicalCritRate;
        this.physicalCritDmg = other.physicalCritDmg;
        this.magicalCritRate = other.magicalCritRate;
        this.magicalCritDmg = other.magicalCritDmg;
        this.hitRate = other.hitRate;
        this.dodgeRate = other.dodgeRate;
        this.debuffResist = other.debuffResist;
        this.mpCostReduction = other.mpCostReduction;
        this.damageReductionRate = other.damageReductionRate;
        this.lootRarityBonus = other.lootRarityBonus;
        this.goldBonus = other.goldBonus;
        this.expBonus = other.expBonus;

        this.percentStrength = other.percentStrength;
        this.percentAgility = other.percentAgility;
        this.percentIntelligence = other.percentIntelligence;
        this.percentSpirit = other.percentSpirit;
        this.percentPhysique = other.percentPhysique;
        this.percentLuck = other.percentLuck;

        this.percentMaxHp = other.percentMaxHp;
        this.percentMaxMp = other.percentMaxMp;
        this.percentPhysicalAtk = other.percentPhysicalAtk;
        this.percentPhysicalDef = other.percentPhysicalDef;
        this.percentMagicalAtk = other.percentMagicalAtk;
        this.percentMagicalDef = other.percentMagicalDef;
        this.percentSpeed = other.percentSpeed;
    }

    /**
     * 创建当前对象的深拷贝
     * @return 新的AttributeSet对象，包含所有相同的值
     */
    public AttributeSet clone() {
        AttributeSet cloned = new AttributeSet();
        cloned.copyFrom(this);
        return cloned;
    }
}
