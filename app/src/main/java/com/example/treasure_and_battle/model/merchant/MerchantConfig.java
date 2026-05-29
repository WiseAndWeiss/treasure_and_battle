package com.example.treasure_and_battle.model.merchant;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.ItemType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 商人配置与商品生成引擎
 *
 * <p>四种商人类型：WANDERING_VENDOR / EQUIPMENT_MERCHANT / CARAVAN / MATERIAL_MERCHANT
 */
public final class MerchantConfig {

    public enum Type {
        WANDERING_VENDOR,
        EQUIPMENT_MERCHANT,
        CARAVAN,
        MATERIAL_MERCHANT
    }

    public static final int SLOTS = 8;

    private MerchantConfig() {}

    // ==================== 商品生成规则 ====================

    @FunctionalInterface
    public interface SlotRule {
        MerchantSlot generate(int slotIndex);
    }

    public static class MerchantSlot {
        public final ItemType itemType;
        public final Rarity rarity;
        public final int slotIndex;

        MerchantSlot(ItemType itemType, Rarity rarity, int slotIndex) {
            this.itemType = itemType;
            this.rarity = rarity;
            this.slotIndex = slotIndex;
        }
    }

    public static List<MerchantSlot> generateSlots(Type type, Random rng) {
        List<MerchantSlot> slots = new ArrayList<>();
        SlotRule rule = getSlotRule(type);
        for (int i = 0; i < SLOTS; i++) {
            MerchantSlot slot = rule.generate(i);
            if (slot != null) slots.add(slot);
        }
        return slots;
    }

    private static SlotRule getSlotRule(Type type) {
        switch (type) {
            case WANDERING_VENDOR: return WANDERING_VENDOR_RULE;
            case EQUIPMENT_MERCHANT: return EQUIPMENT_MERCHANT_RULE;
            case CARAVAN: return CARAVAN_RULE;
            case MATERIAL_MERCHANT: return MATERIAL_MERCHANT_RULE;
            default: throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    // ==================== 流浪商贩 ====================

    private static final Rarity[] WANDERING_RARITY_POOL = {
            Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE
    };

    private static final SlotRule WANDERING_VENDOR_RULE = (index) -> {
        Rarity r = WANDERING_RARITY_POOL[index % WANDERING_RARITY_POOL.length];
        ItemType[] types = {ItemType.EQUIPMENT, ItemType.GEM, ItemType.CONSUMABLE};
        ItemType t = types[index % types.length];
        return new MerchantSlot(t, r, index);
    };

    public static Rarity rollWanderingVendorRarity(Random rng) {
        double roll = rng.nextDouble();
        if (roll < 0.50) return Rarity.COMMON;
        if (roll < 0.75) return Rarity.UNCOMMON;
        return Rarity.RARE;
    }

    public static ItemType rollWanderingVendorType(Random rng) {
        ItemType[] types = {ItemType.EQUIPMENT, ItemType.GEM, ItemType.CONSUMABLE};
        return types[rng.nextInt(types.length)];
    }

    // ==================== 装备商人 ====================

    private static final Rarity[] EQUIP_MERCHANT_POOL = {
            Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY
    };

    private static final SlotRule EQUIPMENT_MERCHANT_RULE = (index) -> {
        Rarity r;
        if (index == 0) {
            r = Rarity.LEGENDARY;
        } else if (index >= SLOTS - 2) {
            r = Rarity.COMMON;
        } else {
            r = EQUIP_MERCHANT_POOL[index % EQUIP_MERCHANT_POOL.length];
        }
        return new MerchantSlot(ItemType.EQUIPMENT, r, index);
    };

    /**
     * 装备商人非固定栏位概率分布：传说3% 史诗12% 罕见20% 稀有25% 普通40%
     */
    public static Rarity rollEquipmentMerchantRarity(Random rng) {
        double roll = rng.nextDouble();
        if (roll < 0.03) return Rarity.LEGENDARY;
        if (roll < 0.15) return Rarity.EPIC;
        if (roll < 0.35) return Rarity.RARE;
        if (roll < 0.60) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    // ==================== 商队 ====================

    private static final Rarity[] CARAVAN_POOL = {
            Rarity.RARE, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY
    };

    private static final SlotRule CARAVAN_RULE = (index) -> {
        Rarity r;
        if (index == 0) {
            return new MerchantSlot(ItemType.GEM, Rarity.LEGENDARY, index);
        } else if (index == 1) {
            return new MerchantSlot(ItemType.CONSUMABLE, Rarity.LEGENDARY, index);
        } else {
            r = CARAVAN_POOL[index % CARAVAN_POOL.length];
            ItemType[] types = {ItemType.EQUIPMENT, ItemType.GEM, ItemType.CONSUMABLE};
            ItemType t = types[index % types.length];
            return new MerchantSlot(t, r, index);
        }
    };

    /**
     * 商队非固定栏位概率分布：传说10% 史诗20% 罕见30% 稀有40%
     */
    public static Rarity rollCaravanRarity(Random rng) {
        double roll = rng.nextDouble();
        if (roll < 0.10) return Rarity.LEGENDARY;
        if (roll < 0.30) return Rarity.EPIC;
        if (roll < 0.60) return Rarity.RARE;
        return Rarity.UNCOMMON;
    }

    public static ItemType rollCaravanType(Random rng) {
        ItemType[] types = {ItemType.EQUIPMENT, ItemType.GEM, ItemType.CONSUMABLE};
        return types[rng.nextInt(types.length)];
    }

    // ==================== 材料商人 ====================

    private static final Rarity[] MATERIAL_MERCHANT_POOL = {
            Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY
    };

    private static final SlotRule MATERIAL_MERCHANT_RULE = (index) -> {
        if (index == 0) {
            return new MerchantSlot(ItemType.GEM, Rarity.LEGENDARY, index);
        } else if (index == 1) {
            return new MerchantSlot(ItemType.CONSUMABLE, Rarity.LEGENDARY, index);
        } else {
            Rarity r = MATERIAL_MERCHANT_POOL[index % MATERIAL_MERCHANT_POOL.length];
            ItemType[] types = {ItemType.GEM, ItemType.CONSUMABLE};
            ItemType t = types[index % types.length];
            return new MerchantSlot(t, r, index);
        }
    };

    /**
     * 材料商人非固定栏位概率分布：传说3% 史诗12% 罕见20% 稀有25% 普通40%
     */
    public static Rarity rollMaterialMerchantRarity(Random rng) {
        return rollEquipmentMerchantRarity(rng);
    }

    // ==================== 背包过滤规则 ====================

    public static List<ItemType> getPlayerBagFilterTypes(Type type) {
        switch (type) {
            case WANDERING_VENDOR:
                return Arrays.asList(ItemType.EQUIPMENT, ItemType.GEM, ItemType.CONSUMABLE, ItemType.MATERIAL);
            case EQUIPMENT_MERCHANT:
                return Collections.singletonList(ItemType.EQUIPMENT);
            case CARAVAN:
                return Arrays.asList(ItemType.EQUIPMENT, ItemType.GEM, ItemType.CONSUMABLE, ItemType.MATERIAL);
            case MATERIAL_MERCHANT:
                return Arrays.asList(ItemType.GEM, ItemType.CONSUMABLE, ItemType.MATERIAL);
            default:
                return Arrays.asList(ItemType.EQUIPMENT, ItemType.GEM, ItemType.CONSUMABLE, ItemType.MATERIAL);
        }
    }

    // ==================== 交易限制 ====================

    public static boolean canBuy(Type type) {
        return true;
    }

    public static boolean canSell(Type type) {
        switch (type) {
            case WANDERING_VENDOR:
                return false;
            case EQUIPMENT_MERCHANT:
                return true;
            case CARAVAN:
                return true;
            case MATERIAL_MERCHANT:
                return true;
            default:
                return false;
        }
    }

    public static String getSellRejectionMessage(Type type) {
        if (type == Type.WANDERING_VENDOR) {
            return "小贩表示不需要这些东西，你无法出售";
        }
        return null;
    }

    // ==================== 装备等级范围 ====================

    public static int getEquipMinLevel(Type type) {
        return 5;
    }

    public static int getEquipMaxLevel(Type type) {
        return 25;
    }

    // ==================== 名称与描述 ====================

    public static String getEventKey(Type type) {
        switch (type) {
            case WANDERING_VENDOR: return "wandering_vendor";
            case EQUIPMENT_MERCHANT: return "equipment_merchant";
            case CARAVAN: return "caravan";
            case MATERIAL_MERCHANT: return "material_merchant";
            default: return "merchant";
        }
    }

    public static String getName(Type type) {
        switch (type) {
            case WANDERING_VENDOR: return "流浪商贩";
            case EQUIPMENT_MERCHANT: return "装备商人";
            case CARAVAN: return "商队";
            case MATERIAL_MERCHANT: return "材料商人";
            default: return "商人";
        }
    }

    public static String getDesc(Type type) {
        switch (type) {
            case WANDERING_VENDOR: return "遇到一位流浪商贩，出售品质不高于罕见的装备、宝石和药水";
            case EQUIPMENT_MERCHANT: return "遇到一位装备商人，专门出售各类装备";
            case CARAVAN: return "遇到一支商队，出售品质不低于稀有的装备、宝石和药水";
            case MATERIAL_MERCHANT: return "遇到一位材料商人，出售宝石和药水，收购材料";
            default: return "遇到一位商人";
        }
    }
}
