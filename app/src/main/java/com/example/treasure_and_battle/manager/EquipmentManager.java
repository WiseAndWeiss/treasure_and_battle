package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.model.item.EquipSlot;
import com.example.treasure_and_battle.model.item.EquipTemplate;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.manager.EquipAffixManager;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EquipmentManager {
    private static EquipmentManager instance;
    private final Context context;
    private final Random random;
    private final Map<Integer, EquipTemplate> templateMap = new HashMap<>();

    private EquipmentManager(Context context) {
        this.context = context.getApplicationContext();
        this.random = new Random();
        loadTemplates();
    }

    public static synchronized EquipmentManager getInstance(Context context) {
        if (instance == null) {
            instance = new EquipmentManager(context);
        }
        return instance;
    }

    // ====================== 1. 加载装备模板 ======================
    private void loadTemplates() {
        try {
            InputStream is = context.getAssets().open("equip_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            EquipConfigWrapper wrapper = gson.fromJson(json, EquipConfigWrapper.class);

            if (wrapper != null && wrapper.equip_templates != null) {
                for (EquipTemplate template : wrapper.equip_templates) {
                    templateMap.put(template.getTemplateId(), template);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 根据数学模型计算不同等级的“核心威力积分(Power)”
    private double calculateBasePower(int level) {
        if (level <= 30) {
            return 10.0 + 0.8 * (level - 1);
        } else {
            double power30 = 10.0 + 0.8 * 29;
            return power30 * Math.pow((double) level / 30.0, 1.4);
        }
    }

    // 根据品质倍率转换
    private double getRarityMultiplier(Rarity rarity) {
        switch (rarity) {
            case COMMON: return 1.0;
            case UNCOMMON: return 1.25;
            case RARE: return 1.5;
            case EPIC: return 1.75;
            case LEGENDARY: return 2.0;
            default: return 1.0;
        }
    }

    public EquipItem generateEquip(int templateId, int level, Rarity rarity) {
        EquipTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        EquipSlot slot;
        try {
            slot = EquipSlot.valueOf(template.getSlot());
        } catch (IllegalArgumentException e) {
            slot = EquipSlot.WEAPON; // fallback
        }

        EquipItem equip = new EquipItem(template.getEquipId(), template.getName(), rarity, level * 10, level, slot);

        // 核心属性分配
        double basePower = calculateBasePower(level);
        double multiplier = getRarityMultiplier(rarity);

        // 浮动系数 0.9 ~ 1.1
        double floatCoefficient = 0.9 + random.nextDouble() * 0.2;
        double finalPower = basePower * multiplier * floatCoefficient;

        AttributeSet attrs = equip.getBaseAttributes();

        // 装备属性基准映射
        switch (slot) {
            case WEAPON:
                attrs.physicalAtk = (int) Math.round(finalPower * 0.8);
                break;
            case HELMET:
            case CHEST:
            case LEGGINGS:
            case BOOTS:
                // 防具
                attrs.maxHp = (int) Math.round(finalPower * 0.4);
                attrs.physicalDef = (int) Math.round(finalPower * 0.1);
                attrs.magicalDef = (int) Math.round(finalPower * 0.1);
                break;
            case NECKLACE:
            case RING:
            case BRACELET:
                // 饰品
                attrs.maxHp = (int) Math.round(finalPower * 0.4);
                attrs.strength = (int) Math.round(finalPower * 0.05);
                attrs.physique = (int) Math.round(finalPower * 0.05);
                break;
        }

        // 附加装备词缀系统，并与属性引擎解耦（交给EquipAffixManager和保底引擎去生成分配）
        List<BaseAffix> baseAffixes = new java.util.ArrayList<>(EquipAffixManager.getInstance(context).generateAffixForEquipment(equip));
        equip.setAffixes(baseAffixes);

        return equip;
    }

    private static class EquipConfigWrapper {
        List<EquipTemplate> equip_templates;
    }
}
