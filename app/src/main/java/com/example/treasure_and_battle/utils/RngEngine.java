package com.example.treasure_and_battle.utils;

import com.example.treasure_and_battle.model.common.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机与保底引擎 (RngEngine)
 * 职责：处理掉落物稀有度升级、保底生成机制。
 * 该类是纯粹的数学计算中心，不负责生成具体物品或词条对象，只负责产出合法的“品质级别”。
 */
public class RngEngine {
    private static final Random random = new Random();

    /**
     * 核心规则：通用战利品稀有度升级算法（复用于物品掉落、词条生成）
     *
     * @param baseRarity 基础品质 (x)
     * @param rarityBonus 稀有度加成百分比 (y)，例如 150 表示 150%
     * @return 升级后的最终品质
     */
    public static Rarity upgradeRarity(Rarity baseRarity, float rarityBonus) {
        int currentOrdinal = baseRarity.ordinal();
        Rarity[] allRarities = Rarity.values();
        int maxOrdinal = allRarities.length - 1; // 默认最大为传说(橙色)

        float remainingBonus = rarityBonus;

        // y >= 100% 时，必定升级为 x+1，且扣除100继续循环
        while (remainingBonus >= 100f && currentOrdinal < maxOrdinal) {
            currentOrdinal++;
            remainingBonus -= 100f;
        }

        // 剩余部分 (0~99.99%) 作为概率随机提升一次
        if (remainingBonus > 0f && currentOrdinal < maxOrdinal) {
            if (random.nextFloat() * 100f < remainingBonus) {
                currentOrdinal++;
            }
        }

        return allRarities[currentOrdinal];
    }

    /**
     * 核心规则：统一保底生成列表计算器
     * 根据需要生成的总量、保底基本品质、保底发生条件以及加成率，返回一组生成的“最终品质列表”。
     * 这个列表后续可以分别给【装备库】或【词条库】用于实例化具体内容。
     *
     * @param count 应当生成的总数量 (N)
     * @param baseRarity 基础掉落品质
     * @param rarityBonus 额外附加的概率提升参数
     * @param generatePity 是否触发保底 (例如蓝色及以上怪物触发保底，传true)
     * @param pityMinRarity 触发保底时的最低品质
     * @return 最终随机得出的一组品质列表
     */
    public static List<Rarity> generateRaritiesWithPity(int count, Rarity baseRarity, float rarityBonus, boolean generatePity, Rarity pityMinRarity) {
        List<Rarity> results = new ArrayList<>();
        if (count <= 0) return results;

        int remainCount = count;

        // [规则]: 传说级怪物必掉落1件橙色品质，或者带有普通保底
        if (generatePity) {
            // 生成保底项
            int minPityOrdinal = pityMinRarity.ordinal();
            int basePityOrdinal = baseRarity.ordinal();
            
            // 保底项的初始品质取 baseRarity 和 pityMinRarity 中较高的一个，并应用通用升级规则
            Rarity initialPity = Rarity.values()[Math.max(minPityOrdinal, basePityOrdinal)];
            Rarity finalPityRarity = upgradeRarity(initialPity, rarityBonus);
            
            results.add(finalPityRarity);
            remainCount--;
        }

        // 剩余的生成走通用基础掉落
        for (int i = 0; i < remainCount; i++) {
            results.add(upgradeRarity(baseRarity, rarityBonus));
        }

        return results;
    }
}
