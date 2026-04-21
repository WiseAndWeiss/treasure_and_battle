package com.example.treasure_and_battle.model.entity;

import android.content.Context;

import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.battle.DamageType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.utils.AttributeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗实体基类 (BattleEntity)
 * 职责：仅持有战斗单位的核心数据和状态，不负责复杂属性计算和伤害计算
 * 核心设计：
 * 1. 所有属性封装到 AttributeSet 中，区分「基础属性」和「最终属性」
 * 2. 集成 Buff 列表和词缀列表（怪物用）
 * 3. 与 AttributeUtils、AffixManager 无缝对接
 */
public abstract class BattleEntity {
    // ====================== 核心标识信息 ======================
    protected final String entityId;   // 实体唯一ID（玩家UID/怪物模板ID）
    protected String name;              // 实体名称
    protected int level;                // 等级
    protected transient Context context; // 加一个 Context 引用

    // ====================== 属性系统（核心重构点） ======================
    // 基础属性：由等级、种族/职业决定的原始值，不会在战斗中改变
    protected AttributeSet baseAttributes;
    // 最终属性：基础属性 + 装备 + 词缀 + Buff 计算后的实时值，战斗中动态变化
    protected AttributeSet finalAttributes;
    // 属性缓存标记：true 表示 finalAttributes 需要重新计算
    protected boolean attributeCacheDirty = true;

    // ====================== 战斗资源（独立管理） ======================
    protected int currentHp;
    protected int currentMp;
    protected int currentActionPoints;

    // ====================== 状态系统（与现有架构集成） ======================
    // Buff 列表：当前生效的所有 Buff
    protected List<BaseBuff> activeBuffList = new ArrayList<>();
    // 词缀列表：怪物专属（玩家词缀由 AffixManager 统一管理）
    protected List<BaseAffix> monsterAffixList = new ArrayList<>();

    // ====================== 战斗状态标记 ======================
    protected boolean isDead = false;
    protected boolean isDefending = false; // 是否处于防御状态

    // ====================== 构造函数（简化，仅初始化核心数据） ======================
    public BattleEntity(String entityId, String name, int level, Context context) {
        this.entityId = entityId;
        this.name = name;
        this.level = level;
        this.context = context;

        // 初始化基础属性（由子类或工厂填充具体数值）
        this.baseAttributes = new AttributeSet();
        this.finalAttributes = new AttributeSet();

        // 战斗资源默认值（后续由 AttributeUtils 同步 maxHp/maxMp）
        this.currentHp = 100;
        this.currentMp = 50;
        this.currentActionPoints = 2;
    }


    // ====================== 核心抽象方法（子类必须实现） ======================
    /**
     * 初始化基础属性模板
     * 玩家子类：从职业配置加载
     * 怪物子类：从怪物模板配置加载
     */
    public abstract void initBaseAttributes();

    // ====================== 属性系统集成方法（与 AttributeUtils 联动） ======================
    /**
     * 标记属性缓存失效，下次获取最终属性时重新计算
     */
    public void markAttributeCacheDirty() {
        this.attributeCacheDirty = true;
    }

    /**
     * 获取最终属性（自动判断是否需要重新计算）
     * 注意：不要直接修改返回的 finalAttributes，修改请通过 markAttributeCacheDirty() 触发重算
     */
    public AttributeSet getFinalAttributes() {
        if (attributeCacheDirty) {
            // 调用 AttributeUtils 重新计算最终属性（玩家和怪物计算逻辑不同）
            recalculateFinalAttributes();
            attributeCacheDirty = false;
        }
        return finalAttributes;
    }

    /**
     * 重新计算最终属性（由子类实现具体逻辑）
     * 玩家：base + 装备 + 词缀 + Buff
     * 怪物：base + 怪物词缀 + Buff
     */
    protected void recalculateFinalAttributes()
    {
        AttributeUtils.calculateFinalAttributes(this, context);
    }


    // ====================== 战斗资源管理（通用方法） ======================
    /**
     * 承受伤害（直接扣血的逻辑）
     */
    public void takeDamage(int damage) {
        this.currentHp = Math.max(0, this.currentHp - damage);
        if (this.currentHp <= 0) {
            this.isDead = true;
        }
    }

    /**
     * 承受伤害（考虑防御状态和伤害类型的逻辑）
     */
    public void takeDamage(int damage, DamageType damageType) {
        int finalDamage = damage;
        if (damageType == DamageType.PHYSICAL) {
            int physicalDef = getFinalAttributes().physicalDef;
            finalDamage = Math.max(1, damage - physicalDef);
        } else if (damageType == DamageType.MAGICAL) {
            int magicalDef = getFinalAttributes().magicalDef;
            finalDamage = Math.max(1, damage - magicalDef);
        }
        // 真实伤害不受防御影响
        takeDamage(finalDamage);
    }

    /**
     * 恢复 HP
     */
    public void healHp(int amount) {
        int maxHp = getFinalAttributes().maxHp;
        this.currentHp = Math.min(maxHp, this.currentHp + amount);
    }

    /**
     * 恢复 MP
     */
    public void healMp(int amount) {
        int maxMp = getFinalAttributes().maxMp;
        this.currentMp = Math.min(maxMp, this.currentMp + amount);
    }

    /**
     * 消耗行动点
     */
    public boolean consumeActionPoints(int cost) {
        if (this.currentActionPoints >= cost) {
            this.currentActionPoints -= cost;
            return true;
        }
        return false;
    }

    /**
     * 重置行动点（回合开始时调用）
     */
    public void resetActionPoints() {
        this.currentActionPoints = getFinalAttributes().maxActionPoints;
    }

    // ====================== 简单 Getters & Setters（仅保留必要的） ======================
    public String getEntityId() { return entityId; }
    public String getName() { return name; }
    public Context getContext() { return context; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; markAttributeCacheDirty(); }

    public AttributeSet getBaseAttributes() { return baseAttributes; }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public int getCurrentMp() { return currentMp; }
    public void setCurrentMp(int currentMp) { this.currentMp = currentMp; }
    public int getCurrentActionPoints() { return currentActionPoints; }
    public void setCurrentActionPoints(int currentActionPoints) { this.currentActionPoints = currentActionPoints; }

    public List<BaseBuff> getActiveBuffList() { return activeBuffList; } // 直接返回引用，由于BuffManager需要操作此列表
    public List<BaseAffix> getMonsterAffixList() { return new ArrayList<>(monsterAffixList); }
    public void setMonsterAffixList(List<BaseAffix> affixList) {
        this.monsterAffixList = new ArrayList<>(affixList);
        markAttributeCacheDirty();
    }

    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }
    public boolean isDefending() { return isDefending; }
    public void setDefending(boolean defending) { isDefending = defending; }
}