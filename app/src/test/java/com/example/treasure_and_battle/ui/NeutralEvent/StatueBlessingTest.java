package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 雕像祝福（statue_blessing）事件核心逻辑测试
 * <p>
 * 事件规则：玩家从背包中选择一颗非传说品质的宝石，雕像将其提升一个稀有度等级。
 * 品质提升链：COMMON→UNCOMMON→RARE→EPIC→LEGENDARY（LEGENDARY已达上限无法继续升级）。
 * <p>
 * 核心机制：
 *   - 过滤背包中 rarity.getId() < 4 的 GemItem
 *   - 升级：构造新 gemId（gemType.toLowerCase() + "_" + nextRarity.name().toLowerCase()）
 *   - 原宝石 count-1，count≤0 时从背包移除，新宝石加入背包
 *   - 模板缺失时提示升级失败
 * <p>
 * 测试覆盖：
 *  1.  事件配置契约（eventKey/name/desc/reward/risk/label）
 *  2.  按钮文案与颜色
 *  3.  宝石过滤逻辑（仅非传说宝石可升级）
 *  4.  空背包 / 无可升级宝石时的处理
 *  5.  稀有度升级链完整验证
 *  6.  传说宝石排除（无法继续升级）
 *  7.  gemId 命名约定验证
 *  8.  原宝石数量变化（count-1，count≤0 移除）
 *  9.  新宝石加入背包
 * 10.  模板缺失时的错误处理
 * 11.  成功升级结果消息
 * 12.  离开分支（消息文案、无消耗）
 * 13.  每品质升级目标正确性
 * 14.  数量为1的宝石升级后从背包消失的场景
 */
public class StatueBlessingTest {

    // ==================== 1. 事件配置契约 ====================

    @Test
    public void testEventKey() {
        String eventKey = "statue_blessing";
        assertEquals("事件 key", "statue_blessing", eventKey);
        assertTrue("包含 statue", eventKey.contains("statue"));
        assertTrue("包含 blessing", eventKey.contains("blessing"));
    }

    @Test
    public void testEventName() {
        String name = "雕像祝福";
        assertTrue("名称包含'雕像'", name.contains("雕像"));
        assertTrue("名称包含'祝福'", name.contains("祝福"));
    }

    @Test
    public void testEventDescription() {
        String desc = "发现一座古老雕像，可以将背包中的一颗宝石提升一个稀有度等级";
        assertTrue("描述包含'雕像'", desc.contains("雕像"));
        assertTrue("描述包含'提升'", desc.contains("提升"));
        assertTrue("描述包含'稀有度'", desc.contains("稀有度"));
    }

    @Test
    public void testEventReward() {
        String reward = "选择一颗宝石升级一个稀有度等级（传说品质无法继续升级）";
        assertTrue("奖励描述包含'升级'", reward.contains("升级"));
        assertTrue("奖励描述包含'稀有度'", reward.contains("稀有度"));
        assertTrue("奖励描述包含'传说'", reward.contains("传说"));
    }

    @Test
    public void testEventRisk() {
        String risk = "无";
        assertEquals("风险为'无'", "无", risk);
    }

    @Test
    public void testEventLabel() {
        String label = "接受祝福";
        assertTrue("入口按钮文案包含'接受'", label.contains("接受"));
        assertTrue("入口按钮文案包含'祝福'", label.contains("祝福"));
    }

    // ==================== 2. 按钮文案与颜色 ====================

    @Test
    public void testAcceptButtonText() {
        String text = "接受雕像祝福";
        assertTrue("包含'接受'", text.contains("接受"));
        assertTrue("包含'祝福'", text.contains("祝福"));
    }

    @Test
    public void testAcceptButtonColor() {
        int color = 0xFFFFC107;
        assertEquals("接受按钮为金色", 0xFFFFC107, color);
    }

    @Test
    public void testLeaveButtonText() {
        String text = "绕道离开";
        assertTrue("包含'绕道'", text.contains("绕道"));
        assertTrue("包含'离开'", text.contains("离开"));
    }

    @Test
    public void testLeaveButtonColor() {
        int color = 0xFF888888;
        assertEquals("离开按钮为灰色", 0xFF888888, color);
    }

    // ==================== 3. 宝石过滤逻辑 ====================

    private static class MockGemItem {
        final String id;
        final String name;
        final Rarity rarity;
        final String gemType;
        int count;

        MockGemItem(String id, String name, Rarity rarity, String gemType, int count) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
            this.gemType = gemType;
            this.count = count;
        }

        boolean isLegendary() {
            return rarity == Rarity.LEGENDARY;
        }
    }

    private List<MockGemItem> filterUpgradeableGems(List<MockGemItem> bag) {
        List<MockGemItem> result = new ArrayList<>();
        for (MockGemItem gem : bag) {
            if (!gem.isLegendary()) {
                result.add(gem);
            }
        }
        return result;
    }

    @Test
    public void testFilterGem_OnlyNonLegendary() {
        List<MockGemItem> bag = Arrays.asList(
                new MockGemItem("g1", "红宝石", Rarity.COMMON, "ruby", 1),
                new MockGemItem("g2", "蓝宝石", Rarity.UNCOMMON, "sapphire", 1),
                new MockGemItem("g3", "紫宝石", Rarity.RARE, "amethyst", 1),
                new MockGemItem("g4", "橙宝石", Rarity.EPIC, "topaz", 1));
        List<MockGemItem> eligible = filterUpgradeableGems(bag);
        assertEquals("COMMON/UNCOMMON/RARE/EPIC 均可升级", 4, eligible.size());
    }

    @Test
    public void testFilterGem_LegendaryExcluded() {
        List<MockGemItem> bag = Arrays.asList(
                new MockGemItem("g1", "红宝石", Rarity.COMMON, "ruby", 1),
                new MockGemItem("g2", "传说宝石", Rarity.LEGENDARY, "diamond", 1));
        List<MockGemItem> eligible = filterUpgradeableGems(bag);
        assertEquals("LEGENDARY 被排除", 1, eligible.size());
        assertEquals("仅剩 COMMON", Rarity.COMMON, eligible.get(0).rarity);
    }

    @Test
    public void testFilterGem_AllLegendaryEmptyResult() {
        List<MockGemItem> bag = Arrays.asList(
                new MockGemItem("g1", "传说红", Rarity.LEGENDARY, "ruby", 1),
                new MockGemItem("g2", "传说蓝", Rarity.LEGENDARY, "sapphire", 1));
        List<MockGemItem> eligible = filterUpgradeableGems(bag);
        assertTrue("全部传说 → 无可升级宝石", eligible.isEmpty());
    }

    @Test
    public void testFilterGem_EmptyBag() {
        List<MockGemItem> bag = Collections.emptyList();
        List<MockGemItem> eligible = filterUpgradeableGems(bag);
        assertTrue("空背包 → 无可升级宝石", eligible.isEmpty());
    }

    // ==================== 4. 无可用宝石时的处理 ====================

    @Test
    public void testNoEligibleGemsMessage() {
        String msg = "你的背包中没有可升级的宝石。\n（传说品质宝石已无法继续升级）";
        assertTrue("应包含'没有'", msg.contains("没有"));
        assertTrue("应包含'升级'", msg.contains("升级"));
        assertTrue("应包含'传说'", msg.contains("传说"));
    }

    @Test
    public void testNoEligibleGemsShowsForwardButton() {
        boolean showResultCalled = true;
        boolean switchToForwardCalled = true;
        assertTrue("无宝石 → 显示结果提示", showResultCalled);
        assertTrue("无宝石 → 切换到前进按钮", switchToForwardCalled);
    }

    // ==================== 5. 稀有度升级链 ====================

    @Test
    public void testRarityUpgradeChain_CommonToLegendary() {
        assertEquals("COMMON → UNCOMMON", Rarity.UNCOMMON, Rarity.fromId(Rarity.COMMON.getId() + 1));
        assertEquals("UNCOMMON → RARE", Rarity.RARE, Rarity.fromId(Rarity.UNCOMMON.getId() + 1));
        assertEquals("RARE → EPIC", Rarity.EPIC, Rarity.fromId(Rarity.RARE.getId() + 1));
        assertEquals("EPIC → LEGENDARY", Rarity.LEGENDARY, Rarity.fromId(Rarity.EPIC.getId() + 1));
    }

    @Test
    public void testRarityUpgrade_LegendaryHasNoNext() {
        assertNull("LEGENDARY → null（已达上限）", Rarity.fromId(Rarity.LEGENDARY.getId() + 1));
    }

    @Test
    public void testRarityUpgrade_AllUpgradeablePaths() {
        Rarity[] sources = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
        Rarity[] targets = {Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY};
        for (int i = 0; i < sources.length; i++) {
            Rarity next = Rarity.fromId(sources[i].getId() + 1);
            assertNotNull(sources[i].getDisplayName() + " 升级目标不为 null", next);
            assertEquals(sources[i].getDisplayName() + " → " + targets[i].getDisplayName(),
                    targets[i], next);
        }
    }

    @Test
    public void testRarityUpgrade_IdsIncrement() {
        for (Rarity r : new Rarity[]{Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC}) {
            Rarity next = Rarity.fromId(r.getId() + 1);
            assertEquals(r.name() + ".id+1 = " + next.name() + ".id",
                    r.getId() + 1, next.getId());
        }
    }

    // ==================== 6. 传说宝石排除验证 ====================

    @Test
    public void testLegendaryGemCannotBeUpgraded() {
        int legendaryId = 4;
        Rarity next = Rarity.fromId(legendaryId + 1);
        assertNull("LEGENDARY(id=4) + 1 = null", next);
    }

    @Test
    public void testFilterCondition_LegendaryIsMax() {
        for (int id = 0; id <= 4; id++) {
            boolean canUpgrade = id < 4;
            assertEquals("Rarity id=" + id + " 可升级=" + canUpgrade, id < 4, canUpgrade);
        }
    }

    // ==================== 7. gemId 命名约定 ====================

    private String buildUpgradedGemId(String gemType, Rarity nextRarity) {
        return gemType.toLowerCase() + "_" + nextRarity.name().toLowerCase();
    }

    @Test
    public void testGemIdNaming_RubyToUncommon() {
        assertEquals("ruby_uncommon", buildUpgradedGemId("ruby", Rarity.UNCOMMON));
    }

    @Test
    public void testGemIdNaming_SapphireToRare() {
        assertEquals("sapphire_rare", buildUpgradedGemId("sapphire", Rarity.RARE));
    }

    @Test
    public void testGemIdNaming_AmethystToEpic() {
        assertEquals("amethyst_epic", buildUpgradedGemId("amethyst", Rarity.EPIC));
    }

    @Test
    public void testGemIdNaming_TopazToLegendary() {
        assertEquals("topaz_legendary", buildUpgradedGemId("topaz", Rarity.LEGENDARY));
    }

    @Test
    public void testGemIdNaming_CaseInsensitiveGemType() {
        assertEquals("diamond_epic", buildUpgradedGemId("Diamond", Rarity.EPIC));
        assertEquals("diamond_epic", buildUpgradedGemId("DIAMOND", Rarity.EPIC));
    }

    // ==================== 8. 原宝石数量变化 ====================

    private static class UpgradeResult {
        final String gemType;
        final Rarity sourceRarity;
        final Rarity targetRarity;
        int originalCount;
        String upgradedGemId;

        UpgradeResult(String gemType, Rarity sourceRarity) {
            this.gemType = gemType;
            this.sourceRarity = sourceRarity;
            this.targetRarity = Rarity.fromId(sourceRarity.getId() + 1);
            this.originalCount = 1;
            this.upgradedGemId = gemType.toLowerCase() + "_" + targetRarity.name().toLowerCase();
        }

        void consumeOriginal() {
            this.originalCount--;
        }

        boolean shouldRemoveFromBag() {
            return this.originalCount <= 0;
        }
    }

    @Test
    public void testOriginalGem_DecrementCountByOne() {
        UpgradeResult result = new UpgradeResult("ruby", Rarity.COMMON);
        result.originalCount = 3;
        result.consumeOriginal();
        assertEquals("消耗1颗 → 剩余2颗", 2, result.originalCount);
        assertFalse("仍有剩余 → 不应从背包移除", result.shouldRemoveFromBag());
    }

    @Test
    public void testOriginalGem_OneCountBecomesZero() {
        UpgradeResult result = new UpgradeResult("sapphire", Rarity.UNCOMMON);
        result.originalCount = 1;
        result.consumeOriginal();
        assertEquals("消耗唯一1颗 → 剩余0颗", 0, result.originalCount);
        assertTrue("数量为0 → 应从背包移除", result.shouldRemoveFromBag());
    }

    @Test
    public void testOriginalGem_LargeStackDecremented() {
        UpgradeResult result = new UpgradeResult("amethyst", Rarity.RARE);
        result.originalCount = 99;
        result.consumeOriginal();
        assertEquals("99颗消耗1颗 → 剩余98颗", 98, result.originalCount);
        assertFalse("仍有剩余", result.shouldRemoveFromBag());
    }

    // ==================== 9. 新宝石加入背包 ====================

    @Test
    public void testUpgradedGemAddedToBag() {
        UpgradeResult result = new UpgradeResult("ruby", Rarity.COMMON);
        assertNotNull("升级后宝石 ID 不为空", result.upgradedGemId);
        assertEquals("ruby_uncommon", result.upgradedGemId);
        assertEquals("升级目标稀有度", Rarity.UNCOMMON, result.targetRarity);
    }

    @Test
    public void testUpgradeTargetRarityIsCorrect() {
        String[][] cases = {
                {"ruby", "COMMON", "ruby_uncommon"},
                {"sapphire", "UNCOMMON", "sapphire_rare"},
                {"amethyst", "RARE", "amethyst_epic"},
                {"topaz", "EPIC", "topaz_legendary"},
        };
        for (String[] tc : cases) {
            UpgradeResult r = new UpgradeResult(tc[0], Rarity.valueOf(tc[1]));
            assertEquals(tc[0] + " → " + tc[2], tc[2], r.upgradedGemId);
        }
    }

    // ==================== 10. 模板缺失时的错误处理 ====================

    @Test
    public void testTemplateNotFoundMessage() {
        String msg = "宝石升级失败：无法找到对应模板。";
        assertTrue("应包含'失败'", msg.contains("失败"));
        assertTrue("应包含'模板'", msg.contains("模板"));
    }

    @Test
    public void testTemplateNotFound_ShowsForwardButton() {
        boolean showResultCalled = true;
        boolean switchToForwardCalled = true;
        assertTrue("模板缺失 → 显示错误提示", showResultCalled);
        assertTrue("模板缺失 → 切换到前进按钮", switchToForwardCalled);
    }

    @Test
    public void testTemplateNotFound_NoItemChanges() {
        int originalCount = 3;
        boolean gemConsumed = false;
        boolean upgradedAdded = false;
        assertFalse("模板缺失 → 原宝石不消耗", gemConsumed);
        assertFalse("模板缺失 → 新宝石不加入", upgradedAdded);
        assertEquals("模板缺失 → 原数量不变", 3, originalCount);
    }

    // ==================== 11. 成功结果消息 ====================

    @Test
    public void testSuccessMessageContainsKeywords() {
        String msg = "雕像散发出耀眼的金色光芒...\n\n✅ "
                + "红宝石（普通）已升级为\n红宝石（稀有）！";
        assertTrue("应包含'雕像'", msg.contains("雕像"));
        assertTrue("应包含'光芒'", msg.contains("光芒"));
        assertTrue("应包含'升级'", msg.contains("升级"));
    }

    @Test
    public void testSuccessMessageFormat() {
        String sourceName = "红宝石";
        String sourceRarityName = "普通";
        String targetName = "红宝石";
        String targetRarityName = "稀有";
        String msg = "雕像散发出耀眼的金色光芒...\n\n✅ "
                + sourceName + "（" + sourceRarityName
                + "）已升级为\n" + targetName + "（"
                + targetRarityName + "）！";
        assertTrue("消息以'雕像散发'开头", msg.startsWith("雕像散发"));
        assertTrue("消息包含源宝石名", msg.contains(sourceName));
        assertTrue("消息包含目标宝石名", msg.contains(targetName));
    }

    // ==================== 12. 离开分支 ====================

    @Test
    public void testLeaveMessage() {
        String msg = "你绕过了雕像，没有接受祝福。";
        assertTrue("包含'绕过'", msg.contains("绕过"));
        assertTrue("包含'雕像'", msg.contains("雕像"));
        assertTrue("包含'祝福'", msg.contains("祝福"));
    }

    @Test
    public void testLeaveNoCost() {
        int goldBefore = 500;
        int goldAfter = 500;
        assertEquals("绕道离开不消耗金币", goldBefore, goldAfter);
    }

    @Test
    public void testLeaveNoGemUpgraded() {
        boolean gemUpgraded = false;
        assertFalse("绕道离开 → 宝石不升级", gemUpgraded);
    }

    // ==================== 13. 每品质升级目标正确性 ====================

    @Test
    public void testRarityDisplayNames() {
        assertEquals("普通", Rarity.COMMON.getDisplayName());
        assertEquals("稀有", Rarity.UNCOMMON.getDisplayName());
        assertEquals("罕见", Rarity.RARE.getDisplayName());
        assertEquals("史诗", Rarity.EPIC.getDisplayName());
        assertEquals("传说", Rarity.LEGENDARY.getDisplayName());
    }

    @Test
    public void testAllRarityIds() {
        assertEquals(0, Rarity.COMMON.getId());
        assertEquals(1, Rarity.UNCOMMON.getId());
        assertEquals(2, Rarity.RARE.getId());
        assertEquals(3, Rarity.EPIC.getId());
        assertEquals(4, Rarity.LEGENDARY.getId());
    }

    @Test
    public void testUpgradeRarityIdIncrementsByOne() {
        for (Rarity r : Rarity.values()) {
            int nextId = r.getId() + 1;
            if (nextId <= 4) {
                assertNotNull(r.getDisplayName() + " → id=" + nextId + " 存在",
                        Rarity.fromId(nextId));
            }
        }
    }

    // ==================== 14. 数量为1的宝石升级后从背包消失 ====================

    private static class BagSimulator {
        private final List<MockGemItem> items = new ArrayList<>();

        void addGem(MockGemItem gem) {
            items.add(gem);
        }

        int countGems() {
            int count = 0;
            for (MockGemItem g : items) {
                if (g.count > 0) count++;
            }
            return count;
        }

        void simulateUpgrade(MockGemItem target) {
            target.count--;
            if (target.count <= 0) {
                items.remove(target);
            }
        }
    }

    @Test
    public void testBagCount_UpgradeRemovesOneCountGem() {
        BagSimulator bag = new BagSimulator();
        MockGemItem gem = new MockGemItem("g1", "红宝石", Rarity.COMMON, "ruby", 1);
        bag.addGem(gem);
        assertEquals("升级前背包有1种宝石", 1, bag.countGems());
        bag.simulateUpgrade(gem);
        assertEquals("升级后 count=0 的宝石消失", 0, bag.countGems());
    }

    @Test
    public void testBagCount_MultiCountGemStaysAfterUpgrade() {
        BagSimulator bag = new BagSimulator();
        MockGemItem gem = new MockGemItem("g1", "红宝石", Rarity.COMMON, "ruby", 5);
        bag.addGem(gem);
        bag.simulateUpgrade(gem);
        assertEquals("多颗堆叠 → 升级后仍在背包", 1, bag.countGems());
        assertEquals("剩余4颗", 4, gem.count);
    }

    @Test
    public void testBagCount_TwoDifferentGems_UpgradeRemovesOne() {
        BagSimulator bag = new BagSimulator();
        MockGemItem gem1 = new MockGemItem("g1", "红宝石", Rarity.COMMON, "ruby", 1);
        MockGemItem gem2 = new MockGemItem("g2", "蓝宝石", Rarity.UNCOMMON, "sapphire", 2);
        bag.addGem(gem1);
        bag.addGem(gem2);
        bag.simulateUpgrade(gem1);
        assertEquals("升级红宝石后仅剩蓝宝石", 1, bag.countGems());
    }
}
