package com.example.treasure_and_battle.model.entity;

import android.content.Context;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.utils.AttributeUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家战斗实体
 * 纯战斗快照，由 Character.generatePlayer() 创建。
 * 持久数据（等级、经验、天赋、金币）和装备所有权归属 Character。
 */
public class Player extends BattleEntity {
    public com.example.treasure_and_battle.character.Character owner;

    private Map<EquipSlot, EquipItem> equippedItems = new HashMap<>();
    private EquipItem leftRing;
    private EquipItem rightRing;

    // ====================== 构造 ======================

    public Player(String name, Context context) {
        super("player_default", name, 1, context);
        initBaseAttributes();
    }

    // ====================== 基础属性 ======================

    @Override
    public void initBaseAttributes() {
        AttributeSet base = this.baseAttributes;

        base.strength = 0;
        base.agility = 0;
        base.intelligence = 0;
        base.spirit = 0;
        base.physique = 0;
        base.luck = 0;

        base.maxHp = 20;
        base.maxMp = 10;
        applyBaseCombatAttributes(base);

        this.currentHp = base.maxHp;
        this.currentMp = base.maxMp;
        this.currentActionPoints = base.maxActionPoints;

        markAttributeCacheDirty();
    }

    public static void applyBaseCombatAttributes(AttributeSet base) {
        base.physicalAtk = 2;
        base.physicalDef = 1;
        base.magicalAtk = 2;
        base.magicalDef = 1;
        base.speed = 10;
        base.maxActionPoints = 3;
        base.hitRate = 0.9f;
        base.goldBonus = 1.0f;
        base.expBonus = 1.0f;
    }

    public static void computeFullBaseCombatAttributes(AttributeSet base) {
        base.physicalAtk = 2 + base.strength;
        base.physicalDef = 1 + base.physique / 2;
        base.magicalAtk = 2 + base.intelligence;
        base.magicalDef = 1 + base.spirit / 2;
        base.speed = 10 + base.agility;
        base.maxActionPoints = 3;
        base.hitRate = 0.9f + base.agility * 0.003f;
        base.physicalCritRate = base.luck * 0.002f;
        base.physicalCritDmg = 2.0f + base.strength * 0.005f;
        base.magicalCritRate = base.luck * 0.002f;
        base.magicalCritDmg = 2.0f + base.intelligence * 0.005f;
        base.dodgeRate = base.agility * 0.004f;
        base.debuffResist = (base.spirit + base.physique) * 0.004f;
        base.mpCostReduction = base.spirit * 0.005f;
        base.lootRarityBonus = base.luck;
        base.goldBonus = 1.0f + base.luck * 0.01f;
        base.expBonus = 1.0f + base.luck * 0.01f;

        base.maxHp = base.maxHp + base.physique * 2 + Math.max(base.strength, 0);
        base.maxMp = base.maxMp + base.intelligence * 2 + base.spirit;
    }

    @Override
    protected void recalculateFinalAttributes() {
        this.finalAttributes.copyFrom(this.baseAttributes);
        AttributeSet calculated = AttributeUtils.calculateFinalAttributes(this, this.getContext());
        this.finalAttributes.copyFrom(calculated);

        this.currentHp = Math.min(this.currentHp, this.finalAttributes.maxHp);
        this.currentMp = Math.min(this.currentMp, this.finalAttributes.maxMp);
    }

    // ====================== 装备 ======================

    public EquipItem equip(EquipItem item) {
        if (item == null) return null;
        if (item.getSlot() == EquipSlot.RING) {
            EquipItem old = leftRing != null ? leftRing : rightRing;
            if (leftRing == null) {
                leftRing = item;
            } else if (rightRing == null) {
                rightRing = item;
            } else {
                leftRing = item;
            }
            markAttributeCacheDirty();
            return old;
        }
        EquipItem old = equippedItems.put(item.getSlot(), item);
        markAttributeCacheDirty();
        return old;
    }

    public EquipItem unequip(EquipSlot slot) {
        if (slot == EquipSlot.RING) {
            EquipItem removed = leftRing;
            leftRing = null;
            if (removed != null) markAttributeCacheDirty();
            return removed;
        }
        EquipItem removed = equippedItems.remove(slot);
        if (removed != null) markAttributeCacheDirty();
        return removed;
    }

    public EquipItem getEquippedItem(EquipSlot slot) {
        if (slot == EquipSlot.RING) return leftRing;
        return equippedItems.get(slot);
    }

    public EquipItem getLeftRing() { return leftRing; }
    public EquipItem getRightRing() { return rightRing; }

    public Collection<EquipItem> getEquippedItems() {
        List<EquipItem> all = new ArrayList<>(equippedItems.values());
        if (leftRing != null) all.add(leftRing);
        if (rightRing != null) all.add(rightRing);
        return all;
    }

    public void copyEquipmentFrom(Map<EquipSlot, EquipItem> source) {
        this.equippedItems.clear();
        if (source != null) {
            this.equippedItems.putAll(source);
        }
        markAttributeCacheDirty();
    }

    public void copyRingsFrom(EquipItem sourceLeft, EquipItem sourceRight) {
        this.leftRing = sourceLeft;
        this.rightRing = sourceRight;
        markAttributeCacheDirty();
    }
}
