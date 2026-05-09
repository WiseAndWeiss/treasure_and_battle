package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 冰霜屏障 (冰灵专属) - 怪物主动技能
 * 凝结冰霜铸成坚壁，获得护盾并提升法防
 */
public class MonsterSkill_IceBarrier extends MonsterActiveSkill {
    public MonsterSkill_IceBarrier(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int shieldPercent = getEffectParams().x;
        int magDefPercent = getEffectParams().y;
        int duration = getEffectParams().z;

        int maxHp = caster.getFinalAttributes().maxHp;
        int shieldValue = (int) (maxHp * shieldPercent / 100.0f);

        ShieldBuff shieldBuff = new ShieldBuff(
                "ice_barrier_shield", "冰霜屏障", "吸收%d点伤害",
                BuffType.BUFF, false, duration, shieldValue, false, 1.0f);
        battleManager.applyBuff(caster, shieldBuff);

        AttributeBuff magDefBuff = new AttributeBuff(
                "ice_barrier_mdef", "冰霜屏障·法防", "法防提升%d%%",
                BuffType.BUFF, false, duration, 1, false, magDefPercent,
                AttributeType.MAGICAL_DEF, ValueType.PERCENTAGE);
        battleManager.applyBuff(caster, magDefBuff);

        context.addLog(LogType.BUFF,
                "【冰霜屏障】[%s] 冰霜凝结，生成了 %d 点护盾，法防+%d%% 持续 %d 回合",
                caster.getName(), shieldValue, magDefPercent, duration);
    }
}
