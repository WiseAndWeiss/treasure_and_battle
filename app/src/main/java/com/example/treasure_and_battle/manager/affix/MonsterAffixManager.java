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

    public BaseMonsterAffix createAffixByTemplateId(int templateId) {
        MonsterAffixTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        MonsterAffixTemplate.RarityParam param = resolveRarityParam(template, Rarity.COMMON);
        if (param == null) return null;

        float randomValue = RandomUtils.getRandomFloat(param.getMinValue(), param.getMaxValue());
        Rarity rarity = Rarity.fromId(param.getRarityId());
        return createAffixFromTemplate(template, rarity, randomValue, param);
    }

    public List<BaseMonsterAffix> generateAffixForMonster(Monster monster) {
        List<BaseMonsterAffix> affixList = new ArrayList<>();
        Rarity monsterRarity = monster.getRarity();
        int affixCount = monsterRarity.getAffixCount();

        for (int i = 0; i < affixCount; i++) {
            MonsterAffixTemplate template = getRandomTemplate();
            if (template == null) continue;

            MonsterAffixTemplate.RarityParam param = resolveRarityParam(template, monsterRarity);
            if (param == null) continue;

            float randomValue = RandomUtils.getRandomFloat(param.getMinValue(), param.getMaxValue());
            Rarity rarity = Rarity.fromId(param.getRarityId());
            BaseMonsterAffix affix = createAffixFromTemplate(template, rarity, randomValue, param);
            if (affix != null) {
                affixList.add(affix);
            }
        }

        return affixList;
    }

    private MonsterAffixTemplate.RarityParam resolveRarityParam(MonsterAffixTemplate template, Rarity maxRarity) {
        if (template.getRarityParams() == null || template.getRarityParams().isEmpty()) return null;
        List<MonsterAffixTemplate.RarityParam> eligible = new ArrayList<>();
        int maxOrdinal = maxRarity.ordinal();
        for (MonsterAffixTemplate.RarityParam p : template.getRarityParams()) {
            if (p.getRarityId() <= maxOrdinal) {
                eligible.add(p);
            }
        }
        if (eligible.isEmpty()) return null;
        return eligible.get(RandomUtils.getRandomInt(0, eligible.size() - 1));
    }

    public MonsterAffixTemplate getRandomTemplate() {
        List<MonsterAffixTemplate> validTemplates = new ArrayList<>(templateMap.values());
        if (validTemplates.isEmpty()) return null;
        return validTemplates.get(RandomUtils.getRandomInt(0, validTemplates.size() - 1));
    }

    public BaseMonsterAffix createAffixFromTemplate(MonsterAffixTemplate template, Rarity rarity,
                                                     float randomValue, MonsterAffixTemplate.RarityParam param) {
        return MonsterAffixFactory.create(template, rarity, template.getTriggerType(), randomValue, param);
    }

    private static class ConfigWrapper {
        List<MonsterAffixTemplate> affix_templates;
    }
}
