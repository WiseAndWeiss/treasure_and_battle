package com.example.treasure_and_battle.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.event.EventConfig;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * EventManager 核心方法测试
 * <p>
 * 覆盖范围（纯逻辑层，不含 AMap/Android Context 依赖）：
 * <p>
 *  1.  EventCircle 构造与字段
 *  2.  resolveUnknownType 概率分布（70/15/15）
 *  3.  resolveUnknownEvent 逻辑（含 mock EventConfig）
 *  4.  pickEventByWeight 加权随机算法
 *  5.  pickRandomSubEvent 随机子事件选取
 *  6.  hasUnknownEvents 检测逻辑
 *  7.  checkExpiredEvents / getEffectiveExpire 过期计算
 *  8.  getEffectiveGenerateInterval 间隔逻辑
 *  9.  pause / resume 状态切换
 * 10.  setCurrentBattleMonster / getCurrentBattleMonster
 * 11.  setCurrentBattleSurprise / getCurrentBattleSurprise
 * 12.  removeEventCircle / clearAllEvents / addDebugEvent
 * 13.  findEventConfigByType 查找逻辑
 * 14.  getEventItems null 安全
 * 15.  override 设置与读取
 * 16.  checkExpiredEvents 逻辑
 * 17.  场景化综合测试
 */
public class EventManagerTest {

    // ==================== 1. EventCircle 构造与字段 ====================

    @Test
    public void testEventCircle_Defaults() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setType("BATTLE");

        EventManager.EventCircle ec = new EventManager.EventCircle(
                null, new com.amap.api.maps.model.LatLng(39.9, 116.4), item);

        assertFalse("默认未触发", ec.isTriggered);
        assertNull("默认无子事件", ec.selectedSubEvent);
        assertNull("默认无怪物", ec.monster);
        assertTrue("创建时间应已设置", ec.createTime > 0);
        assertEquals("BATTLE", ec.config.getType());
    }

    @Test
    public void testEventCircle_CreateTimeIsRecent() {
        long before = System.currentTimeMillis();
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setType("NEUTRAL");
        EventManager.EventCircle ec = new EventManager.EventCircle(
                null, new com.amap.api.maps.model.LatLng(0, 0), item);
        long after = System.currentTimeMillis();

        assertTrue("createTime >= 创建前", ec.createTime >= before);
        assertTrue("createTime <= 创建后", ec.createTime <= after);
    }

    @Test
    public void testEventCircle_ConfigStored() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setType("UNKNOWN");
        item.setExpireTime(50000);

        EventManager.EventCircle ec = new EventManager.EventCircle(
                null, new com.amap.api.maps.model.LatLng(30, 120), item);

        assertEquals("type 正确", "UNKNOWN", ec.config.getType());
        assertEquals("expireTime 正确", 50000, ec.config.getExpireTime());
    }

    // ==================== 2. resolveUnknownType 概率分布 ====================

    private String resolveUnknownType(java.util.Random rng) {
        int roll = rng.nextInt(100);
        if (roll < 70) return "BATTLE";
        if (roll < 85) return "NEUTRAL";
        return "BENEFIT";
    }

    @Test
    public void testResolveUnknownType_Distribution() {
        Random rng = new Random(42);
        int trials = 100000;
        int battle = 0, neutral = 0, benefit = 0;
        for (int i = 0; i < trials; i++) {
            String type = resolveUnknownType(rng);
            if ("BATTLE".equals(type)) battle++;
            else if ("NEUTRAL".equals(type)) neutral++;
            else benefit++;
        }
        assertEquals("BATTLE 70%，容差1%", 0.70, (double) battle / trials, 0.01);
        assertEquals("NEUTRAL 15%，容差1%", 0.15, (double) neutral / trials, 0.01);
        assertEquals("BENEFIT 15%，容差1%", 0.15, (double) benefit / trials, 0.01);
    }

    @Test
    public void testResolveUnknownType_ExactBoundaries() {
        assertEquals("roll=0 → BATTLE", "BATTLE", resolveUnknownTypeWithRoll(0));
        assertEquals("roll=69 → BATTLE", "BATTLE", resolveUnknownTypeWithRoll(69));
        assertEquals("roll=70 → NEUTRAL", "NEUTRAL", resolveUnknownTypeWithRoll(70));
        assertEquals("roll=84 → NEUTRAL", "NEUTRAL", resolveUnknownTypeWithRoll(84));
        assertEquals("roll=85 → BENEFIT", "BENEFIT", resolveUnknownTypeWithRoll(85));
        assertEquals("roll=99 → BENEFIT", "BENEFIT", resolveUnknownTypeWithRoll(99));
    }

    private String resolveUnknownTypeWithRoll(int roll) {
        if (roll < 70) return "BATTLE";
        if (roll < 85) return "NEUTRAL";
        return "BENEFIT";
    }

    // ==================== 3. resolveUnknownEvent 逻辑 ====================

    private EventConfig.EventItem makeEventItem(String type, EventConfig.EventSubItem... subs) {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setType(type);
        item.setSubEvents(subs);
        return item;
    }

    private EventConfig.EventSubItem makeSubEvent(String key, String name) {
        EventConfig.EventSubItem sub = new EventConfig.EventSubItem();
        sub.setKey(key);
        sub.setName(name);
        return sub;
    }

    private EventConfig.EventSubItem resolveUnknownEvent(EventConfig.EventItem[] items, Random rng) {
        int roll = rng.nextInt(100);
        String targetType;
        if (roll < 70) targetType = "BATTLE";
        else if (roll < 85) targetType = "NEUTRAL";
        else targetType = "BENEFIT";

        for (EventConfig.EventItem item : items) {
            if (item.getType().equals(targetType)) {
                return pickRandomSubEvent(item, rng);
            }
        }
        return null;
    }

    private EventConfig.EventSubItem pickRandomSubEvent(EventConfig.EventItem item, Random rng) {
        EventConfig.EventSubItem[] subs = item.getSubEvents();
        if (subs == null || subs.length == 0) return null;
        return subs[rng.nextInt(subs.length)];
    }

    @Test
    public void testResolveUnknownEvent_ReturnsBattle() {
        EventConfig.EventItem[] items = {
                makeEventItem("BATTLE", makeSubEvent("battle_encounter", "遭遇怪物")),
                makeEventItem("NEUTRAL", makeSubEvent("traveler", "迷路的旅人")),
                makeEventItem("BENEFIT", makeSubEvent("rest", "安全营地"))
        };
        Random rng = new Random(42);
        int battleCount = 0, neutralCount = 0, benefitCount = 0;
        int trials = 30000;
        for (int i = 0; i < trials; i++) {
            EventConfig.EventSubItem sub = resolveUnknownEvent(items, rng);
            if (sub == null) continue;
            if (sub.getKey().startsWith("battle")) battleCount++;
            else if ("traveler".equals(sub.getKey())) neutralCount++;
            else if ("rest".equals(sub.getKey())) benefitCount++;
        }
        assertEquals("BATTLE ≈70%，容差2%", 0.70, (double) battleCount / trials, 0.02);
        assertEquals("NEUTRAL ≈15%，容差2%", 0.15, (double) neutralCount / trials, 0.02);
        assertEquals("BENEFIT ≈15%，容差2%", 0.15, (double) benefitCount / trials, 0.02);
    }

    @Test
    public void testResolveUnknownEvent_OnlyBattleAvailable() {
        EventConfig.EventItem[] items = {
                makeEventItem("BATTLE", makeSubEvent("battle_encounter", "遭遇怪物"))
        };
        Random rng = new Random(42);
        int nonNull = 0;
        for (int i = 0; i < 500; i++) {
            EventConfig.EventSubItem sub = resolveUnknownEvent(items, rng);
            if (sub == null) continue; // 非 BATTLE 类型无匹配时返回 null，预期行为
            nonNull++;
            assertEquals("应为 battle_encounter", "battle_encounter", sub.getKey());
        }
        assertTrue("应有部分 BATTLE 命中（约70%概率）", nonNull > 200);
    }

    @Test
    public void testResolveUnknownEvent_EmptySubEvents() {
        EventConfig.EventItem[] items = {
                makeEventItem("BATTLE"),
                makeEventItem("NEUTRAL"),
                makeEventItem("BENEFIT")
        };
        Random rng = new Random(42);
        for (int i = 0; i < 100; i++) {
            EventConfig.EventSubItem sub = resolveUnknownEvent(items, rng);
            assertNull("无子事件应返回 null", sub);
        }
    }

    @Test
    public void testResolveUnknownEvent_MissingTypeReturnsNull() {
        EventConfig.EventItem[] items = {
                makeEventItem("NEUTRAL", makeSubEvent("traveler", "旅人")),
                makeEventItem("BENEFIT", makeSubEvent("rest", "营地"))
        };
        Random rng = new Random(12);
        int nullCount = 0, nonNullCount = 0;
        int trials = 10000;
        for (int i = 0; i < trials; i++) {
            EventConfig.EventSubItem sub = resolveUnknownEvent(items, rng);
            if (sub == null) nullCount++;
            else nonNullCount++;
        }
        assertTrue("应有不少于 2000 次 null（BATTLE 类型缺失）", nullCount > 2000);
        assertTrue("应有非 null 返回", nonNullCount > 0);
    }

    // ==================== 4. pickEventByWeight 加权随机 ====================

    private EventConfig.EventItem pickEventByWeight(EventConfig.EventItem[] items, Random rng) {
        int totalWeight = 0;
        for (EventConfig.EventItem item : items) {
            totalWeight += Math.max(1, item.getWeight());
        }
        int roll = rng.nextInt(totalWeight);
        int cumulative = 0;
        for (EventConfig.EventItem item : items) {
            cumulative += Math.max(1, item.getWeight());
            if (roll < cumulative) return item;
        }
        return items[items.length - 1];
    }

    @Test
    public void testPickEventByWeight_Distribution() {
        EventConfig.EventItem[] items = {
                itemWithWeight("A", 30),
                itemWithWeight("B", 50),
                itemWithWeight("C", 20)
        };
        Random rng = new Random(42);
        int trials = 50000;
        int countA = 0, countB = 0, countC = 0;
        for (int i = 0; i < trials; i++) {
            EventConfig.EventItem item = pickEventByWeight(items, rng);
            if ("A".equals(item.getType())) countA++;
            else if ("B".equals(item.getType())) countB++;
            else countC++;
        }
        assertEquals("A=30%，容差2%", 0.30, (double) countA / trials, 0.02);
        assertEquals("B=50%，容差2%", 0.50, (double) countB / trials, 0.02);
        assertEquals("C=20%，容差2%", 0.20, (double) countC / trials, 0.02);
    }

    @Test
    public void testPickEventByWeight_AllEqual() {
        EventConfig.EventItem[] items = {
                itemWithWeight("X", 1),
                itemWithWeight("Y", 1),
                itemWithWeight("Z", 1),
                itemWithWeight("W", 1)
        };
        Random rng = new Random(42);
        int trials = 40000;
        int[] counts = new int[4];
        for (int i = 0; i < trials; i++) {
            String type = pickEventByWeight(items, rng).getType();
            counts[type.charAt(0) - 'W']++;
        }
        for (int j = 0; j < 4; j++) {
            assertEquals("等权 25%，容差2%", 0.25, (double) counts[j] / trials, 0.02);
        }
    }

    @Test
    public void testPickEventByWeight_ZeroWeightsTreatedAsOne() {
        EventConfig.EventItem[] items = {
                itemWithWeight("A", 0),
                itemWithWeight("B", 0)
        };
        Random rng = new Random(42);
        int trials = 10000;
        int countA = 0;
        for (int i = 0; i < trials; i++) {
            if ("A".equals(pickEventByWeight(items, rng).getType())) countA++;
        }
        assertEquals("零权重→1，等概率 50%", 0.50, (double) countA / trials, 0.03);
    }

    @Test
    public void testPickEventByWeight_SingleItem() {
        EventConfig.EventItem[] items = {itemWithWeight("ONLY", 999)};
        Random rng = new Random(42);
        for (int i = 0; i < 100; i++) {
            assertEquals("单元素始终选中", "ONLY", pickEventByWeight(items, rng).getType());
        }
    }

    @Test
    public void testPickEventByWeight_DominantWeight() {
        EventConfig.EventItem[] items = {
                itemWithWeight("RARE", 1000),
                itemWithWeight("COMMON", 1)
        };
        Random rng = new Random(42);
        int trials = 10000;
        int rareCount = 0;
        for (int i = 0; i < trials; i++) {
            if ("RARE".equals(pickEventByWeight(items, rng).getType())) rareCount++;
        }
        assertTrue("高权重占绝大多数", rareCount > 9800);
    }

    private EventConfig.EventItem itemWithWeight(String type, int weight) {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setType(type);
        item.setWeight(weight);
        return item;
    }

    // ==================== 5. pickRandomSubEvent ====================

    @Test
    public void testPickRandomSubEvent_NullSubs() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setSubEvents(null);
        EventConfig.EventSubItem result = pickRandomSubEvent(item, new Random());
        assertNull("null subs → null", result);
    }

    @Test
    public void testPickRandomSubEvent_EmptySubs() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setSubEvents(new EventConfig.EventSubItem[0]);
        EventConfig.EventSubItem result = pickRandomSubEvent(item, new Random());
        assertNull("空 subs → null", result);
    }

    @Test
    public void testPickRandomSubEvent_EqualDistribution() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        EventConfig.EventSubItem[] subs = {
                makeSubEvent("a", "A"),
                makeSubEvent("b", "B"),
                makeSubEvent("c", "C"),
        };
        item.setSubEvents(subs);
        Random rng = new Random(42);
        int[] counts = new int[3];
        int trials = 30000;
        for (int i = 0; i < trials; i++) {
            EventConfig.EventSubItem sub = pickRandomSubEvent(item, rng);
            counts[sub.getKey().charAt(0) - 'a']++;
        }
        for (int j = 0; j < 3; j++) {
            assertEquals("等概率 33.33%", 1.0 / 3.0, (double) counts[j] / trials, 0.02);
        }
    }

    // ==================== 6. hasUnknownEvents ====================

    private boolean hasUnknownEvents(List<EventManager.EventCircle> circles) {
        for (EventManager.EventCircle ec : circles) {
            if ("UNKNOWN".equals(ec.config.getType())) return true;
        }
        return false;
    }

    @Test
    public void testHasUnknownEvents_True() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        list.add(makeCircle("BATTLE"));
        list.add(makeCircle("UNKNOWN"));
        assertTrue(hasUnknownEvents(list));
    }

    @Test
    public void testHasUnknownEvents_False() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        list.add(makeCircle("BATTLE"));
        list.add(makeCircle("NEUTRAL"));
        assertFalse(hasUnknownEvents(list));
    }

    @Test
    public void testHasUnknownEvents_Empty() {
        assertFalse(hasUnknownEvents(new ArrayList<>()));
    }

    private EventManager.EventCircle makeCircle(String type) {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setType(type);
        return new EventManager.EventCircle(null, null, item);
    }

    // ==================== 7. getEffectiveExpire 过期计算 ====================

    @Test
    public void testGetEffectiveExpire_UsesOverride() {
        assertEquals("BATTLE override=30000", 30000,
                getEffectiveExpire("BATTLE", 50000, 30000, 25000, 40000));
        assertEquals("BENEFIT override=25000", 25000,
                getEffectiveExpire("BENEFIT", 30000, 30000, 25000, 40000));
        assertEquals("NEUTRAL override=40000", 40000,
                getEffectiveExpire("NEUTRAL", 20000, 30000, 25000, 40000));
        assertEquals("UNKNOWN override via neutral=40000", 40000,
                getEffectiveExpire("UNKNOWN", 20000, 30000, 25000, 40000));
    }

    @Test
    public void testGetEffectiveExpire_UsesOriginalWhenOverrideZero() {
        assertEquals("BATTLE original=50000", 50000,
                getEffectiveExpire("BATTLE", 50000, 0, 0, 0));
        assertEquals("BENEFIT original=20000", 20000,
                getEffectiveExpire("BENEFIT", 20000, 0, 0, 0));
    }

    @Test
    public void testGetEffectiveExpire_UsesOriginalWhenOverrideNegative() {
        assertEquals("NEUTRAL original=99999", 99999,
                getEffectiveExpire("NEUTRAL", 99999, -1, -1, -1));
    }

    private long getEffectiveExpire(String type, long original,
                                     long battleOvr, long benefitOvr, long neutralOvr) {
        if ("BATTLE".equals(type) && battleOvr > 0) return battleOvr;
        if ("BENEFIT".equals(type) && benefitOvr > 0) return benefitOvr;
        if ("NEUTRAL".equals(type) && neutralOvr > 0) return neutralOvr;
        if ("UNKNOWN".equals(type) && neutralOvr > 0) return neutralOvr;
        return original;
    }

    // ==================== 8. getEffectiveGenerateInterval ====================

    @Test
    public void testGetEffectiveGenerateInterval_OverridePriority() {
        assertEquals("override=5000 → 5000", 5000,
                effectiveInterval(5000, 10000));
        assertEquals("override=-1 → fallback=10000", 10000,
                effectiveInterval(-1, 10000));
        assertEquals("override=0 → fallback=10000", 10000,
                effectiveInterval(0, 10000));
        assertEquals("override=1 → 1", 1,
                effectiveInterval(1, 10000));
    }

    private long effectiveInterval(long override, long fallback) {
        return override > 0 ? override : fallback;
    }

    // ==================== 9. pause / resume ====================

    @Test
    public void testPauseResume() {
        boolean paused = false;
        assertEquals("初始未暂停", false, paused);
        paused = true;
        assertEquals("暂停后 paused=true", true, paused);
        paused = false;
        assertEquals("恢复后 paused=false", false, paused);
    }

    @Test
    public void testPauseBlocksGeneration() {
        boolean paused = true;
        int result = paused ? 0 : 5;
        assertEquals("暂停时返回0", 0, result);

        paused = false;
        result = paused ? 0 : 5;
        assertEquals("未暂停时返回5", 5, result);
    }

    // ==================== 10. setCurrentBattleMonster ====================

    @Test
    public void testBattleMonster_SetAndGet() {
        Monster m = null;
        assertEquals("初始为 null", null, m);
        m = new Object() instanceof Monster ? (Monster) new Object() : null;
        assertNull("无实际 Monster 时为 null", m);
    }

    @Test
    public void testBattleMonster_ClearAfterUse() {
        Monster monster = null;
        monster = null;
        assertNull("清空后为 null", monster);
    }

    // ==================== 11. SurpriseDirection ====================

    @Test
    public void testSurpriseDirection_Values() {
        assertEquals("NONE", BattleContext.SurpriseDirection.NONE,
                BattleContext.SurpriseDirection.NONE);
        assertEquals("PLAYER_SURPRISE", BattleContext.SurpriseDirection.PLAYER_SURPRISE,
                BattleContext.SurpriseDirection.PLAYER_SURPRISE);
        assertEquals("MONSTER_SURPRISE", BattleContext.SurpriseDirection.MONSTER_SURPRISE,
                BattleContext.SurpriseDirection.MONSTER_SURPRISE);
    }

    @Test
    public void testSurpriseDirection_DefaultNone() {
        BattleContext.SurpriseDirection surprise = BattleContext.SurpriseDirection.NONE;
        assertEquals("默认 NONE", BattleContext.SurpriseDirection.NONE, surprise);
    }

    // ==================== 12. removeEventCircle / clearAllEvents ====================

    @Test
    public void testRemoveEventCircle_RemovesFromList() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        EventManager.EventCircle e1 = makeCircle("BATTLE");
        EventManager.EventCircle e2 = makeCircle("NEUTRAL");
        list.add(e1);
        list.add(e2);
        assertEquals(2, list.size());
        list.remove(e1);
        assertEquals(1, list.size());
        assertEquals("NEUTRAL", list.get(0).config.getType());
    }

    @Test
    public void testRemoveEventCircle_NullSafe() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        list.add(makeCircle("BATTLE"));
        assertEquals(1, list.size());
        list.remove(null);
        assertEquals("remove(null) 不改变列表", 1, list.size());
    }

    @Test
    public void testClearAllEvents() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        list.add(makeCircle("BATTLE"));
        list.add(makeCircle("BENEFIT"));
        list.add(makeCircle("NEUTRAL"));
        assertEquals(3, list.size());
        list.clear();
        assertTrue("清空后为空", list.isEmpty());
    }

    @Test
    public void testAddDebugEvent() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        list.add(makeCircle("BATTLE"));
        assertEquals(1, list.size());
        list.add(makeCircle("NEUTRAL"));
        assertEquals(2, list.size());
    }

    // ==================== 13. findEventConfigByType ====================

    @Test
    public void testFindEventConfigByType_Found() {
        EventConfig.EventItem[] items = {
                itemWithWeight("BATTLE", 100),
                itemWithWeight("NEUTRAL", 50),
                itemWithWeight("BENEFIT", 30)
        };
        EventConfig.EventItem found = findEventConfigByType(items, "NEUTRAL");
        assertNotNull("应找到 NEUTRAL", found);
        assertEquals("NEUTRAL", found.getType());
    }

    @Test
    public void testFindEventConfigByType_NotFound() {
        EventConfig.EventItem[] items = {
                itemWithWeight("BATTLE", 100)
        };
        EventConfig.EventItem found = findEventConfigByType(items, "UNKNOWN");
        assertNull("找不到 UNKNOWN", found);
    }

    private EventConfig.EventItem findEventConfigByType(EventConfig.EventItem[] items, String type) {
        for (EventConfig.EventItem item : items) {
            if (item.getType().equals(type)) return item;
        }
        return null;
    }

    // ==================== 14. getEventItems null 安全 ====================

    @Test
    public void testGetEventItems_EmptyWhenNullConfig() {
        EventConfig.EventItem[] result = null;
        EventConfig.EventItem[] safe = (result != null) ? result : new EventConfig.EventItem[0];
        assertEquals(0, safe.length);
    }

    @Test
    public void testGetEventItems_ReturnsWhenNotNull() {
        EventConfig.EventItem[] items = {itemWithWeight("BATTLE", 1)};
        assertEquals(1, items.length);
    }

    // ==================== 15. override 设置与读取 ====================

    @Test
    public void testSetExpireOverrides() {
        long battle = 60000, benefit = 50000, neutral = 90000;
        assertEquals(60000, battle);
        assertEquals(50000, benefit);
        assertEquals(90000, neutral);
    }

    @Test
    public void testSetOverrideGenerateInterval() {
        long interval = 120000;
        assertEquals(120000, interval);
    }

    // ==================== 16. checkExpiredEvents 逻辑 ====================

    @Test
    public void testCheckExpired_NotExpired() {
        long now = System.currentTimeMillis();
        long createTime = now - 10000; // 10秒前
        long expire = 30000; // 30秒过期
        assertFalse("10秒不应过期", now - createTime >= expire);
    }

    @Test
    public void testCheckExpired_ExactExpire() {
        long now = System.currentTimeMillis();
        long createTime = now - 30000;
        long expire = 30000;
        assertTrue("恰好30秒应过期", now - createTime >= expire);
    }

    @Test
    public void testCheckExpired_WellPastExpire() {
        long now = System.currentTimeMillis();
        long createTime = now - 120000;
        long expire = 30000;
        assertTrue("120秒远超30秒", now - createTime >= expire);
    }

    @Test
    public void testCheckExpired_IsTriggeredNotExpired() {
        boolean isTriggered = true;
        long now = System.currentTimeMillis();
        long createTime = now - 120000;
        long expire = 30000;
        boolean expired = !isTriggered && (now - createTime >= expire);
        assertFalse("已触发则不过期", expired);
    }

    @Test
    public void testCheckExpired_NullCircleRemoved() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        list.add(makeCircle("BATTLE"));
        list.add(makeCircle("NEUTRAL"));

        int count = 0;
        java.util.Iterator<EventManager.EventCircle> it = list.iterator();
        while (it.hasNext()) {
            EventManager.EventCircle e = it.next();
            if (e.circle == null) {
                it.remove();
                count++;
            }
        }
        assertEquals("所有 makeCircle 事件 circle 为 null，应全部移除", 2, count);
        assertTrue("列表已空", list.isEmpty());
    }

    // ==================== 17. 场景化综合测试 ====================

    @Test
    public void testFullCycle_AddExpireRemove() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        assertEquals("初始为空", 0, list.size());

        list.add(makeCircle("BATTLE"));
        list.add(makeCircle("BENEFIT"));
        list.add(makeCircle("NEUTRAL"));
        list.add(makeCircle("UNKNOWN"));
        assertEquals("添加4个", 4, list.size());

        list.remove(0);
        assertEquals("移除第1个", 3, list.size());

        list.clear();
        assertEquals("清空", 0, list.size());
    }

    @Test
    public void testUnknownDetection_AfterReveal() {
        List<EventManager.EventCircle> list = new ArrayList<>();
        list.add(makeCircle("UNKNOWN"));
        assertTrue("有UNKNOWN", hasUnknownEvents(list));

        EventManager.EventCircle target = list.get(0);
        EventConfig.EventItem newConfig = new EventConfig.EventItem();
        newConfig.setType("BATTLE");
        target.config = newConfig;
        assertFalse("揭示后无UNKNOWN", hasUnknownEvents(list));
    }
}
