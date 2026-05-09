package com.example.treasure_and_battle.utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * 随机数工具类
 * 覆盖游戏开发中所有常用的随机需求：
 * 1. 概率判定（checkProbability）
 * 2. 范围随机数（Int/Float/Long）
 * 3. 集合随机操作（随机选元素、打乱列表）
 */
public class RandomUtils {
    // 使用单例 Random，避免重复创建对象
    private static final Random RANDOM = new Random();

    // ====================== 【核心】概率判定方法 ======================

    /**
     * 概率判定：输入0.0-1.0之间的概率值，返回是否命中
     * 例如：checkProbability(0.3f) 有30%概率返回true
     *
     * @param probability 命中概率（0.0-1.0），超出范围会自动截断
     * @return true=命中，false=未命中
     */
    public static boolean checkProbability(float probability) {
        // 安全截断：确保概率在0.0到1.0之间
        float clampedProb = Math.max(0.0f, Math.min(1.0f, probability));
        return RANDOM.nextFloat() < clampedProb;
    }

    /**
     * 概率判定（double版本，方便调用）
     */
    public static boolean checkProbability(double probability) {
        double clampedProb = Math.max(0.0, Math.min(1.0, probability));
        return RANDOM.nextDouble() < clampedProb;
    }

    // ====================== 范围随机数方法 ======================

    /**
     * 获取随机整数，包含 minValue 和 maxValue
     * 例如：getRandomInt(1, 10) 可能返回1到10之间的任意整数
     */
    public static int getRandomInt(int minValue, int maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        // 确保 min < max
        int min = Math.min(minValue, maxValue);
        int max = Math.max(minValue, maxValue);
        return RANDOM.nextInt(max - min + 1) + min;
    }

    /**
     * 获取随机浮点数，包含 minValue 和 maxValue
     */
    public static float getRandomFloat(float minValue, float maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        float min = Math.min(minValue, maxValue);
        float max = Math.max(minValue, maxValue);
        return RANDOM.nextFloat() * (max - min) + min;
    }

    /**
     * 获取随机长整型，包含 minValue 和 maxValue
     * （用于生成唯一ID、大数值随机等）
     */
    public static long getRandomLong(long minValue, long maxValue) {
        if (minValue == maxValue) {
            return minValue;
        }
        long min = Math.min(minValue, maxValue);
        long max = Math.max(minValue, maxValue);
        return min + (long) (RANDOM.nextDouble() * (max - min + 1));
    }

    // ====================== 集合随机操作方法 ======================

    /**
     * 从列表中随机选取一个元素
     * （用于随机掉落、随机选怪物意图、随机选技能等）
     *
     * @param list 源列表，不能为空
     * @param <T>  列表元素类型
     * @return 随机选中的元素
     */
    public static <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("列表不能为空");
        }
        int randomIndex = getRandomInt(0, list.size() - 1);
        return list.get(randomIndex);
    }

    /**
     * 从列表中随机选取指定数量的元素（不重复）
     *
     * @param list  源列表
     * @param count 要选取的数量
     * @param <T>   列表元素类型
     * @return 随机选中的元素列表
     */
    public static <T> List<T> getRandomElements(List<T> list, int count) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("列表不能为空");
        }
        if (count <= 0 || count > list.size()) {
            throw new IllegalArgumentException("选取数量必须在1到列表大小之间");
        }

        // 先打乱列表，再取前count个
        List<T> shuffled = new ArrayList<>(list);
        shuffleList(shuffled);
        return shuffled.subList(0, count);
    }

    /**
     * 随机打乱列表（Fisher-Yates洗牌算法）
     * （用于打乱敌人顺序、打乱卡牌、打乱掉落池等）
     *
     * @param list 要打乱的列表
     * @param <T>  列表元素类型
     */
    public static <T> void shuffleList(List<T> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        for (int i = list.size() - 1; i > 0; i--) {
            int j = getRandomInt(0, i);
            // 交换 i 和 j 位置的元素
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    // ====================== 种子控制（可选，用于测试时复现随机结果） ======================

    /**
     * 设置随机种子（用于测试时复现随机结果）
     */
    public static void setSeed(long seed) {
        RANDOM.setSeed(seed);
    }
}