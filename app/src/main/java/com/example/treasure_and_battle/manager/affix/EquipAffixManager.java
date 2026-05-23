package com.example.treasure_and_battle.manager.affix;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.affix.EquipAffixFactory;
import com.example.treasure_and_battle.utils.RngEngine;

import com.example.treasure_and_battle.model.affix.EquipAffixTemplate;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.utils.RandomUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 装备词缀管理器。
 * 职责：
 * 1. 从配置加载装备词缀模板。
 * 2. 按装备品质与槽位规则生成随机词缀。
 */
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
            // 启动时一次性加载词缀模板，后续生成流程仅走内存映射，避免重复IO。
            InputStream is = context.getAssets().open("equip_affix_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<ConfigWrapper>() {}.getType();
            ConfigWrapper wrapper = gson.fromJson(json, type);
            for (EquipAffixTemplate template : wrapper.affix_templates) {
                // templateId 作为唯一键，便于后续快速查找/扩展。
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

        // 先确定每条词缀目标稀有度：数量由装备品质决定，且支持保底机制。
        List<Rarity> generatedRarities = RngEngine.generateRaritiesWithPity(
                affixCount,
                Rarity.COMMON,
                0f,
                true, // 开启保底
                equipmentRarity
        );

        EquipCategory category = equipment.getSlot().getCategory();

        for (Rarity targetRarity : generatedRarities) {
            // 优先按目标稀有度筛模板，若没有可选模板则降级为“仅按槽位筛”。
            EquipAffixTemplate template = getRandomEquipTemplate(category, targetRarity);
            if (template == null) {
                template = getRandomEquipTemplate(category, null); 
                if (template == null) continue;
            }

            float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
            EquipCategory[] categories = getCategoriesFromTemplate(template);

            BaseEquipAffix affix = EquipAffixFactory.create(template, targetRarity, template.getTriggerType(), categories, randomValue);
            if (affix != null) {
                affixList.add(affix);
            }
        }

        return affixList;
    }

    public BaseEquipAffix generateSingleAffixForEquipment(EquipItem equipment) {
        Rarity equipmentRarity = equipment.getRarity();
        List<Rarity> generatedRarities = RngEngine.generateRaritiesWithPity(
                1, Rarity.COMMON, 0f, true, equipmentRarity);

        EquipCategory category = equipment.getSlot().getCategory();

        for (Rarity targetRarity : generatedRarities) {
            EquipAffixTemplate template = getRandomEquipTemplate(category, targetRarity);
            if (template == null) {
                template = getRandomEquipTemplate(category, null);
                if (template == null) return null;
            }

            float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
            EquipCategory[] categories = getCategoriesFromTemplate(template);

            BaseEquipAffix affix = EquipAffixFactory.create(template, targetRarity, template.getTriggerType(), categories, randomValue);
            if (affix != null) {
                return affix;
            }
        }
        return null;
    }

    private EquipAffixTemplate getRandomEquipTemplate(EquipCategory category, Rarity targetRarity) {
        List<EquipAffixTemplate> validTemplates = new ArrayList<>();
        for (EquipAffixTemplate template : templateMap.values()) {
            // allowCategories 为空表示“全槽位通用”。
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
                // 模板 rarityId 小于等于目标稀有度即视为可投放。
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
        // 构建词缀实例时将字符串槽位转为枚举，避免运行时频繁解析。
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
