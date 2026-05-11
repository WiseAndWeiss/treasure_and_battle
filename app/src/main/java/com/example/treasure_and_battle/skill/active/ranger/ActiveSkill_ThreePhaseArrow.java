package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 三相箭 - 游侠主动技能
 * 效果：对单个敌人造成x%物理攻击伤害、y%法术攻击伤害与敏捷*z点真实伤害
 */
public class ActiveSkill_ThreePhaseArrow extends ActiveSkill {

    public ActiveSkill_ThreePhaseArrow(SkillTemplate template) {
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
        int physicalDamagePercent = getEffectParams().x; // 物理伤害百分比
        int magicalDamagePercent = getEffectParams().y;  // 法术伤害百分比
        int agilityMultiplier = getEffectParams().z;     // 敏捷倍率

        // 计算物理伤害部分
        int physicalDamage = (int) (caster.getFinalAttributes().physicalAtk * physicalDamagePercent / 100.0f);
        int physicalDamageDealt = battleManager.dealPhysicalDamage(caster, target, physicalDamage, context);

        // 计算法术伤害部分
        int magicalDamage = (int) (caster.getFinalAttributes().magicalAtk * magicalDamagePercent / 100.0f);
        int magicalDamageDealt = battleManager.dealMagicalDamage(caster, target, magicalDamage, context);

        // 计算真实伤害部分（基于敏捷）
        int trueDamage = (int) (caster.getFinalAttributes().agility * agilityMultiplier);
        battleManager.dealTrueDamage(target, trueDamage, context);

        int totalDamage = physicalDamageDealt + magicalDamageDealt + trueDamage;

        // 记录日志
        context.addLog(LogType.DAMAGE,
                "【三相箭】[%s] 对 [%s] 发射三相箭，造成%d点物理伤害、%d点法术伤害、%d点真实伤害，总计%d点伤害",
                caster.getName(), target.getName(), physicalDamageDealt, magicalDamageDealt, trueDamage, totalDamage);
    }
}
