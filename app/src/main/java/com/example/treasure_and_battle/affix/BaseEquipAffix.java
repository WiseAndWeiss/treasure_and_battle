package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.model.common.TriggerType;


import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipCategory;

/**
 * 装备专属词缀基类
 */
public abstract class BaseEquipAffix extends BaseAffix {
    protected final EquipCategory[] allowCategories; // 允许出现的大类：WEAPON, ARMOR, ACCESSORY

    public BaseEquipAffix(int affixId, String affixName, String description, Rarity rarity,
                          TriggerType triggerType, EquipCategory[] allowCategories, float affixValue) {
        super(affixId, affixName, description, rarity, triggerType, new int[0], affixValue); 
        this.allowCategories = allowCategories;
    }

    public EquipCategory[] getAllowCategories() { return allowCategories; }
}
