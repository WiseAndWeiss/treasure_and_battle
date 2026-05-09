package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;

import java.util.List;

/**
 * 战斗奖励计算器
 * 从 BattleManager.settleBattleResult() 中提取，职责单一
 */
public class RewardCalculator {

    public static int calculateExp(Player player, List<Monster> monsters) {
        int baseExp = 0;
        int monsterLevel = player.getLevel();
        for (Monster m : monsters) {
            if (m == null) continue;
            baseExp += m.getExpReward();
            monsterLevel = Math.max(monsterLevel, m.getLevel());
        }

        int playerLevel = player.getLevel();
        double expBonus = 1.0;
        if (playerLevel < monsterLevel) {
            expBonus += 0.1 * (monsterLevel - playerLevel);
        }

        return (int) (baseExp * expBonus * player.getFinalAttributes().expBonus);
    }

    public static int calculateGold(Player player, List<Monster> monsters) {
        int baseGold = 0;
        for (Monster m : monsters) {
            if (m == null) continue;
            baseGold += m.getGoldReward();
        }
        return (int) (baseGold * player.getFinalAttributes().goldBonus);
    }
}
