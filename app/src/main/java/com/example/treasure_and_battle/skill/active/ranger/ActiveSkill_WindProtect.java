package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.special.WindProtectBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 御风护体 - 游侠主动技能
 * 效果：下一次受到攻击时，免疫该次攻击全额伤害，并将该次伤害的x%溅射向全场所有敌人
 */
public class ActiveSkill_WindProtect extends ActiveSkill {

    public ActiveSkill_WindProtect(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        float splashDamagePercent = getEffectParams().x; // 溅射伤害百分比

        // 创建御风护体buff（持续时间不衰退，永久存在直到触发）
        WindProtectBuff windProtectBuff = new WindProtectBuff(
                "wind_protect_buff",
                "御风护体",
                "风之屏障护体，将伤害溅射向敌人",
                BuffType.BUFF,
                false, // 不可驱散
                -1,    // 永久持续时间（直到触发）
                1,     // 最大层数
                false, // 不刷新
                0,     // buff值
                caster,
                splashDamagePercent,
                context,
                battleManager
        );

        // 应用buff到施法者
        battleManager.applyBuff(caster, windProtectBuff);
        caster.markAttributeCacheDirty();

        // 记录日志
        context.addLog(LogType.BUFF,
                "【御风护体】[%s] 凝聚风之屏障，下一次受到攻击时免疫伤害并将%.0f%%伤害溅射向全场敌人",
                caster.getName(), splashDamagePercent);
    }
}
