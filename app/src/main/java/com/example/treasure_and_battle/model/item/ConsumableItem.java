package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.common.Rarity;

import java.util.ArrayList;
import java.util.List;

public class ConsumableItem extends Item {
    private boolean usableInBattle;
    private boolean usableOutBattle;
    private List<Effect> effects;

    public enum EffectType {
        HEAL_HP, HEAL_MP, DAMAGE, BUFF, CLEANSE, ESCAPE, UTILITY
    }

    public enum Target {
        SELF, SINGLE_ENEMY, ALL_ENEMIES
    }

    public static class BuffEntry {
        public int buffTemplateId;
        public int stacks;
        public int duration;
        public boolean isPercent;

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
        public boolean isPercent;
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
}
