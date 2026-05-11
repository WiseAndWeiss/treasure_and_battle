package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.buff.impl.skill.FirmAsRockBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 坚如磐石 - 战士主动技能
 * 效果：生成x%最大生命值的护盾，并获得持续两回合物防、法防提升y%
 */
public class ActiveSkill_FirmAsRock extends ActiveSkill {
    public ActiveSkill_FirmAsRock(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int shieldPercent = getEffectParams().x;  // 护盾百分比
        int defenseBoostPercent = getEffectParams().y;  // 双防提升百分比

        // 计算护盾值
        int maxHp = caster.getFinalAttributes().maxHp;
        int shieldValue = (int) (maxHp * shieldPercent / 100.0f);

        // 创建护盾buff（永久，直到被击碎）
        ShieldBuff shieldBuff = new ShieldBuff(
            "firm_as_rock_shield",
            "坚如磐石护盾",
            "吸收%d点伤害",
            BuffType.BUFF,
            false,  // 不可驱散
            -1,    // 永久持续（直到护盾值归零）
            shieldValue,  // maxStackCount作为护盾值
            false,
            1.0f
        );

        // 创建双防提升buff（持续2回合）
        FirmAsRockBuff defenseBuff = new FirmAsRockBuff(
            "firm_as_rock_defense",
            "坚如磐石",
            "物理防御与法术防御提升%d%%",
            BuffType.BUFF,
            true,  // 可驱散
            2,    // 持续2回合
            1,    // 最多1层
            false,
            defenseBoostPercent,
            defenseBoostPercent
        );

        // 应用buff
        battleManager.applyBuff(caster, shieldBuff);
        battleManager.applyBuff(caster, defenseBuff);

        // 记录日志
        context.addLog(LogType.BUFF,
            "【坚如磐石】[%s] 生成了 %d 点护盾，物理防御与法术防御提升 %d%%，持续2回合",
            caster.getName(), shieldValue, defenseBoostPercent);
    }
}
