package com.example.treasure_and_battle.model.entity;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.battle.action.ActionIntent;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.utils.AttributeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 敌对实体类 (Monster)
 * 设计：六维驱动 + 怪物职业系数
 * 模板定义六维（build风格），等级缩放六维，派生战斗属性后乘系数
 */
public class Monster extends BattleEntity {
    private int expReward;
    private int goldReward;

    private Rarity rarity;
    private List<ActionIntent> intentPool;
    private int templateId;
    private Map<String, ActiveSkill> monsterSkillMap = new HashMap<>();

    private Random random = new Random();

    // 专用于动画系统的唯一ID
    private String animationUniqueId;

    private boolean escaped;

    public Monster(String entityId, String name, int level, Rarity rarity,
                   int strength, int agility, int intelligence,
                   int spirit, int physique, int luck,
                   int expReward, int goldReward,
                   float hpMultiplier, float atkMultiplier,
                   float defMultiplier, float spdMultiplier,
                   Context context) {
        super(entityId, name, level, context);

        this.rarity = rarity;
        this.expReward = expReward;
        this.goldReward = goldReward;
        this.intentPool = new ArrayList<>();

        initBaseAttributes(strength, agility, intelligence, spirit, physique, luck,
                hpMultiplier, atkMultiplier, defMultiplier, spdMultiplier);
    }

    @Override
    public void initBaseAttributes() {
    }

    private void initBaseAttributes(int strength, int agility, int intelligence,
                                     int spirit, int physique, int luck,
                                     float hpMul, float atkMul, float defMul, float spdMul) {
        AttributeSet base = this.baseAttributes;

        base.strength = strength;
        base.agility = agility;
        base.intelligence = intelligence;
        base.spirit = spirit;
        base.physique = physique;
        base.luck = luck;

        AttributeUtils.calculateMonsterBaseAttributesWithCoefficients(
                base, hpMul, atkMul, defMul, spdMul);

        this.currentHp = base.maxHp;
        this.currentMp = base.maxMp;
        this.currentActionPoints = base.maxActionPoints;

        markAttributeCacheDirty();
    }

    @Override
    protected void recalculateFinalAttributes() {
        this.finalAttributes.copyFrom(this.baseAttributes);

        AttributeSet calculatedAttrs =
            AttributeUtils.calculateFinalAttributes(this, this.getContext());
        this.finalAttributes.copyFrom(calculatedAttrs);

        int oldMaxHp = this.finalAttributes.maxHp;
        if (oldMaxHp > 0 && this.currentHp > 0) {
            double hpRatio = (double) this.currentHp / oldMaxHp;
            this.currentHp = (int) (this.finalAttributes.maxHp * hpRatio);
        }
        this.currentHp = Math.min(this.currentHp, this.finalAttributes.maxHp);
        this.currentMp = Math.min(this.currentMp, this.finalAttributes.maxMp);
    }

    public List<ActionIntent> decideNextTurnIntents() {
        List<ActionIntent> chosenIntents = new ArrayList<>();

        int remainingAp = this.currentActionPoints;
        int remainingMp = this.currentMp;

        if (intentPool == null || intentPool.isEmpty()) {
            return chosenIntents;
        }

        while (remainingAp > 0) {
            List<ActionIntent> availableIntents = new ArrayList<>();
            for (ActionIntent i : intentPool) {
                if (isIntentAvailable(i, remainingAp, remainingMp)) {
                    availableIntents.add(i);
                }
            }

            if (availableIntents.isEmpty()) {
                break;
            }

            ActionIntent selected = selectIntentByPriorityAndWeight(availableIntents);
            if (selected == null) {
                break;
            }

            chosenIntents.add(selected);
            remainingAp -= selected.getApCost();
            remainingMp -= selected.getMpCost();

            if (selected.getType() == ActionIntent.IntentType.ESCAPE) {
                break;
            }
        }

        return chosenIntents;
    }

    private boolean isIntentAvailable(ActionIntent intent, int remainingAp, int remainingMp) {
        if (intent == null) return false;
        if (intent.getApCost() > remainingAp || intent.getMpCost() > remainingMp) return false;

        float hpRate = getSelfHpRate();
        float minHp = intent.getMinSelfHpRate();
        float maxHp = intent.getMaxSelfHpRate();

        if (minHp >= 0f && hpRate < minHp) return false;
        if (maxHp >= 0f && hpRate > maxHp) return false;

        if (intent.getType() == ActionIntent.IntentType.SKILL) {
            ActiveSkill skill = monsterSkillMap.get(intent.getActionRefId());
            if (skill != null && !skill.isCooldownReady()) return false;
        }

        return true;
    }

    private ActionIntent selectIntentByPriorityAndWeight(List<ActionIntent> availableIntents) {
        int maxPriority = Integer.MIN_VALUE;
        for (ActionIntent intent : availableIntents) {
            maxPriority = Math.max(maxPriority, intent.getPriority());
        }

        List<ActionIntent> topPriorityIntents = new ArrayList<>();
        for (ActionIntent intent : availableIntents) {
            if (intent.getPriority() == maxPriority) {
                topPriorityIntents.add(intent);
            }
        }

        int totalWeight = 0;
        for (ActionIntent intent : topPriorityIntents) {
            totalWeight += Math.max(1, intent.getWeight());
        }
        if (totalWeight <= 0) return null;

        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (ActionIntent intent : topPriorityIntents) {
            currentWeight += Math.max(1, intent.getWeight());
            if (roll < currentWeight) return intent;
        }
        return topPriorityIntents.get(0);
    }

    private float getSelfHpRate() {
        int maxHp = Math.max(1, getFinalAttributes().maxHp);
        return (float) this.currentHp / maxHp;
    }

    public void addAffix(BaseAffix affix) {
        int maxAffixes = calculateMaxAffixesByRarity();
        if (this.entityAffixList.size() < maxAffixes) {
            this.entityAffixList.add(affix);
            markAttributeCacheDirty();
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

    public void addIntent(ActionIntent intent) {
        if (this.intentPool != null && intent != null) {
            this.intentPool.add(intent);
        }
    }

    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }

    public List<ActionIntent> getIntentPool() { return intentPool; }
    public void setIntentPool(List<ActionIntent> intentPool) { this.intentPool = intentPool; }

    public int getExpReward() { return expReward; }
    public void setExpReward(int expReward) { this.expReward = expReward; }

    public int getGoldReward() { return goldReward; }
    public void setGoldReward(int goldReward) { this.goldReward = goldReward; }

    public void addMonsterSkill(String skillId, ActiveSkill skill) {
        monsterSkillMap.put(skillId, skill);
    }

    public ActiveSkill getMonsterSkill(String skillId) {
        return monsterSkillMap.get(skillId);
    }

    public Map<String, ActiveSkill> getMonsterSkillMap() {
        return monsterSkillMap;
    }

    /**
     * 获取专用于动画系统的唯一ID
     * 每个怪物实例都有不同的动画ID，用于动画系统准确定位
     */
    public String getAnimationId() { return animationUniqueId; }

    /**
     * 设置动画唯一ID（由MonsterManager在创建时设置）
     */
    public void setAnimationUniqueId(String animationId) { this.animationUniqueId = animationId; }

    public void tickSkillCooldowns() {
        for (ActiveSkill skill : monsterSkillMap.values()) {
            skill.decreaseCooldown();
        }
    }

    public boolean isEscaped() { return escaped; }
    public void setEscaped(boolean escaped) { this.escaped = escaped; }
}
