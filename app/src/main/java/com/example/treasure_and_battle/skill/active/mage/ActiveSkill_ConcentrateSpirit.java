package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 凝神定气技能 - 法师主动技能
 *
 * 技能效果：
 * - 提升自身x点魔法攻击与y点法术暴击率，持续1回合
 * - 无冷却时间
 */
public class ActiveSkill_ConcentrateSpirit extends ActiveSkill {

    public ActiveSkill_ConcentrateSpirit(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        // 发送凝神定气动画信号
        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_concentrate_spirit", casterId, new ArrayList<>(), null)
        );

        BattleContext context = battleManager.getContext();

        int magicalAtkBonus = getEffectParams().x;
        int magicalCritRateBonus = getEffectParams().y;

        // 1. 添加魔法攻击提升buff
        AttributeBuff magicalAtkBuff = new AttributeBuff(
                "concentrate_spirit_magical_atk",
                "凝神定气-魔法攻击",
                "魔法攻击提升%d点",
                BuffType.BUFF,
                true,  // 可驱散
                1,     // 持续1回合
                1,     // 最大层数
                false, // 不刷新
                magicalAtkBonus,
                AttributeType.MAGICAL_ATK,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );

        // 2. 添加法术暴击率提升buff
        AttributeBuff magicalCritRateBuff = new AttributeBuff(
                "concentrate_spirit_magical_crit",
                "凝神定气-法术暴击",
                "法术暴击率提升%d%%",
                BuffType.BUFF,
                true,  // 可驱散
                1,     // 持续1回合
                1,     // 最大层数
                false, // 不刷新
                magicalCritRateBonus,
                AttributeType.MAGICAL_CRIT_RATE,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );

        // 3. 添加buff到玩家
        BuffManager.getInstance(caster.getContext()).addBuff(caster, magicalAtkBuff);
        BuffManager.getInstance(caster.getContext()).addBuff(caster, magicalCritRateBuff);

        // 4. 记录日志
        context.addLog(LogType.BUFF,
                "【凝神定气】[%s] 凝神聚气，魔法攻击提升%d点，法术暴击率提升%d%%，持续1回合！",
                caster.getName(), magicalAtkBonus, magicalCritRateBonus);
    }
}
