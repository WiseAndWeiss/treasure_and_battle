package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.MonsterTemplate;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonsterManager {
    private static MonsterManager instance;
    private Context context;
    private Map<Integer, MonsterTemplate> templateMap = new HashMap<>();

    private MonsterManager(Context context) {
        this.context = context.getApplicationContext();
        loadTemplates();
    }

    public static MonsterManager getInstance(Context context) {
        if (instance == null) {
            instance = new MonsterManager(context);
        }
        return instance;
    }

    // 1. 加载怪物配置文件
    private void loadTemplates() {
        try {
            InputStream is = context.getAssets().open("monster_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            MonsterConfigWrapper wrapper = gson.fromJson(json, MonsterConfigWrapper.class);

            if (wrapper != null && wrapper.monster_templates != null) {
                for (MonsterTemplate template : wrapper.monster_templates) {
                    templateMap.put(template.getTemplateId(), template);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. 根据模板ID创建怪物实例
    public Monster createMonsterByTemplateId(int templateId) {
        MonsterTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        Rarity rarity = Rarity.fromId(template.getRarityId());

        Monster monster = new Monster(
            template.getEntityId(),
            template.getName(),
            template.getLevel(),
            rarity,
            template.getMaxHp(),
            template.getMaxMp(),
            template.getPatk(),
            template.getMatt(),
            template.getPdef(),
            template.getMdef(),
            template.getSpeed(),
            template.getStrength(),
            template.getAgility(),
            template.getIntelligence(),
            template.getSpirit(),
            template.getPhysique(),
            template.getLuck(),
            template.getExpReward(),
            template.getGoldReward(),
            context
        );

        if (template.getIntents() != null) {
            IntentManager intentManager = IntentManager.getInstance(context);
            for (MonsterTemplate.IntentReference ref : template.getIntents()) {
                com.example.treasure_and_battle.model.entity.MonsterIntent intentItem = intentManager.createIntentById(ref.getIntentId(), ref.getWeight());
                if (intentItem != null) {
                    monster.addIntent(intentItem);
                }
            }
        }

        return monster;
    }

    // 用于解析的包装类
    private static class MonsterConfigWrapper {
        List<MonsterTemplate> monster_templates;
    }
}
