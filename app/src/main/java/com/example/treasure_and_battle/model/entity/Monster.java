package com.example.treasure_and_battle.model.entity;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.MonsterIntent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 敌对实体类 (Monster)
 * 基于重构后的 BattleEntity 和用户 AttributeSet 完全适配
 * 核心保留：
 * 1. 怪物稀有度 Rarity（玩家已移除，怪物专属）
 * 2. 怪物词缀系统
 * 3. 意图池决策系统
 * 核心改动：
 * 1. 所有属性迁移到 AttributeSet
 * 2. 实现基类抽象方法
 * 3. 适配 BattleEntity 新构造函数（已移除 Rarity）
 */
public class Monster extends BattleEntity {
    // ====================== 怪物专属掉落/奖励属性 ======================
    private int expReward;     // 玩家击杀后获得的经验值
    private int goldReward;    // 玩家击杀后掉落的金币

    // ====================== 怪物核心拓展模块（保留） ======================
    private Rarity rarity;                     // 怪物的稀有度（专属，决定实力层次和词缀数量）
    private List<MonsterIntent> intentPool;    // 怪物意图池

    // 随机数生成器
    private transient Random random = new Random();

    // ====================== 构造函数（完全适配新架构） ======================
    public Monster(String entityId, String name, int level, Rarity rarity,
                   int maxHp, int maxMp, int patk, int matt, int pdef, int mdef, int speed,
                   int strength, int agility, int intelligence, int spirit, int physique, int luck,
                   int expReward, int goldReward, Context context) {
        // 调用重构后的基类构造函数（已移除 Rarity 参数）
        super(entityId, name, level, context);

        // 初始化怪物专属属性
        this.rarity = rarity;
        this.expReward = expReward;
        this.goldReward = goldReward;
        this.intentPool = new ArrayList<>();

        // 初始化基础属性（从构造函数参数填充）
        initBaseAttributes(maxHp, maxMp, patk, matt, pdef, mdef, speed,
                strength, agility, intelligence, spirit, physique, luck);
    }

    // ====================== 实现基类抽象方法 1：初始化基础属性 ======================
    @Override
    public void initBaseAttributes() {
        // 空实现，怪物使用带参数的 initBaseAttributes 从构造函数初始化
    }

    /**
     * 怪物专属：从构造函数参数初始化基础属性
     */
    private void initBaseAttributes(int maxHp, int maxMp, int patk, int matt, int pdef, int mdef, int speed,
                                    int strength, int agility, int intelligence, int spirit, int physique, int luck) {
        AttributeSet base = this.baseAttributes;

        // 1. 填充六维属性
        base.strength = strength;
        base.agility = agility;
        base.intelligence = intelligence;
        base.spirit = spirit;
        base.physique = physique;
        base.luck = luck;

        // 2. 填充核心战斗属性（完全适配用户 AttributeSet 命名）
        base.maxHp = maxHp;
        base.maxMp = maxMp;
        base.physicalAtk = patk;
        base.magicalAtk = matt;
        base.physicalDef = pdef;
        base.magicalDef = mdef;
        base.speed = speed;
        base.maxActionPoints = 2; // 默认2点行动点

        // 3. 附加战斗属性使用 AttributeSet 构造函数默认值即可

        // 4. 同步战斗资源到最大值
        this.currentHp = base.maxHp;
        this.currentMp = base.maxMp;
        this.currentActionPoints = base.maxActionPoints;

        // 初始化完成，标记属性缓存失效
        markAttributeCacheDirty();
    }

    // ====================== 实现基类抽象方法 2：重新计算最终属性 ======================
    @Override
    protected void recalculateFinalAttributes() {
        // 1. 复制基础属性
        this.finalAttributes.copyFrom(this.baseAttributes);

        // 2. 叠加怪物词缀（利用基类的 monsterAffixList）
        if (this.monsterAffixList != null && !this.monsterAffixList.isEmpty()) {
            // 调用 AffixManager 应用怪物常驻词缀加成
            // AffixManager.getInstance().applyMonsterPermanentAffixBonus(this.finalAttributes, this);
        }

        // 3. 叠加 Buff（预留位置）
        // BuffManager.getInstance().applyBuffBonus(this.finalAttributes, this.activeBuffList);

        // 4. 同步战斗资源上限（保持 HP/MP 比例）
        int oldMaxHp = this.finalAttributes.maxHp;
        if (oldMaxHp > 0 && this.currentHp > 0) {
            double hpRatio = (double) this.currentHp / oldMaxHp;
            this.currentHp = (int) (this.finalAttributes.maxHp * hpRatio);
        }
        this.currentHp = Math.min(this.currentHp, this.finalAttributes.maxHp);
        this.currentMp = Math.min(this.currentMp, this.finalAttributes.maxMp);
    }

    // ====================== 【核心特性】意图决策（保留并适配） ======================
    public List<MonsterIntent> decideNextTurnIntents() {
        List<MonsterIntent> chosenIntents = new ArrayList<>();

        // 临时记录剩余资源（从基类获取当前值）
        int remainingAp = this.currentActionPoints;
        int remainingMp = this.currentMp;

        if (intentPool == null || intentPool.isEmpty()) {
            return chosenIntents;
        }

        // 构建初始可用意图池
        List<MonsterIntent> availableIntents = new ArrayList<>();
        for (MonsterIntent i : intentPool) {
            if (i.getApCost() <= remainingAp && i.getMpCost() <= remainingMp) {
                availableIntents.add(i);
            }
        }

        // 轮盘赌选择意图
        while (!availableIntents.isEmpty()) {
            int totalWeight = 0;
            for (MonsterIntent i : availableIntents) {
                totalWeight += Math.max(1, i.getWeight());
            }

            int roll = random.nextInt(totalWeight);
            int currentWeight = 0;
            MonsterIntent selected = null;

            for (MonsterIntent i : availableIntents) {
                currentWeight += Math.max(1, i.getWeight());
                if (roll < currentWeight) {
                    selected = i;
                    break;
                }
            }

            if (selected != null) {
                chosenIntents.add(selected);
                remainingAp -= selected.getApCost();
                remainingMp -= selected.getMpCost();
            }

            // 更新可用意图池
            availableIntents.clear();
            for (MonsterIntent i : intentPool) {
                if (i.getApCost() <= remainingAp && i.getMpCost() <= remainingMp) {
                    availableIntents.add(i);
                }
            }
        }

        return chosenIntents;
    }

    // ====================== 词缀管理（适配基类 monsterAffixList） ======================
    public void addAffix(BaseAffix affix) {
        int maxAffixes = calculateMaxAffixesByRarity();
        if (this.monsterAffixList.size() < maxAffixes) {
            this.monsterAffixList.add(affix);
            markAttributeCacheDirty(); // 词缀变化，属性需要重算
        }
    }

    private int calculateMaxAffixesByRarity() {
        if (rarity == null) return 1;
        switch (rarity) {
            case COMMON: return 1;
            case UNCOMMON: return 2;
            case RARE: return 3;
            case EPIC: return 4;
            case LEGENDARY: return 5;
            default: return 1;
        }
    }

    // ====================== 意图池管理 ======================
    public void addIntent(MonsterIntent intent) {
        if (this.intentPool != null && intent != null) {
            this.intentPool.add(intent);
        }
    }

    // ====================== Getters & Setters ======================
    // 注意：属性操作通过 getBaseAttributes() 和 getFinalAttributes() 进行

    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }

    public List<MonsterIntent> getIntentPool() { return intentPool; }
    public void setIntentPool(List<MonsterIntent> intentPool) { this.intentPool = intentPool; }

    public int getExpReward() { return expReward; }
    public void setExpReward(int expReward) { this.expReward = expReward; }

    public int getGoldReward() { return goldReward; }
    public void setGoldReward(int goldReward) { this.goldReward = goldReward; }

    // 词缀列表的 Getter/Setter（适配基类）
    public List<BaseAffix> getAffixes() { return new ArrayList<>(this.monsterAffixList); }
    public void setAffixes(List<BaseAffix> affixes) {
        this.monsterAffixList = new ArrayList<>(affixes);
        markAttributeCacheDirty();
    }
}