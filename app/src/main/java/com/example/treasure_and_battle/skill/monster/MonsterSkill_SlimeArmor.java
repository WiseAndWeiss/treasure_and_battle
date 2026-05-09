package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 黏液护甲 (史莱姆专属) - 怪物主动技能
 * 分泌黏液包裹全身，大幅提升双防
 */
public class MonsterSkill_SlimeArmor extends MonsterActiveSkill {
    public MonsterSkill_SlimeArmor(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int physDefPercent = getEffectParams().x;
        int magDefPercent = getEffectParams().y;
        int duration = getEffectParams().z;

        AttributeBuff physDefBuff = new AttributeBuff(
                "slime_armor_pdef", "黏液护甲·物防", "物防提升%d%%",
                BuffType.BUFF, false, duration, 1, false, physDefPercent,
                AttributeType.PHYSICAL_DEF, ValueType.PERCENTAGE);
        battleManager.applyBuff(caster, physDefBuff);

        AttributeBuff magDefBuff = new AttributeBuff(
                "slime_armor_mdef", "黏液护甲·法防", "法防提升%d%%",
                BuffType.BUFF, false, duration, 1, false, magDefPercent,
                AttributeType.MAGICAL_DEF, ValueType.PERCENTAGE);
        battleManager.applyBuff(caster, magDefBuff);

        context.addLog(LogType.BUFF,
                "【黏液护甲】[%s] 分泌黏液护甲，物防+%d%% 法防+%d%% 持续 %d 回合",
                caster.getName(), physDefPercent, magDefPercent, duration);
    }
}
