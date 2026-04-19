package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
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

    private void loadTemplates() {
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
        AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());

        try {
            Class<?> affixClass = Class.forName(template.getAffixClass());
            return (BaseMonsterAffix) affixClass.getConstructor(
                    int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, float.class
            ).newInstance(
                    template.getTemplateId(),
                    template.getAffixName(),
                    template.getDescriptionFormat(),
                    rarity,
                    triggerType,
                    randomValue
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());

            try {
                Class<?> affixClass = Class.forName(template.getAffixClass());
                BaseMonsterAffix affix = (BaseMonsterAffix) affixClass.getConstructor(
                        int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, float.class
                ).newInstance(
                        template.getTemplateId(),
                        template.getAffixName(),
                        template.getDescriptionFormat(),
                        rarity,
                        triggerType,
                        randomValue
                );
                affixList.add(affix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return affixList;
    }

    private MonsterAffixTemplate getRandomTemplate() {
        List<MonsterAffixTemplate> validTemplates = new ArrayList<>(templateMap.values());
        if (validTemplates.isEmpty()) return null;
        return validTemplates.get(RandomUtils.getRandomInt(0, validTemplates.size() - 1));
    }

    private static class ConfigWrapper {
        List<MonsterAffixTemplate> affix_templates;
    }
}
