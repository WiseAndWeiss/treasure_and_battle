package com.example.treasure_and_battle.manager.affix;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.affix.MonsterAffixFactory;

import com.example.treasure_and_battle.model.affix.MonsterAffixTemplate;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.utils.RandomUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonsterAffixManager {
    private static MonsterAffixManager instance;
    private final Context context;
    private final Gson gson;

    private final Map<Integer, MonsterAffixTemplate> templateMap = new HashMap<>();

    private MonsterAffixManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadTemplates();
    }

    public static synchronized MonsterAffixManager getInstance(Context context) {
        if (instance == null) {
            instance = new MonsterAffixManager(context);
        }
        return instance;
    }

    public void loadTemplates() {
        try {
            InputStream is = context.getAssets().open("monster_affix_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<ConfigWrapper>() {}.getType();
            ConfigWrapper wrapper = gson.fromJson(json, type);
            for (MonsterAffixTemplate template : wrapper.affix_templates) {
                templateMap.put(template.getTemplateId(), template);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 精确创建怪物词缀（测试/指定用）
    public BaseMonsterAffix createAffixByTemplateId(int templateId) {
        MonsterAffixTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
        Rarity rarity = Rarity.fromId(template.getRarityId());
        return createAffixFromTemplate(template, rarity, randomValue);
    }

    // 随机给怪物生成词缀：可以结合引擎，这里直接随机
    public List<BaseMonsterAffix> generateAffixForMonster(Monster monster) {
        List<BaseMonsterAffix> affixList = new ArrayList<>();
        Rarity monsterRarity = monster.getRarity();
        int affixCount = monsterRarity.getAffixCount();

        for (int i = 0; i < affixCount; i++) {
            MonsterAffixTemplate template = getRandomTemplate();
            if (template == null) continue;

            float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
            Rarity rarity = Rarity.fromId(template.getRarityId());
            BaseMonsterAffix affix = createAffixFromTemplate(template, rarity, randomValue);
            if (affix != null) {
                affixList.add(affix);
            }
        }

        return affixList;
    }

    public MonsterAffixTemplate getRandomTemplate() {
        List<MonsterAffixTemplate> validTemplates = new ArrayList<>(templateMap.values());
        if (validTemplates.isEmpty()) return null;
        return validTemplates.get(RandomUtils.getRandomInt(0, validTemplates.size() - 1));
    }

    public BaseMonsterAffix createAffixFromTemplate(MonsterAffixTemplate template, Rarity rarity, float randomValue) {
        return MonsterAffixFactory.create(template, rarity, template.getTriggerType(), randomValue);
    }

    private static class ConfigWrapper {
        List<MonsterAffixTemplate> affix_templates;
    }
}
