package com.example.treasure_and_battle.manager.item;

import android.content.Context;
import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.manager.affix.EquipAffixManager;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.equip.ArmorType;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.item.equip.EquipTemplate;
import com.example.treasure_and_battle.model.item.equip.WeaponType;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EquipmentManager {
    public static final double BALANCE_FACTOR = 2.0;
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

    private void loadTemplates() {
        try {
            InputStream is = context.getAssets().open("configs/equip_config.json");
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

    private double calculateBasePower(int level) {
        double power = 0;
        if (level <= 30) {
            power = 10.0 + 0.8 * (level - 1);
        } else {
            double power30 = 10.0 + 0.8 * 29;
            power = power30 * Math.pow((double) level / 30.0, 1.4);
        }
        return power * BALANCE_FACTOR;
    }

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
            slot = EquipSlot.WEAPON;
        }

        EquipItem equip = new EquipItem(template.getEquipId(), template.getName(), rarity, level * 10, level, slot);

        double basePower = calculateBasePower(level);
        double multiplier = getRarityMultiplier(rarity);
        double floatCoefficient = 0.9 + random.nextDouble() * 0.2;
        double finalPower = basePower * multiplier * floatCoefficient;

        AttributeSet attrs = equip.getBaseAttributes();

        switch (slot.getCategory()) {
            case WEAPON:
                applyWeaponAttributes(attrs, template.getWeaponType(), finalPower);
                break;
            case ARMOR:
                applyArmorAttributes(attrs, template.getArmorType(), slot, finalPower);
                break;
            case ACCESSORY:
                applyAccessoryAttributes(attrs, finalPower);
                break;
            default:
                android.util.Log.w("EquipmentManager",
                        "Unknown equip category: " + slot.getCategory() + " for slot " + slot);
                break;
        }

        equip.snapshotRawBaseAttributes();
        List<BaseAffix> baseAffixes = new ArrayList<>(EquipAffixManager.getInstance(context).generateAffixForEquipment(equip));
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

    public int getTemplateIdByEquipId(String equipId) {
        for (Map.Entry<Integer, EquipTemplate> entry : templateMap.entrySet()) {
            if (entry.getValue().getEquipId().equals(equipId)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    // ====================== 武器属性 ======================

    private void applyWeaponAttributes(AttributeSet attrs, String weaponTypeStr, double finalPower) {
        WeaponType weaponType = WeaponType.SWORD;
        if (weaponTypeStr != null) {
            try { weaponType = WeaponType.valueOf(weaponTypeStr); } catch (IllegalArgumentException ignored) {}
        }

        switch (weaponType) {
            case SWORD:
                attrs.physicalAtk = (int) Math.round(finalPower * 0.7);
                attrs.physicalDef = (int) Math.round(finalPower * 0.1);
                attrs.strength = (int) Math.round(finalPower * 0.1);
                break;
            case BOW:
                attrs.physicalAtk = (int) Math.round(finalPower * 0.5);
                attrs.physicalCritRate = (float)(finalPower / (finalPower + 1000));
                attrs.agility = (int) Math.round(finalPower * 0.1);
                break;
            case STAFF:
                attrs.magicalAtk = (int) Math.round(finalPower * 0.7);
                attrs.magicalCritRate = (float)(finalPower / (finalPower + 1000));
                attrs.intelligence = (int) Math.round(finalPower * 0.1);
                break;
        }
    }

    // ====================== 护甲属性 ======================

    private void applyArmorAttributes(AttributeSet attrs, String armorTypeStr, EquipSlot slot, double finalPower) {
        ArmorType armorType = ArmorType.HEAVY;
        if (armorTypeStr != null) {
            try { armorType = ArmorType.valueOf(armorTypeStr); } catch (IllegalArgumentException ignored) {}
        }

        switch (armorType) {
            case HEAVY:
                applyHeavyArmor(attrs, slot, finalPower);
                break;
            case LIGHT:
                applyLightArmor(attrs, slot, finalPower);
                break;
            case CLOTH:
                applyClothArmor(attrs, slot, finalPower);
                break;
        }
    }

    private void applyHeavyArmor(AttributeSet attrs, EquipSlot slot, double p) {
        switch (slot) {
            case CHEST:
                attrs.maxHp = (int) Math.round(p * 0.6);
                attrs.physicalDef = (int) Math.round(p * 0.15);
                attrs.damageReductionRate = (float)(p / (4*p) + 1000);
                attrs.physique = (int) Math.round(p * 0.08);
                break;
            case HELMET:
                attrs.maxHp = (int) Math.round(p * 0.4);
                attrs.physicalDef = (int) Math.round(p * 0.10);
                attrs.damageReductionRate = (float)(p / (4*p) + 1500);
                attrs.physique = (int) Math.round(p * 0.05);
                break;
            case LEGGINGS:
                attrs.maxHp = (int) Math.round(p * 0.45);
                attrs.physicalDef = (int) Math.round(p * 0.12);
                attrs.damageReductionRate = (float)(p / (4*p) + 1000);
                attrs.physique = (int) Math.round(p * 0.06);
                break;
            case BOOTS:
                attrs.maxHp = (int) Math.round(p * 0.3);
                attrs.physicalDef = (int) Math.round(p * 0.08);
                attrs.damageReductionRate = (float)(p / (4*p) + 1500);
                attrs.physique = (int) Math.round(p * 0.04);
                break;
            default: break;
        }
    }

    private void applyLightArmor(AttributeSet attrs, EquipSlot slot, double p) {
        switch (slot) {
            case CHEST:
                attrs.maxHp = (int) Math.round(p * 0.4);
                attrs.physicalDef = (int) Math.round(p * 0.08);
                attrs.magicalDef = (int) Math.round(p * 0.08);
                attrs.dodgeRate = (float)(p / (4*p) + 1000);
                attrs.luck = (int) Math.round(p * 0.08);
                break;
            case HELMET:
                attrs.maxHp = (int) Math.round(p * 0.3);
                attrs.physicalDef = (int) Math.round(p * 0.06);
                attrs.magicalDef = (int) Math.round(p * 0.06);
                attrs.dodgeRate = (float)(p / (4*p) + 1500);
                attrs.luck = (int) Math.round(p * 0.05);
                break;
            case LEGGINGS:
                attrs.maxHp = (int) Math.round(p * 0.35);
                attrs.physicalDef = (int) Math.round(p * 0.07);
                attrs.magicalDef = (int) Math.round(p * 0.07);
                attrs.dodgeRate = (float)(p / (4*p) + 1000);
                attrs.luck = (int) Math.round(p * 0.06);
                break;
            case BOOTS:
                attrs.maxHp = (int) Math.round(p * 0.25);
                attrs.physicalDef = (int) Math.round(p * 0.05);
                attrs.magicalDef = (int) Math.round(p * 0.05);
                attrs.dodgeRate = (float)(p / (4*p) + 1000);
                attrs.luck = (int) Math.round(p * 0.04);
                break;
            default: break;
        }
    }

    private void applyClothArmor(AttributeSet attrs, EquipSlot slot, double p) {
        switch (slot) {
            case CHEST:
                attrs.maxHp = (int) Math.round(p * 0.25);
                attrs.maxMp = (int) Math.round(p * 0.4);
                attrs.magicalDef = (int) Math.round(p * 0.15);
                attrs.debuffResist = (float)(p / (4*p) + 1000);
                attrs.spirit = (int) Math.round(p * 0.08);
                break;
            case HELMET:
                attrs.maxHp = (int) Math.round(p * 0.2);
                attrs.maxMp = (int) Math.round(p * 0.3);
                attrs.magicalDef = (int) Math.round(p * 0.10);
                attrs.debuffResist = (float)(p / (4*p) + 1500);
                attrs.spirit = (int) Math.round(p * 0.05);
                break;
            case LEGGINGS:
                attrs.maxHp = (int) Math.round(p * 0.22);
                attrs.maxMp = (int) Math.round(p * 0.3);
                attrs.magicalDef = (int) Math.round(p * 0.12);
                attrs.debuffResist = (float)(p / (4*p) + 1000);
                attrs.spirit = (int) Math.round(p * 0.06);
                break;
            case BOOTS:
                attrs.maxHp = (int) Math.round(p * 0.18);
                attrs.maxMp = (int) Math.round(p * 0.25);
                attrs.magicalDef = (int) Math.round(p * 0.08);
                attrs.debuffResist = (float)(p / (4*p) + 1000);
                attrs.spirit = (int) Math.round(p * 0.04);
                break;
            default: break;
        }
    }

    // ====================== 饰品属性（随机双属性） ======================

    private static final List<String> ACCESSORY_STAT_POOL = Collections.unmodifiableList(Arrays.asList(
        "strength", "agility", "intelligence", "spirit", "physique", "luck",
        "maxHp", "maxMp", "physicalAtk", "magicalAtk",
        "physicalDef", "magicalDef", "speed",
        "physicalCritRate", "magicalCritRate", "dodgeRate", "hitRate", "debuffResist",
        "damageReductionRate"
    ));

    private void applyAccessoryAttributes(AttributeSet attrs, double finalPower) {
        List<String> pool = new ArrayList<>(ACCESSORY_STAT_POOL);
        Collections.shuffle(pool, random);
        String stat1 = pool.get(0);
        String stat2 = pool.get(1);

        applyAccessoryStat(attrs, stat1, finalPower);
        applyAccessoryStat(attrs, stat2, finalPower);
    }

    private void applyAccessoryStat(AttributeSet attrs, String stat, double p) {
        double ratio = 0.05 + random.nextDouble() * 0.07; // 0.05 ~ 0.12
        switch (stat) {
            case "strength":      attrs.strength = (int) Math.round(p * ratio); break;
            case "agility":       attrs.agility = (int) Math.round(p * ratio); break;
            case "intelligence":  attrs.intelligence = (int) Math.round(p * ratio); break;
            case "spirit":        attrs.spirit = (int) Math.round(p * ratio); break;
            case "physique":      attrs.physique = (int) Math.round(p * ratio); break;
            case "luck":          attrs.luck = (int) Math.round(p * ratio); break;
            case "maxHp":         attrs.maxHp = (int) Math.round(p * (0.2 + random.nextDouble() * 0.3)); break;
            case "maxMp":         attrs.maxMp = (int) Math.round(p * (0.15 + random.nextDouble() * 0.25)); break;
            case "physicalAtk":   attrs.physicalAtk = (int) Math.round(p * (0.15 + random.nextDouble() * 0.2)); break;
            case "magicalAtk":    attrs.magicalAtk = (int) Math.round(p * (0.15 + random.nextDouble() * 0.2)); break;
            case "physicalDef":   attrs.physicalDef = (int) Math.round(p * (0.05 + random.nextDouble() * 0.1)); break;
            case "magicalDef":    attrs.magicalDef = (int) Math.round(p * (0.05 + random.nextDouble() * 0.1)); break;
            case "speed":         attrs.speed = (int) Math.round(p * (0.03 + random.nextDouble() * 0.05)); break;
            case "physicalCritRate":  attrs.physicalCritRate = 0.02f + random.nextFloat() * 0.03f; break;
            case "magicalCritRate":   attrs.magicalCritRate = 0.02f + random.nextFloat() * 0.03f; break;
            case "dodgeRate":         attrs.dodgeRate = 0.02f + random.nextFloat() * 0.03f; break;
            case "hitRate":           attrs.hitRate = 0.02f + random.nextFloat() * 0.03f; break;
            case "debuffResist":      attrs.debuffResist = 0.02f + random.nextFloat() * 0.03f; break;
            case "damageReductionRate": attrs.damageReductionRate = 0.01f + random.nextFloat() * 0.02f; break;
        }
    }

    // ====================== EQUIPMENT_ONLY 词缀应用 ======================

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
