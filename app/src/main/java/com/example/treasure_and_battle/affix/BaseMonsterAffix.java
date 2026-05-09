package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.model.common.TriggerType;


import com.example.treasure_and_battle.model.common.Rarity;

/**
 * 怪物专属词缀基类
 */
public abstract class BaseMonsterAffix extends BaseAffix {
    // 怪物本身没有槽位概念，因此不再需要 allowSlots
    
    public BaseMonsterAffix(int affixId, String affixName, String description, Rarity rarity,
                            TriggerType triggerType, float affixValue) {
        super(affixId, affixName, description, rarity, triggerType, new int[0], affixValue);
    }
}
