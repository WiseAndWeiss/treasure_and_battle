package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.core.RngEngine;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.affix.EquipAffixTemplate;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipCategory;
import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.utils.RandomUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipAffixManager {
    private static EquipAffixManager instance;
    private final Context context;
    private final Gson gson;

    private final Map<Integer, EquipAffixTemplate> templateMap = new HashMap<>();

    private EquipAffixManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadTemplates();
    }

    public static synchronized EquipAffixManager getInstance(Context context) {
        if (instance == null) {
            instance = new EquipAffixManager(context);
        }
        return instance;
    }

    private void loadTemplates() {
        try {
            InputStream is = context.getAssets().open("equip_affix_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<ConfigWrapper>() {}.getType();
            ConfigWrapper wrapper = gson.fromJson(json, type);
            for (EquipAffixTemplate template : wrapper.affix_templates) {
                templateMap.put(template.getTemplateId(), template);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 给装备生成随机词缀
    public List<BaseEquipAffix> generateAffixForEquipment(EquipItem equipment) {
        List<BaseEquipAffix> affixList = new ArrayList<>();
        Rarity equipmentRarity = equipment.getRarity();
        int affixCount = equipmentRarity.getAffixCount();

        List<Rarity> generatedRarities = RngEngine.generateRaritiesWithPity(
                affixCount,
                Rarity.COMMON,
                0f,
                true, // 开启保底
                equipmentRarity
        );

        EquipCategory category = equipment.getSlot().getCategory();

        for (Rarity targetRarity : generatedRarities) {
            EquipAffixTemplate template = getRandomEquipTemplate(category, targetRarity);
            if (template == null) {
                template = getRandomEquipTemplate(category, null); 
                if (template == null) continue;
            }

            float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
            AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());
            EquipCategory[] categories = getCategoriesFromTemplate(template);

            try {
                Class<?> affixClass = Class.forName(template.getAffixClass());
                BaseEquipAffix affix = (BaseEquipAffix) affixClass.getConstructor(
                        int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, EquipCategory[].class, float.class
                ).newInstance(
                        template.getTemplateId(),
                        template.getAffixName(),
                        template.getDescriptionFormat(),
                        targetRarity, 
                        triggerType,
                        categories,
                        randomValue
                );
                affixList.add(affix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return affixList;
    }

    private EquipAffixTemplate getRandomEquipTemplate(EquipCategory category, Rarity targetRarity) {
        List<EquipAffixTemplate> validTemplates = new ArrayList<>();
        for (EquipAffixTemplate template : templateMap.values()) {
            boolean matchCategory = false;
            String[] cats = template.getAllowCategories();
            if (cats == null || cats.length == 0) {
                matchCategory = true;
            } else {
                for (String sc : cats) {
                    if (sc.equals(category.name())) {
                        matchCategory = true;
                        break;
                    }
                }
            }
            
            boolean matchRarity = true;
            if (targetRarity != null) {
                matchRarity = (template.getRarityId() <= targetRarity.ordinal()); 
            }

            if (matchCategory && matchRarity) {
                validTemplates.add(template);
            }
        }
        if (validTemplates.isEmpty()) return null;
        return validTemplates.get(RandomUtils.getRandomInt(0, validTemplates.size() - 1));
    }

    private EquipCategory[] getCategoriesFromTemplate(EquipAffixTemplate template) {
        String[] cats = template.getAllowCategories();
        if (cats == null) return new EquipCategory[0];
        EquipCategory[] res = new EquipCategory[cats.length];
        for (int i = 0; i < cats.length; i++) {
            res[i] = EquipCategory.valueOf(cats[i]);
        }
        return res;
    }

    private static class ConfigWrapper {
        List<EquipAffixTemplate> affix_templates;
    }
}
