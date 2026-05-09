package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.affix.BaseAffix;
import java.util.ArrayList;
import java.util.List;

public class EquipItem extends Item {
    private int level;
    private EquipSlot slot;
    private AttributeSet baseAttributes;
    private AttributeSet finalAttributes;
    private List<BaseAffix> affixes;
    private int maxSockets;
    private List<GemItem> socketedGems;

    public EquipItem(String id, String name, Rarity rarity, int baseValue, int level, EquipSlot slot) {
        super(id, name, rarity, baseValue, ItemType.EQUIPMENT, 1, 1);
        this.level = level;
        this.slot = slot;
        this.baseAttributes = new AttributeSet();
        this.finalAttributes = new AttributeSet();
        this.maxSockets = Math.max(1, rarity.getId() + 1);
        this.affixes = new ArrayList<>();
        this.socketedGems = new ArrayList<>();
    }

    public int getLevel() { return level; }
    public EquipSlot getSlot() { return slot; }
    public AttributeSet getBaseAttributes() { return baseAttributes; }
    public int getMaxSockets() { return maxSockets; }
    public List<GemItem> getSocketedGems() { return socketedGems; }
    public List<BaseAffix> getAffixes() { return affixes; }
    public void setAffixes(List<BaseAffix> affixes) { this.affixes = affixes; }

    public boolean socketGem(GemItem gem) {
        if (gem == null) return false;
        if (socketedGems.size() >= maxSockets) return false;
        for (GemItem g : socketedGems) {
            if (g.getGemType().equals(gem.getGemType())) return false;
        }
        socketedGems.add(gem);
        return true;
    }

    public GemItem unsocketGem(int index) {
        if (index < 0 || index >= socketedGems.size()) return null;
        return socketedGems.remove(index);
    }

    public AttributeSet getTotalGemBonuses() {
        AttributeSet total = new AttributeSet();
        EquipCategory cat = slot.getCategory();
        for (GemItem gem : socketedGems) {
            total.add(gem.getBonusForCategory(cat));
        }
        return total;
    }
}
