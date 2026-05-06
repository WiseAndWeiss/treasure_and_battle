package com.example.treasure_and_battle.buff;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

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
     * 触发效果：在对应时机调用
     * @param owner 拥有该Buff的实体
     * @param context 战斗上下文
     * @param triggerType 触发类型
     */
    public abstract void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType);

    // ====================== 生命周期通用方法 ======================
    /**
     * 回合Tick衰减机制。
     * 默认逻辑：每回合持续时间-1。
     * 如果子类有特殊衰减逻辑（如层数衰减、层数减半等），可以重写此方法。
     * @return 返回 true 表示 Buff 已失效，应当被移除
     */
    public boolean tick() {
        if (this.remainingDuration > 0) {
            this.remainingDuration--;
        }
        return this.isExpired();
    }

    /**
     * 尝试堆叠Buff，和词缀生成逻辑对齐
     */
    public void tryStack(BaseBuff newBuff) {
        // 默认堆叠逻辑：如果当前层数未满，直接加1层；如果已满，则不增加层数但可能刷新持续时间
        if (this.stackCount < this.maxStackCount) {
            this.stackCount++;
        }
        // 如果刷新持续时间，直接重置为最大持续时间
        if (this.refreshOnApply) {
            this.remainingDuration = this.maxDuration;
        }
        // 选择更高的数值进行覆盖
        this.buffValue = Math.max(this.buffValue, newBuff.buffValue);
    }

    public void tryStack(BaseBuff newBuff, int additionalStacks) {
        if (this.stackCount < this.maxStackCount) {
            this.stackCount = Math.min(this.stackCount + additionalStacks, this.maxStackCount);
        }
        if (this.refreshOnApply) {
            this.remainingDuration = this.maxDuration;
        }
        this.buffValue = Math.max(this.buffValue, newBuff.buffValue);
    }

    // 直接设置层数，通常用于特殊Buff的衰减逻辑
    public void setStack(int stackCount) {
        this.stackCount = Math.max(0, Math.min(stackCount, this.maxStackCount));
    }

    public void setRemainingDuration(int duration) {
        this.remainingDuration = Math.max(0, Math.min(duration, this.maxDuration));
    }

    /**
     * 判断Buff是否应该被移除。
     * 只要持续回合耗尽（等于0），或者层数归零（<=0），即视为过期。
     * （如果 remainingDuration < 0 可以作为永久Buff的标记）
     */
    public boolean isExpired() {
        return this.remainingDuration == 0 || this.stackCount <= 0;
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