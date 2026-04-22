package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRecoverAffix;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.affix.MonsterAffixTemplate;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
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

    private MonsterAffixTemplate getRandomTemplate() {
        List<MonsterAffixTemplate> validTemplates = new ArrayList<>(templateMap.values());
        if (validTemplates.isEmpty()) return null;
        return validTemplates.get(RandomUtils.getRandomInt(0, validTemplates.size() - 1));
    }

    private BaseMonsterAffix createAffixFromTemplate(MonsterAffixTemplate template, Rarity rarity, float randomValue) {
        AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());
        try {
            Class<?> affixClass = Class.forName(template.getAffixClass());
            if (affixClass == MonsterAttributeAffix.class) {
                AttributeType attributeType = AttributeType.valueOf(template.getAttributeType());
                ValueType valueType = parseValueType(template.getValueType());
                return (BaseMonsterAffix) affixClass.getConstructor(
                    int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, float.class,
                    AttributeType.class, ValueType.class
                ).newInstance(
                    template.getTemplateId(),
                    template.getAffixName(),
                    template.getDescriptionFormat(),
                    rarity,
                    triggerType,
                    randomValue,
                    attributeType,
                    valueType
                );
            }

            if (affixClass == MonsterTriggerBuffAffix.class) {
                Integer buffTemplateId = template.getBuffTemplateId();
                if (buffTemplateId == null) {
                    throw new IllegalArgumentException("MonsterTriggerBuffAffix template missing buffTemplateId: " + template.getTemplateId());
                }

                AffixBuffApplyTarget applyTarget = parseApplyTarget(template.getApplyTarget());
                int applyStacks = template.getApplyStacks() == null ? 1 : Math.max(1, template.getApplyStacks());
                float damageToStackRatio = template.getDamageToStackRatio() == null
                    ? 0f
                    : Math.max(0f, template.getDamageToStackRatio());

                return (BaseMonsterAffix) affixClass.getConstructor(
                    int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, float.class,
                    int.class, AffixBuffApplyTarget.class, int.class, float.class
                ).newInstance(
                    template.getTemplateId(),
                    template.getAffixName(),
                    template.getDescriptionFormat(),
                    rarity,
                    triggerType,
                    randomValue,
                    buffTemplateId,
                    applyTarget,
                    applyStacks,
                    damageToStackRatio
                );
            }

            if (affixClass == MonsterTriggerRecoverAffix.class) {
                AffixRecoverResourceType recoverResourceType = parseRecoverResourceType(template.getRecoverResourceType());
                ValueType recoverValueType = parseValueType(template.getRecoverValueType());
                int recoverValue = template.getRecoverValue() == null ? 0 : Math.max(0, template.getRecoverValue());
                float damageToRecoverRatio = template.getDamageToRecoverRatio() == null
                    ? 0f
                    : Math.max(0f, template.getDamageToRecoverRatio());

                return (BaseMonsterAffix) affixClass.getConstructor(
                    int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, float.class,
                    AffixRecoverResourceType.class, ValueType.class, int.class, float.class
                ).newInstance(
                    template.getTemplateId(),
                    template.getAffixName(),
                    template.getDescriptionFormat(),
                    rarity,
                    triggerType,
                    randomValue,
                    recoverResourceType,
                    recoverValueType,
                    recoverValue,
                    damageToRecoverRatio
                );
            }

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

    private ValueType parseValueType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return ValueType.FLAT;
        }
        return ValueType.valueOf(rawType.trim().toUpperCase());
    }

    private static class ConfigWrapper {
        List<MonsterAffixTemplate> affix_templates;
    }
}
