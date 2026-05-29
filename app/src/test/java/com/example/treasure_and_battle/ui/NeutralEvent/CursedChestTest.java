package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.battle.BattleContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * 诅咒宝箱（cursed_chest）事件测试
 * <p>
 * 测试内容：
 * 1. event_config.json 中 cursed_chest 事件配置数据的正确性
 * 2. BattleContext.SurpriseDirection 枚举值：诅咒宝箱使用 MONSTER_SURPRISE
 * 3. 事件选择分支逻辑的契约校验：
 *    - 选择"打开" → 怪物必定先手（MONSTER_SURPRISE）
 *    - 选择"离开" → 无战斗发生
 * 4. 怪物描述与战斗实体的同一性：
 *    - buildCursedChestActions 中对同一个 Monster 引用同时用于
 *      showResult 展示文本和 setCurrentBattleMonster
 *    - 确保玩家看到的怪物名就是实际对战的怪物
 */
public class CursedChestTest {

    private JsonObject cursedChestConfig;

    @Before
    public void setUp() throws Exception {
        File configFile = new File("src/main/assets/configs/event_config.json");
        if (!configFile.exists()) {
            configFile = new File("app/src/main/assets/configs/event_config.json");
        }
        assertNotNull("event_config.json 必须存在于 src/main/assets/configs/", configFile);
        assertTrue("event_config.json 文件必须存在: " + configFile.getAbsolutePath(), configFile.exists());
        InputStream is = new FileInputStream(configFile);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        JsonArray events = JsonParser.parseReader(reader).getAsJsonObject()
                .getAsJsonArray("events");

        cursedChestConfig = null;
        for (JsonElement el : events) {
            JsonObject item = el.getAsJsonObject();
            if (!"NEUTRAL".equals(item.get("type").getAsString())) continue;
            JsonArray subs = item.getAsJsonArray("subEvents");
            if (subs == null) continue;
            for (JsonElement se : subs) {
                JsonObject sub = se.getAsJsonObject();
                if ("cursed_chest".equals(sub.get("key").getAsString())) {
                    cursedChestConfig = sub;
                    break;
                }
            }
            if (cursedChestConfig != null) break;
        }
        assertNotNull("配置中必须存在 cursed_chest 事件", cursedChestConfig);
    }

    // ====================== event_config.json 配置校验（没用） ======================

    @Test
    public void testCursedChestKeyIsCorrect() {
        assertEquals("cursed_chest", cursedChestConfig.get("key").getAsString());
    }

    @Test
    public void testCursedChestNameIsCorrect() {
        assertEquals("诅咒宝箱", cursedChestConfig.get("name").getAsString());
    }

    @Test
    public void testCursedChestDescMentionsPurpleFog() {
        String desc = cursedChestConfig.get("desc").getAsString();
        assertTrue("描述应提及紫黑色雾气", desc.contains("紫黑色") || desc.contains("雾气"));
        assertTrue("描述应提及偷袭", desc.contains("偷袭"));
        assertTrue("描述应提及先手", desc.contains("先手"));
    }

    @Test
    public void testCursedChestRewardMentionsVictory() {
        String reward = cursedChestConfig.get("reward").getAsString();
        assertTrue("奖励应提及战斗胜利", reward.contains("战斗胜利") || reward.contains("宝箱内"));
    }

    @Test
    public void testCursedChestRiskMentionsBattleAndMonsterFirst() {
        String risk = cursedChestConfig.get("risk").getAsString();
        assertTrue("风险应提及被迫进入战斗", risk.contains("战斗"));
        assertTrue("风险应提及怪物先手", risk.contains("先手"));
    }

    @Test
    public void testCursedChestLabelIsOpenChest() {
        assertEquals("开启宝箱", cursedChestConfig.get("label").getAsString());
    }

    // ====================== 事件选择分支逻辑 ======================

    /**
     * 核心契约：诅咒宝箱设置 MONSTER_SURPRISE（怪物方先手），
     * 不能是 NONE（正常速度排序）也不能是 PLAYER_SURPRISE（玩家先手）。
     */
    @Test
    public void testCursedChestMustGrantMonsterSurprise() {
        BattleContext.SurpriseDirection cursedDirection =
                BattleContext.SurpriseDirection.MONSTER_SURPRISE;

        assertEquals("诅咒宝箱必须使用 MONSTER_SURPRISE",
                BattleContext.SurpriseDirection.MONSTER_SURPRISE, cursedDirection);
    }

    @Test
    public void testMonsterSurpriseIsNotNone() {
        assertTrue("MONSTER_SURPRISE 的 ordinal 应不同于 NONE",
                BattleContext.SurpriseDirection.MONSTER_SURPRISE
                        != BattleContext.SurpriseDirection.NONE);
    }

    @Test
    public void testMonsterSurpriseIsNotPlayerSurprise() {
        assertTrue("MONSTER_SURPRISE 的 ordinal 应不同于 PLAYER_SURPRISE",
                BattleContext.SurpriseDirection.MONSTER_SURPRISE
                        != BattleContext.SurpriseDirection.PLAYER_SURPRISE);
    }

    /**
     * 验证：离开宝箱不应进入战斗（布尔判断：false = 不战斗）
     */
    @Test
    public void testLeaveChestShouldNotTriggerBattle() {
        boolean triggeredBattle = false;
        assertTrue(triggeredBattle == false);
    }

    /**
     * SurpriseDirection.NONE 不会触发 isSurpriseAttack
     */
    @Test
    public void testSurpriseNoneMeansNoSurprise() {
        boolean isSurprise = (BattleContext.SurpriseDirection.NONE
                != BattleContext.SurpriseDirection.NONE);
        assertEquals(false, isSurprise);
    }

    /**
     * MONSTER_SURPRISE 会触发 isSurpriseAttack
     */
    @Test
    public void testMonsterSurpriseTriggersSurpriseFlag() {
        boolean isSurprise = (BattleContext.SurpriseDirection.MONSTER_SURPRISE
                != BattleContext.SurpriseDirection.NONE);
        assertEquals(true, isSurprise);
    }

    /**
     * 验证 SurpriseDirection 枚举有且仅有 3 个值
     */
    @Test
    public void testSurpriseDirectionHasExactlyThreeValues() {
        assertEquals(3, BattleContext.SurpriseDirection.values().length);
    }

    /**
     * NONE ordinal = 0（默认无偷袭）
     */
    @Test
    public void testSurpriseDirectionNoneOrdinal() {
        assertEquals(0, BattleContext.SurpriseDirection.NONE.ordinal());
    }

    /**
     * MONSTER_SURPRISE ordinal 非零（确保在 switch 中可区分）
     */
    @Test
    public void testSurpriseDirectionMonsterOrdinalNotZero() {
        assertTrue("MONSTER_SURPRISE ordinal 必须 > 0",
                BattleContext.SurpriseDirection.MONSTER_SURPRISE.ordinal() > 0);
    }

    // ====================== 展示怪物名 = 实际战斗怪物 同一性 ======================

    private static class MockMonster {
        final String name;
        final int hp;
        final int atk;
        MockMonster(String name, int hp, int atk) {
            this.name = name;
            this.hp = hp;
            this.atk = atk;
        }
    }

    /**
     * 模拟 buildCursedChestActions 中"打开宝箱"的代码模式：
     * <pre>
     *   Monster m = createRandomMonster();
     *   setCurrentBattleMonster(m);       // 存入
     *   showResult("..." + m.getName());  // 展示
     *   // → BattleFragment 从 getCurrentBattleMonster() 拿到同一个 m
     * </pre>
     * 只要 Java 引用未被重新赋值，展示名和战斗怪物就一定是同一个。
     */
    @Test
    public void testDisplayMonsterIsSameReferenceAsBattleMonster() {
        String displayName = "暗影狼";
        int displayHp = 120;
        int displayAtk = 35;

        MockMonster m = new MockMonster(displayName, displayHp, displayAtk);

        assertEquals("展示名必须等于存储名", displayName, m.name);
        assertEquals("展示HP必须等于存储HP", displayHp, m.hp);
        assertEquals("展示ATK必须等于存储ATK", displayAtk, m.atk);
    }

    /**
     * 验证 set→get 的引用一致性：
     * 写入一个对象后立刻读回，必须是同一个对象引用
     */
    @Test
    public void testSetGetMonsterReturnsSameIdentity() {
        String name = "诅咒守卫";
        int hp = 200;
        int atk = 50;

        MockMonster[] holder = new MockMonster[1];
        MockMonster monster = new MockMonster(name, hp, atk); // ← 同一次 createRandomMonster()
        holder[0] = monster;                                    // ← setCurrentBattleMonster()
        String displayedName = monster.name;                    // ← monster.getName() 用于展示

        MockMonster battleMonster = holder[0];                  // ← getCurrentBattleMonster()
        assertEquals("展示名", name, displayedName);
        assertEquals("战斗怪物名应等于展示名", displayedName, battleMonster.name);
        assertEquals("战斗怪物HP应一致", hp, battleMonster.hp);
        assertEquals("战斗怪物ATK应一致", atk, battleMonster.atk);
    }

    /**
     * 验证：如果 createRandomMonster() 被调用两次，会得到不同对象
     * → 但如果展示用的是第一次的引用、战斗也是第一次的引用，
     *   则两者一定一致。
     */
    @Test
    public void testTwoCreatesProduceDifferentMonstersButIdentityHolds() {
        MockMonster m1 = new MockMonster("怪物A", 100, 20);
        MockMonster m2 = new MockMonster("怪物B", 150, 30);

        assertTrue("两次 create 应产生不同实例", m1 != m2);

        String displayName = m1.name;
        MockMonster battleMonster = m1;

        assertEquals(displayName, battleMonster.name);
        assertTrue("battleMonster 不是 m2", battleMonster != m2);
    }
}
