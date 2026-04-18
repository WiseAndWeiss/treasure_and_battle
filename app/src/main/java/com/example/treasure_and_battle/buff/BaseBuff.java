package com.example.treasure_and_battle.buff;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;

/**
 * Buff抽象基类，和BaseAffix设计完全对齐
 * 所有具体Buff必须继承此类
 */
public abstract class BaseBuff {
    // ====================== 通用基础属性 ======================
    protected final String buffId;
    protected final String buffName;
    protected final String descriptionFormat;
    protected final BuffType buffType;
    protected final BuffTriggerType triggerType;
    protected final boolean isDispellable;

    // ====================== 生命周期属性 ======================
    protected int remainingDuration;
    protected final int maxDuration;
    protected int stackCount;
    protected final int maxStackCount;
    protected final boolean refreshOnApply;

    // ====================== 效果数值 ======================
    protected float buffValue;

    // ====================== 构造函数 ======================
    public BaseBuff(String buffId, String buffName, String descriptionFormat,
                    BuffType buffType, BuffTriggerType triggerType, boolean isDispellable,
                    int maxDuration, int maxStackCount, boolean refreshOnApply, float buffValue) {
        this.buffId = buffId;
        this.buffName = buffName;
        this.descriptionFormat = descriptionFormat;
        this.buffType = buffType;
        this.triggerType = triggerType;
        this.isDispellable = isDispellable;
        this.maxDuration = maxDuration;
        this.remainingDuration = maxDuration;
        this.maxStackCount = maxStackCount;
        this.stackCount = 1;
        this.refreshOnApply = refreshOnApply;
        this.buffValue = buffValue;
    }

    // ====================== 核心抽象方法（和BaseAffix对齐） ======================
    /**
     * 常驻属性加成：在属性重算时调用，对应Affix的applyAttributeBonus
     */
    public abstract void applyAttributeBonus(AttributeSet attributeSet);

    /**
     * 触发效果：在对应时机调用，对应Affix的onTrigger
     */
    public abstract void onTrigger(BattleContext context, BuffTriggerType triggerType);

    // ====================== 生命周期通用方法 ======================
    /**
     * 回合Tick，返回true表示Buff已过期
     */
    public boolean tick() {
        this.remainingDuration--;
        return this.remainingDuration <= 0;
    }

    /**
     * 尝试堆叠Buff，和词缀生成逻辑对齐
     */
    public void tryStack(BaseBuff newBuff) {
        if (this.stackCount < this.maxStackCount) {
            this.stackCount++;
        }
        if (this.refreshOnApply) {
            this.remainingDuration = this.maxDuration;
        }
        this.buffValue = newBuff.buffValue;
    }

    public boolean isExpired() {
        return this.remainingDuration <= 0;
    }

    // ====================== Getters（只读，和BaseAffix对齐） ======================
    public String getBuffId() { return buffId; }
    public String getBuffName() { return buffName; }
    public String getDescription() { return String.format(descriptionFormat, buffValue, stackCount); }
    public BuffType getBuffType() { return buffType; }
    public BuffTriggerType getTriggerType() { return triggerType; }
    public boolean isDispellable() { return isDispellable; }
    public int getRemainingDuration() { return remainingDuration; }
    public int getStackCount() { return stackCount; }
    public float getBuffValue() { return buffValue; }
}