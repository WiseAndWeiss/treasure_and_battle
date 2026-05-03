package com.example.treasure_and_battle.utils;

import android.content.Context;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.manager.EquipmentManager;
import com.example.treasure_and_battle.manager.SkillManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.Player;

public class AttributeUtils {
    // 单例属性快照，避免频繁计算
    private static AttributeSet cachedFinalAttr = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION = 100; // 100ms缓存，避免同一帧多次计算

    // ====================== 【唯一入口】通用属性计算方法 ======================
    public static AttributeSet calculateFinalAttributes(BattleEntity entity, Context context) {
        AttributeSet modifiers = new AttributeSet();
        modifiers.maxHp = 0;
        modifiers.maxMp = 0;
        modifiers.maxActionPoints = 0;
        modifiers.physicalCritRate = 0f;
        modifiers.magicalCritRate = 0f;
        modifiers.physicalCritDmg = 0f;
        modifiers.magicalCritDmg = 0f;
        modifiers.hitRate = 0f;
        modifiers.dodgeRate = 0f;
        modifiers.debuffResist = 0f;

        if (entity instanceof Player) {
            applyPlayerSpecificBonus((Player) entity, modifiers, context);      
        } else if (entity instanceof Monster) {
            applyMonsterSpecificBonus((Monster) entity, modifiers, context);    
        }

        BuffManager.getInstance(context).applyAllBuffAttributeBonus(modifiers, entity);

        // 应用被动技能的属性加成
        if (entity.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : entity.getPassiveSkillList()) {
                passiveSkill.applyAttributeBonus(modifiers);
            }
        }

        // 开始计算最终属性
        AttributeSet baseAttr = entity.getBaseAttributes();
        AttributeSet finalAttr = new AttributeSet();  // 使用临时对象避免递归
        finalAttr.copyFrom(baseAttr);

        // ------------------ 阶段 1: 计算最终六维属性 ------------------
        finalAttr.strength = (int) (baseAttr.strength * (1f + modifiers.percentStrength)) + modifiers.strength;
        finalAttr.agility = (int) (baseAttr.agility * (1f + modifiers.percentAgility)) + modifiers.agility;
        finalAttr.intelligence = (int) (baseAttr.intelligence * (1f + modifiers.percentIntelligence)) + modifiers.intelligence;
        finalAttr.spirit = (int) (baseAttr.spirit * (1f + modifiers.percentSpirit)) + modifiers.spirit;
        finalAttr.physique = (int) (baseAttr.physique * (1f + modifiers.percentPhysique)) + modifiers.physique;
        finalAttr.luck = (int) (baseAttr.luck * (1f + modifiers.percentLuck)) + modifiers.luck;

        // ------------------ 阶段 2: 根据最新六维，产生新的“基石战斗属性” ------------------
        int diffStrength = finalAttr.strength - baseAttr.strength;
        int diffPhysique = finalAttr.physique - baseAttr.physique;
        int diffIntelligence = finalAttr.intelligence - baseAttr.intelligence;  
        int diffSpirit = finalAttr.spirit - baseAttr.spirit;
        int diffAgility = finalAttr.agility - baseAttr.agility;
        int diffLuck = finalAttr.luck - baseAttr.luck;

        int strForHpBase = Math.max(baseAttr.strength, 0);
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

        // ------------------ 阶段 3: 计算同乘区百分比及固定数值加成 ------------------
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

        applyHardCaps(finalAttr);

        return finalAttr;  // 返回计算结果
    }

    // ====================== 玩家专属加成逻辑 ======================     
    private static void applyPlayerSpecificBonus(Player player, AttributeSet modifiers, Context context) {
        for (com.example.treasure_and_battle.model.item.EquipItem item : player.getEquippedItems()) {
            modifiers.add(item.getBaseAttributes());
            if (item.getAffixes() != null) {
                for (com.example.treasure_and_battle.affix.BaseAffix affix : item.getAffixes()) {
                    if (affix.getTriggerType() == com.example.treasure_and_battle.model.affix.AffixTriggerType.PERMANENT) {
                        affix.applyAttributeBonus(modifiers);
                    }
                }
            }
        }
    }

    // ====================== 怪物专属加成逻辑 ======================    
    private static void applyMonsterSpecificBonus(Monster monster, AttributeSet modifiers, Context context) {
        for (com.example.treasure_and_battle.affix.BaseAffix affix : monster.getAffixes()) {
            if (affix.getTriggerType() == com.example.treasure_and_battle.model.affix.AffixTriggerType.PERMANENT) {
                affix.applyAttributeBonus(modifiers);
            }
        }
    }

    // ====================== 基础属性计算 ======================
    public static void calculatePlayerBaseAttributes(Player player) {
        AttributeSet base = player.getBaseAttributes();
        int level = player.getLevel();

        base.strength = level * 2;
        base.agility = level * 2;
        base.intelligence = level * 2;
        base.spirit = level * 2;
        base.physique = level * 2;
        base.luck = level;

        calculateDerivedAttributes(base);

        player.setCurrentHp(base.maxHp);
        player.setCurrentMp(base.maxMp);
        player.setCurrentActionPoints(base.maxActionPoints);
    }

    private static AttributeSet calculateBaseAttributes(Player player) {        
        AttributeSet base = new AttributeSet();
        int level = player.getLevel();

        base.maxHp = base.physique * 2 + level * 10;
        base.maxMp = base.intelligence * 2 + level * 5;
        base.physicalAtk = base.strength;
        base.physicalDef = base.physique / 2;
        base.magicalAtk = base.intelligence;
        base.magicalDef = base.spirit / 2;
        base.speed = base.agility;

        base.physicalCritRate = base.luck * 0.002f;
        base.physicalCritDmg = base.strength * 0.005f;
        base.magicalCritRate = base.luck * 0.002f;
        base.magicalCritDmg = base.intelligence * 0.005f;
        base.hitRate = base.agility * 0.003f;
        base.dodgeRate = base.agility * 0.004f;
        base.debuffResist = (base.spirit + base.physique) * 0.004f;
        base.mpCostReduction = base.spirit * 0.005f;
        base.lootRarityBonus = base.luck;
        base.goldBonus = base.luck * 0.01f;
        base.expBonus = base.luck * 0.01f;

        return base;
    }

    public static void calculateMonsterBaseAttributes(Monster monster) {        
        AttributeSet base = monster.getBaseAttributes();
        calculateDerivedAttributes(base);

        monster.setCurrentHp(base.maxHp);
        monster.setCurrentMp(base.maxMp);
        monster.setCurrentActionPoints(base.maxActionPoints);
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
}
