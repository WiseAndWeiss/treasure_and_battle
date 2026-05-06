package com.example.treasure_and_battle.model.skill;

public class SkillCostParams {
    public int actionPointCost; // 消耗行动点数
    public int mpCost; // 消耗魔法值
    public int hpCost; // 消耗生命值

    public SkillCostParams(int actionPointCost, int mpCost, int hpCost) {
        this.actionPointCost = actionPointCost;
        this.mpCost = mpCost;
        this.hpCost = hpCost;
    }

    public SkillCostParams() {
        this(0, 0, 0);
    }

    public SkillCostParams(SkillCostParams other) {
        this(other.actionPointCost, other.mpCost, other.hpCost);
    }
}
