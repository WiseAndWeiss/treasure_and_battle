package com.example.treasure_and_battle.utils;

import android.content.Context;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.affix.BaseAffix;

import java.util.Collection;

public class AttributeUtils {

    public static AttributeSet calculateFinalAttributes(BattleEntity entity, Context context) {
        AttributeSet modifiers = new AttributeSet();

        if (entity instanceof Player) {
            applyPlayerEquipmentBonus((Player) entity, modifiers);
        } else if (entity instanceof Monster) {
            applyEntityAffixBonus((Monster) entity, modifiers);
        }

        BuffManager.getInstance(context).applyAllBuffAttributeBonus(modifiers, entity);

        if (entity.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : entity.getPassiveSkillList()) {
                passiveSkill.applyAttributeBonus(modifiers);
            }
        }

        AttributeSet finalAttr = deriveFinalFromBase(entity.getBaseAttributes(), modifiers);
        applyHardCaps(finalAttr);
        return finalAttr;
    }

    public static AttributeSet calculateCharacterAttributes(Character character) {
        if (character == null) {
            return new AttributeSet();
        }
        AttributeSet base = buildCharacterBaseAttributes(character);
        AttributeSet modifiers = new AttributeSet();

        applyEquipmentBonusesFromCharacter(character, modifiers);

        AttributeSet finalAttr = deriveFinalFromBase(base, modifiers);
        applyHardCaps(finalAttr);
        return finalAttr;
    }

    private static AttributeSet buildCharacterBaseAttributes(Character character) {
        AttributeSet base = new AttributeSet();
        base.strength = character.getAllocatedStrength();
        base.agility = character.getAllocatedAgility();
        base.intelligence = character.getAllocatedIntelligence();
        base.spirit = character.getAllocatedSpirit();
        base.physique = character.getAllocatedPhysique();
        base.luck = character.getAllocatedLuck();

        base.maxHp = character.getBaseMaxHp();
        base.maxMp = character.getBaseMaxMp();
        Player.applyBaseCombatAttributes(base);

        return base;
    }

    private static void applyEquipmentBonusesFromCharacter(Character character, AttributeSet modifiers) {
        Collection<EquipItem> items = character.getEquippedItems();
        if (items == null) return;
        for (EquipItem item : items) {
            if (item == null) continue;
            modifiers.add(item.getBaseAttributes());
            if (item.getAffixes() != null) {
                for (BaseAffix affix : item.getAffixes()) {
                    if (affix.getTriggerType() == TriggerType.PERMANENT) {
                        affix.applyAttributeBonus(modifiers);
                    }
                }
            }
            modifiers.add(item.getTotalGemBonuses());
        }
    }

    static AttributeSet deriveFinalFromBase(AttributeSet base, AttributeSet modifiers) {
        AttributeSet finalAttr = new AttributeSet();
        finalAttr.copyFrom(base);

        finalAttr.strength = (int) (base.strength * (1f + modifiers.percentStrength)) + modifiers.strength;
        finalAttr.agility = (int) (base.agility * (1f + modifiers.percentAgility)) + modifiers.agility;
        finalAttr.intelligence = (int) (base.intelligence * (1f + modifiers.percentIntelligence)) + modifiers.intelligence;
        finalAttr.spirit = (int) (base.spirit * (1f + modifiers.percentSpirit)) + modifiers.spirit;
        finalAttr.physique = (int) (base.physique * (1f + modifiers.percentPhysique)) + modifiers.physique;
        finalAttr.luck = (int) (base.luck * (1f + modifiers.percentLuck)) + modifiers.luck;

        int diffStrength = finalAttr.strength - base.strength;
        int diffPhysique = finalAttr.physique - base.physique;
        int diffIntelligence = finalAttr.intelligence - base.intelligence;
        int diffSpirit = finalAttr.spirit - base.spirit;
        int diffAgility = finalAttr.agility - base.agility;
        int diffLuck = finalAttr.luck - base.luck;

        int strForHpBase = Math.max(base.strength, 0);
        int strForHpFinal = Math.max(finalAttr.strength, 0);

        finalAttr.maxHp += diffPhysique * 2 + (strForHpFinal - strForHpBase);
        finalAttr.maxMp += diffIntelligence * 2 + diffSpirit;
        finalAttr.physicalAtk += diffStrength;
        finalAttr.physicalDef += diffPhysique / 2;
        finalAttr.magicalAtk += diffIntelligence;
        finalAttr.magicalDef += diffSpirit / 2;
        finalAttr.speed += diffAgility;

        finalAttr.physicalCritRate += diffLuck * 0.002f;
        finalAttr.physicalCritDmg += diffStrength * 0.005f;
        finalAttr.magicalCritRate += diffLuck * 0.002f;
        finalAttr.magicalCritDmg += diffIntelligence * 0.005f;
        finalAttr.hitRate += diffAgility * 0.003f;
        finalAttr.dodgeRate += diffAgility * 0.004f;
        finalAttr.debuffResist += (diffSpirit + diffPhysique) * 0.004f;
        finalAttr.mpCostReduction += diffSpirit * 0.005f;
        finalAttr.lootRarityBonus += diffLuck;
        finalAttr.goldBonus += diffLuck * 0.01f;
        finalAttr.expBonus += diffLuck * 0.01f;

        finalAttr.physicalAtk = Math.round(finalAttr.physicalAtk * (1f + modifiers.percentPhysicalAtk)) + modifiers.physicalAtk;
        finalAttr.magicalAtk = Math.round(finalAttr.magicalAtk * (1f + modifiers.percentMagicalAtk)) + modifiers.magicalAtk;
        finalAttr.physicalDef = Math.round(finalAttr.physicalDef * (1f + modifiers.percentPhysicalDef)) + modifiers.physicalDef;
        finalAttr.magicalDef = Math.round(finalAttr.magicalDef * (1f + modifiers.percentMagicalDef)) + modifiers.magicalDef;
        finalAttr.speed = Math.round(finalAttr.speed * (1f + modifiers.percentSpeed)) + modifiers.speed;

        finalAttr.maxHp = Math.round(finalAttr.maxHp * (1f + modifiers.percentMaxHp)) + modifiers.maxHp;
        finalAttr.maxMp = Math.round(finalAttr.maxMp * (1f + modifiers.percentMaxMp)) + modifiers.maxMp;

        finalAttr.physicalCritRate += modifiers.physicalCritRate;
        finalAttr.physicalCritDmg += modifiers.physicalCritDmg;
        finalAttr.magicalCritRate += modifiers.magicalCritRate;
        finalAttr.magicalCritDmg += modifiers.magicalCritDmg;
        finalAttr.hitRate += modifiers.hitRate;
        finalAttr.dodgeRate += modifiers.dodgeRate;
        finalAttr.debuffResist += modifiers.debuffResist;
        finalAttr.mpCostReduction += modifiers.mpCostReduction;
        finalAttr.lootRarityBonus += modifiers.lootRarityBonus;
        finalAttr.goldBonus += modifiers.goldBonus;
        finalAttr.expBonus += modifiers.expBonus;

        return finalAttr;
    }

    private static void applyEntityAffixBonus(BattleEntity entity, AttributeSet modifiers) {
        for (BaseAffix affix : entity.getEntityAffixList()) {
            if (affix.getTriggerType() == TriggerType.PERMANENT) {
                affix.applyAttributeBonus(modifiers);
            }
        }
    }

    private static void applyPlayerEquipmentBonus(Player player, AttributeSet modifiers) {
        for (EquipItem item : player.getEquippedItems()) {
            modifiers.add(item.getBaseAttributes());
            if (item.getAffixes() != null) {
                for (BaseAffix affix : item.getAffixes()) {
                    if (affix.getTriggerType() == TriggerType.PERMANENT) {
                        affix.applyAttributeBonus(modifiers);
                    }
                }
            }
            modifiers.add(item.getTotalGemBonuses());
        }
    }

    public static void calculateMonsterBaseAttributes(Monster monster) {
        AttributeSet base = monster.getBaseAttributes();
        calculateDerivedAttributes(base);

        monster.setCurrentHp(base.maxHp);
        monster.setCurrentMp(base.maxMp);
        monster.setCurrentActionPoints(base.maxActionPoints);
    }

    public static void calculateMonsterBaseAttributesWithCoefficients(AttributeSet base,
            float hpMul, float atkMul, float defMul, float spdMul) {
        calculateDerivedAttributes(base);

        base.maxHp = (int) (base.maxHp * hpMul);
        base.maxMp = (int) (base.maxMp * hpMul);
        base.physicalAtk = (int) (base.physicalAtk * atkMul);
        base.magicalAtk = (int) (base.magicalAtk * atkMul);
        base.physicalDef = (int) (base.physicalDef * defMul);
        base.magicalDef = (int) (base.magicalDef * defMul);
        base.speed = (int) (base.speed * spdMul);
    }

    private static void calculateDerivedAttributes(AttributeSet base) {
        base.maxHp = base.physique * 2 + (base.strength > 0 ? base.strength : 0) + 10;
        base.maxMp = base.intelligence * 2 + base.spirit + 5;
        base.physicalAtk = base.strength;
        base.physicalDef = base.physique / 2;
        base.magicalAtk = base.intelligence;
        base.magicalDef = base.spirit / 2;
        base.speed = base.agility;
        base.maxActionPoints = 2;

        base.physicalCritRate = base.luck * 0.002f;
        base.physicalCritDmg = 2.0f + base.strength * 0.005f;
        base.magicalCritRate = base.luck * 0.002f;
        base.magicalCritDmg = 2.0f + base.intelligence * 0.005f;
        base.hitRate = 0.9f + base.agility * 0.003f;
        base.dodgeRate = base.agility * 0.004f;
        base.debuffResist = (base.spirit + base.physique) * 0.004f;
        base.mpCostReduction = base.spirit * 0.005f;

        base.lootRarityBonus = base.luck;
        base.goldBonus = 1.0f + base.luck * 0.01f;
        base.expBonus = 1.0f + base.luck * 0.01f;
    }

    private static void applyHardCaps(AttributeSet attr) {
        attr.mpCostReduction = Math.min(attr.mpCostReduction, 1.0f);
        attr.debuffResist = Math.min(attr.debuffResist, 1.0f);
        attr.damageReductionRate = Math.min(attr.damageReductionRate, 1.0f);
    }

    public static void invalidateCache() {
        cachedFinalAttr = null;
    }

    private static AttributeSet cachedFinalAttr = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION = 100;
}
