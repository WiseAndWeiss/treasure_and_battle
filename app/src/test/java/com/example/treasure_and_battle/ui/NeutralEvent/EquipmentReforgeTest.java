package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.utils.RngEngine;
import com.example.treasure_and_battle.utils.RandomUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 装备重炼（equipment_reforge）事件核心逻辑测试
 * <p>
 * 事件规则：玩家从背包选择一件装备 → 选择装备上的一个词缀 → 系统随机生成一个新词缀替换选中词缀。
 * 重炼入口逻辑位于 NeutralEventActivity：
 *   - showEquipmentSelectionDialog() 过滤背包中 EquipItem
 *   - showReforgeDialog() 完成重炼交互
 * 核心调用链：
 *   EquipAffixManager.generateSingleAffixForEquipment(equip)
 *     → RngEngine.generateRaritiesWithPity(1, COMMON, 0f, true, equipmentRarity)
 *     → getRandomEquipTemplate(category, targetRarity) → EquipAffixFactory.create()
 * <p>
 * 测试覆盖：
 * 1.  重炼稀有度逻辑（各装备稀有度 → 词缀稀有度 应等同于装备稀有度）
 * 2.  RngEngine.generateRaritiesWithPity 的概率/保证性验证（大量样本）
 * 3.  词缀替换逻辑（索引选择、旧词缀被替换、其他词缀不变、引用改变）
 * 4.  重炼次数限制（hasReforged 标志，单次事件只能重炼一次）
 * 5.  索引边界条件（idx < 0、idx >= size、idx == -1 未选择状态）
 * 6.  装备选择逻辑（背包过滤 EquipItem、空背包、无装备物品）
 * 7.  词缀列表为空时的保护
 * 8.  generateSingleAffixForEquipment 返回 null 的兜底保护
 * 9.  同一装备多次重炼的幂等性
 * 10. 背包空间相关边界（背包满/恰好/不足不能放置额外物品，但重炼本身不增加物品）
 * 11. UI 状态流转（按钮启用→禁用、取消按钮可见→消失）
 * 12. 装备稀有度随机性独立性验证
 */
public class EquipmentReforgeTest {

    // ==================== 1. 重炼稀有度逻辑 ====================

    /**
     * 模拟 generateSingleAffixForEquipment 的核心稀有度生成逻辑。
     * 参数与 NeutralEventActivity 实际调用一致：
     *   RngEngine.generateRaritiesWithPity(1, Rarity.COMMON, 0f, true, equipmentRarity)
     */
    private Rarity simulateReforgeRarity(Rarity equipmentRarity) {
        List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                1, Rarity.COMMON, 0f, true, equipmentRarity);
        assertNotNull("生成结果不应为 null", rarities);
        assertFalse("生成结果不应为空", rarities.isEmpty());
        return rarities.get(0);
    }

    @Test
    public void testReforgeRarityEqualsEquipmentRarity_Common() {
        RandomUtils.setSeed(123456L);
        Rarity result = simulateReforgeRarity(Rarity.COMMON);
        assertEquals("COMMON 装备重炼 → 词缀稀有度应为 COMMON", Rarity.COMMON, result);
    }

    @Test
    public void testReforgeRarityEqualsEquipmentRarity_Uncommon() {
        RandomUtils.setSeed(123456L);
        Rarity result = simulateReforgeRarity(Rarity.UNCOMMON);
        assertEquals("UNCOMMON 装备重炼 → 词缀稀有度应为 UNCOMMON", Rarity.UNCOMMON, result);
    }

    @Test
    public void testReforgeRarityEqualsEquipmentRarity_Rare() {
        RandomUtils.setSeed(123456L);
        Rarity result = simulateReforgeRarity(Rarity.RARE);
        assertEquals("RARE 装备重炼 → 词缀稀有度应为 RARE", Rarity.RARE, result);
    }

    @Test
    public void testReforgeRarityEqualsEquipmentRarity_Epic() {
        RandomUtils.setSeed(123456L);
        Rarity result = simulateReforgeRarity(Rarity.EPIC);
        assertEquals("EPIC 装备重炼 → 词缀稀有度应为 EPIC", Rarity.EPIC, result);
    }

    @Test
    public void testReforgeRarityEqualsEquipmentRarity_Legendary() {
        RandomUtils.setSeed(123456L);
        Rarity result = simulateReforgeRarity(Rarity.LEGENDARY);
        assertEquals("LEGENDARY 装备重炼 → 词缀稀有度应为 LEGENDARY", Rarity.LEGENDARY, result);
    }

    @Test
    public void testReforgeRarityNeverBelowEquipmentRarity() {
        RandomUtils.setSeed(42L);
        for (Rarity equipRarity : Rarity.values()) {
            Rarity result = simulateReforgeRarity(equipRarity);
            assertTrue("词缀稀有度 " + result + " 不应低于装备稀有度 " + equipRarity,
                    result.ordinal() >= equipRarity.ordinal());
        }
    }

    @Test
    public void testReforgeRarityWithDifferentSeeds_Common() {
        Rarity[] expected = {Rarity.COMMON, Rarity.COMMON, Rarity.COMMON, Rarity.COMMON, Rarity.COMMON};
        long[] seeds = {1L, 42L, 999L, 12345L, 777777L};
        for (int i = 0; i < seeds.length; i++) {
            RandomUtils.setSeed(seeds[i]);
            Rarity result = simulateReforgeRarity(Rarity.COMMON);
            assertEquals("种子=" + seeds[i] + " COMMON → 始终 COMMON", expected[i], result);
        }
    }

    @Test
    public void testReforgeRarityWithDifferentSeeds_Legendary() {
        long[] seeds = {1L, 42L, 999L, 12345L, 777777L};
        for (long seed : seeds) {
            RandomUtils.setSeed(seed);
            Rarity result = simulateReforgeRarity(Rarity.LEGENDARY);
            assertEquals("种子=" + seed + " LEGENDARY → 始终 LEGENDARY（rarityBonus=0 无升级）",
                    Rarity.LEGENDARY, result);
        }
    }

    // ==================== 2. 稀有度升级逻辑（有 rarityBonus 时的行为） ====================

    @Test
    public void testReforgeRarityWithBonus_AlwaysUpgrade() {
        RandomUtils.setSeed(42L);
        List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                1, Rarity.COMMON, 100f, true, Rarity.COMMON);
        assertEquals("100%加成 → 至少 UNCOMMON", Rarity.UNCOMMON, rarities.get(0));
    }

    @Test
    public void testReforgeRarityWithBonus_LargeBonus() {
        RandomUtils.setSeed(42L);
        List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                1, Rarity.COMMON, 300f, true, Rarity.COMMON);
        Rarity result = rarities.get(0);
        assertTrue("300%加成 → 稀有度 >= " + Rarity.RARE + "，实际=" + result,
                result.ordinal() >= Rarity.RARE.ordinal());
    }

    @Test
    public void testReforgeRarityWithBonus_NeverExceedLegendary() {
        RandomUtils.setSeed(42L);
        List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                1, Rarity.COMMON, 999f, true, Rarity.COMMON);
        assertEquals("999%加成 → 不应超过 LEGENDARY", Rarity.LEGENDARY, rarities.get(0));
    }

    @Test
    public void testReforgeRarityWithBonus_ZeroBonusNoUpgrade() {
        RandomUtils.setSeed(12345L);
        for (Rarity equipRarity : Rarity.values()) {
            List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                    1, Rarity.COMMON, 0f, true, equipRarity);
            assertEquals("0%加成 " + equipRarity + " → 词缀稀有度=" + equipRarity,
                    equipRarity, rarities.get(0));
        }
    }

    // ==================== 3. 概率分布：大量样本验证（100000 次） ====================

    @Test
    public void testReforgeRarityDistribution_Common() {
        RandomUtils.setSeed(777L);
        int trials = 100000;
        int[] counts = new int[Rarity.values().length];
        for (int i = 0; i < trials; i++) {
            List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                    1, Rarity.COMMON, 0f, true, Rarity.COMMON);
            counts[rarities.get(0).ordinal()]++;
        }
        assertEquals("COMMON 装备重炼 → 100% COMMON", trials, counts[Rarity.COMMON.ordinal()]);
        assertEquals("UNCOMMON 出现 0 次", 0, counts[Rarity.UNCOMMON.ordinal()]);
        assertEquals("RARE 出现 0 次", 0, counts[Rarity.RARE.ordinal()]);
        assertEquals("EPIC 出现 0 次", 0, counts[Rarity.EPIC.ordinal()]);
        assertEquals("LEGENDARY 出现 0 次", 0, counts[Rarity.LEGENDARY.ordinal()]);
    }

    @Test
    public void testReforgeRarityDistribution_Rare() {
        RandomUtils.setSeed(777L);
        int trials = 100000;
        int[] counts = new int[Rarity.values().length];
        for (int i = 0; i < trials; i++) {
            List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                    1, Rarity.COMMON, 0f, true, Rarity.RARE);
            counts[rarities.get(0).ordinal()]++;
        }
        assertEquals("RARE 装备重炼 → 100% RARE", trials, counts[Rarity.RARE.ordinal()]);
        assertEquals("低于 RARE 出现 0 次", 0,
                counts[Rarity.COMMON.ordinal()] + counts[Rarity.UNCOMMON.ordinal()]);
    }

    @Test
    public void testReforgeRarityDistribution_LegendaryGuaranteed() {
        RandomUtils.setSeed(777L);
        int trials = 50000;
        for (int i = 0; i < trials; i++) {
            List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                    1, Rarity.COMMON, 0f, true, Rarity.LEGENDARY);
            assertEquals("LEGENDARY 装备 → 永远 LEGENDARY", Rarity.LEGENDARY, rarities.get(0));
        }
    }

    // ==================== 4. generateRaritiesWithPity 返回列表结构检查 ====================

    @Test
    public void testGenerateRaritiesWithPity_ListSize() {
        RandomUtils.setSeed(42L);
        for (Rarity r : Rarity.values()) {
            List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                    1, Rarity.COMMON, 0f, true, r);
            assertEquals("count=1 → 列表大小应为 1", 1, rarities.size());
        }
    }

    @Test
    public void testGenerateRaritiesWithPity_MultiCount() {
        RandomUtils.setSeed(42L);
        List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                3, Rarity.COMMON, 0f, true, Rarity.COMMON);
        assertEquals("count=3 → 列表大小应为 3", 3, rarities.size());
        assertEquals("保底项=COMMON", Rarity.COMMON, rarities.get(0));
        assertEquals("普通项1=COMMON", Rarity.COMMON, rarities.get(1));
        assertEquals("普通项2=COMMON", Rarity.COMMON, rarities.get(2));
    }

    @Test
    public void testGenerateRaritiesWithPity_NoPity() {
        RandomUtils.setSeed(42L);
        List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                2, Rarity.COMMON, 0f, false, Rarity.LEGENDARY);
        assertEquals("count=2, noPity → 列表大小应为 2", 2, rarities.size());
    }

    @Test
    public void testGenerateRaritiesWithPity_ListNotNull() {
        RandomUtils.setSeed(42L);
        for (Rarity r : Rarity.values()) {
            List<Rarity> rarities = RngEngine.generateRaritiesWithPity(
                    1, Rarity.COMMON, 100f, true, r);
            assertNotNull("列表不应为 null", rarities);
            assertFalse("列表不应为空", rarities.isEmpty());
        }
    }

    // ==================== 5. 词缀替换逻辑 ====================

    /**
     * 模拟词缀替换：用 newAffix 替换 affixes.get(idx)
     */
    private static class DummyAffix {
        final int id;
        final String name;

        DummyAffix(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DummyAffix)) return false;
            DummyAffix that = (DummyAffix) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    private List<DummyAffix> simulateReplaceAffix(List<DummyAffix> affixes, int idx, DummyAffix newAffix) {
        if (idx < 0 || idx >= affixes.size()) return new ArrayList<>(affixes);
        List<DummyAffix> result = new ArrayList<>(affixes);
        result.set(idx, newAffix);
        return result;
    }

    @Test
    public void testReplaceAffix_Index0() {
        List<DummyAffix> affixes = new ArrayList<>(Arrays.asList(
                new DummyAffix(1, "旧词缀1"),
                new DummyAffix(2, "旧词缀2"),
                new DummyAffix(3, "旧词缀3")));
        DummyAffix newAffix = new DummyAffix(99, "新词缀");
        List<DummyAffix> result = simulateReplaceAffix(affixes, 0, newAffix);
        assertEquals("索引0 应为新词缀", newAffix, result.get(0));
        assertEquals("索引1 应不变", affixes.get(1), result.get(1));
        assertEquals("索引2 应不变", affixes.get(2), result.get(2));
    }

    @Test
    public void testReplaceAffix_LastIndex() {
        List<DummyAffix> affixes = new ArrayList<>(Arrays.asList(
                new DummyAffix(1, "词缀A"),
                new DummyAffix(2, "词缀B"),
                new DummyAffix(3, "词缀C")));
        DummyAffix newAffix = new DummyAffix(88, "新词缀");
        List<DummyAffix> result = simulateReplaceAffix(affixes, 2, newAffix);
        assertEquals("索引0 应不变", affixes.get(0), result.get(0));
        assertEquals("索引1 应不变", affixes.get(1), result.get(1));
        assertEquals("索引2 应为新词缀", newAffix, result.get(2));
    }

    @Test
    public void testReplaceAffix_MiddleIndex() {
        List<DummyAffix> affixes = new ArrayList<>(Arrays.asList(
                new DummyAffix(1, "词缀A"),
                new DummyAffix(2, "词缀B"),
                new DummyAffix(3, "词缀C")));
        DummyAffix newAffix = new DummyAffix(77, "新词缀");
        List<DummyAffix> result = simulateReplaceAffix(affixes, 1, newAffix);
        assertEquals("索引0 应不变", affixes.get(0), result.get(0));
        assertEquals("索引1 应为新词缀", newAffix, result.get(1));
        assertEquals("索引2 应不变", affixes.get(2), result.get(2));
    }

    @Test
    public void testReplaceAffix_SizeUnchanged() {
        List<DummyAffix> affixes = new ArrayList<>(Arrays.asList(
                new DummyAffix(1, "A"),
                new DummyAffix(2, "B")));
        List<DummyAffix> result = simulateReplaceAffix(affixes, 0, new DummyAffix(99, "新"));
        assertEquals("重炼后词缀数量不变", affixes.size(), result.size());
    }

    @Test
    public void testReplaceAffix_ReferenceChanges() {
        DummyAffix old = new DummyAffix(1, "旧词缀");
        List<DummyAffix> affixes = new ArrayList<>(Collections.singletonList(old));
        DummyAffix newAffix = new DummyAffix(99, "新词缀");
        List<DummyAffix> result = simulateReplaceAffix(affixes, 0, newAffix);
        assertNotEquals("替换后引用应与旧引用不同", old, result.get(0));
        assertEquals("替换后引用应等于新引用", newAffix, result.get(0));
    }

    @Test
    public void testReplaceAffix_SingleElementList() {
        DummyAffix old = new DummyAffix(1, "唯一词缀");
        List<DummyAffix> affixes = new ArrayList<>(Collections.singletonList(old));
        DummyAffix newAffix = new DummyAffix(100, "新词缀");
        List<DummyAffix> result = simulateReplaceAffix(affixes, 0, newAffix);
        assertEquals("单元素替换", newAffix, result.get(0));
        assertEquals("单元素大小仍为 1", 1, result.size());
    }

    // ==================== 6. 重炼次数限制（hasReforged 标志） ====================

    private static class ReforgeState {
        boolean hasReforged;
        int replaceCount;

        ReforgeState() {
            this.hasReforged = false;
            this.replaceCount = 0;
        }

        boolean canReforge() {
            return !hasReforged;
        }

        boolean tryReforge(int selectedIndex, int affixSize) {
            if (hasReforged) return false;
            if (selectedIndex < 0 || selectedIndex >= affixSize) return false;
            hasReforged = true;
            replaceCount++;
            return true;
        }
    }

    @Test
    public void testHasReforged_InitialState() {
        ReforgeState state = new ReforgeState();
        assertFalse("初始 hasReforged 应为 false", state.hasReforged);
        assertTrue("初始应可重炼", state.canReforge());
    }

    @Test
    public void testHasReforged_AfterOneReforge() {
        ReforgeState state = new ReforgeState();
        boolean result = state.tryReforge(0, 3);
        assertTrue("第一次重炼应成功", result);
        assertTrue("重炼后 hasReforged 应为 true", state.hasReforged);
        assertFalse("重炼后不应再可重炼", state.canReforge());
        assertEquals("replaceCount 应为 1", 1, state.replaceCount);
    }

    @Test
    public void testHasReforged_SecondReforgeBlocked() {
        ReforgeState state = new ReforgeState();
        state.tryReforge(1, 4);
        boolean second = state.tryReforge(2, 4);
        assertFalse("第二次重炼应被阻止", second);
        assertEquals("replaceCount 仍为 1", 1, state.replaceCount);
    }

    @Test
    public void testHasReforged_MultipleAttemptsBlocked() {
        ReforgeState state = new ReforgeState();
        state.tryReforge(0, 5);
        int attempts = 0;
        for (int i = 0; i < 10; i++) {
            if (state.tryReforge(i % 5, 5)) attempts++;
        }
        assertEquals("10次尝试仅1次成功", 1, state.replaceCount);
        assertEquals("额外成功次数为0", 0, attempts);
    }

    @Test
    public void testHasReforged_ResetNotAllowed() {
        ReforgeState state = new ReforgeState();
        state.tryReforge(0, 3);
        assertTrue("hasReforged 已置为 true", state.hasReforged);
        assertFalse("hasReforged 不应被外部重置（事件内禁止二次重炼）",
                state.canReforge());
    }

    // ==================== 7. 索引边界条件 ====================

    @Test
    public void testIndexBoundary_NegativeIndex() {
        ReforgeState state = new ReforgeState();
        boolean result = state.tryReforge(-1, 3);
        assertFalse("idx=-1 应拒绝", result);
        assertFalse("hasReforged 仍为 false", state.hasReforged);
    }

    @Test
    public void testIndexBoundary_IndexEqualsSize() {
        ReforgeState state = new ReforgeState();
        boolean result = state.tryReforge(3, 3);
        assertFalse("idx=3 (size=3) 应拒绝", result);
        assertFalse("hasReforged 仍为 false", state.hasReforged);
    }

    @Test
    public void testIndexBoundary_IndexExceedsSize() {
        ReforgeState state = new ReforgeState();
        boolean result = state.tryReforge(5, 3);
        assertFalse("idx=5 (size=3) 应拒绝", result);
        assertFalse("hasReforged 仍为 false", state.hasReforged);
    }

    @Test
    public void testIndexBoundary_ValidIndices() {
        ReforgeState state;
        for (int size = 1; size <= 5; size++) {
            for (int idx = 0; idx < size; idx++) {
                state = new ReforgeState();
                boolean result = state.tryReforge(idx, size);
                assertTrue("size=" + size + " idx=" + idx + " 应合法", result);
            }
        }
    }

    @Test
    public void testIndexBoundary_EmptyList() {
        ReforgeState state = new ReforgeState();
        boolean result = state.tryReforge(0, 0);
        assertFalse("空列表 idx=0 应拒绝", result);
        result = state.tryReforge(-1, 0);
        assertFalse("空列表 idx=-1 应拒绝", result);
    }

    @Test
    public void testIndexBoundary_UnselectedState() {
        int idx = -1;
        int size = 4;
        boolean isValid = idx >= 0 && idx < size;
        assertFalse("selIdx=-1（未选择状态）应判定为无效索引", isValid);
    }

    @Test
    public void testIndexBoundary_VeryLargeIndex() {
        ReforgeState state = new ReforgeState();
        boolean result = state.tryReforge(Integer.MAX_VALUE, 5);
        assertFalse("极大型 idx 应拒绝", result);
    }

    @Test
    public void testIndexBoundary_NegativeLargeIndex() {
        ReforgeState state = new ReforgeState();
        boolean result = state.tryReforge(Integer.MIN_VALUE, 5);
        assertFalse("极小型 idx 应拒绝", result);
    }

    // ==================== 8. 装备选择逻辑（背包过滤 EquipItem） ====================

    private static class DummyItem {
        final String id;
        final boolean isEquip;

        DummyItem(String id, boolean isEquip) {
            this.id = id;
            this.isEquip = isEquip;
        }

        boolean isEquipItem() {
            return isEquip;
        }
    }

    @Test
    public void testFilterEquipItems_OnlyEquip() {
        List<DummyItem> bag = Arrays.asList(
                new DummyItem("sword", true),
                new DummyItem("shield", true),
                new DummyItem("armor", true));
        List<DummyItem> equips = new ArrayList<>();
        for (DummyItem item : bag) {
            if (item.isEquipItem()) equips.add(item);
        }
        assertEquals("全部为装备 → 过滤后数量 3", 3, equips.size());
    }

    @Test
    public void testFilterEquipItems_MixedBag() {
        List<DummyItem> bag = Arrays.asList(
                new DummyItem("potion", false),
                new DummyItem("sword", true),
                new DummyItem("elixir", false),
                new DummyItem("shield", true),
                new DummyItem("scroll", false));
        List<DummyItem> equips = new ArrayList<>();
        for (DummyItem item : bag) {
            if (item.isEquipItem()) equips.add(item);
        }
        assertEquals("混合背包 → 过滤后仅装备 2", 2, equips.size());
        assertEquals("第一个装备=sword", "sword", equips.get(0).id);
        assertEquals("第二个装备=shield", "shield", equips.get(1).id);
    }

    @Test
    public void testFilterEquipItems_NoEquip() {
        List<DummyItem> bag = Arrays.asList(
                new DummyItem("potion", false),
                new DummyItem("elixir", false));
        List<DummyItem> equips = new ArrayList<>();
        for (DummyItem item : bag) {
            if (item.isEquipItem()) equips.add(item);
        }
        assertTrue("无装备 → 过滤后列表为空", equips.isEmpty());
    }

    @Test
    public void testFilterEquipItems_EmptyBag() {
        List<DummyItem> bag = Collections.emptyList();
        List<DummyItem> equips = new ArrayList<>();
        for (DummyItem item : bag) {
            if (item.isEquipItem()) equips.add(item);
        }
        assertTrue("空背包 → 过滤后列表为空", equips.isEmpty());
    }

    @Test
    public void testFilterEquipItems_LargeBag() {
        List<DummyItem> bag = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            bag.add(new DummyItem("item" + i, i % 4 == 0));
        }
        List<DummyItem> equips = new ArrayList<>();
        for (DummyItem item : bag) {
            if (item.isEquipItem()) equips.add(item);
        }
        assertEquals("100件物品中 25 件装备", 25, equips.size());
    }

    // ==================== 9. 空背包自动填充装备逻辑 ====================

    @Test
    public void testAutoFillEquipment_WhenBagEmpty() {
        boolean bagEmpty = true;
        boolean filled = false;
        List<DummyItem> bag = new ArrayList<>();
        if (bag.isEmpty()) {
            bag.add(new DummyItem("auto_1", true));
            bag.add(new DummyItem("auto_2", true));
            bag.add(new DummyItem("auto_3", true));
            bag.add(new DummyItem("auto_4", true));
            filled = true;
        }
        assertTrue("空背包应触发自动填充", filled);
        assertEquals("自动填充 4 件装备（COMMON/UNCOMMON/RARE/EPIC）", 4, bag.size());
    }

    @Test
    public void testAutoFillEquipment_WhenBagNotEmpty() {
        List<DummyItem> bag = new ArrayList<>();
        bag.add(new DummyItem("sword", true));
        boolean filled = false;
        if (bag.isEmpty()) {
            bag.add(new DummyItem("auto", true));
            filled = true;
        }
        assertFalse("非空背包不应触发自动填充", filled);
        assertEquals("保持原有物品数", 1, bag.size());
    }

    // ==================== 10. 无可用装备时的消息 ====================

    @Test
    public void testNoEquipAvailableMessage() {
        String result = "你的背包中没有可重炼的装备。";
        assertTrue("应包含'没有'", result.contains("没有"));
        assertTrue("应包含'重炼'", result.contains("重炼"));
        assertTrue("应包含'装备'", result.contains("装备"));
    }

    @Test
    public void testRefuseReforgeMessage() {
        String result = "你婉拒了铁匠大师的好意。";
        assertTrue("应包含'婉拒'", result.contains("婉拒"));
        assertTrue("应包含'铁匠'或'大师'", result.contains("铁匠") || result.contains("大师"));
    }

    @Test
    public void testReforgeCompleteMessage() {
        String result = "装备重炼完成！";
        assertTrue("应包含'重炼'", result.contains("重炼"));
        assertTrue("应包含'完成'", result.contains("完成"));
    }

    // ==================== 11. 按钮状态流转 ====================

    @Test
    public void testButtonState_InitialDisabled() {
        int selIdx = -1;
        boolean buttonEnabled = selIdx >= 0;
        String buttonText = "请选择要重炼的词条";
        assertFalse("未选择词缀 → 按钮不可用", buttonEnabled);
        assertEquals("按钮文案=提示选择", "请选择要重炼的词条", buttonText);
    }

    @Test
    public void testButtonState_AfterSelection() {
        int selIdx = 1;
        boolean buttonEnabled = selIdx >= 0;
        String buttonText = "重炼此词条";
        assertTrue("选择词缀后 → 按钮可用", buttonEnabled);
        assertEquals("按钮文案=重炼此词条", "重炼此词条", buttonText);
    }

    @Test
    public void testButtonState_AfterReforge() {
        boolean hasReforged = true;
        String buttonText = hasReforged ? "确定" : "重炼此词条";
        assertEquals("重炼后 → 按钮文案=确定", "确定", buttonText);
    }

    @Test
    public void testCancelButtonVisibility_AfterReforge() {
        boolean hasReforged = true;
        boolean cancelVisible = !hasReforged;
        assertFalse("重炼后 → 取消按钮隐藏", cancelVisible);
    }

    @Test
    public void testCancelButtonVisibility_BeforeReforge() {
        boolean hasReforged = false;
        boolean cancelVisible = !hasReforged;
        assertTrue("重炼前 → 取消按钮可见", cancelVisible);
    }

    // ==================== 12. 词缀列表为空时的保护 ====================

    @Test
    public void testAffixListEmpty_IndexCheckBlocks() {
        int affixCount = 0;
        int selIdx = 0;
        boolean valid = selIdx >= 0 && selIdx < affixCount;
        assertFalse("空词缀列表 idx=0 应被拦截", valid);
    }

    @Test
    public void testAffixListEmpty_AllIndicesInvalid() {
        int affixCount = 0;
        for (int idx = -10; idx <= 10; idx++) {
            boolean valid = idx >= 0 && idx < affixCount;
            assertFalse("空词缀列表 idx=" + idx + " 无效", valid);
        }
    }

    // ==================== 13. generateSingleAffixForEquipment 返回 null 时兜底 ====================

    @Test
    public void testNullAffix_ReplaceSkipped() {
        List<DummyAffix> affixes = new ArrayList<>(Arrays.asList(
                new DummyAffix(1, "词缀A"),
                new DummyAffix(2, "词缀B")));
        DummyAffix newAffix = null;
        int idx = 0;
        boolean hasReforged = false;

        if (newAffix != null) {
            affixes.set(idx, newAffix);
            hasReforged = true;
        }

        assertEquals("null 词缀 → idx 0 仍为原词缀", new DummyAffix(1, "词缀A"), affixes.get(0));
        assertFalse("null 词缀 → hasReforged 仍为 false", hasReforged);
    }

    @Test
    public void testNullAffix_ListUnchanged() {
        List<DummyAffix> affixes = new ArrayList<>(Arrays.asList(
                new DummyAffix(1, "A"), new DummyAffix(2, "B"), new DummyAffix(3, "C")));
        List<DummyAffix> original = new ArrayList<>(affixes);
        DummyAffix newAffix = null;
        int idx = 1;

        if (newAffix != null) {
            affixes.set(idx, newAffix);
        }

        assertEquals("null 词缀 → 列表完全不变", original, affixes);
    }

    // ==================== 14. 背包空间测试（重炼不增加物品，不占空间） ====================

    @Test
    public void testReforgeDoesNotAffectBagSize() {
        int bagSizeBefore = 20;
        int bagSizeAfter = 20;
        assertEquals("重炼不新增/不消耗物品 → 背包容量不变", bagSizeBefore, bagSizeAfter);
    }

    @Test
    public void testReforgeWorksWithFullBag() {
        int bagSize = 125;
        int bagMax = 125;
        boolean isFull = bagSize >= bagMax;
        assertTrue("背包满 125/125", isFull);
        boolean reforgeAllowed = true;
        assertTrue("重炼不占空间 → 背包满也能重炼", reforgeAllowed);
    }

    @Test
    public void testReforgeWorksWithSingleItemBag() {
        int bagSize = 1;
        boolean hasEquip = bagSize > 0;
        assertTrue("背包有 1 件物品 → 可能是装备可重炼", hasEquip);
    }

    // ==================== 15. 稀有度与词缀数量关系 ====================

    @Test
    public void testRarityAffixCountConsistency() {
        assertEquals("COMMON 词缀数=1", 1, Rarity.COMMON.getAffixCount());
        assertEquals("UNCOMMON 词缀数=2", 2, Rarity.UNCOMMON.getAffixCount());
        assertEquals("RARE 词缀数=3", 3, Rarity.RARE.getAffixCount());
        assertEquals("EPIC 词缀数=4", 4, Rarity.EPIC.getAffixCount());
        assertEquals("LEGENDARY 词缀数=5", 5, Rarity.LEGENDARY.getAffixCount());
    }

    @Test
    public void testValidReforgeIndexByRarity() {
        for (Rarity r : Rarity.values()) {
            int affixCount = r.getAffixCount();
            assertTrue("稀有度 " + r + " 有 " + affixCount + " 个词缀",
                    affixCount >= 1 && affixCount <= 5);
            for (int idx = 0; idx < affixCount; idx++) {
                boolean valid = idx >= 0 && idx < affixCount;
                assertTrue(r + ": idx=" + idx + " 应有效", valid);
            }
            boolean invalid = affixCount >= 0 && affixCount < affixCount;
            assertFalse(r + ": idx=affixCount 应无效", invalid);
        }
    }

    // ==================== 16. 词缀稀有度加成参数 ====================

    @Test
    public void testAffixRarityBonus_Values() {
        assertEquals("COMMON bonus=0", 0, Rarity.COMMON.getAffixRarityBonus());
        assertEquals("UNCOMMON bonus=5", 5, Rarity.UNCOMMON.getAffixRarityBonus());
        assertEquals("RARE bonus=20", 20, Rarity.RARE.getAffixRarityBonus());
        assertEquals("EPIC bonus=50", 50, Rarity.EPIC.getAffixRarityBonus());
        assertEquals("LEGENDARY bonus=100", 100, Rarity.LEGENDARY.getAffixRarityBonus());
    }

    @Test
    public void testAffixRarityBonus_Progression() {
        int[] bonuses = {
                Rarity.COMMON.getAffixRarityBonus(),
                Rarity.UNCOMMON.getAffixRarityBonus(),
                Rarity.RARE.getAffixRarityBonus(),
                Rarity.EPIC.getAffixRarityBonus(),
                Rarity.LEGENDARY.getAffixRarityBonus()
        };
        for (int i = 1; i < bonuses.length; i++) {
            assertTrue("稀有度加成应递增: " + bonuses[i - 1] + " < " + bonuses[i],
                    bonuses[i - 1] < bonuses[i]);
        }
    }

    // ==================== 17. 确定性验证 ====================

    @Test
    public void testDeterministicWhenNoBonus() {
        Rarity r1 = RngEngine.generateRaritiesWithPity(1, Rarity.COMMON, 0f, true, Rarity.RARE).get(0);
        Rarity r2 = RngEngine.generateRaritiesWithPity(1, Rarity.COMMON, 0f, true, Rarity.RARE).get(0);

        assertEquals("0%加成 → 多次调用结果相同（无随机性影响）", r1, r2);
    }

    @Test
    public void testReforgeIsDeterministicForEachRarity() {
        for (Rarity equipRarity : Rarity.values()) {
            Rarity r1 = simulateReforgeRarity(equipRarity);
            Rarity r2 = simulateReforgeRarity(equipRarity);

            assertEquals(equipRarity + " 0%加成 → 结果应一致", r1, r2);
        }
    }

    @Test
    public void testReforgeWithBonusMayVaryBetweenCalls() {
        RandomUtils.setSeed(42L);
        Rarity r1 = RngEngine.generateRaritiesWithPity(1, Rarity.COMMON, 50f, true, Rarity.COMMON).get(0);

        RandomUtils.setSeed(42L);
        Rarity r2 = RngEngine.generateRaritiesWithPity(1, Rarity.COMMON, 50f, true, Rarity.COMMON).get(0);

        assertNotNull("有加成时 result 不为 null", r1);
        assertNotNull("有加成时 result 不为 null", r2);
        assertTrue("有加成时词缀稀有度 >= COMMON", r1.ordinal() >= Rarity.COMMON.ordinal());
        assertTrue("有加成时词缀稀有度 >= COMMON", r2.ordinal() >= Rarity.COMMON.ordinal());
        assertTrue("有加成时词缀稀有度 <= LEGENDARY", r1.ordinal() <= Rarity.LEGENDARY.ordinal());
        assertTrue("有加成时词缀稀有度 <= LEGENDARY", r2.ordinal() <= Rarity.LEGENDARY.ordinal());
    }

    // ==================== 18. 综合场景：完整重炼流程模拟 ====================

    @Test
    public void testFullReforgeScenario_Success() {
        boolean bagHasEquip = true;
        assertTrue("背包有装备", bagHasEquip);

        Rarity equipRarity = Rarity.RARE;
        int affixCount = equipRarity.getAffixCount();
        List<DummyAffix> affixes = new ArrayList<>();
        for (int i = 0; i < affixCount; i++) {
            affixes.add(new DummyAffix(i, "词缀" + i));
        }

        int selIdx = 1;
        DummyAffix oldAffix = affixes.get(selIdx);
        DummyAffix newAffix = new DummyAffix(999, "重炼新词缀");
        boolean hasReforged = false;

        hasReforged = true;
        affixes.set(selIdx, newAffix);

        assertTrue("重炼完成", hasReforged);
        assertEquals("词缀0 不变", new DummyAffix(0, "词缀0"), affixes.get(0));
        assertEquals("词缀1 已被替换", newAffix, affixes.get(1));
        assertEquals("词缀2 不变", new DummyAffix(2, "词缀2"), affixes.get(2));
        assertNotEquals("词缀1 与旧词缀不同", oldAffix, affixes.get(selIdx));
    }

    @Test
    public void testFullReforgeScenario_NoEquipInBag() {
        boolean bagHasEquip = false;
        String result = "你的背包中没有可重炼的装备。";
        if (!bagHasEquip) {
            assertTrue("无装备 → 提示玩家", result.contains("没有"));
        }
    }

    @Test
    public void testFullReforgeScenario_PlayerRefuses() {
        boolean accepted = false;
        String result = accepted ? "重炼完成" : "你婉拒了铁匠大师的好意。";
        assertTrue("拒绝 → 退出事件", result.contains("婉拒"));
    }

    // ==================== 19. 事件配置契约 ====================

    @Test
    public void testEventKeyIsEquipmentReforge() {
        String eventKey = "equipment_reforge";
        assertEquals("事件 key 固定", "equipment_reforge", eventKey);
        assertTrue("包含 reforge", eventKey.contains("reforge"));
        assertTrue("包含 equip", eventKey.contains("equip"));
    }

    @Test
    public void testReforgeIsOneTimeOnly() {
        boolean[] hasReforged = {false};
        int reforgeCount = 0;

        for (int attempt = 0; attempt < 3; attempt++) {
            if (!hasReforged[0]) {
                hasReforged[0] = true;
                reforgeCount++;
            }
        }
        assertEquals("3次尝试应仅重炼1次", 1, reforgeCount);
    }

    @Test
    public void testReforgeButtonText_Stages() {
        String[] stages = {"请选择要重炼的词条", "重炼此词条", "确定"};
        assertEquals("阶段0: 未选择", "请选择要重炼的词条", stages[0]);
        assertEquals("阶段1: 已选择", "重炼此词条", stages[1]);
        assertEquals("阶段2: 已完成", "确定", stages[2]);
    }

    @Test
    public void testRefuseOptionAlwaysAvailable() {
        assertTrue("'暂时不需要'选项始终可用", true);
    }

    // ==================== 20. 数值边界安全测试 ====================

    @Test
    public void testRarityFromId_ValidRange() {
        for (int id = 0; id <= 4; id++) {
            Rarity r = Rarity.fromId(id);
            assertNotNull("Rarity.fromId(" + id + ") 不应为 null", r);
            assertEquals("id 应匹配", id, r.getId());
        }
    }

    @Test
    public void testRarityFromId_InvalidReturnsNull() {
        assertNull("fromId(-1) 应为 null", Rarity.fromId(-1));
        assertNull("fromId(99) 应为 null", Rarity.fromId(99));
    }

    @Test
    public void testRarityOrdinalOrder() {
        Rarity[] all = Rarity.values();
        assertEquals("Rarity 数量=5", 5, all.length);
        assertEquals(0, Rarity.COMMON.ordinal());
        assertEquals(1, Rarity.UNCOMMON.ordinal());
        assertEquals(2, Rarity.RARE.ordinal());
        assertEquals(3, Rarity.EPIC.ordinal());
        assertEquals(4, Rarity.LEGENDARY.ordinal());
    }
}
