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
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class GameManagerTest {

    private Context context;
    private GameManager gameManager;

    @Before
    public void setUp() throws Exception {
        resetSingletons();
        context = RuntimeEnvironment.application;
        gameManager = GameManager.getInstance(context);
    }

    @After
    public void tearDown() throws Exception {
        cleanSavesDir();
        resetSingletons();
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

    private static void resetSingletons() throws Exception {
        for (String name : new String[]{"instance"}) {
            try {
                Field f = GameManager.class.getDeclaredField("instance");
                f.setAccessible(true);
                f.set(null, null);
            } catch (NoSuchFieldException ignored) {}
        }
        try {
            Field f = SaveManager.class.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, null);
        } catch (NoSuchFieldException ignored) {}
    }

    private Character createTestCharacter() {
        return new Character(1, "测试角色", ProfessionType.WARRIOR, context);
    }

    // ====================== startGame ======================

    @Test
    public void testStartGame_SetsCharacter() {
        Character ch = createTestCharacter();
        gameManager.startGame(ch);
        assertNotNull(gameManager.getCurrentCharacter());
        assertEquals("测试角色", gameManager.getCurrentCharacter().getName());
    }

    @Test
    public void testStartGame_SetsActive() {
        gameManager.startGame(createTestCharacter());
        assertTrue(gameManager.isGameActive());
    }

    @Test
    public void testStartGame_StartsPlayTimeTracking() {
        SaveManager sm = SaveManager.getInstance(context);
        assertEquals(0, sm.getPlayTimeSeconds());

        gameManager.startGame(createTestCharacter());

        ShadowLooper.idleMainLooper(2000);
        assertTrue("游戏时长应该开始计时", sm.getPlayTimeSeconds() >= 1);
    }

    // ====================== onGamePause ======================

    @Test
    public void testOnGamePause_SetsInactive() {
        gameManager.startGame(createTestCharacter());
        gameManager.onGamePause();
        assertFalse(gameManager.isGameActive());
    }

    @Test
    public void testOnGamePause_AutoSaves() {
        Character ch = createTestCharacter();
        ch.addGold(500);
        gameManager.startGame(ch);

        ShadowLooper.idleMainLooper(100);

        gameManager.onGamePause();

        SaveManager sm = SaveManager.getInstance(context);
        Character loaded = sm.loadGame("auto");
        assertNotNull("暂停时应自动存档", loaded);
        assertEquals(500, loaded.getGold());
    }

    @Test
    public void testOnGamePause_NoCharacter_NoCrash() {
        gameManager.onGamePause();
        assertFalse(gameManager.isGameActive());
    }

    // ====================== onGameResume ======================

    @Test
    public void testOnGameResume_SetsActive() {
        gameManager.startGame(createTestCharacter());
        gameManager.onGamePause();
        assertFalse(gameManager.isGameActive());

        gameManager.onGameResume();
        assertTrue(gameManager.isGameActive());
    }

    @Test
    public void testOnGameResume_NoCharacter_NoCrash() {
        gameManager.onGameResume();
        assertFalse(gameManager.isGameActive());
    }

    // ====================== pause / resume cycle ======================

    @Test
    public void testPauseResumeCycle_MultipleTimes() {
        Character ch = createTestCharacter();
        gameManager.startGame(ch);

        for (int i = 0; i < 3; i++) {
            gameManager.onGamePause();
            assertFalse(gameManager.isGameActive());

            gameManager.onGameResume();
            assertTrue(gameManager.isGameActive());
        }
    }

    @Test
    public void testPauseSavesEachTime() {
        Character ch = createTestCharacter();
        gameManager.startGame(ch);

        ch.addGold(100);
        gameManager.onGamePause();
        ShadowLooper.idleMainLooper(100);

        SaveManager sm = SaveManager.getInstance(context);
        assertEquals(100, sm.loadGame("auto").getGold());

        ch = SaveManager.getInstance(context).loadGame("auto");
        ch.addGold(200);
        gameManager.startGame(ch);

        gameManager.onGamePause();
        ShadowLooper.idleMainLooper(100);

        assertEquals(300, sm.loadGame("auto").getGold());
    }

    // ====================== triggerAutoSave ======================

    @Test
    public void testTriggerAutoSave_SavesToAutoSlot() {
        Character ch = createTestCharacter();
        ch.addGold(777);
        gameManager.startGame(ch);

        ShadowLooper.idleMainLooper(100);

        gameManager.triggerAutoSave();

        SaveManager sm = SaveManager.getInstance(context);
        Character loaded = sm.loadGame("auto");
        assertNotNull(loaded);
        assertEquals(777, loaded.getGold());
    }

    @Test
    public void testTriggerAutoSave_NoCharacter_NoSave() {
        gameManager.triggerAutoSave();

        SaveManager sm = SaveManager.getInstance(context);
        assertNull(sm.loadGame("auto"));
    }

    // ====================== startGame after load ======================

    @Test
    public void testStartGame_AfterLoad_CharacterRestored() {
        Character ch = createTestCharacter();
        ch.addGold(5000);
        ch.gainExp(ch.getExpToNextLevel() * 15);
        gameManager.startGame(ch);
        gameManager.triggerAutoSave();

        SaveManager sm = SaveManager.getInstance(context);
        Character loaded = sm.loadGame("auto");
        assertNotNull(loaded);
        assertEquals(5000, loaded.getGold());
        assertTrue(loaded.getLevel() >= 4);
    }

    // ====================== getCurrentCharacter ======================

    @Test
    public void testGetCurrentCharacter_InitiallyNull() {
        assertNull(gameManager.getCurrentCharacter());
    }

    @Test
    public void testGetCurrentCharacter_AfterStart() {
        Character ch = createTestCharacter();
        ch.setCurrentHp(12);
        gameManager.startGame(ch);
        assertEquals(12, gameManager.getCurrentCharacter().getCurrentHp());
    }

    // ====================== isGameActive ======================

    @Test
    public void testIsGameActive_InitiallyFalse() {
        assertFalse(gameManager.isGameActive());
    }

    @Test
    public void testIsGameActive_AfterStart_True() {
        gameManager.startGame(createTestCharacter());
        assertTrue(gameManager.isGameActive());
    }

    // ====================== stop auto save timer ======================

    @Test
    public void testStopAutoSaveTimer_OnPause() {
        Character ch = createTestCharacter();
        gameManager.startGame(ch);
        gameManager.onGamePause();

        ShadowLooper.idleMainLooper(200);
        SaveManager sm = SaveManager.getInstance(context);
        Character loaded = sm.loadGame("auto");
        assertNotNull(loaded);
    }
}
