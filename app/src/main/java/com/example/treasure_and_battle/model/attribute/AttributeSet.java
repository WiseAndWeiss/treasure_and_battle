package com.example.treasure_and_battle.model.attribute;

// 完整属性容器，承载六维属性+战斗属性
public class AttributeSet {
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

        // 核心战斗属性默认 0
        this.physicalAtk = 0;
        this.magicalAtk = 0;
        this.physicalDef = 0;
        this.magicalDef = 0;
        this.speed = 0;

        // 资源上限默认值
        this.maxHp = 0;
        this.maxMp = 0;
        this.maxActionPoints = 0;

        // 附加属性默认值
        this.physicalCritRate = 0.0f;
        this.magicalCritRate = 0.0f;
        this.physicalCritDmg = 0.0f;
        this.magicalCritDmg = 0.0f;
        this.hitRate = 0.0f;
        this.dodgeRate = 0.0f;
        this.debuffResist = 0.0f;
        this.mpCostReduction = 0.0f;
        this.damageReductionRate = 0.0f;

        // 额外收益默认值
        this.lootRarityBonus = 0.0f;
        this.goldBonus = 0.0f;
        this.expBonus = 0.0f;
    }

    // 加法叠加另一个AttributeSet（用于天赋、装备、技能被动）
    public void add(AttributeSet other) {
        strength += other.strength;
        agility += other.agility;
        intelligence += other.intelligence;
        spirit += other.spirit;
        physique += other.physique;
        luck += other.luck;
        maxHp += other.maxHp;
        maxMp += other.maxMp;
        maxActionPoints += other.maxActionPoints;
        physicalAtk += other.physicalAtk;
        physicalDef += other.physicalDef;
        magicalAtk += other.magicalAtk;
        magicalDef += other.magicalDef;
        speed += other.speed;
        physicalCritRate += other.physicalCritRate;
        physicalCritDmg += other.physicalCritDmg;
        magicalCritRate += other.magicalCritRate;
        magicalCritDmg += other.magicalCritDmg;
        hitRate += other.hitRate;
        dodgeRate += other.dodgeRate;
        debuffResist += other.debuffResist;
        mpCostReduction += other.mpCostReduction;
        damageReductionRate += other.damageReductionRate;
        lootRarityBonus += other.lootRarityBonus;
        goldBonus += other.goldBonus;
        expBonus += other.expBonus;

        percentStrength += other.percentStrength;
        percentAgility += other.percentAgility;
        percentIntelligence += other.percentIntelligence;
        percentSpirit += other.percentSpirit;
        percentPhysique += other.percentPhysique;
        percentLuck += other.percentLuck;
        percentMaxHp += other.percentMaxHp;
        percentMaxMp += other.percentMaxMp;
        percentPhysicalAtk += other.percentPhysicalAtk;
        percentPhysicalDef += other.percentPhysicalDef;
        percentMagicalAtk += other.percentMagicalAtk;
        percentMagicalDef += other.percentMagicalDef;
        percentSpeed += other.percentSpeed;
    }

    // 乘法叠加（单乘区等独立乘法机制使用）
    public void multiply(float multiplier) {
        physicalAtk *= multiplier;
        physicalDef *= multiplier;
        magicalAtk *= multiplier;
        magicalDef *= multiplier;
        speed *= multiplier;
    }

    // 克隆方法，创建一个属性的深复制（用于计算临时属性）
    public void copyFrom(AttributeSet other) {
        this.strength = other.strength;
        this.agility = other.agility;
        this.intelligence = other.intelligence;
        this.spirit = other.spirit;
        this.physique = other.physique;
        this.luck = other.luck;

        this.physicalAtk = other.physicalAtk;
        this.magicalAtk = other.magicalAtk;
        this.physicalDef = other.physicalDef;
        this.magicalDef = other.magicalDef;
        this.speed = other.speed;

        this.maxHp = other.maxHp;
        this.maxMp = other.maxMp;
        this.maxActionPoints = other.maxActionPoints;

        this.physicalCritRate = other.physicalCritRate;
        this.magicalCritRate = other.magicalCritRate;
        this.physicalCritDmg = other.physicalCritDmg;
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

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getAgility() {
        return agility;
    }

    public void setAgility(int agility) {
        this.agility = agility;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getSpirit() {
        return spirit;
    }

    public void setSpirit(int spirit) {
        this.spirit = spirit;
    }

    public int getPhysique() {
        return physique;
    }

    public void setPhysique(int physique) {
        this.physique = physique;
    }

    public int getLuck() {
        return luck;
    }

    public void setLuck(int luck) {
        this.luck = luck;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getMaxMp() {
        return maxMp;
    }

    public void setMaxMp(int maxMp) {
        this.maxMp = maxMp;
    }

    public int getPhysicalAtk() {
        return physicalAtk;
    }

    public void setPhysicalAtk(int physicalAtk) {
        this.physicalAtk = physicalAtk;
    }

    public int getPhysicalDef() {
        return physicalDef;
    }

    public void setPhysicalDef(int physicalDef) {
        this.physicalDef = physicalDef;
    }

    public int getMagicalAtk() {
        return magicalAtk;
    }

    public void setMagicalAtk(int magicalAtk) {
        this.magicalAtk = magicalAtk;
    }

    public int getMagicalDef() {
        return magicalDef;
    }

    public void setMagicalDef(int magicalDef) {
        this.magicalDef = magicalDef;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getMaxActionPoints() {
        return maxActionPoints;
    }

    public void setMaxActionPoints(int maxActionPoints) {
        this.maxActionPoints = maxActionPoints;
    }

    public float getPhysicalCritRate() {
        return physicalCritRate;
    }

    public void setPhysicalCritRate(float physicalCritRate) {
        this.physicalCritRate = physicalCritRate;
    }

    public float getPhysicalCritDmg() {
        return physicalCritDmg;
    }

    public void setPhysicalCritDmg(float physicalCritDmg) {
        this.physicalCritDmg = physicalCritDmg;
    }

    public float getMagicalCritRate() {
        return magicalCritRate;
    }

    public void setMagicalCritRate(float magicalCritRate) {
        this.magicalCritRate = magicalCritRate;
    }

    public float getMagicalCritDmg() {
        return magicalCritDmg;
    }

    public void setMagicalCritDmg(float magicalCritDmg) {
        this.magicalCritDmg = magicalCritDmg;
    }

    public float getDodgeRate() {
        return dodgeRate;
    }

    public void setDodgeRate(float dodgeRate) {
        this.dodgeRate = dodgeRate;
    }

    public float getHitRate() {
        return hitRate;
    }

    public void setHitRate(float hitRate) {
        this.hitRate = hitRate;
    }

    public float getDebuffResist() {
        return debuffResist;
    }

    public void setDebuffResist(float debuffResist) {
        this.debuffResist = debuffResist;
    }

    public float getMpCostReduction() {
        return mpCostReduction;
    }

    public void setMpCostReduction(float mpCostReduction) {
        this.mpCostReduction = mpCostReduction;
    }

    public float getLootRarityBonus() {
        return lootRarityBonus;
    }

    public void setLootRarityBonus(float lootRarityBonus) {
        this.lootRarityBonus = lootRarityBonus;
    }

    public float getGoldBonus() {
        return goldBonus;
    }

    public void setGoldBonus(float goldBonus) {
        this.goldBonus = goldBonus;
    }

    public float getExpBonus() {
        return expBonus;
    }

    public void setExpBonus(float expBonus) {
        this.expBonus = expBonus;
    }
}