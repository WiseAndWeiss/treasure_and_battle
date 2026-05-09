package com.example.treasure_and_battle.model.affix;

public enum EquipAffixScope {
    GLOBAL,           // 全局属性词缀：直接加到角色最终属性
    EQUIPMENT_ONLY    // 装备自身属性词缀：只加到装备基础属性，通过装备属性影响角色属性
}
