package com.example.treasure_and_battle.model;

import java.util.ArrayList;
import java.util.List;

// 装备子类：继承自Item，严格对应9+装备槽位与文档设定
public class Equipment extends Item {
    // ====================== 装备槽位常量（按设计补充9大槽位体系） ======================
    public static final int SLOT_WEAPON = 0;      // 武器
    public static final int SLOT_HELMET = 1;      // 头盔
    public static final int SLOT_CHEST = 2;       // 胸甲
    public static final int SLOT_LEGGINGS = 3;    // 护腿
    public static final int SLOT_BOOTS = 4;       // 鞋子
    public static final int SLOT_NECKLACE = 5;    // 项链
    public static final int SLOT_BRACELET = 6;    // 手镯
    public static final int SLOT_RING = 7;        // 戒指（装备可以装在任意一个戒指槽）

    // ====================== 武器与护甲细分类型（MD文档增补） ======================
    public static final int SUBTYPE_WEAPON_SWORD = 10; // 近战物理(剑)
    public static final int SUBTYPE_WEAPON_BOW = 11;   // 远程物理(弓)
    public static final int SUBTYPE_WEAPON_STAFF = 12; // 法术武器(法杖)
    public static final int SUBTYPE_ARMOR_HEAVY = 20;  // 重甲(最高物防)
    public static final int SUBTYPE_ARMOR_LIGHT = 21;  // 轻甲(相对均衡)
    public static final int SUBTYPE_ARMOR_CLOTH = 22;  // 布甲(魔防倾向)
    public static final int SUBTYPE_ACCESSORY = 30;    // 饰品

    // ====================== 装备专属成长属性 ======================
    private int slotType;           // 装备槽位（对应上面的SLOT常量）
    private int subType;            // 【新增】装备细分类型（对应上面的SUBTYPE常量）
    private int equipmentLevel;     // 【新增】装备等级（随玩家等级浮动，影响基础数值运算规则）
    private int requireLevel;       // 穿戴等级要求
    private int[] attrBonus;        // 属性加成数组： [力量,敏捷,智力,精神,体魄,幸运] 或 其他直接战斗属性

    // ====================== 附属养成属性（词条与宝石体系） ======================
    private List<Affix> affixes;    // 【新增】词条列表（固定词条数量受Rarity品质影响）
    private Gem[] socketedGems;     // 【新增】宝石槽数组（槽位数量受Rarity品质影响：白1/绿2/蓝3/紫4/橙5）

    // 构造函数
    public Equipment(int itemId, String itemName, Rarity rarity, int iconResId, String description, 
                     int baseValue, int slotType, int subType, int equipmentLevel, int requireLevel, int[] attrBonus) {
        // 装备类型固定为TYPE_EQUIPMENT，堆叠上限固定为1（不可堆叠）
        super(itemId, itemName, TYPE_EQUIPMENT, rarity, iconResId, description, 1, baseValue);
        this.slotType = slotType;
        this.subType = subType;
        this.equipmentLevel = equipmentLevel;
        this.requireLevel = requireLevel;
        this.attrBonus = attrBonus;
        
        // 根据MD文档规则：读取我们刚才写好的Rarity枚举常量，初始化词条列表容量与宝石槽孔位
        this.affixes = new ArrayList<>(rarity.getAffixCount());
        this.socketedGems = new Gem[rarity.getGemSlotCount()];
    }

    // ====================== Getter & Setter ======================
    public int getSlotType() { return slotType; }
    public void setSlotType(int slotType) { this.slotType = slotType; }
    public int getSubType() { return subType; }
    public void setSubType(int subType) { this.subType = subType; }
    public int getEquipmentLevel() { return equipmentLevel; }
    public void setEquipmentLevel(int equipmentLevel) { this.equipmentLevel = equipmentLevel; }
    public int getRequireLevel() { return requireLevel; }
    public void setRequireLevel(int requireLevel) { this.requireLevel = requireLevel; }
    public int[] getAttrBonus() { return attrBonus; }
    public void setAttrBonus(int[] attrBonus) { this.attrBonus = attrBonus; }
    
    public List<Affix> getAffixes() { return affixes; }
    public void setAffixes(List<Affix> affixes) { this.affixes = affixes; }
    public Gem[] getSocketedGems() { return socketedGems; }
    public void setSocketedGems(Gem[] socketedGems) { this.socketedGems = socketedGems; }
}
