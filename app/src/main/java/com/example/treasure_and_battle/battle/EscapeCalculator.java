package com.example.treasure_and_battle.battle;

/**
 * 逃跑概率计算工具类
 * 统一玩家和怪物的逃跑判定公式
 */
public class EscapeCalculator {

    /**
     * 计算逃跑成功率
     * 公式：0.2 + (runnerSpeed / chaserSpeed - 1) * 0.5，钳制在 [0.1, 0.9]
     */
    public static double calculateEscapeChance(double runnerSpeed, double chaserSpeed) {
        double chance = 0.2 + ((runnerSpeed / Math.max(1, chaserSpeed)) - 1) * 0.5;
        return Math.max(0.1, Math.min(0.9, chance));
    }
}
