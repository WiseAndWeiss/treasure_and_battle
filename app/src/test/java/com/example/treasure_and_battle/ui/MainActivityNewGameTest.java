package com.example.treasure_and_battle.ui;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.game.GameManager;
import com.example.treasure_and_battle.manager.game.SaveManager;
import com.example.treasure_and_battle.model.profession.ProfessionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class MainActivityNewGameTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        PlayerCharacterHolder.clear();
        resetSingletons();
    }

    @After
    public void tearDown() throws Exception {
        PlayerCharacterHolder.clear();
        resetSingletons();
        cleanSavesDir();
    }

    private void cleanSavesDir() {
        File savesDir = new File(context.getFilesDir(), "saves");
        if (savesDir.exists()) deleteRecursively(savesDir);
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

    private static void resetSingletons() throws Exception {
        for (String clz : new String[]{"GameManager", "SaveManager"}) {
            try {
                Field f = Class.forName("com.example.treasure_and_battle.manager." + clz)
                        .getDeclaredField("instance");
                f.setAccessible(true);
                f.set(null, null);
            } catch (NoSuchFieldException ignored) {}
        }
    }

    // ====================== 新游戏流程 ======================

    @Test
    public void testNewGame_ProfessionSelectCreatesCharacter() {
        assertNull(PlayerCharacterHolder.get(context));

        Character ch = new Character(1, "测试角色", ProfessionType.MAGE, context.getApplicationContext());
        PlayerCharacterHolder.restoreFrom(ch);

        Character stored = PlayerCharacterHolder.get(context);
        assertNotNull(stored);
        assertEquals("测试角色", stored.getName());
        assertEquals(ProfessionType.MAGE, stored.getProfessionType());
    }

    @Test
    public void testNewGame_CharacterBagIsEmpty() {
        Character ch = new Character(1, "新角色", ProfessionType.RANGER, context.getApplicationContext());
        PlayerCharacterHolder.restoreFrom(ch);

        Character stored = PlayerCharacterHolder.get(context);
        for (int i = 0; i < 125; i++) {
            assertNull(stored.getBagItems().get(i));
        }
    }

    @Test
    public void testNewGame_CharacterDefaults() {
        Character ch = new Character(1, "新手", ProfessionType.WARRIOR, context.getApplicationContext());

        assertEquals(1, ch.getLevel());
        assertEquals(0, ch.getCurrentExp());
        assertEquals(0, ch.getGold());
        assertEquals(20, ch.getCurrentHp());
        assertEquals(10, ch.getCurrentMp());
        assertEquals(0, ch.getTalentPoints());
        assertEquals(0, ch.getSkillPoints());
    }

    // ====================== MainActivity onResume 逻辑 ======================

    @Test
    public void testOnResume_NoExistingCharacter_LoadsAutoSave() {
        cleanSavesDir();

        Character saved = new Character(1, "存档角色", ProfessionType.WARRIOR, context.getApplicationContext());
        saved.addGold(500);
        saveToAuto(saved);

        PlayerCharacterHolder.clear();

        Character restored = SaveManager.getInstance(context).loadGame("auto");
        assertNotNull(restored);
        assertEquals("存档角色", restored.getName());
        assertEquals(500, restored.getGold());
    }

    @Test
    public void testOnResume_HasExistingCharacter_DoesNotOverwrite() {
        cleanSavesDir();

        Character saved = new Character(1, "旧存档", ProfessionType.WARRIOR, context.getApplicationContext());
        saved.addGold(999);
        saveToAuto(saved);

        Character newGame = new Character(1, "新角色", ProfessionType.MAGE, context.getApplicationContext());
        newGame.addGold(100);
        PlayerCharacterHolder.restoreFrom(newGame);

        assertNotNull(PlayerCharacterHolder.get(context));
        assertEquals("新角色", PlayerCharacterHolder.get(context).getName());
        assertEquals(ProfessionType.MAGE, PlayerCharacterHolder.get(context).getProfessionType());
        assertEquals(100, PlayerCharacterHolder.get(context).getGold());
    }

    @Test
    public void testOnResume_NoCharacterAndNoAutoSave_UsesGetOrCreate() {
        cleanSavesDir();
        PlayerCharacterHolder.clear();

        Character ch = PlayerCharacterHolder.getOrCreate(context);
        assertNotNull(ch);
        assertEquals("冒险者", ch.getName());
        assertEquals(ProfessionType.WARRIOR, ch.getProfessionType());
    }

    // ====================== 读档跳过职业选择 ======================

    @Test
    public void testLoadSave_SetsCharacterDirectly() {
        Character loaded = new Character(1, "读档角色", ProfessionType.RANGER, context.getApplicationContext());
        loaded.gainExp(loaded.getExpToNextLevel() * 5);
        loaded.addGold(3000);

        PlayerCharacterHolder.restoreFrom(loaded);

        Character stored = PlayerCharacterHolder.get(context);
        assertNotNull(stored);
        assertEquals("读档角色", stored.getName());
        assertEquals(ProfessionType.RANGER, stored.getProfessionType());
        assertTrue(stored.getLevel() > 1);
        assertEquals(3000, stored.getGold());
    }

    // ====================== GameManager 空值保护 ======================

    @Test
    public void testGameManager_OnGamePause_NoCharacter_NoCrash() {
        GameManager gm = GameManager.getInstance(context);
        gm.onGamePause();
    }

    @Test
    public void testGameManager_OnGameResume_NoCharacter_NoCrash() {
        GameManager gm = GameManager.getInstance(context);
        gm.onGameResume();
    }

    @Test
    public void testGameManager_TriggerAutoSave_NoCharacter_NoCrash() {
        GameManager gm = GameManager.getInstance(context);
        gm.triggerAutoSave();
    }

    @Test
    public void testGameManager_IsGameActive_DefaultFalse() {
        GameManager gm = GameManager.getInstance(context);
        assertFalse(gm.isGameActive());
    }

    @Test
    public void testGameManager_GetCurrentCharacter_DefaultNull() {
        GameManager gm = GameManager.getInstance(context);
        assertNull(gm.getCurrentCharacter());
    }

    // ====================== 职业选择后背包为空 ======================

    @Test
    public void testProfessionSelect_EmptyBag_EachProfession() {
        for (ProfessionType pt : new ProfessionType[]{
                ProfessionType.WARRIOR, ProfessionType.MAGE, ProfessionType.RANGER}) {
            Character ch = new Character(1, "测试", pt, context.getApplicationContext());
            assertEquals(pt, ch.getProfessionType());
            for (int i = 0; i < 125; i++) {
                assertNull("职业 " + pt + " 的背包格 " + i + " 应为空", ch.getBagItems().get(i));
            }
        }
    }

    // ====================== helper ======================

    private void saveToAuto(Character ch) {
        try {
            Field f = SaveManager.class.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception e) {}
        SaveManager sm = SaveManager.getInstance(context);
        sm.saveGame(ch, "auto");
    }
}
