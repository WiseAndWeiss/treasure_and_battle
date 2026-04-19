package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.model.entity.IntentTemplate;
import com.example.treasure_and_battle.model.entity.MonsterIntent;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntentManager {
    private static IntentManager instance;
    private Context context;
    private Map<String, IntentTemplate> templateMap = new HashMap<>();

    private IntentManager(Context context) {
        this.context = context.getApplicationContext();
        loadTemplates();
    }

    public static IntentManager getInstance(Context context) {
        if (instance == null) {
            instance = new IntentManager(context);
        }
        return instance;
    }

    private void loadTemplates() {
        try {
            InputStream is = context.getAssets().open("intent_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            IntentConfigWrapper wrapper = gson.fromJson(json, IntentConfigWrapper.class);

            if (wrapper != null && wrapper.intents != null) {
                for (IntentTemplate template : wrapper.intents) {
                    templateMap.put(template.getIntentId(), template);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MonsterIntent createIntentById(String intentId, int weight) {
        IntentTemplate template = templateMap.get(intentId);
        if (template == null) return null;

        return new MonsterIntent(
            template.getName(),
            template.getDescription(),
            template.getType(),
            template.getApCost(),
            template.getMpCost(),
            template.getPowerMultiplier(),
            weight
        );
    }

    private static class IntentConfigWrapper {
        List<IntentTemplate> intents;
    }
}
