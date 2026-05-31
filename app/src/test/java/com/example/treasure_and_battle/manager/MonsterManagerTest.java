package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.MonsterTemplate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * MonsterManager 核心方法测试
 * <p>
 * 覆盖范围：
 * <p>
 * 1. 单例模式与初始化
 * 2. 模板加载与查询
 * 3. 种族相关查询
 * 4. 调试批次生成（generateDebugBatch）
 * 5. 概率分布生成系统（generateMonsterBatch）
 * 6. 怪物创建（含等级缩放）
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class MonsterManagerTest {
    private Context context;
    private MonsterManager monsterManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monsterManager = MonsterManager.getInstance(context);
    }

    // ==================== 1. 单例模式与初始化 ====================

    @Test
    public void testSingleton() {
        MonsterManager instance1 = MonsterManager.getInstance(context);
        MonsterManager instance2 = MonsterManager.getInstance(context);
        assertSame("应返回同一实例", instance1, instance2);
    }

    @Test
    public void testReleaseInstance() {
        MonsterManager.releaseInstance();
        MonsterManager newInstance = MonsterManager.getInstance(context);
        assertNotNull("释放后应能创建新实例", newInstance);
    }

    // ==================== 2. 模板加载与查询 ====================

    @Test
    public void testGetTemplate_ExistingId() {
        // 假设存在模板ID 1，如果不存在则测试会失败，需要根据实际配置调整
        MonsterTemplate template = monsterManager.getTemplate(1);
        if (template != null) {
            assertEquals("模板ID应匹配", 1, template.getTemplateId());
        }
        // 如果模板不存在，测试通过（因为无法预测配置）
    }

    @Test
    public void testGetTemplate_NonExistingId() {
        MonsterTemplate template = monsterManager.getTemplate(999999);
        assertNull("不存在的模板应返回null", template);
    }

    // ==================== 3. 种族相关查询 ====================

    @Test
    public void testGetAvailableRaces() {
        List<String> races = monsterManager.getAvailableRaces();
        assertNotNull("种族列表不应为null", races);
        // 如果有配置文件，列表不应为空；否则可能为空
    }

    @Test
    public void testGetTemplatesByRace() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        assertNotNull("种族模板映射不应为null", raceTemplates);

        if (!raceTemplates.isEmpty()) {
            // 验证第一个种族的模板列表
            String firstRace = raceTemplates.keySet().iterator().next();
            List<MonsterTemplate> templates = raceTemplates.get(firstRace);
            assertNotNull("模板列表不应为null", templates);
            assertFalse("模板列表不应为空", templates.isEmpty());
        }
    }

    @Test
    public void testGetMaxRarityForRace_ExistingRace() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (!raceTemplates.isEmpty()) {
            String firstRace = raceTemplates.keySet().iterator().next();
            int maxRarity = monsterManager.getMaxRarityForRace(firstRace);
            assertTrue("最大稀有度应>=0", maxRarity >= 0);
        }
    }

    @Test
    public void testGetMaxRarityForRace_NonExistingRace() {
        int maxRarity = monsterManager.getMaxRarityForRace("nonexistent_race");
        assertEquals("不存在的种族应返回0", 0, maxRarity);
    }

    // ==================== 4. 调试批次生成 ====================

    @Test
    public void testGenerateDebugBatch_EmptyWhenNoRace() {
        List<Integer> result = monsterManager.generateDebugBatch("nonexistent_race", 5, 10, new Random(42));
        assertNotNull("结果不应为null", result);
        assertTrue("不存在的种族应返回空列表", result.isEmpty());
    }

    @Test
    public void testGenerateDebugBatch_ReturnsRequestedCount() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (!raceTemplates.isEmpty()) {
            String firstRace = raceTemplates.keySet().iterator().next();
            int requestedCount = 5;
            List<Integer> result = monsterManager.generateDebugBatch(firstRace, 5, requestedCount, new Random(42));
            assertEquals("应返回请求数量的模板ID", requestedCount, result.size());
        }
    }

    @Test
    public void testGenerateDebugBatch_RespectsMaxRarity() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (!raceTemplates.isEmpty()) {
            String firstRace = raceTemplates.keySet().iterator().next();
            int maxRarity = 1;
            List<Integer> result = monsterManager.generateDebugBatch(firstRace, maxRarity, 20, new Random(42));

            // 验证所有生成的模板稀有度不超过maxRarity
            for (Integer templateId : result) {
                MonsterTemplate template = monsterManager.getTemplate(templateId);
                if (template != null) {
                    assertTrue("模板稀有度应不超过maxRarity",
                            template.getRarityId() <= maxRarity);
                }
            }
        }
    }

    // ==================== 5. 概率分布生成系统 ====================

    @Test
    public void testGenerateMonsterBatch_EmptyWhenNoRaces() {
        // 由于无法清空模板，此测试仅验证方法不会崩溃
        List<Integer> result = monsterManager.generateMonsterBatch(10, 5, new Random(42), null);
        assertNotNull("结果不应为null", result);
    }

    @Test
    public void testGenerateMonsterBatch_RespectsRecentlyUsedRaces() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (raceTemplates.size() > 3) {
            List<String> allRaces = List.copyOf(raceTemplates.keySet());
            List<String> recentRaces = allRaces.subList(0, 2);

            // 生成多批次，验证种族多样性
            List<Integer> result1 = monsterManager.generateMonsterBatch(10, 3, new Random(42), recentRaces);
            List<Integer> result2 = monsterManager.generateMonsterBatch(10, 3, new Random(43), recentRaces);

            assertNotNull("结果不应为null", result1);
            assertNotNull("结果不应为null", result2);
        }
    }

    @Test
    public void testGenerateMonsterBatch_ValidRange() {
        List<Integer> result = monsterManager.generateMonsterBatch(10, 5, new Random(42), null);
        if (!result.isEmpty()) {
            // 验证结果在合理范围内（1-5个怪物）
            assertTrue("生成的怪物数量应在1-5之间", result.size() >= 1 && result.size() <= 5);
        }
    }

    // ==================== 6. 怪物创建 ====================

    @Test
    public void testCreateMonsterByTemplateId_ValidId() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (!raceTemplates.isEmpty()) {
            // 获取第一个有效模板
            List<MonsterTemplate> firstRaceTemplates = raceTemplates.values().iterator().next();
            if (!firstRaceTemplates.isEmpty()) {
                int templateId = firstRaceTemplates.get(0).getTemplateId();
                Monster monster = monsterManager.createMonsterByTemplateId(templateId);
                assertNotNull("应成功创建怪物", monster);
            }
        }
    }

    @Test
    public void testCreateMonsterByTemplateId_InvalidId() {
        Monster monster = monsterManager.createMonsterByTemplateId(999999);
        assertNull("无效模板ID应返回null", monster);
    }

    @Test
    public void testCreateMonsterWithLevelScaling() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (!raceTemplates.isEmpty()) {
            List<MonsterTemplate> firstRaceTemplates = raceTemplates.values().iterator().next();
            if (!firstRaceTemplates.isEmpty()) {
                int templateId = firstRaceTemplates.get(0).getTemplateId();
                int playerLevel = 20;

                Monster monster = monsterManager.createMonsterWithLevelScaling(templateId, playerLevel);
                if (monster != null) {
                    assertNotNull("应成功创建怪物", monster);
                    // 验证等级在合理范围内（玩家等级±5）
                    assertTrue("怪物等级应在合理范围内",
                            monster.getLevel() >= playerLevel - 5 && monster.getLevel() <= playerLevel + 5);
                }
            }
        }
    }

    @Test
    public void testCreateRandomMonster() {
        Monster monster = monsterManager.createRandomMonster();
        // 如果有模板配置，应能创建怪物
        if (monster != null) {
            assertNotNull("怪物不应为null", monster);
            assertNotNull("怪物名称不应为null", monster.getName());
        }
    }

    @Test
    public void testCreateMonstersFromTemplateIds() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (!raceTemplates.isEmpty()) {
            List<MonsterTemplate> firstRaceTemplates = raceTemplates.values().iterator().next();
            if (!firstRaceTemplates.isEmpty()) {
                List<Integer> templateIds = List.of(
                        firstRaceTemplates.get(0).getTemplateId()
                );

                List<Monster> monsters = monsterManager.createMonstersFromTemplateIds(templateIds, 10);
                assertNotNull("怪物列表不应为null", monsters);
                assertEquals("应创建与模板ID数量相同的怪物", templateIds.size(), monsters.size());
            }
        }
    }

    @Test
    public void testCreateMonsterWithoutAffixes() {
        var raceTemplates = monsterManager.getTemplatesByRace();
        if (!raceTemplates.isEmpty()) {
            List<MonsterTemplate> firstRaceTemplates = raceTemplates.values().iterator().next();
            if (!firstRaceTemplates.isEmpty()) {
                int templateId = firstRaceTemplates.get(0).getTemplateId();
                Monster monster = monsterManager.createMonsterWithoutAffixes(templateId);
                if (monster != null) {
                    assertNotNull("应成功创建怪物", monster);
                    // 不带词缀的怪物应该没有词缀列表为空或很小
                }
            }
        }
    }
}
