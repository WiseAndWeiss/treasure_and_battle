package com.example.treasure_and_battle.manager.item;

import android.content.Context;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.consumable.ConsumableTemplate;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.gem.GemTemplate;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.material.MaterialItem;
import com.example.treasure_and_battle.model.item.material.MaterialTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.example.treasure_and_battle.utils.RandomUtils;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private static ItemManager instance;
    private Context context;
    private Gson gson;

    private Map<String, MaterialTemplate> materialTemplates = new HashMap<>();
    private Map<String, ConsumableTemplate> consumableTemplates = new HashMap<>();
    private Map<String, GemTemplate> gemTemplates = new HashMap<>();

    private ItemManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadMaterialTemplates();
        loadConsumableTemplates();
        loadGemTemplates();
    }

    public static synchronized ItemManager getInstance(Context context) {
        if (instance == null) {
            instance = new ItemManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    // ====================== 加载模板 ======================

    private void loadMaterialTemplates() {
        try {
            InputStream is = context.getAssets().open("material_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<MaterialConfigWrapper>() {}.getType();
            MaterialConfigWrapper wrapper = gson.fromJson(json, type);
            if (wrapper != null && wrapper.material_templates != null) {
                for (MaterialTemplate t : wrapper.material_templates) {
                    materialTemplates.put(t.getMaterialId(), t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConsumableTemplates() {
        try {
            InputStream is = context.getAssets().open("consumable_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<ConsumableConfigWrapper>() {}.getType();
            ConsumableConfigWrapper wrapper = gson.fromJson(json, type);
            if (wrapper != null && wrapper.consumable_templates != null) {
                for (ConsumableTemplate t : wrapper.consumable_templates) {
                    consumableTemplates.put(t.getConsumableId(), t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGemTemplates() {
        try {
            InputStream is = context.getAssets().open("gem_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<GemConfigWrapper>() {}.getType();
            GemConfigWrapper wrapper = gson.fromJson(json, type);
            if (wrapper != null && wrapper.gem_templates != null) {
                for (GemTemplate t : wrapper.gem_templates) {
                    gemTemplates.put(t.getGemId(), t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== 创建物品（统一入口） ======================

    public Item createItem(String itemId) {
        if (itemId == null) return null;

        MaterialTemplate mat = materialTemplates.get(itemId);
        if (mat != null) return createMaterial(mat);

        ConsumableTemplate con = consumableTemplates.get(itemId);
        if (con != null) return createConsumable(con);

        GemTemplate gem = gemTemplates.get(itemId);
        if (gem != null) return createGem(gem);

        return null;
    }

    public MaterialItem createMaterial(String materialId) {
        MaterialTemplate t = materialTemplates.get(materialId);
        return t == null ? null : createMaterial(t);
    }

    public ConsumableItem createConsumable(String consumableId) {
        ConsumableTemplate t = consumableTemplates.get(consumableId);
        return t == null ? null : createConsumable(t);
    }

    public GemItem createGem(String gemId) {
        GemTemplate t = gemTemplates.get(gemId);
        return t == null ? null : createGem(t);
    }

    // ====================== 内部创建方法 ======================

    private MaterialItem createMaterial(MaterialTemplate t) {
        return new MaterialItem(t.getMaterialId(), t.getName(),
                Rarity.fromId(t.getRarityId()), t.getBaseValue(),
                t.getMaxStack(), t.getDropFrom());
    }

    private ConsumableItem createConsumable(ConsumableTemplate t) {
        return new ConsumableItem(t.getConsumableId(), t.getName(),
                Rarity.fromId(t.getRarityId()), t.getBaseValue(),
                t.getMaxStack(), t.isUsableInBattle(), t.isUsableOutBattle(),
                t.getEffects(),
                t.getDescription());
    }

    private GemItem createGem(GemTemplate t) {
        GemItem gem = new GemItem(t.getGemId(), t.getName(),
                Rarity.fromId(t.getRarityId()), t.getBaseValue(),
                t.getGemType());

        if (t.getAccessoryBonuses() != null) {
            for (GemTemplate.BonusEntry be : t.getAccessoryBonuses()) {
                applyBonus(gem.getAccessoryBonus(), be.type, be.value);
            }
        }
        if (t.getWeaponBonuses() != null) {
            for (GemTemplate.BonusEntry be : t.getWeaponBonuses()) {
                applyBonus(gem.getWeaponBonus(), be.type, be.value);
            }
        }
        if (t.getArmorBonuses() != null) {
            for (GemTemplate.BonusEntry be : t.getArmorBonuses()) {
                applyBonus(gem.getArmorBonus(), be.type, be.value);
            }
        }
        gem.setDescription(buildGemDescription(gem));
        return gem;
    }

    private String buildGemDescription(GemItem gem) {
        StringBuilder sb = new StringBuilder();
        String w = formatAttrSetBonus(gem.getWeaponBonus());
        String a = formatAttrSetBonus(gem.getArmorBonus());
        String ac = formatAttrSetBonus(gem.getAccessoryBonus());
        if (!w.isEmpty()) sb.append("· 镶嵌在武器上时，").append(w).append("\n");
        if (!a.isEmpty()) sb.append("· 镶嵌在护甲上时，").append(a).append("\n");
        if (!ac.isEmpty()) sb.append("· 镶嵌在饰品上时，").append(ac);
        return sb.toString().trim();
    }

    private String formatAttrSetBonus(com.example.treasure_and_battle.model.attribute.AttributeSet attr) {
        java.util.LinkedHashMap<String, String> labels = new java.util.LinkedHashMap<>();
        labels.put("strength", "力量");
        labels.put("agility", "敏捷");
        labels.put("intelligence", "智力");
        labels.put("spirit", "精神");
        labels.put("physique", "体魄");
        labels.put("luck", "幸运");
        labels.put("maxHp", "生命上限");
        labels.put("maxMp", "法力上限");
        labels.put("physicalAtk", "物理攻击力");
        labels.put("magicalAtk", "法术攻击力");
        labels.put("physicalDef", "物理防御");
        labels.put("magicalDef", "法术防御");
        labels.put("speed", "速度");
        labels.put("physicalCritRate", "物理暴击率");
        labels.put("magicalCritRate", "魔法暴击率");
        labels.put("physicalCritDmg", "物理暴伤");
        labels.put("magicalCritDmg", "魔法暴伤");
        labels.put("hitRate", "命中率");
        labels.put("dodgeRate", "闪避率");
        labels.put("debuffResist", "异常抵抗");
        labels.put("damageReductionRate", "伤害减免");
        labels.put("goldBonus", "金币加成");
        labels.put("expBonus", "经验加成");

        java.util.Set<String> pctAttrs = new java.util.HashSet<>(java.util.Arrays.asList(
                "physicalCritRate", "magicalCritRate", "physicalCritDmg", "magicalCritDmg",
                "hitRate", "dodgeRate", "debuffResist", "damageReductionRate"));

        for (java.lang.reflect.Field field : com.example.treasure_and_battle.model.attribute.AttributeSet.class.getFields()) {
            String name = field.getName();
            if (!labels.containsKey(name)) continue;
            try {
                Object val = field.get(attr);
                if (val instanceof Integer && (Integer) val != 0) {
                    return labels.get(name) + " + " + val;
                } else if (val instanceof Float && Math.abs((Float) val) > 0.0001f) {
                    if (pctAttrs.contains(name)) {
                        return labels.get(name) + " + " + String.format(java.util.Locale.CHINA, "%.1f%%", (Float) val * 100f);
                    }
                    return labels.get(name) + " + " + String.format(java.util.Locale.CHINA, "%.1f", val);
                }
            } catch (IllegalAccessException ignored) {}
        }
        return "";
    }

    private void applyBonus(com.example.treasure_and_battle.model.attribute.AttributeSet attr,
                            String type, float value) {
        com.example.treasure_and_battle.utils.AttributeUtils.applyBonusToAttrSet(attr, type, value);
    }

    // ====================== 查询 ======================

    public MaterialTemplate getMaterialTemplate(String materialId) {
        return materialTemplates.get(materialId);
    }

    public ConsumableTemplate getConsumableTemplate(String consumableId) {
        return consumableTemplates.get(consumableId);
    }

    public GemTemplate getGemTemplate(String gemId) {
        return gemTemplates.get(gemId);
    }

    public List<GemTemplate> getAllGemTemplates() {
        return new ArrayList<>(gemTemplates.values());
    }

    public List<ConsumableTemplate> getAllConsumableTemplates() {
        return new ArrayList<>(consumableTemplates.values());
    }

    public List<MaterialTemplate> getAllMaterialTemplates() {
        return new ArrayList<>(materialTemplates.values());
    }

    // ====================== 按稀有度随机选取 ======================

    public ConsumableItem getRandomConsumableByRarity(Rarity rarity) {
        List<ConsumableTemplate> pool = new ArrayList<>();
        for (ConsumableTemplate t : consumableTemplates.values()) {
            if (t.getRarityId() == rarity.getId()) {
                pool.add(t);
            }
        }
        if (pool.isEmpty()) return null;
        ConsumableTemplate t = pool.get(RandomUtils.getRandomInt(0, pool.size() - 1));
        return createConsumable(t);
    }

    public GemItem getRandomGemByRarity(Rarity rarity) {
        List<GemTemplate> pool = new ArrayList<>();
        for (GemTemplate t : gemTemplates.values()) {
            if (t.getRarityId() == rarity.getId()) {
                pool.add(t);
            }
        }
        if (pool.isEmpty()) return null;
        GemTemplate t = pool.get(RandomUtils.getRandomInt(0, pool.size() - 1));
        return createGem(t);
    }

    public EquipItem getRandomEquipByRarity(int level, Rarity rarity, Context context) {
        return EquipmentManager.getInstance(context).generateRandomEquip(level, rarity);
    }

    // ====================== JSON 包装类 ======================

    private static class MaterialConfigWrapper {
        List<MaterialTemplate> material_templates;
    }

    private static class ConsumableConfigWrapper {
        List<ConsumableTemplate> consumable_templates;
    }

    private static class GemConfigWrapper {
        List<GemTemplate> gem_templates;
    }
}
