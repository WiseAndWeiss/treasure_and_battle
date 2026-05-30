package com.example.treasure_and_battle.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.ui.FloatMsgOverlay;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SaveManager {
    private static final String SAVE_DIR = "saves";
    private static final String AUTO_SLOT = "auto";
    private static final int MANUAL_SLOT_COUNT = 19;
    private static final String SAVE_FILE = "save.json";
    private static final String META_FILE = "meta.json";

    private static SaveManager instance;
    private final Context context;
    private final Gson gson;
    private final File saveRoot;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoSaveRunnable;
    private Runnable playTimeRunnable;
    private long playTimeSeconds;
    private boolean isTracking;

    public static class SlotMeta {
        public String slotId;
        public String displayName;
        public boolean isAuto;
        public boolean isEmpty = true;
        public String characterName;
        public int level;
        public int currentExp;
        public int expToNextLevel;
        public long timestamp;
    }

    private SaveManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = createGson();
        this.saveRoot = new File(context.getFilesDir(), SAVE_DIR);
        ensureDirectories();
    }

    public static synchronized SaveManager getInstance(Context context) {
        if (instance == null) {
            instance = new SaveManager(context);
        }
        return instance;
    }

    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new ItemTypeAdapterFactory())
                .registerTypeAdapterFactory(new AffixTypeAdapterFactory())
                .create();
    }

    private void ensureDirectories() {
        saveRoot.mkdirs();
        new File(saveRoot, AUTO_SLOT).mkdirs();
        for (int i = 0; i < MANUAL_SLOT_COUNT; i++) {
            new File(saveRoot, "slot_" + i).mkdirs();
        }
    }

    private File getSlotDir(String slotId) {
        return new File(saveRoot, slotId);
    }

    private File getSaveFile(String slotId) {
        return new File(getSlotDir(slotId), SAVE_FILE);
    }

    private File getMetaFile(String slotId) {
        return new File(getSlotDir(slotId), META_FILE);
    }

    public List<SlotMeta> getAllSlotMetas() {
        List<SlotMeta> list = new ArrayList<>();
        list.add(loadSlotMeta(AUTO_SLOT));
        for (int i = 0; i < MANUAL_SLOT_COUNT; i++) {
            list.add(loadSlotMeta("slot_" + i));
        }
        return list;
    }

    public SlotMeta getSlotMeta(int index) {
        if (index == 0) return loadSlotMeta(AUTO_SLOT);
        if (index >= 1 && index <= MANUAL_SLOT_COUNT) return loadSlotMeta("slot_" + (index - 1));
        return null;
    }

    private SlotMeta loadSlotMeta(String slotId) {
        SlotMeta meta = new SlotMeta();
        meta.slotId = slotId;
        meta.isAuto = AUTO_SLOT.equals(slotId);
        if (meta.isAuto) {
            meta.displayName = "自动存档";
        } else {
            int num = Integer.parseInt(slotId.substring("slot_".length())) + 1;
            meta.displayName = "存档" + num;
        }

        File metaFile = getMetaFile(slotId);
        if (metaFile.exists()) {
            try (FileReader reader = new FileReader(metaFile)) {
                SlotMeta saved = gson.fromJson(reader, SlotMeta.class);
                if (saved != null && !saved.isEmpty) {
                    meta.characterName = saved.characterName;
                    meta.level = saved.level;
                    meta.currentExp = saved.currentExp;
                    meta.expToNextLevel = saved.expToNextLevel;
                    meta.timestamp = saved.timestamp;
                    meta.isEmpty = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return meta;
    }

    public boolean saveGame(Character character, String slotId) {
        if (character == null) return false;
        try {
            Character.SaveData saveData = character.toSaveData();
            saveData.timestamp = System.currentTimeMillis();
            saveData.playTimeSeconds = playTimeSeconds;

            File saveFile = getSaveFile(slotId);
            try (FileWriter writer = new FileWriter(saveFile)) {
                gson.toJson(saveData, writer);
            }

            SlotMeta meta = new SlotMeta();
            meta.slotId = slotId;
            meta.isEmpty = false;
            meta.characterName = character.getName();
            meta.level = character.getLevel();
            meta.currentExp = character.getCurrentExp();
            meta.expToNextLevel = character.getExpToNextLevel();
            meta.timestamp = saveData.timestamp;

            File metaFile = getMetaFile(slotId);
            try (FileWriter writer = new FileWriter(metaFile)) {
                gson.toJson(meta, writer);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveToSlot(Character character, int slotIndex) {
        if (slotIndex < 1 || slotIndex > MANUAL_SLOT_COUNT) return false;
        return saveGame(character, "slot_" + (slotIndex - 1));
    }

    public Character loadGame(String slotId) {
        File saveFile = getSaveFile(slotId);
        if (!saveFile.exists()) return null;

        try (FileReader reader = new FileReader(saveFile)) {
            Character.SaveData saveData = gson.fromJson(reader, Character.SaveData.class);
            if (saveData == null || saveData.character == null) return null;

            if (saveData.playTimeSeconds > 0) {
                playTimeSeconds = saveData.playTimeSeconds;
            }

            return Character.fromSaveData(saveData.character, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Character loadFromSlot(int slotIndex) {
        String slotId;
        if (slotIndex == 0) {
            slotId = AUTO_SLOT;
        } else if (slotIndex >= 1 && slotIndex <= MANUAL_SLOT_COUNT) {
            slotId = "slot_" + (slotIndex - 1);
        } else {
            return null;
        }
        return loadGame(slotId);
    }

    public boolean isAutoSlot(String slotId) {
        return AUTO_SLOT.equals(slotId);
    }

    public boolean isSlotEmpty(String slotId) {
        return !getSaveFile(slotId).exists();
    }

    public boolean autoSave(Character character) {
        if (character == null) return false;
        boolean ok = saveGame(character, AUTO_SLOT);
        if (ok) {
            FloatMsgOverlay.showFloatMsg(context, "自动存档成功");
        }
        return ok;
    }

    public void startAutoSaveTimer(Character character, long intervalMs) {
        stopAutoSaveTimer();
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                autoSave(character);
                handler.postDelayed(this, intervalMs);
            }
        };
        handler.postDelayed(autoSaveRunnable, intervalMs);
    }

    public void stopAutoSaveTimer() {
        if (autoSaveRunnable != null) {
            handler.removeCallbacks(autoSaveRunnable);
            autoSaveRunnable = null;
        }
    }

    public void startPlayTimeTracking() {
        if (isTracking) return;
        isTracking = true;
        playTimeRunnable = new Runnable() {
            @Override
            public void run() {
                playTimeSeconds++;
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(playTimeRunnable, 1000);
    }

    public void stopPlayTimeTracking() {
        isTracking = false;
        if (playTimeRunnable != null) {
            handler.removeCallbacks(playTimeRunnable);
            playTimeRunnable = null;
        }
    }

    public long getPlayTimeSeconds() {
        return playTimeSeconds;
    }
}
