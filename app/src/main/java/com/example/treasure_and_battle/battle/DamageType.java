package com.example.treasure_and_battle.battle;

public enum DamageType {
    PHYSICAL,   // 物理伤害（命中→暴击→防御→减伤→护盾→扣血）
    MAGICAL,    // 魔法伤害（同上，用魔法攻防）
    TRUE,       // 真实伤害（只做闪避判定，无视一切防御/减伤/护盾）
    PIERCING    // 穿甲伤害（无视防御和减伤，但受护盾吸收）
}
