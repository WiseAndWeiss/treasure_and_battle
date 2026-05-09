package com.example.treasure_and_battle.skill.passive.general;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 坚韧护体 - 通用被动技能
 * 效果：在战斗中，持续提升{x}%物防，{y}%法防，每回合结束时回复{z}%的最大生命值
 */
public class PassiveSkill_ToughGuard extends PassiveSkill {
    public PassiveSkill_ToughGuard(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void applyAttributeBonus(AttributeSet modifiers) {
        // 提供物防和法防加成
        int physicalDefPercent = getEffectParams().x; // x参数是物防提升百分比
        int magicalDefPercent = getEffectParams().y;  // y参数是法防提升百分比

        modifiers.percentPhysicalDef += physicalDefPercent / 100.0f;
        modifiers.percentMagicalDef += magicalDefPercent / 100.0f;
    }

    @Override
    public void onBattleStart(BattleEntity owner, BattleContext context) {
        // 战斗开始时，永久提升物防和法防
        int defenseBonusPercent = getEffectParams().x; // 物防和法防提升百分比相同

        // 记录日志
        context.addLog(LogType.BUFF,
            "【坚韧护体】[%s] 的防御提升了 %d%%",
            owner.getName(), defenseBonusPercent);

        // 标记属性缓存为脏，触发重新计算
        owner.markAttributeCacheDirty();
    }

    @Override
    public void onRoundEnd(BattleEntity owner, BattleContext context) {
        // 每回合结束时回复生命值
        int healPercent = getEffectParams().z; // z参数是回复百分比
        if (healPercent <= 0) {
            return; // 低等级时可能不回血
        }

        int maxHp = owner.getFinalAttributes().maxHp;
        int healAmount = (int) (maxHp * healPercent / 100.0f);

        if (healAmount > 0 && !owner.isDead()) {
            int hpBefore = owner.getCurrentHp();
            owner.healHp(healAmount);
            int actualHealed = owner.getCurrentHp() - hpBefore;

            if (actualHealed > 0) {
                context.addLog(LogType.HEAL,
                    "【坚韧护体】[%s] 回复了 %d 点生命值（%d%%）",
                    owner.getName(), actualHealed, healPercent);
            }
        }
    }
}
