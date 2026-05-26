package com.example.treasure_and_battle.model.item.consumable;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;

import java.util.ArrayList;
import java.util.List;

public class ConsumableItem extends Item {
    private boolean usableInBattle;
    private boolean usableOutBattle;
    private List<Effect> effects;

    public enum EffectType {
        HEAL_HP, HEAL_MP, DAMAGE, BUFF, CLEANSE, ESCAPE, UTILITY_PASSIVE, UTILITY_ACTIVE
    }

    public enum Target {
        SELF, SINGLE_ENEMY, ALL_ENEMIES
    }

    public static class BuffEntry {
        public int buffTemplateId;
        public int stacks;
        public int duration;
        public String valueType;

        public BuffEntry() {}
    }

    public static class DebuffEntry {
        public int buffTemplateId;
        public float stackRatio;

        public DebuffEntry() {}
    }

    public static class Effect {
        public EffectType type;
        public Target target;
        public float value;
        public String valueType;
        public int shieldDuration;
        public String utilityId;
        public List<BuffEntry> buffs;
        public List<DebuffEntry> debuffs;

        public Effect() {}

        public Effect(EffectType type) {
            this.type = type;
        }
    }

    public ConsumableItem(String id, String name, Rarity rarity, int baseValue,
                          int maxStack, boolean usableInBattle, boolean usableOutBattle,
                          List<Effect> effects, String description) {
        super(id, name, rarity, baseValue, ItemType.CONSUMABLE, 1, maxStack);
        this.usableInBattle = usableInBattle;
        this.usableOutBattle = usableOutBattle;
        this.effects = effects != null ? effects : new ArrayList<>();
        this.description = description;
    }

    public boolean isUsableInBattle() { return usableInBattle; }
    public boolean isUsableOutBattle() { return usableOutBattle; }
    public List<Effect> getEffects() { return effects; }

    public boolean isAnyPassiveUtility() {
        if (effects == null) return false;
        for (Effect e : effects) {
            if (e.type == EffectType.UTILITY_PASSIVE) return true;
        }
        return false;
    }
}
