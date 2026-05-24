package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;

public class GameManager {
    private static final long AUTO_SAVE_INTERVAL_MS = 15 * 60 * 1000;

    private static GameManager instance;
    private final Context context;
    private Character currentCharacter;
    private boolean isGameActive;
    private SaveManager saveManager;

    private GameManager(Context context) {
        this.context = context.getApplicationContext();
        this.saveManager = SaveManager.getInstance(context);
    }

    public static synchronized GameManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameManager(context);
        }
        return instance;
    }

    public void startGame(Character character) {
        this.currentCharacter = character;
        this.isGameActive = true;
        saveManager.startPlayTimeTracking();
        saveManager.startAutoSaveTimer(character, AUTO_SAVE_INTERVAL_MS);
    }

    public void onGameResume() {
        if (currentCharacter != null) {
            isGameActive = true;
            saveManager.startPlayTimeTracking();
            saveManager.startAutoSaveTimer(currentCharacter, AUTO_SAVE_INTERVAL_MS);
        }
    }

    public void onGamePause() {
        isGameActive = false;
        saveManager.stopAutoSaveTimer();
        saveManager.stopPlayTimeTracking();
        if (currentCharacter != null) {
            saveManager.autoSave(currentCharacter);
        }
    }

    public void triggerAutoSave() {
        if (currentCharacter != null) {
            saveManager.autoSave(currentCharacter);
        }
    }

    public Character getCurrentCharacter() {
        return currentCharacter;
    }

    public boolean isGameActive() {
        return isGameActive;
    }
}
