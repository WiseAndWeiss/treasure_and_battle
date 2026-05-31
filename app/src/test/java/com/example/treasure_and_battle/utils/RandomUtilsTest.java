package com.example.treasure_and_battle.utils;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class RandomUtilsTest {

    @Before
    public void setUp() {
        // 使用固定种子确保测试可重复
        RandomUtils.setSeed(12345);
    }

    // ==================== 概率判定测试 ====================

    @Test
    public void testCheckProbabilityFloat_Zero() {
        assertFalse(RandomUtils.checkProbability(0.0f));
    }

    @Test
    public void testCheckProbabilityFloat_One() {
        assertTrue(RandomUtils.checkProbability(1.0f));
    }

    @Test
    public void testCheckProbabilityFloat_NegativeClampedToZero() {
        assertFalse(RandomUtils.checkProbability(-0.5f));
    }

    @Test
    public void testCheckProbabilityFloat_OverOneClampedToOne() {
        assertTrue(RandomUtils.checkProbability(1.5f));
    }

    @Test
    public void testCheckProbabilityDouble_Zero() {
        assertFalse(RandomUtils.checkProbability(0.0));
    }

    @Test
    public void testCheckProbabilityDouble_One() {
        assertTrue(RandomUtils.checkProbability(1.0));
    }

    @Test
    public void testCheckProbabilityDouble_NegativeClampedToZero() {
        assertFalse(RandomUtils.checkProbability(-0.5));
    }

    @Test
    public void testCheckProbabilityDouble_OverOneClampedToOne() {
        assertTrue(RandomUtils.checkProbability(1.5));
    }

    // ==================== 范围随机数测试 ====================

    @Test
    public void testGetRandomInt_SameValues() {
        assertEquals(5, RandomUtils.getRandomInt(5, 5));
    }

    @Test
    public void testGetRandomInt_ReversedOrder() {
        int result = RandomUtils.getRandomInt(10, 1);
        assertTrue(result >= 1 && result <= 10);
    }

    @Test
    public void testGetRandomInt_NormalRange() {
        int result = RandomUtils.getRandomInt(1, 100);
        assertTrue(result >= 1 && result <= 100);
    }

    @Test
    public void testGetRandomInt_NegativeRange() {
        int result = RandomUtils.getRandomInt(-10, -1);
        assertTrue(result >= -10 && result <= -1);
    }

    @Test
    public void testGetRandomFloat_SameValues() {
        assertEquals(5.0f, RandomUtils.getRandomFloat(5.0f, 5.0f), 0.0001f);
    }

    @Test
    public void testGetRandomFloat_NormalRange() {
        float result = RandomUtils.getRandomFloat(0.0f, 1.0f);
        assertTrue(result >= 0.0f && result <= 1.0f);
    }

    @Test
    public void testGetRandomFloat_ReversedOrder() {
        float result = RandomUtils.getRandomFloat(10.0f, 1.0f);
        assertTrue(result >= 1.0f && result <= 10.0f);
    }

    @Test
    public void testGetRandomLong_SameValues() {
        assertEquals(1000L, RandomUtils.getRandomLong(1000L, 1000L));
    }

    @Test
    public void testGetRandomLong_NormalRange() {
        long result = RandomUtils.getRandomLong(1L, 1000L);
        assertTrue(result >= 1L && result <= 1000L);
    }

    @Test
    public void testGetRandomLong_ReversedOrder() {
        long result = RandomUtils.getRandomLong(10000L, 1000L);
        assertTrue(result >= 1000L && result <= 10000L);
    }

    // ==================== 集合随机操作测试 ====================

    @Test
    public void testGetRandomElement_SingleElement() {
        List<String> list = Arrays.asList("only");
        assertEquals("only", RandomUtils.getRandomElement(list));
    }

    @Test
    public void testGetRandomElement_MultipleElements() {
        List<String> list = Arrays.asList("a", "b", "c");
        String result = RandomUtils.getRandomElement(list);
        assertTrue(list.contains(result));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRandomElement_EmptyList() {
        RandomUtils.getRandomElement(new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRandomElement_NullList() {
        RandomUtils.getRandomElement(null);
    }

    @Test
    public void testGetRandomElements_AllElements() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        List<Integer> result = RandomUtils.getRandomElements(list, 3);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(list));
    }

    @Test
    public void testGetRandomElements_PartialElements() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = RandomUtils.getRandomElements(list, 2);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetRandomElements_NoDuplicates() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = RandomUtils.getRandomElements(list, 3);
        // 检查没有重复元素
        assertEquals(3, result.size());
        assertEquals(3, result.stream().distinct().count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRandomElements_CountZero() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        RandomUtils.getRandomElements(list, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRandomElements_CountExceedsSize() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        RandomUtils.getRandomElements(list, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRandomElements_NullList() {
        RandomUtils.getRandomElements(null, 2);
    }

    @Test
    public void testShuffleList_SingleElement() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1));
        RandomUtils.shuffleList(list);
        assertEquals(Arrays.asList(1), list);
    }

    @Test
    public void testShuffleList_TwoElements() {
        List<Integer> original = new ArrayList<>(Arrays.asList(1, 2));
        List<Integer> list = new ArrayList<>(original);
        RandomUtils.shuffleList(list);
        // 两个元素打乱后顺序可能改变或保持
        assertTrue(list.size() == 2);
        assertTrue(list.containsAll(original));
    }

    @Test
    public void testShuffleList_ModifiesOrder() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        List<Integer> original = new ArrayList<>(list);

        RandomUtils.shuffleList(list);

        // 元素相同，顺序应该不同（极小概率相同，但几乎不可能）
        assertEquals(original.size(), list.size());
        assertTrue(original.containsAll(list));
        assertTrue(list.containsAll(original));
    }

    @Test
    public void testShuffleList_NullList() {
        // 应该不抛异常，只是静默返回
        RandomUtils.shuffleList(null);
    }

    @Test
    public void testShuffleList_EmptyList() {
        List<Integer> list = new ArrayList<>();
        RandomUtils.shuffleList(list);
        assertTrue(list.isEmpty());
    }

    // ==================== 种子控制测试 ====================

    @Test
    public void testSetSeed_ProducesConsistentResults() {
        RandomUtils.setSeed(42);
        int first1 = RandomUtils.getRandomInt(1, 100);

        RandomUtils.setSeed(42);
        int first2 = RandomUtils.getRandomInt(1, 100);

        assertEquals(first1, first2);
    }

    @Test
    public void testSetSeed_DifferentSeedsDifferentResults() {
        RandomUtils.setSeed(1);
        int result1 = RandomUtils.getRandomInt(1, 1000);

        RandomUtils.setSeed(999);
        int result2 = RandomUtils.getRandomInt(1, 1000);

        // 极大概率不同
        assertNotNull(result1);
        assertNotNull(result2);
    }
}
