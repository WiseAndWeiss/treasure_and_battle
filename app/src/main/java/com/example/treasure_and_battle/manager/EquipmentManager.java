package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.model.item.EquipSlot;
import com.example.treasure_and_battle.model.item.EquipTemplate;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.manager.EquipAffixManager;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.ArrayList;
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
        int iconRes = resolveEquipIconRes(template.getEquipId());
        if (iconRes != 0) {
            equip.setIconResId(iconRes);
        }

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
        applyEquipmentOnlyAffixes(equip, baseAffixes);
        equip.setAffixes(baseAffixes);

        return equip;
    }

    public EquipItem generateRandomEquip(int level, Rarity rarity) {
        if (templateMap.isEmpty()) return null;
        List<EquipTemplate> templates = new ArrayList<>(templateMap.values());
        EquipTemplate template = templates.get(random.nextInt(templates.size()));
        return generateEquip(template.getTemplateId(), level, rarity);
    }

    private void applyEquipmentOnlyAffixes(EquipItem equip, List<BaseAffix> affixes) {
        if (equip == null || affixes == null || affixes.isEmpty()) {
            return;
        }

        AttributeSet equipmentOnlyModifiers = new AttributeSet();
        boolean hasEquipmentOnlyAffix = false;

        for (BaseAffix affix : affixes) {
            if (!(affix instanceof EquipAttributeAffix)) {
                continue;
            }

            EquipAttributeAffix equipAffix = (EquipAttributeAffix) affix;
            if (equipAffix.getAffixScope() != EquipAffixScope.EQUIPMENT_ONLY) {
                continue;
            }

            equipAffix.applyToEquipmentAttributeBonus(equipmentOnlyModifiers);
            hasEquipmentOnlyAffix = true;
        }

        if (!hasEquipmentOnlyAffix) {
            return;
        }

        applyModifiersToEquipmentBaseAttributes(equip.getBaseAttributes(), equipmentOnlyModifiers);
    }

    private static int resolveEquipIconRes(String equipId) {
        if (equipId == null) {
            return 0;
        }
        switch (equipId) {
            case "equip_weapon_sword_iron":
                return R.drawable.iron_sword_icon;
            case "equip_armor_chest_leather":
                return R.drawable.ic_backpack;
            case "equip_accessory_ring_iron":
                return R.drawable.ic_config;
            default:
                return R.drawable.ic_map;
        }
    }

    private void applyModifiersToEquipmentBaseAttributes(AttributeSet base, AttributeSet modifiers) {
        base.strength = (int) (base.strength * (1f + modifiers.percentStrength)) + modifiers.strength;
        base.agility = (int) (base.agility * (1f + modifiers.percentAgility)) + modifiers.agility;
        base.intelligence = (int) (base.intelligence * (1f + modifiers.percentIntelligence)) + modifiers.intelligence;
        base.spirit = (int) (base.spirit * (1f + modifiers.percentSpirit)) + modifiers.spirit;
        base.physique = (int) (base.physique * (1f + modifiers.percentPhysique)) + modifiers.physique;
        base.luck = (int) (base.luck * (1f + modifiers.percentLuck)) + modifiers.luck;

        base.maxHp = (int) (base.maxHp * (1f + modifiers.percentMaxHp)) + modifiers.maxHp;
        base.maxMp = (int) (base.maxMp * (1f + modifiers.percentMaxMp)) + modifiers.maxMp;
        base.maxActionPoints += modifiers.maxActionPoints;

        base.physicalAtk = (int) (base.physicalAtk * (1f + modifiers.percentPhysicalAtk)) + modifiers.physicalAtk;
        base.magicalAtk = (int) (base.magicalAtk * (1f + modifiers.percentMagicalAtk)) + modifiers.magicalAtk;
        base.physicalDef = (int) (base.physicalDef * (1f + modifiers.percentPhysicalDef)) + modifiers.physicalDef;
        base.magicalDef = (int) (base.magicalDef * (1f + modifiers.percentMagicalDef)) + modifiers.magicalDef;
        base.speed = (int) (base.speed * (1f + modifiers.percentSpeed)) + modifiers.speed;

        base.physicalCritRate += modifiers.physicalCritRate;
        base.physicalCritDmg += modifiers.physicalCritDmg;
        base.magicalCritRate += modifiers.magicalCritRate;
        base.magicalCritDmg += modifiers.magicalCritDmg;
        base.hitRate += modifiers.hitRate;
        base.dodgeRate += modifiers.dodgeRate;
        base.debuffResist += modifiers.debuffResist;
        base.damageReductionRate += modifiers.damageReductionRate;
    }



    private static class EquipConfigWrapper {
        List<EquipTemplate> equip_templates;
    }
}
