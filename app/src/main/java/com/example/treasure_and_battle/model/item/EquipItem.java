package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.affix.BaseAffix;
import java.util.ArrayList;
import java.util.List;

public class EquipItem extends Item {
    private int level;
    private EquipSlot slot;
    private AttributeSet baseAttributes; // 装备基础属性
    private AttributeSet finalAttributes; // 最终属性（计算词缀后的属性）
    private List<BaseAffix> affixes;
    private int maxSockets;

    public EquipItem(String id, String name, Rarity rarity, int baseValue, int level, EquipSlot slot) {
        super(id, name, rarity, baseValue, ItemType.EQUIPMENT, 1, 1);
        this.level = level;
        this.slot = slot;
        this.baseAttributes = new AttributeSet();
        this.finalAttributes = new AttributeSet();
        this.maxSockets = rarity.getId(); // 根据规则，宝石槽位默认由品质决定
        this.affixes = new ArrayList<>();
    }

    public int getLevel() { return level; }
    public EquipSlot getSlot() { return slot; }
    public AttributeSet getBaseAttributes() { return baseAttributes; }
    public int getMaxSockets() { return maxSockets; }
    public List<BaseAffix> getAffixes() { return affixes; }
    public void setAffixes(List<BaseAffix> affixes) { this.affixes = affixes; }
}
