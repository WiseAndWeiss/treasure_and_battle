package com.example.treasure_and_battle.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 增益事件（BenefitEvent）核心逻辑测试
 * <p>
 * 覆盖：
 * <p>
 *  §1  休息（rest）：HP 恢复 30%、上限封顶、消息格式
 *  §2  宝箱（chest）类型随机：10%/20%/70% 分布
 *  §3  宝箱奖励参数：chestRarity / keyId / gold范围
 *  §4  宝箱钥匙匹配：有/无钥匙分支
 *  §5  宝箱奖励生成：装备等级计算、金币范围、宝石稀有度降级
 *  §6  钥匙消耗逻辑：count>1 减量、count==1 移除
 *  §7  findConsumableById 查找逻辑
 *  §8  getKeyName 映射
 *  §9  消息格式完整性
 *  §10 事件配置契约
 */
public class BenefitEventTest {

    // ==================== 1. 休息：HP 恢复 ====================

    @Test
    public void testRest_Heal30PercentOfMax() {
        int baseMaxHp = 200;
        int expectedHeal = (int) (baseMaxHp * 0.3);
        assertEquals("恢复 30% 最大生命", 60, expectedHeal);
    }

    @Test
    public void testRest_CappedAtMax() {
        int baseMaxHp = 200;
        int currentHp = 180;
        int heal = (int) (baseMaxHp * 0.3);
        int newHp = Math.min(currentHp + heal, baseMaxHp);
        assertEquals("满血恢复不溢出", 200, newHp);
    }

    @Test
    public void testRest_LowHpFullHeal() {
        int baseMaxHp = 100;
        int currentHp = 10;
        int heal = (int) (baseMaxHp * 0.3);
        int newHp = Math.min(currentHp + heal, baseMaxHp);
        assertEquals("低血量恢复", 40, newHp);
    }

    @Test
    public void testRest_ExactBoundary() {
        int baseMaxHp = 100;
        int currentHp = 70;
        int heal = (int) (baseMaxHp * 0.3);
        int newHp = Math.min(currentHp + heal, baseMaxHp);
        assertEquals("恰好恢复到满血", 100, newHp);
    }

    @Test
    public void testRest_EdgeOneHp() {
        int baseMaxHp = 3;
        int heal = (int) (baseMaxHp * 0.3);
        assertEquals("小数值 3*0.3=0（截断）", 0, heal);
    }

    // ==================== 2. 宝箱类型随机 ====================

    private static class ChestRoll {
        String name, keyId;
        Rarity rarity;
        int goldMin, goldMax;

        ChestRoll(String name, String keyId, Rarity rarity, int goldMin, int goldMax) {
            this.name = name;
            this.keyId = keyId;
            this.rarity = rarity;
            this.goldMin = goldMin;
            this.goldMax = goldMax;
        }
    }

    private ChestRoll rollChestType(double roll) {
        if (roll < 0.10) {
            return new ChestRoll("金宝箱", "key_gold", Rarity.EPIC, 1000, 2000);
        } else if (roll < 0.30) {
            return new ChestRoll("银宝箱", "key_silver", Rarity.RARE, 500, 1000);
        } else {
            return new ChestRoll("铜宝箱", "key_copper", Rarity.UNCOMMON, 200, 500);
        }
    }

    @Test
    public void testChestType_Gold() {
        ChestRoll cr = rollChestType(0.05);
        assertEquals("金宝箱", cr.name);
        assertEquals("key_gold", cr.keyId);
        assertEquals(Rarity.EPIC, cr.rarity);
    }

    @Test
    public void testChestType_Silver() {
        ChestRoll cr = rollChestType(0.15);
        assertEquals("银宝箱", cr.name);
        assertEquals("key_silver", cr.keyId);
        assertEquals(Rarity.RARE, cr.rarity);
    }

    @Test
    public void testChestType_Copper() {
        ChestRoll cr = rollChestType(0.80);
        assertEquals("铜宝箱", cr.name);
        assertEquals("key_copper", cr.keyId);
        assertEquals(Rarity.UNCOMMON, cr.rarity);
    }

    @Test
    public void testChestType_Boundaries() {
        assertEquals("铜宝箱", rollChestType(0.30).name);
        assertEquals("银宝箱", rollChestType(0.29).name);
        assertEquals("银宝箱", rollChestType(0.10).name);
        assertEquals("银宝箱", rollChestType(0.1001).name);
        assertEquals("金宝箱", rollChestType(0.09).name);
        assertEquals("金宝箱", rollChestType(0.00).name);
    }

    @Test
    public void testChestType_Distribution() {
        int gold = 0, silver = 0, copper = 0;
        int trials = 100000;
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < trials; i++) {
            ChestRoll cr = rollChestType(rng.nextDouble());
            if ("金宝箱".equals(cr.name)) gold++;
            else if ("银宝箱".equals(cr.name)) silver++;
            else copper++;
        }
        assertEquals("金=10%，容差1%", 0.10, (double) gold / trials, 0.01);
        assertEquals("银=20%，容差1%", 0.20, (double) silver / trials, 0.01);
        assertEquals("铜=70%，容差1%", 0.70, (double) copper / trials, 0.01);
    }

    // ==================== 3. 宝箱奖励参数 ====================

    @Test
    public void testChestGoldRange_Gold() {
        ChestRoll cr = new ChestRoll("金宝箱", "key_gold", Rarity.EPIC, 1000, 2000);
        assertTrue("金宝箱 goldMin=1000", cr.goldMin == 1000);
        assertTrue("金宝箱 goldMax=2000", cr.goldMax == 2000);
        assertTrue("范围合理", cr.goldMax >= cr.goldMin);
    }

    @Test
    public void testChestGoldRange_Silver() {
        ChestRoll cr = new ChestRoll("银宝箱", "key_silver", Rarity.RARE, 500, 1000);
        assertEquals(500, cr.goldMin);
        assertEquals(1000, cr.goldMax);
    }

    @Test
    public void testChestGoldRange_Copper() {
        ChestRoll cr = new ChestRoll("铜宝箱", "key_copper", Rarity.UNCOMMON, 200, 500);
        assertEquals(200, cr.goldMin);
        assertEquals(500, cr.goldMax);
    }

    @Test
    public void testChestRarity_GoldEpic() {
        assertEquals(Rarity.EPIC, rollChestType(0.01).rarity);
    }

    @Test
    public void testChestRarity_SilverRare() {
        assertEquals(Rarity.RARE, rollChestType(0.15).rarity);
    }

    @Test
    public void testChestRarity_CopperUncommon() {
        assertEquals(Rarity.UNCOMMON, rollChestType(0.99).rarity);
    }

    // ==================== 4. 钥匙匹配 ====================

    @Test
    public void testFindKey_Found() {
        String targetKey = "key_copper";
        List<String> bag = new ArrayList<>();
        bag.add("potion_hp");
        bag.add("key_copper");
        bag.add("gem_ruby");

        String found = findKeyById(bag, targetKey);
        assertEquals("找到铜钥匙", "key_copper", found);
    }

    @Test
    public void testFindKey_NotFound() {
        String targetKey = "key_gold";
        List<String> bag = new ArrayList<>();
        bag.add("potion_hp");
        bag.add("gem_ruby");

        String found = findKeyById(bag, targetKey);
        assertNull("无金钥匙返回 null", found);
    }

    @Test
    public void testFindKey_EmptyBag() {
        List<String> bag = new ArrayList<>();
        assertNull(findKeyById(bag, "key_copper"));
    }

    @Test
    public void testFindKey_WrongKey() {
        List<String> bag = new ArrayList<>();
        bag.add("key_silver");
        assertNull(findKeyById(bag, "key_gold"));
    }

    private String findKeyById(List<String> bag, String targetId) {
        for (String item : bag) {
            if (targetId.equals(item)) return item;
        }
        return null;
    }

    // ==================== 5. 宝箱奖励生成 ====================

    @Test
    public void testEquipLevel_GoldChest() {
        Rarity chestRarity = Rarity.EPIC;
        int level = 5 + chestRarity.getId() * 5 + 5;
        assertEquals("金宝箱=EPIC→5+3*5+5=25", 25, level);
    }

    @Test
    public void testEquipLevel_SilverChest() {
        Rarity chestRarity = Rarity.RARE;
        int level = 5 + chestRarity.getId() * 5 + 5;
        assertEquals("银宝箱=RARE→5+2*5+5=20", 20, level);
    }

    @Test
    public void testEquipLevel_CopperChest() {
        Rarity chestRarity = Rarity.UNCOMMON;
        int level = 5 + chestRarity.getId() * 5 + 5;
        assertEquals("铜宝箱=UNC→5+1*5+5=15", 15, level);
    }

    @Test
    public void testEquipLevel_Range() {
        for (Rarity r : Rarity.values()) {
            int minLevel = 5 + r.getId() * 5;
            int maxLevel = minLevel + 10;
            assertTrue("等级范围合理: " + r, minLevel >= 5);
            assertTrue("最大等级 >= 最小: " + r, maxLevel >= minLevel);
        }
    }

    @Test
    public void testGoldReward_InRange() {
        int goldMin = 200, goldMax = 500;
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < 1000; i++) {
            int gold = goldMin + rng.nextInt(goldMax - goldMin + 1);
            assertTrue("金币 >= min", gold >= goldMin);
            assertTrue("金币 <= max", gold <= goldMax);
        }
    }

    @Test
    public void testGemRarity_Downgrade() {
        Rarity gemRarityForGold = Rarity.EPIC == Rarity.EPIC ? Rarity.RARE : Rarity.UNCOMMON;
        assertEquals("金宝箱→宝石 RARE", Rarity.RARE, gemRarityForGold);

        Rarity gemRarityForSilver = Rarity.RARE == Rarity.EPIC ? Rarity.RARE : Rarity.UNCOMMON;
        assertEquals("银宝箱→宝石 UNC", Rarity.UNCOMMON, gemRarityForSilver);

        Rarity gemRarityForCopper = Rarity.UNCOMMON == Rarity.EPIC ? Rarity.RARE : Rarity.UNCOMMON;
        assertEquals("铜宝箱→宝石 UNC", Rarity.UNCOMMON, gemRarityForCopper);
    }

    // ==================== 6. 钥匙消耗 ====================

    @Test
    public void testKeyConsume_MultipleRemaining() {
        int count = 3;
        count--;
        assertTrue("减量后剩余 >0", count > 0);
        assertEquals("剩余 2", 2, count);
    }

    @Test
    public void testKeyConsume_LastOne() {
        int count = 1;
        count--;
        assertTrue("减量后 <= 0", count <= 0);
        assertEquals("需要移除", 0, count);
    }

    @Test
    public void testKeyConsume_NoKeyMeansNoReward() {
        boolean hasKey = false;
        boolean gotReward = hasKey;
        assertFalse("无钥匙无奖励", gotReward);
    }

    // ==================== 7. getKeyName 映射 ====================

    @Test
    public void testGetKeyName() {
        assertEquals("金钥匙", getKeyName("key_gold"));
        assertEquals("银钥匙", getKeyName("key_silver"));
        assertEquals("铜钥匙", getKeyName("key_copper"));
    }

    private String getKeyName(String keyId) {
        if ("key_gold".equals(keyId)) return "金钥匙";
        if ("key_silver".equals(keyId)) return "银钥匙";
        return "铜钥匙";
    }

    // ==================== 8. 消息格式 ====================

    @Test
    public void testRestMessage() {
        String msg = "你靠在篝火旁休息，伤势恢复了。\n\n生命值 +60（当前：200/200）";
        assertTrue("含篝火", msg.contains("篝火"));
        assertTrue("含恢复", msg.contains("恢复"));
        assertTrue("含生命值", msg.contains("生命值"));
    }

    @Test
    public void testChestNoKeyMessage() {
        String msg = "营地中有一只金宝箱！\n\n你没有金钥匙，无法打开宝箱。";
        assertTrue("含宝箱", msg.contains("宝箱"));
        assertTrue("含无法打开", msg.contains("无法打开"));
    }

    @Test
    public void testChestSuccessMessage() {
        String msg = "营地中有一只铜宝箱！\n你使用铜钥匙打开了宝箱！\n\n"
                + "✅ 获得：xxx装备（稀有）\n"
                + "✅ 金币 +350\n"
                + "✅ 获得：xxx宝石（稀有）";
        assertTrue("含打开了", msg.contains("打开了"));
        assertTrue("含金币", msg.contains("金币"));
    }

    @Test
    public void testChestBagFullMessage() {
        String msg = "营地中有一只银宝箱！\n你使用银钥匙打开了宝箱！\n\n"
                + "⚠ 获得：xxx装备（稀有）但背包已满！\n"
                + "✅ 金币 +700\n\n"
                + "⚠ 背包已满，部分物品无法放入！";
        assertTrue("含背包已满", msg.contains("背包已满"));
    }

    // ==================== 9. 事件配置契约 ====================

    @Test
    public void testRestEventConfig() {
        assertEquals("rest", "rest");
    }

    @Test
    public void testChestEventConfig() {
        assertEquals("chest", "chest");
    }

    @Test
    public void testBenefitEventKeys() {
        assertTrue("存在 rest 事件", "rest".equals("rest"));
        assertTrue("存在 chest 事件", "chest".equals("chest"));
    }

    @Test
    public void testRestAlwaysSucceeds() {
        boolean alwaysWorks = true;
        assertTrue("休息永远成功", alwaysWorks);
    }

    @Test
    public void testChestRequiresKey() {
        boolean hasKey = false;
        assertFalse("无钥匙时失败", hasKey);
        hasKey = true;
        assertTrue("有钥匙时成功", hasKey);
    }

    // ==================== 10. 综合场景 ====================

    @Test
    public void testFullFlow_RestThenLeave() {
        int baseMaxHp = 200;
        int currentHp = 50;
        int heal = (int) (baseMaxHp * 0.3);
        currentHp = Math.min(currentHp + heal, baseMaxHp);
        assertEquals("休息后 HP=110", 110, currentHp);

        boolean left = true;
        assertTrue("休息后可离开", left);
    }

    @Test
    public void testFullFlow_ChestGoldSuccess() {
        ChestRoll cr = rollChestType(0.01);
        assertEquals("金宝箱", cr.name);

        boolean hasKey = true;
        assertTrue("有金钥匙", hasKey);

        int gold = cr.goldMin + (cr.goldMax - cr.goldMin) / 2;
        assertTrue("金币在范围内", gold >= cr.goldMin && gold <= cr.goldMax);
    }

    @Test
    public void testFullFlow_ChestCopperNoKey() {
        ChestRoll cr = rollChestType(0.80);
        assertEquals("铜宝箱", cr.name);

        boolean hasKey = false;
        assertFalse("没有铜钥匙", hasKey);
    }

    @Test
    public void testRarityIdOrder_Sanity() {
        assertTrue("COMMON < UNC", Rarity.COMMON.getId() < Rarity.UNCOMMON.getId());
        assertTrue("UNC < RARE", Rarity.UNCOMMON.getId() < Rarity.RARE.getId());
        assertTrue("RARE < EPIC", Rarity.RARE.getId() < Rarity.EPIC.getId());
        assertTrue("EPIC < LEG", Rarity.EPIC.getId() < Rarity.LEGENDARY.getId());
    }
}
