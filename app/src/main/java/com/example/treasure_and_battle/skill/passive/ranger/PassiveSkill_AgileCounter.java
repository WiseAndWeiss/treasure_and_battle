package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 轻灵反击 - 游侠被动技能
 * 效果：战斗开始时，闪避率提升x%；每次成功闪避攻击时，对攻击者造成y%物理攻击伤害
 */
public class PassiveSkill_AgileCounter extends PassiveSkill {

    private static final String AGILE_COUNTER_BUFF_ID = "agile_counter_dodge_boost";

    public PassiveSkill_AgileCounter(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onBattleStart(BattleEntity entity, BattleContext context) {
        int dodgeBonusPercent = getEffectParams().x;

        // 创建闪避率提升buff（永久持续）
        AttributeBuff dodgeBuff = new AttributeBuff(
                AGILE_COUNTER_BUFF_ID,
                "轻灵反击-闪避",
                "闪避率提升%d%%",
                BuffType.BUFF,
                false, // 不可驱散
                -1,    // 永久持续
                1,     // 最大层数
                false, // 不刷新
                dodgeBonusPercent / 100.0f,  // 转换为小数
                AttributeType.DODGE_RATE,
                ValueType.PERCENTAGE
        );

        BuffManager.getInstance(entity.getContext()).addBuff(entity, dodgeBuff);
        entity.markAttributeCacheDirty();

        context.addLog(LogType.BUFF,
                "【轻灵反击】[%s] 闪避率提升%d%%",
                entity.getName(), dodgeBonusPercent);
    }

    @Override
    public void onDodge(BattleEntity entity, BattleEntity attacker, BattleContext context) {
        BattleManager battleManager = BattleManager.getInstance(entity.getContext());

        int damagePercent = getEffectParams().y;

        int damage = (int) (entity.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
        int actualDamage = battleManager.dealPhysicalDamage(entity, attacker, damage, context);

        context.addLog(LogType.DAMAGE,
                "【轻灵反击】[%s] 闪避后反击，对 [%s] 造成%d点伤害",
                entity.getName(), attacker.getName(), actualDamage);
    }
}
