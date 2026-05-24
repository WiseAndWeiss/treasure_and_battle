package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.profession.ProfessionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SaveManagerTest {

    private Context context;
    private SaveManager saveManager;

    @Before
    public void setUp() throws Exception {
        resetSaveManagerSingleton();
        context = RuntimeEnvironment.application;
        saveManager = SaveManager.getInstance(context);
        cleanSavesDir();
    }

    @After
    public void tearDown() throws Exception {
        cleanSavesDir();
        resetSaveManagerSingleton();
    }

    private void cleanSavesDir() {
        File savesDir = new File(context.getFilesDir(), "saves");
        if (savesDir.exists()) {
            deleteRecursively(savesDir);
        }
    }

    private void deleteRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteRecursively(f);
                f.delete();
            }
        }
        dir.delete();
    }

    private static void resetSaveManagerSingleton() throws Exception {
        Field f = SaveManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    private Character createTestCharacter(String name, ProfessionType pt, int level) {
        Character ch = new Character(1, name, pt, context);
        if (level > 1) {
            int expNeeded = 0;
            for (int lv = 1; lv < level; lv++) {
                expNeeded += ch.getExpToNextLevel();
                ch.gainExp(ch.getExpToNextLevel());
            }
        }
        return ch;
    }

    // ====================== getAllSlotMetas ======================

    @Test
    public void testGetAllSlotMetas_CorrectCount() {
        List<SaveManager.SlotMeta> metas = saveManager.getAllSlotMetas();
        assertEquals(20, metas.size());
    }

    @Test
    public void testGetAllSlotMetas_AllInitiallyEmpty() {
        List<SaveManager.SlotMeta> metas = saveManager.getAllSlotMetas();
        for (int i = 0; i < metas.size(); i++) {
            assertTrue("槽位 " + i + " 应为空", metas.get(i).isEmpty);
        }
    }

    @Test
    public void testGetAllSlotMetas_FirstIsAuto() {
        List<SaveManager.SlotMeta> metas = saveManager.getAllSlotMetas();
        assertTrue(metas.get(0).isAuto);
        assertEquals("自动存档", metas.get(0).displayName);
    }

    @Test
    public void testGetAllSlotMetas_ManualSlotsNamedCorrectly() {
        List<SaveManager.SlotMeta> metas = saveManager.getAllSlotMetas();
        for (int i = 1; i <= 19; i++) {
            assertFalse("槽位 " + i + " 不应是自动存档", metas.get(i).isAuto);
            assertEquals("存档" + i, metas.get(i).displayName);
        }
    }

    // ====================== getSlotMeta ======================

    @Test
    public void testGetSlotMeta_Index0_IsAuto() {
        SaveManager.SlotMeta meta = saveManager.getSlotMeta(0);
        assertNotNull(meta);
        assertTrue(meta.isAuto);
    }

    @Test
    public void testGetSlotMeta_Index1_IsManual() {
        SaveManager.SlotMeta meta = saveManager.getSlotMeta(1);
        assertNotNull(meta);
        assertFalse(meta.isAuto);
        assertEquals("存档1", meta.displayName);
    }

    @Test
    public void testGetSlotMeta_Index20_ReturnsNull() {
        assertNull(saveManager.getSlotMeta(20));
    }

    @Test
    public void testGetSlotMeta_NegativeIndex_ReturnsNull() {
        assertNull(saveManager.getSlotMeta(-1));
    }

    // ====================== isAutoSlot / isSlotEmpty ======================

    @Test
    public void testIsAutoSlot() {
        assertTrue(saveManager.isAutoSlot("auto"));
        assertFalse(saveManager.isAutoSlot("slot_0"));
        assertFalse(saveManager.isAutoSlot("slot_5"));
    }

    @Test
    public void testIsSlotEmpty_EmptySlot() {
        assertTrue(saveManager.isSlotEmpty("auto"));
        assertTrue(saveManager.isSlotEmpty("slot_0"));
        assertTrue(saveManager.isSlotEmpty("slot_18"));
    }

    // ====================== saveGame / loadGame roundtrip ======================

    @Test
    public void testSaveAndLoadRoundTrip_BasicCharacter() {
        Character original = createTestCharacter("测试角色", ProfessionType.WARRIOR, 1);
        assertTrue(saveManager.saveGame(original, "auto"));

        Character loaded = saveManager.loadGame("auto");
        assertNotNull(loaded);
        assertEquals("测试角色", loaded.getName());
        assertEquals(ProfessionType.WARRIOR, loaded.getProfessionType());
        assertEquals(1, loaded.getLevel());
        assertEquals(0, loaded.getGold());
        assertEquals(20, loaded.getCurrentHp());
        assertEquals(10, loaded.getCurrentMp());
    }

    @Test
    public void testSaveAndLoadRoundTrip_LeveledCharacter() {
        Character original = createTestCharacter("练级王", ProfessionType.MAGE, 5);
        int gold = 1500;
        while (original.getGold() < gold) original.addGold(100);

        assertTrue(saveManager.saveGame(original, "slot_0"));

        Character loaded = saveManager.loadGame("slot_0");
        assertNotNull(loaded);
        assertEquals("练级王", loaded.getName());
        assertEquals(ProfessionType.MAGE, loaded.getProfessionType());
        assertEquals(5, loaded.getLevel());
        assertTrue(loaded.getGold() >= gold);
        assertTrue(loaded.getTalentPoints() > 0);
    }

    @Test
    public void testSaveAndLoadRoundTrip_WithExpAndGold() {
        Character original = createTestCharacter("财主", ProfessionType.RANGER, 1);
        original.gainExp(50);
        original.addGold(9999);

        assertTrue(saveManager.saveGame(original, "auto"));

        Character loaded = saveManager.loadGame("auto");
        assertNotNull(loaded);
        assertEquals(50, loaded.getCurrentExp());
        assertEquals(9999, loaded.getGold());
    }

    @Test
    public void testSaveAndLoadRoundTrip_WithHpMpDamage() {
        Character original = createTestCharacter("伤兵", ProfessionType.WARRIOR, 1);
        original.setCurrentHp(5);
        original.setCurrentMp(3);

        assertTrue(saveManager.saveGame(original, "auto"));

        Character loaded = saveManager.loadGame("auto");
        assertNotNull(loaded);
        assertEquals(5, loaded.getCurrentHp());
        assertEquals(3, loaded.getCurrentMp());
    }

    @Test
    public void testSaveAndLoadRoundTrip_AllStatsAllocated() {
        Character original = createTestCharacter("天赋怪", ProfessionType.WARRIOR, 1);
        Character.SaveData sd = original.toSaveData();
        assertNotNull(sd);

        original.gainExp(original.getExpToNextLevel() * 30);
        assertTrue(original.getTalentPoints() >= 6);
        original.allocateTalentPoint("STRENGTH");
        original.allocateTalentPoint("AGILITY");
        original.allocateTalentPoint("INTELLIGENCE");
        original.allocateTalentPoint("SPIRIT");
        original.allocateTalentPoint("PHYSIQUE");
        original.allocateTalentPoint("LUCK");

        assertTrue(saveManager.saveGame(original, "auto"));

        Character loaded = saveManager.loadGame("auto");
        assertNotNull(loaded);
        assertEquals(1, loaded.getAllocatedStrength());
        assertEquals(1, loaded.getAllocatedAgility());
        assertEquals(1, loaded.getAllocatedIntelligence());
        assertEquals(1, loaded.getAllocatedSpirit());
        assertEquals(1, loaded.getAllocatedPhysique());
        assertEquals(1, loaded.getAllocatedLuck());
    }

    // ====================== saveToSlot / loadFromSlot ======================

    @Test
    public void testSaveToSlot_ValidIndex_1() {
        Character ch = createTestCharacter("槽1角色", ProfessionType.WARRIOR, 1);
        assertTrue(saveManager.saveToSlot(ch, 1));

        Character loaded = saveManager.loadFromSlot(1);
        assertNotNull(loaded);
        assertEquals("槽1角色", loaded.getName());
    }

    @Test
    public void testSaveToSlot_ValidIndex_19() {
        Character ch = createTestCharacter("槽19角色", ProfessionType.RANGER, 1);
        assertTrue(saveManager.saveToSlot(ch, 19));
        assertNotNull(saveManager.loadFromSlot(19));
    }

    @Test
    public void testSaveToSlot_InvalidIndex_0() {
        Character ch = createTestCharacter("禁入自动", ProfessionType.WARRIOR, 1);
        assertFalse(saveManager.saveToSlot(ch, 0));
    }

    @Test
    public void testSaveToSlot_InvalidIndex_Negative() {
        Character ch = createTestCharacter("负数", ProfessionType.WARRIOR, 1);
        assertFalse(saveManager.saveToSlot(ch, -1));
    }

    @Test
    public void testSaveToSlot_InvalidIndex_TooLarge() {
        Character ch = createTestCharacter("过大", ProfessionType.WARRIOR, 1);
        assertFalse(saveManager.saveToSlot(ch, 20));
    }

    @Test
    public void testLoadFromSlot_Index0_AutoSlot() {
        Character ch = createTestCharacter("自动党", ProfessionType.MAGE, 1);
        assertTrue(saveManager.saveGame(ch, "auto"));
        assertNotNull(saveManager.loadFromSlot(0));
    }

    @Test
    public void testLoadFromSlot_InvalidIndex_ReturnsNull() {
        assertNull(saveManager.loadFromSlot(20));
        assertNull(saveManager.loadFromSlot(-1));
    }

    @Test
    public void testLoadFromSlot_EmptySlot_ReturnsNull() {
        assertNull(saveManager.loadFromSlot(5));
    }

    // ====================== saveGame null edge cases ======================

    @Test
    public void testSaveGame_NullCharacter_ReturnsFalse() {
        assertFalse(saveManager.saveGame(null, "auto"));
        assertFalse(saveManager.saveGame(null, "slot_0"));
    }

    @Test
    public void testLoadGame_NonexistentSlot_ReturnsNull() {
        assertNull(saveManager.loadGame("nonexistent"));
    }

    // ====================== overwrite ======================

    @Test
    public void testOverwriteSlot_LatestWins() {
        Character first = createTestCharacter("初代", ProfessionType.WARRIOR, 1);
        first.addGold(100);
        assertTrue(saveManager.saveGame(first, "slot_0"));

        Character second = createTestCharacter("二代", ProfessionType.MAGE, 3);
        second.addGold(9999);
        assertTrue(saveManager.saveGame(second, "slot_0"));

        Character loaded = saveManager.loadGame("slot_0");
        assertNotNull(loaded);
        assertEquals("二代", loaded.getName());
        assertEquals(ProfessionType.MAGE, loaded.getProfessionType());
        assertEquals(3, loaded.getLevel());
        assertEquals(9999, loaded.getGold());
    }

    // ====================== metadata ======================

    @Test
    public void testSlotMeta_AfterSave_HasCorrectInfo() {
        Character ch = createTestCharacter("元数据测试", ProfessionType.RANGER, 2);
        ch.gainExp(30);
        assertTrue(saveManager.saveGame(ch, "slot_5"));

        SaveManager.SlotMeta meta = saveManager.getSlotMeta(6);
        assertNotNull(meta);
        assertFalse(meta.isEmpty);
        assertEquals("元数据测试", meta.characterName);
        assertEquals(2, meta.level);
        assertTrue(meta.timestamp > 0);
    }

    @Test
    public void testSlotMeta_AfterSave_ShowsInList() {
        Character ch = createTestCharacter("列表可见", ProfessionType.WARRIOR, 1);
        assertTrue(saveManager.saveGame(ch, "auto"));

        List<SaveManager.SlotMeta> metas = saveManager.getAllSlotMetas();
        assertFalse(metas.get(0).isEmpty);
        assertEquals("列表可见", metas.get(0).characterName);
    }

    // ====================== multiple slots independent ======================

    @Test
    public void testMultipleSlots_Independent() {
        Character ch1 = createTestCharacter("战士", ProfessionType.WARRIOR, 1);
        Character ch2 = createTestCharacter("法师", ProfessionType.MAGE, 2);
        Character ch3 = createTestCharacter("游侠", ProfessionType.RANGER, 3);

        assertTrue(saveManager.saveGame(ch1, "slot_0"));
        assertTrue(saveManager.saveGame(ch2, "slot_1"));
        assertTrue(saveManager.saveGame(ch3, "slot_2"));

        assertEquals("战士", saveManager.loadGame("slot_0").getName());
        assertEquals("法师", saveManager.loadGame("slot_1").getName());
        assertEquals("游侠", saveManager.loadGame("slot_2").getName());
    }

    @Test
    public void testAutoSlotIndependentFromManual() {
        Character autoCh = createTestCharacter("自动角色", ProfessionType.WARRIOR, 1);
        Character manualCh = createTestCharacter("手动角色", ProfessionType.MAGE, 2);

        assertTrue(saveManager.saveGame(autoCh, "auto"));
        assertTrue(saveManager.saveGame(manualCh, "slot_0"));

        assertEquals("自动角色", saveManager.loadGame("auto").getName());
        assertEquals("手动角色", saveManager.loadGame("slot_0").getName());
    }

    // ====================== isSlotEmpty after save ======================

    @Test
    public void testIsSlotEmpty_AfterSave_ReturnsFalse() {
        Character ch = createTestCharacter("非空", ProfessionType.WARRIOR, 1);
        saveManager.saveGame(ch, "slot_3");
        assertFalse(saveManager.isSlotEmpty("slot_3"));
    }

    @Test
    public void testIsSlotEmpty_EmptySlot_ReturnsTrue() {
        assertTrue(saveManager.isSlotEmpty("slot_10"));
    }

    // ====================== save with empty bag ======================

    @Test
    public void testSaveAndLoad_EmptyBag_125Slots() {
        Character original = createTestCharacter("空包", ProfessionType.WARRIOR, 1);
        assertTrue(saveManager.saveGame(original, "auto"));

        Character loaded = saveManager.loadGame("auto");
        assertNotNull(loaded);
        assertEquals(125, loaded.getBagItems().size());
        for (int i = 0; i < 125; i++) {
            assertNull(loaded.getBagItems().get(i));
        }
    }

    // ====================== play time ======================

    @Test
    public void testPlayTime_InitiallyZero() {
        assertEquals(0, saveManager.getPlayTimeSeconds());
    }

    // ====================== save across all manual slots ======================

    @Test
    public void testSaveAll19ManualSlots() {
        for (int i = 1; i <= 19; i++) {
            Character ch = createTestCharacter("角色" + i, ProfessionType.WARRIOR, 1);
            assertTrue("槽位 " + i + " 保存失败", saveManager.saveToSlot(ch, i));
        }
        for (int i = 1; i <= 19; i++) {
            Character loaded = saveManager.loadFromSlot(i);
            assertNotNull("槽位 " + i + " 加载失败", loaded);
            assertEquals("角色" + i, loaded.getName());
        }
    }
}
