package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRecoverAffix;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;
import com.example.treasure_and_battle.core.RngEngine;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.affix.EquipAffixTemplate;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
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
            AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());
            EquipCategory[] categories = getCategoriesFromTemplate(template);

            try {
                // 使用模板中的实现类反射构建词缀实例，保证配置可扩展。
                Class<?> affixClass = Class.forName(template.getAffixClass());
                BaseEquipAffix affix;
                if (affixClass == EquipAttributeAffix.class) {
                    AttributeType attributeType = AttributeType.valueOf(template.getAttributeType());
                    ValueType valueType = ValueType.valueOf(template.getValueType());
                    EquipAffixScope affixScope = parseAffixScope(template.getAffixScope());

                    affix = (BaseEquipAffix) affixClass.getConstructor(
                        int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, EquipCategory[].class, float.class,
                        AttributeType.class, ValueType.class, EquipAffixScope.class
                    ).newInstance(
                        template.getTemplateId(),
                        template.getAffixName(),
                        template.getDescriptionFormat(),
                        targetRarity,
                        triggerType,
                        categories,
                        randomValue,
                        attributeType,
                        valueType,
                        affixScope
                    );
                } else if (affixClass == EquipTriggerBuffAffix.class) {
                    Integer buffTemplateId = template.getBuffTemplateId();
                    if (buffTemplateId == null) {
                        throw new IllegalArgumentException("EquipTriggerBuffAffix template missing buffTemplateId: " + template.getTemplateId());
                    }

                    AffixBuffApplyTarget applyTarget = parseApplyTarget(template.getApplyTarget());
                    int applyStacks = template.getApplyStacks() == null ? 1 : Math.max(1, template.getApplyStacks());
                    float damageToStackRatio = template.getDamageToStackRatio() == null
                        ? 0f
                        : Math.max(0f, template.getDamageToStackRatio());

                    affix = (BaseEquipAffix) affixClass.getConstructor(
                        int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, EquipCategory[].class, float.class,
                        int.class, AffixBuffApplyTarget.class, int.class, float.class
                    ).newInstance(
                        template.getTemplateId(),
                        template.getAffixName(),
                        template.getDescriptionFormat(),
                        targetRarity,
                        triggerType,
                        categories,
                        randomValue,
                        buffTemplateId,
                        applyTarget,
                        applyStacks,
                        damageToStackRatio
                    );
                } else if (affixClass == EquipTriggerRecoverAffix.class) {
                    AffixRecoverResourceType recoverResourceType = parseRecoverResourceType(template.getRecoverResourceType());
                    ValueType recoverValueType = parseRecoverValueType(template.getRecoverValueType());
                    int recoverValue = template.getRecoverValue() == null ? 0 : Math.max(0, template.getRecoverValue());
                    float damageToRecoverRatio = template.getDamageToRecoverRatio() == null
                        ? 0f
                        : Math.max(0f, template.getDamageToRecoverRatio());

                    affix = (BaseEquipAffix) affixClass.getConstructor(
                        int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, EquipCategory[].class, float.class,
                        AffixRecoverResourceType.class, ValueType.class, int.class, float.class
                    ).newInstance(
                        template.getTemplateId(),
                        template.getAffixName(),
                        template.getDescriptionFormat(),
                        targetRarity,
                        triggerType,
                        categories,
                        randomValue,
                        recoverResourceType,
                        recoverValueType,
                        recoverValue,
                        damageToRecoverRatio
                    );
                } else {
                    affix = (BaseEquipAffix) affixClass.getConstructor(
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
                }
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

    private EquipAffixScope parseAffixScope(String rawScope) {
        if (rawScope == null || rawScope.trim().isEmpty()) {
            return EquipAffixScope.GLOBAL;
        }

        String normalized = rawScope.trim().toUpperCase();
        if ("EQUIP_ONLY".equals(normalized)) {
            return EquipAffixScope.EQUIPMENT_ONLY;
        }
        return EquipAffixScope.valueOf(normalized);
    }

    private AffixBuffApplyTarget parseApplyTarget(String rawTarget) {
        if (rawTarget == null || rawTarget.trim().isEmpty()) {
            return AffixBuffApplyTarget.TARGET;
        }
        return AffixBuffApplyTarget.valueOf(rawTarget.trim().toUpperCase());
    }

    private AffixRecoverResourceType parseRecoverResourceType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return AffixRecoverResourceType.HP;
        }
        return AffixRecoverResourceType.valueOf(rawType.trim().toUpperCase());
    }

    private ValueType parseRecoverValueType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return ValueType.FLAT;
        }
        return ValueType.valueOf(rawType.trim().toUpperCase());
    }

    private static class ConfigWrapper {
        List<EquipAffixTemplate> affix_templates;
    }
}
