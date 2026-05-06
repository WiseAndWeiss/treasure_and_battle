package com.example.treasure_and_battle.battle.action;

import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 统一战斗动作抽象：玩家与怪物共用。
 * 当前仅落地 ATTACK / SKILL(TODO) / ITEM(TODO) / ESCAPE。
 */
public class BattleAction {

    public enum ActionType {
        ATTACK,
        SKILL,
        ITEM,
        ESCAPE
    }

    private final ActionType type;
    private final BattleEntity actor;
    private final BattleEntity target;
    private final int apCost;
    private final int mpCost;
    private final int hpCost;
    private final double powerMultiplier;
    private final String actionRefId; // 预留：skillId/itemId
    private final String displayName;

    public BattleAction(ActionType type, BattleEntity actor, BattleEntity target,
                        int apCost, int mpCost, int hpCost,
                        double powerMultiplier, String actionRefId, String displayName) {
        this.type = type;
        this.actor = actor;
        this.target = target;
        this.apCost = Math.max(0, apCost);
        this.mpCost = Math.max(0, mpCost);
        this.hpCost = Math.max(0, hpCost);
        this.powerMultiplier = powerMultiplier <= 0 ? 1.0 : powerMultiplier;
        this.actionRefId = actionRefId;
        this.displayName = displayName == null ? type.name() : displayName;
    }

    public static BattleAction normalAttack(BattleEntity actor, BattleEntity target) {
        return new BattleAction(ActionType.ATTACK, actor, target, 1, 0, 0, 1.0, null, "普通攻击");
    }

    public static BattleAction escape(BattleEntity actor, BattleEntity target) {
        return new BattleAction(ActionType.ESCAPE, actor, target, 1, 0, 0, 1.0, null, "逃跑");
    }

    public static BattleAction skillTodo(BattleEntity actor, BattleEntity target,
                                         String skillId, int apCost, int mpCost, double powerMultiplier, String displayName) {
        return new BattleAction(ActionType.SKILL, actor, target, apCost, mpCost, 0, powerMultiplier, skillId, displayName);
    }

    public ActionType getType() { return type; }
    public BattleEntity getActor() { return actor; }
    public BattleEntity getTarget() { return target; }
    public int getApCost() { return apCost; }
    public int getMpCost() { return mpCost; }
    public int getHpCost() { return hpCost; }
    public double getPowerMultiplier() { return powerMultiplier; }
    public String getActionRefId() { return actionRefId; }
    public String getDisplayName() { return displayName; }
}
