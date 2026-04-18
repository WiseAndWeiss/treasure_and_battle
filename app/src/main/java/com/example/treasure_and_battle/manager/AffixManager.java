package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixTemplate;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.item.Equipment;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.utils.AttributeUtils;
import com.example.treasure_and_battle.utils.RandomUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 词缀统一管理类：单例
public class AffixManager {
    private static AffixManager instance;
    private Context context;
    private Gson gson;

    // 词缀模板库：key=模板ID，value=模板
    private Map<Integer, AffixTemplate> templateMap = new HashMap<>();

    private AffixManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadAffixTemplates();
    }

    public static synchronized AffixManager getInstance(Context context) {
        if (instance == null) {
            instance = new AffixManager(context);
        }
        return instance;
    }

    // ====================== 1. 加载词缀模板 ======================
    private void loadAffixTemplates() {
        try {
            InputStream is = context.getAssets().open("affix_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<AffixConfigWrapper>() {}.getType();
            AffixConfigWrapper wrapper = gson.fromJson(json, type);
            for (AffixTemplate template : wrapper.affix_templates) {
                templateMap.put(template.getTemplateId(), template);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== 2. 生成词缀实例（提供精确创建、随机生成等方式） ======================
    public BaseAffix createAffixByTemplateId(int templateId) {
        AffixTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
        Rarity rarity = Rarity.fromId(template.getRarityId());
        AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());

        try {
            Class<?> affixClass = Class.forName(template.getAffixClass());
            return (BaseAffix) affixClass.getConstructor(
                    int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, int[].class, float.class
            ).newInstance(
                    template.getTemplateId(),
                    template.getAffixName(),
                    template.getDescriptionFormat(),
                    rarity,
                    triggerType,
                    template.getAllowSlots(),
                    randomValue
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 给装备生成随机词缀
    public List<BaseAffix> generateAffixForEquipment(Equipment equipment) {
        List<BaseAffix> affixList = new ArrayList<>();
        Rarity equipmentRarity = equipment.getRarity();
        // 按装备稀有度获取词缀数量（和之前的Rarity系统联动）
        int affixCount = equipmentRarity.getAffixCount();

        for (int i = 0; i < affixCount; i++) {
            // 随机选一个符合部位的词缀模板
            AffixTemplate template = getRandomTemplate(equipment.getSlotType());
            if (template == null) continue;

            // 随机生成词缀数值
            float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
            Rarity rarity = Rarity.fromId(template.getRarityId());
            AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());

            // 反射生成词缀实例
            try {
                Class<?> affixClass = Class.forName(template.getAffixClass());
                BaseAffix affix = (BaseAffix) affixClass.getConstructor(
                        int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, int[].class, float.class
                ).newInstance(
                        template.getTemplateId(),
                        template.getAffixName(),
                        template.getDescriptionFormat(),
                        rarity,
                        triggerType,
                        template.getAllowSlots(),
                        randomValue
                );
                affixList.add(affix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return affixList;
    }

    // ====================== 新增：给怪物生成随机词缀 ======================
    public List<BaseAffix> generateAffixForMonster(Monster monster) {
        List<BaseAffix> affixList = new ArrayList<>();
        Rarity monsterRarity = monster.getRarity();
        // 按怪物稀有度获取词缀数量（可以和装备共用Rarity的affixCount，也可以单独定义）
        int affixCount = monsterRarity.getAffixCount();

        for (int i = 0; i < affixCount; i++) {
            // 随机选一个怪物词缀模板
            AffixTemplate template = getRandomMonsterTemplate();
            if (template == null) continue;

            // 随机生成词缀数值（怪物词缀可以用单独的数值范围，这里复用模板的min/max）
            float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
            Rarity rarity = Rarity.fromId(template.getRarityId());
            AffixTriggerType triggerType = AffixTriggerType.valueOf(template.getTriggerType());

            // 反射生成词缀实例（和装备词缀完全一样的逻辑）
            try {
                Class<?> affixClass = Class.forName(template.getAffixClass());
                BaseAffix affix = (BaseAffix) affixClass.getConstructor(
                        int.class, String.class, String.class, Rarity.class, AffixTriggerType.class, int[].class, float.class
                ).newInstance(
                        template.getTemplateId(),
                        template.getAffixName(),
                        template.getDescriptionFormat(),
                        rarity,
                        triggerType,
                        template.getAllowSlots(),
                        randomValue
                );
                affixList.add(affix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return affixList;
    }

    // ====================== 3. 词缀生效/失效管理 ======================
    public void addAffix(BattleEntity entity, BaseAffix affix) {
        if (entity instanceof com.example.treasure_and_battle.model.entity.Player) {
            // Player 目前没有装备词缀列表。可以通过扩展使其包含与怪物同源或独有的 List
            // 目前先复用 entity.getMonsterAffixList() 或者是需要特殊对待
            // TODO: 未来完善 Player 的全局词缀容器支持
        } else if (entity instanceof com.example.treasure_and_battle.model.entity.Monster) {
            ((com.example.treasure_and_battle.model.entity.Monster) entity).addAffix(affix);
        }
    }

    public void removeAffix(BattleEntity entity, int affixId) {
        if (entity instanceof com.example.treasure_and_battle.model.entity.Monster) {
            List<BaseAffix> affixes = entity.getMonsterAffixList();
            affixes.removeIf(affix -> affix.getAffixId() == affixId);
            ((com.example.treasure_and_battle.model.entity.Monster) entity).setAffixes(affixes);
        }
    }

    // ====================== 4. 给属性系统提供词缀加成 ======================
    public void applyAllPermanentAffixBonus(AttributeSet attributeSet, BattleEntity entity) {
        List<BaseAffix> affixes = entity.getMonsterAffixList(); // 根据你的设计，BattleEntity 里有 monsterAffixList 字段或通用 getAffixes
        for (BaseAffix affix : affixes) {
            if (affix.getTriggerType() == AffixTriggerType.PERMANENT) {
                affix.applyAttributeBonus(attributeSet);
            }
        }
    }

    // ====================== 5. 战斗系统中统一触发当前实体的各时机词缀 ======================
    public void triggerAffixes(BattleEntity entity, BattleContext context, AffixTriggerType triggerType) {
        List<BaseAffix> affixes = entity.getMonsterAffixList();
        for (BaseAffix affix : affixes) {
            if (affix.getTriggerType() == triggerType) {
                affix.onTrigger(context);
            }
        }
    }

    // ====================== 内部工具方法 ======================
    // 随机获取符合部位的词缀模板
    private AffixTemplate getRandomTemplate(int slotType) {
        List<AffixTemplate> validTemplates = new ArrayList<>();
        for (AffixTemplate template : templateMap.values()) {
            // 全部位可用，或者符合当前装备部位
            if (template.getAllowSlots().length == 0 || contains(template.getAllowSlots(), slotType)) {
                validTemplates.add(template);
            }
        }
        if (validTemplates.isEmpty()) return null;
        return validTemplates.get(RandomUtils.getRandomInt(0, validTemplates.size() - 1));
    }

    // ====================== 内部工具方法：随机获取怪物词缀模板 ======================
    private AffixTemplate getRandomMonsterTemplate() {
        List<AffixTemplate> validTemplates = new ArrayList<>();
        for (AffixTemplate template : templateMap.values()) {
            // 只筛选怪物词缀
            if ("MONSTER".equals(template.getAffixType())) {
                validTemplates.add(template);
            }
        }
        if (validTemplates.isEmpty()) return null;
        return validTemplates.get(RandomUtils.getRandomInt(0, validTemplates.size() - 1));
    }

    private boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) return true;
        }
        return false;
    }

    // 配置文件包装类
    private static class AffixConfigWrapper {
        List<AffixTemplate> affix_templates;
    }
}