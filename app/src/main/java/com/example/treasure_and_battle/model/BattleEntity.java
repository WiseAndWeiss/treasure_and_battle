package com.example.treasure_and_battle.model;

/**
 * 战斗实体基类 (BattleEntity)
 * 包含游戏中所有参与战斗的单位（玩家、怪物、NPC）所共有的基础属性和方法。
 */
public abstract class BattleEntity {
    protected String name;
    protected int level;

    // 核心生命/魔法属性
    protected int hp;
    protected int maxHp;
    protected int mp;
    protected int maxMp;

    // 核心战斗属性
    protected int physicalAttack;  // 物理攻击
    protected int magicAttack;     // 魔法攻击
    protected int physicalDefense; // 物理防御
    protected int magicDefense;    // 魔法防御
    protected int speed;           // 速度 (决定出手顺序、逃跑率)

    // 附加战斗属性
    protected double physicalCritRate;     // 物理暴击率
    protected double magicCritRate;        // 法术暴击率
    protected double physicalCritDamage;   // 物理暴击伤害
    protected double magicCritDamage;      // 法术暴击伤害
    protected double hitRate;              // 命中率
    protected double dodgeRate;            // 闪避率
    protected double statusResistance;     // 异常状态抵抗率

    // 六维核心属性
    protected int strength;    // 力量
    protected int agility;     // 敏捷
    protected int intelligence;// 智力
    protected int spirit;      // 精神
    protected int physique;    // 体魄
    protected int luck;        // 幸运

    // 战斗资源
    protected int actionPoints;            // 当前回合行动点
    protected int maxActionPoints;         // 最大行动点

    public BattleEntity(String name, int level) {
        this.name = name;
        this.level = level;
        
        // 赋予附加属性默认值
        this.physicalCritRate = 0.0;      // 默认0%暴击率
        this.magicCritRate = 0.0;         // 默认0%暴击率
        this.physicalCritDamage = 2.0;     // 默认200%暴击伤害
        this.magicCritDamage = 2.0;        // 默认200%暴击伤害
        this.hitRate = 0.9;                // 默认90%命中
        this.dodgeRate = 0.0;              // 默认0%闪避
        this.statusResistance = 0.0;       // 默认0抵抗
        this.maxActionPoints = 2;          // 默认3点行动力
        this.actionPoints = 2;
    }

    /**
     * 承受物理伤害
     */
    public int takePhysicalDamage(int rawDamage) {
        int finalDamage = Math.max(1, rawDamage - physicalDefense); // 至少造成1点伤害
        this.hp = Math.max(0, this.hp - finalDamage);
        return finalDamage;
    }

    /**
     * 承受魔法伤害
     */
    public int takeMagicDamage(int rawDamage) {
        int finalDamage = Math.max(1, rawDamage - magicDefense); // 至少造成1点伤害
        this.hp = Math.max(0, this.hp - finalDamage);
        return finalDamage;
    }

    /**
     * 恢复HP
     */
    public void healHp(int amount) {
        this.hp = Math.min(this.maxHp, this.hp + amount);
    }

    /**
     * 恢复MP
     */
    public void healMp(int amount) {
        this.mp = Math.min(this.maxMp, this.mp + amount);
    }

    /**
     * 判定实体是否死亡
     */
    public boolean isDead() {
        return this.hp <= 0;
    }

    // ================= Getters and Setters =================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public int getMp() { return mp; }
    public void setMp(int mp) { this.mp = mp; }

    public int getMaxMp() { return maxMp; }
    public void setMaxMp(int maxMp) { this.maxMp = maxMp; }

    public int getPhysicalAttack() { return physicalAttack; }
    public void setPhysicalAttack(int physicalAttack) { this.physicalAttack = physicalAttack; }

    public int getMagicAttack() { return magicAttack; }
    public void setMagicAttack(int magicAttack) { this.magicAttack = magicAttack; }

    public int getPhysicalDefense() { return physicalDefense; }
    public void setPhysicalDefense(int physicalDefense) { this.physicalDefense = physicalDefense; }

    public int getMagicDefense() { return magicDefense; }
    public void setMagicDefense(int magicDefense) { this.magicDefense = magicDefense; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    // ====== 新增附加属性 Getters & Setters ======
    public double getPhysicalCritRate() { return physicalCritRate; }
    public void setPhysicalCritRate(double physicalCritRate) { this.physicalCritRate = physicalCritRate; }

    public double getMagicCritRate() { return magicCritRate; }
    public void setMagicCritRate(double magicCritRate) { this.magicCritRate = magicCritRate; }

    public double getPhysicalCritDamage() { return physicalCritDamage; }
    public void setPhysicalCritDamage(double physicalCritDamage) { this.physicalCritDamage = physicalCritDamage; }

    public double getMagicCritDamage() { return magicCritDamage; }
    public void setMagicCritDamage(double magicCritDamage) { this.magicCritDamage = magicCritDamage; }

    public double getHitRate() { return hitRate; }
    public void setHitRate(double hitRate) { this.hitRate = hitRate; }

    public double getDodgeRate() { return dodgeRate; }
    public void setDodgeRate(double dodgeRate) { this.dodgeRate = dodgeRate; }

    public double getStatusResistance() { return statusResistance; }
    public void setStatusResistance(double statusResistance) { this.statusResistance = statusResistance; }

    public int getActionPoints() { return actionPoints; }
    public void setActionPoints(int actionPoints) { this.actionPoints = actionPoints; }

    public int getMaxActionPoints() { return maxActionPoints; }
    public void setMaxActionPoints(int maxActionPoints) { this.maxActionPoints = maxActionPoints; }
}
