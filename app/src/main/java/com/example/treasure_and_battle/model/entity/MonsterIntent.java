package com.example.treasure_and_battle.model.entity;

/**
 * 怪物意图类：仅承载攻击/技能/逃跑三类动作。
 * @deprecated 逐步迁移到 {@link ActionIntent}，当前保留用于平滑兼容旧代码。
 */
@Deprecated
public class MonsterIntent extends ActionIntent {
    public MonsterIntent(String name, String description, ActionIntent.IntentType type,
                        int apCost, int mpCost, double powerMultiplier, int weight) {
        this(name, description, type, apCost, mpCost, powerMultiplier, weight, 0, -1f, -1f, null);
    }

    public MonsterIntent(String name, String description, ActionIntent.IntentType type,
                        int apCost, int mpCost, double powerMultiplier, int weight, int priority,
                        float minSelfHpRate, float maxSelfHpRate, String actionRefId) {
        super(name, description, type, apCost, mpCost, powerMultiplier, weight,
                priority, minSelfHpRate, maxSelfHpRate, actionRefId);
    }
}