package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 斩击 - 战士主动技能
 * 效果：对目标造成{x}%物理攻击伤害，附加{y}%无视防御与护盾的破甲伤害
 */
public class ActiveSkill_Slash extends ActiveSkill {
    public ActiveSkill_Slash(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x;  // 物理伤害百分比
        int piercingPercent = getEffectParams().y;  // 破甲伤害百分比

        // 分别计算物理伤害和破甲伤害
        int physicalDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
        int piercingDamage = (int) (caster.getFinalAttributes().physicalAtk * piercingPercent / 100.0f);

        // 造成物理伤害（会被防御和护盾抵消）
        int physicalDamageDealt = battleManager.dealPhysicalDamage(caster, target, physicalDamage, context);

        // 造成破甲伤害（无视防御和护盾）
        int piercingDamageDealt = battleManager.dealPiercingDamage(caster, target, piercingDamage, context);

        int totalDamage = physicalDamageDealt + piercingDamageDealt;

        // 记录日志
        context.addLog(LogType.DAMAGE,
            "【斩击】[%s] 对 [%s] 造成 %d 点伤害（物理%d + 真实伤害%d）",
            caster.getName(), target.getName(), totalDamage, physicalDamageDealt, piercingDamageDealt);
    }
}
