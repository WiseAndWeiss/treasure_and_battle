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
        return gem;
    }

    private void applyBonus(com.example.treasure_and_battle.model.attribute.AttributeSet attr,
                            String type, float value) {
        switch (type) {
            case "STRENGTH":          attr.strength = (int)value; break;
            case "AGILITY":           attr.agility = (int)value; break;
            case "INTELLIGENCE":      attr.intelligence = (int)value; break;
            case "SPIRIT":            attr.spirit = (int)value; break;
            case "PHYSIQUE":          attr.physique = (int)value; break;
            case "LUCK":              attr.luck = (int)value; break;
            case "PHYSICAL_ATK":      attr.physicalAtk = (int)value; break;
            case "MAGICAL_ATK":       attr.magicalAtk = (int)value; break;
            case "PHYSICAL_DEF":      attr.physicalDef = (int)value; break;
            case "MAGICAL_DEF":       attr.magicalDef = (int)value; break;
            case "SPEED":             attr.speed = (int)value; break;
            case "MAX_HP":            attr.maxHp = (int)value; break;
            case "MAX_MP":            attr.maxMp = (int)value; break;
            case "PHYSICAL_CRIT_RATE":  attr.physicalCritRate = value; break;
            case "MAGICAL_CRIT_RATE":   attr.magicalCritRate = value; break;
            case "PHYSICAL_CRIT_DMG":   attr.physicalCritDmg = value; break;
            case "MAGICAL_CRIT_DMG":    attr.magicalCritDmg = value; break;
            case "HIT_RATE":          attr.hitRate = value; break;
            case "DODGE_RATE":        attr.dodgeRate = value; break;
            case "DEBUFF_RESIST":     attr.debuffResist = value; break;
            case "GOLD_BONUS":        attr.goldBonus = value; break;
            case "EXP_BONUS":         attr.expBonus = value; break;
            case "PHYSICAL_AND_MAGICAL_CRIT_RATE":
                attr.physicalCritRate = value;
                attr.magicalCritRate = value;
                break;
            default: break;
        }
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
