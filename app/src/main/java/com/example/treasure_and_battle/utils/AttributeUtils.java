package com.example.treasure_and_battle.utils;

import android.content.Context;

import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.manager.AffixManager;
import com.example.treasure_and_battle.manager.EquipmentManager;
import com.example.treasure_and_battle.manager.SkillManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.Player;

public class AttributeUtils {
    // 单例属性快照，避免频繁计算
    private static AttributeSet cachedFinalAttr = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION = 100; // 100ms缓存，避免同一帧多次计算

    // 核心方法：获取玩家最终属性（唯一入口）
    // ====================== 【唯一入口】通用属性计算方法 ======================
    /**
     * 计算战斗实体的最终属性
     * 会自动区分是 Player 还是 Monster，应用不同的加成规则
     *
     * @param entity  战斗实体（Player 或 Monster）
     * @param context Android Context
     */
    public static void calculateFinalAttributes(BattleEntity entity, Context context) {
        // 1. 初始化收集器，收集装备、词缀、Buff的所有【固定加成】和【百分比加成】
        // 这里约定：如果技能/Buff加的是外层属性（如 int physicalAtk）即为固定加成；如果加在 percentXXX 上即为百分比。
        AttributeSet modifiers = new AttributeSet();
        // 清除AttributeSet构造函数里赋予的默认值，因为modifiers作为【增量累加器】，初始值必须全为0
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

        // 2. 根据实体类型，应用不同的基础、词缀等加成（结果累加到 modifiers 中）
        if (entity instanceof Player) {
            applyPlayerSpecificBonus((Player) entity, modifiers, context);
        } else if (entity instanceof Monster) {
            applyMonsterSpecificBonus((Monster) entity, modifiers, context);
        }

        // 3. 【通用】叠加 Buff 加成（同样累加到 modifiers 中，实现百分比同一乘区、与固定值剥离）
        BuffManager.getInstance(context).applyAllBuffAttributeBonus(modifiers, entity);

        // 4. 开始计算面板数值
        AttributeSet baseAttr = entity.getBaseAttributes();
        AttributeSet finalAttr = entity.getFinalAttributes();
        
        // 为了避免累加时的脏数据，我们始终以当下的 baseAttr 为“原生值”拷贝一份
        finalAttr.copyFrom(baseAttr);

        // ------------------ 阶段 1: 计算最终六维属性 ------------------
        // 公式：最终六维 = 基础六维 * (1 + 六维百分比之和) + 六维固定加成
        finalAttr.strength = (int) (baseAttr.strength * (1f + modifiers.percentStrength)) + modifiers.strength;
        finalAttr.agility = (int) (baseAttr.agility * (1f + modifiers.percentAgility)) + modifiers.agility;
        finalAttr.intelligence = (int) (baseAttr.intelligence * (1f + modifiers.percentIntelligence)) + modifiers.intelligence;
        finalAttr.spirit = (int) (baseAttr.spirit * (1f + modifiers.percentSpirit)) + modifiers.spirit;
        finalAttr.physique = (int) (baseAttr.physique * (1f + modifiers.percentPhysique)) + modifiers.physique;
        finalAttr.luck = (int) (baseAttr.luck * (1f + modifiers.percentLuck)) + modifiers.luck;

        // ------------------ 阶段 2: 根据最新六维，产生新的“基石战斗属性” ------------------
        // 计算六维增长带来的额外派生战斗属性增量（这样不破坏base自带的手设固定值，比如Boss的5000血）
        int diffStrength = finalAttr.strength - baseAttr.strength;
        int diffPhysique = finalAttr.physique - baseAttr.physique;
        int diffIntelligence = finalAttr.intelligence - baseAttr.intelligence;
        int diffSpirit = finalAttr.spirit - baseAttr.spirit;
        int diffAgility = finalAttr.agility - baseAttr.agility;
        int diffLuck = finalAttr.luck - baseAttr.luck;

        // 核心额外增量添加
        int strForHpBase = Math.max(baseAttr.strength, 0);
        int strForHpFinal = Math.max(finalAttr.strength, 0);

        finalAttr.maxHp += diffPhysique * 2 + (strForHpFinal - strForHpBase);
        finalAttr.maxMp += diffIntelligence * 2 + diffSpirit;
        finalAttr.physicalAtk += diffStrength;
        finalAttr.physicalDef += diffPhysique / 2;
        finalAttr.magicalAtk += diffIntelligence;
        finalAttr.magicalDef += diffSpirit / 2;
        finalAttr.speed += diffAgility;

        // 次要衍生属性增量 (与calculateDerivedAttributes公式保持一致增幅比例)
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
        // 核心公式： 最终属性 = 基石战斗属性 * (1 + 累计百分比之和) + 累计固定加成值
        finalAttr.physicalAtk = (int) (finalAttr.physicalAtk * (1f + modifiers.percentPhysicalAtk)) + modifiers.physicalAtk;
        finalAttr.magicalAtk = (int) (finalAttr.magicalAtk * (1f + modifiers.percentMagicalAtk)) + modifiers.magicalAtk;
        finalAttr.physicalDef = (int) (finalAttr.physicalDef * (1f + modifiers.percentPhysicalDef)) + modifiers.physicalDef;
        finalAttr.magicalDef = (int) (finalAttr.magicalDef * (1f + modifiers.percentMagicalDef)) + modifiers.magicalDef;
        finalAttr.speed = (int) (finalAttr.speed * (1f + modifiers.percentSpeed)) + modifiers.speed;
        
        finalAttr.maxHp = (int) (finalAttr.maxHp * (1f + modifiers.percentMaxHp)) + modifiers.maxHp;
        finalAttr.maxMp = (int) (finalAttr.maxMp * (1f + modifiers.percentMaxMp)) + modifiers.maxMp;

        // 次要战斗属性（如各种暴击率），通常是加法计算：最终率 = 基础率 + 加成池累积率(这些本身就是float)
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

        // 4. 应用硬上限
        applyHardCaps(finalAttr);
    }

    // ====================== 玩家专属加成逻辑 ======================
    private static void applyPlayerSpecificBonus(Player player, AttributeSet modifiers, Context context) {
        // TODO: 2. 第二层：叠加天赋加成（后续实现）
        // AttributeSet talentAttr = calculateTalentAttributes(player);
        // modifiers.add(talentAttr);

        // TODO: 3. 第三层：叠加装备加成（后续实现）
        // AttributeSet equipAttr = EquipmentManager.getInstance(context).getEquipAttributeSet(player);
        // modifiers.add(equipAttr);

        // TODO: 4. 第四层：叠加技能被动加成（后续实现）
        // AttributeSet skillAttr = SkillManager.getInstance(context).getPassiveSkillAttributes(player);
        // modifiers.add(skillAttr);

        // 5. 第五层：叠加玩家词缀加成（后续实现）
        // AffixManager.getInstance(context).applyPlayerAffixBonus(modifiers, player);
    }

    // ====================== 怪物专属加成逻辑 ======================
    private static void applyMonsterSpecificBonus(Monster monster, AttributeSet modifiers, Context context) {
        // 怪物专属：叠加怪物词缀加成
        for (com.example.treasure_and_battle.affix.BaseAffix affix : monster.getAffixes()) {
            if (affix.getTriggerType() == com.example.treasure_and_battle.model.affix.AffixTriggerType.PERMANENT) {
                affix.applyAttributeBonus(modifiers);
            }
        }
    }

    // ====================== 基础属性计算（保留你的逻辑，拆分玩家/怪物） ======================
    /**
     * 计算玩家基础属性（等级+职业）
     */
    public static void calculatePlayerBaseAttributes(Player player) {
        AttributeSet base = player.getBaseAttributes();
        int level = player.getLevel();

        // TODO: 后续接入职业系统
        // int profession = player.getProfession();
        // base.strength = ConfigUtils.getBaseAttr(profession, "strength") + level * ConfigUtils.getLevelGrowth(profession, "strength");
        // ... 其他六维属性 ...

        // 临时：使用简单的等级成长
        base.strength = level * 2;
        base.agility = level * 2;
        base.intelligence = level * 2;
        base.spirit = level * 2;
        base.physique = level * 2;
        base.luck = level;

        // 基础战斗属性（由六维属性转换而来，完全保留你的逻辑）
        calculateDerivedAttributes(base);

        // 初始化玩家资源
        player.setCurrentHp(base.maxHp);
        player.setCurrentMp(base.maxMp);
        player.setCurrentActionPoints(base.maxActionPoints);
    }

    // 计算第一层：基础属性（等级自带）
    private static AttributeSet calculateBaseAttributes(Player player) {
        AttributeSet base = new AttributeSet();
        int level = player.getLevel();

        /*
        int profession = player.getProfession();
        // 基础六维属性（职业初始值 + 等级成长）
        base.strength = ConfigUtils.getBaseAttr(profession, "strength") + level * ConfigUtils.getLevelGrowth(profession, "strength");
        base.agility = ConfigUtils.getBaseAttr(profession, "agility") + level * ConfigUtils.getLevelGrowth(profession, "agility");
        base.intelligence = ConfigUtils.getBaseAttr(profession, "intelligence") + level * ConfigUtils.getLevelGrowth(profession, "intelligence");
        base.spirit = ConfigUtils.getBaseAttr(profession, "spirit") + level * ConfigUtils.getLevelGrowth(profession, "spirit");
        base.physique = ConfigUtils.getBaseAttr(profession, "physique") + level * ConfigUtils.getLevelGrowth(profession, "physique");
        base.luck = ConfigUtils.getBaseAttr(profession, "luck") + level * ConfigUtils.getLevelGrowth(profession, "luck");
        */

        // 基础战斗属性（由六维属性转换而来，完全复用你之前的设定）
        base.maxHp = base.physique * 2 + level * 10;
        base.maxMp = base.intelligence * 2 + level * 5;
        base.physicalAtk = base.strength;
        base.physicalDef = base.physique / 2;
        base.magicalAtk = base.intelligence;
        base.magicalDef = base.spirit / 2;
        base.speed = base.agility;

        // 基础附加属性
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

    /**
     * 计算怪物基础属性（从怪物模板加载）
     */
    public static void calculateMonsterBaseAttributes(Monster monster) {
        AttributeSet base = monster.getBaseAttributes();
        // 怪物的基础属性通常在构造函数或模板加载时已经设置好
        // 这里只需要计算衍生属性
        calculateDerivedAttributes(base);

        // 初始化怪物资源
        monster.setCurrentHp(base.maxHp);
        monster.setCurrentMp(base.maxMp);
        monster.setCurrentActionPoints(base.maxActionPoints);
    }

    /**
     * 【通用】计算衍生战斗属性（六维 → 战斗属性）
     * 完全保留你的逻辑，玩家和怪物共用
     */
    private static void calculateDerivedAttributes(AttributeSet base) {
        // 核心战斗属性
        base.maxHp = base.physique * 2 + (base.strength > 0 ? base.strength : 0) + 10;
        base.maxMp = base.intelligence * 2 + base.spirit + 5;
        base.physicalAtk = base.strength;
        base.physicalDef = base.physique / 2;
        base.magicalAtk = base.intelligence;
        base.magicalDef = base.spirit / 2;
        base.speed = base.agility;
        base.maxActionPoints = 2; // 默认2点行动点

        // 附加战斗属性
        base.physicalCritRate = base.luck * 0.002f;
        base.physicalCritDmg = 2.0f + base.strength * 0.005f;
        base.magicalCritRate = base.luck * 0.002f;
        base.magicalCritDmg = 2.0f + base.intelligence * 0.005f;
        base.hitRate = 0.9f + base.agility * 0.003f;
        base.dodgeRate = base.agility * 0.004f;
        base.debuffResist = (base.spirit + base.physique) * 0.004f;
        base.mpCostReduction = base.spirit * 0.005f;

        // 收益属性
        base.lootRarityBonus = base.luck;
        base.goldBonus = 1.0f + base.luck * 0.01f;
        base.expBonus = 1.0f + base.luck * 0.01f;
    }

    // 应用数值硬上限
    private static void applyHardCaps(AttributeSet attr) {
        attr.dodgeRate = Math.min(attr.dodgeRate, 0.8f);  // 闪避最高80%
        attr.hitRate = Math.min(attr.hitRate, 1.0f);      // 命中最高100%
        attr.physicalCritRate = Math.min(attr.physicalCritRate, 0.95f); // 暴击最高95%
        attr.magicalCritRate = Math.min(attr.magicalCritRate, 0.95f);
        attr.mpCostReduction = Math.min(attr.mpCostReduction, 0.5f); // 蓝耗减免最高50%
        attr.debuffResist = Math.min(attr.debuffResist, 1.0f); // 异常抵抗最高100%
    }

    // 通知缓存失效（属性变化时调用，如穿装备、加天赋、加Buff）
    public static void invalidateCache() {
        cachedFinalAttr = null;
    }
}