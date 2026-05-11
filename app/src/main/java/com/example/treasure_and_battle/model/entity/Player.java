package com.example.treasure_and_battle.model.entity;

import android.content.Context;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.utils.AttributeUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * 玩家战斗实体
 * 纯战斗快照，由 Character.generatePlayer() 创建。
 * 持久数据（等级、经验、天赋、金币）和装备所有权归属 Character。
 */
public class Player extends BattleEntity {
    public com.example.treasure_and_battle.character.Character owner;

    private Map<EquipSlot, EquipItem> equippedItems = new HashMap<>();

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
        base.maxActionPoints = 2;
        base.hitRate = 0.9f;
        base.goldBonus = 1.0f;
        base.expBonus = 1.0f;
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
        EquipItem old = equippedItems.put(item.getSlot(), item);
        markAttributeCacheDirty();
        return old;
    }

    public EquipItem unequip(EquipSlot slot) {
        EquipItem removed = equippedItems.remove(slot);
        if (removed != null) {
            markAttributeCacheDirty();
        }
        return removed;
    }

    public EquipItem getEquippedItem(EquipSlot slot) {
        return equippedItems.get(slot);
    }

    public Collection<EquipItem> getEquippedItems() {
        return equippedItems.values();
    }

    public void copyEquipmentFrom(Map<EquipSlot, EquipItem> source) {
        this.equippedItems.clear();
        if (source != null) {
            this.equippedItems.putAll(source);
        }
        markAttributeCacheDirty();
    }
}
