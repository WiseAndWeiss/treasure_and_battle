package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 巡回狩猎 - 游侠被动技能
 * 效果：每回合开始时，对场上当前生命值比例最高的敌人造成x%物理攻击伤害，并将造成伤害的y%转化为自身生命值治疗
 */
public class PassiveSkill_HuntCircuit extends PassiveSkill {

    public PassiveSkill_HuntCircuit(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onRoundStart(BattleEntity entity, BattleContext context) {
        BattleManager battleManager = BattleManager.getInstance(entity.getContext());

        int damagePercent = getEffectParams().x;
        int healPercent = getEffectParams().y;

        // 获取所有敌方单位
        List<BattleEntity> enemies = getEnemies(entity, context);

        if (enemies.isEmpty()) {
            return;
        }

        // 找到当前生命值比例最高的敌人
        BattleEntity target = enemies.stream()
                .filter(e -> e.getCurrentHp() > 0)
                .max(Comparator.comparingDouble(e -> (double) e.getCurrentHp() / e.getFinalAttributes().maxHp))
                .orElse(null);

        if (target == null) {
            return;
        }

        // 造成伤害
        int damage = (int) (entity.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
        int actualDamage = battleManager.dealPhysicalDamage(entity, target, damage, context);

        // 治疗自身
        int healAmount = (int) (actualDamage * healPercent / 100.0f);
        if (healAmount > 0) {
            int oldHp = entity.getCurrentHp();
            entity.setCurrentHp(Math.min(entity.getCurrentHp() + healAmount, entity.getFinalAttributes().maxHp));
            int actualHeal = entity.getCurrentHp() - oldHp;

            context.addLog(LogType.HEAL,
                    "【巡回狩猎】[%s] 狩猎 [%s]，造成%d点伤害，回复%d点生命",
                    entity.getName(), target.getName(), actualDamage, actualHeal);
        } else {
            context.addLog(LogType.DAMAGE,
                    "【巡回狩猎】[%s] 狩猎 [%s]，造成%d点伤害",
                    entity.getName(), target.getName(), actualDamage);
        }
    }

    /**
     * 根据实体类型获取敌方单位列表
     */
    private List<BattleEntity> getEnemies(BattleEntity entity, BattleContext context) {
        boolean isPlayerParty = context.playerParty.contains(entity);
        if (isPlayerParty) {
            return context.monsters.stream()
                    .filter(m -> m != null && !m.isDead())
                    .collect(Collectors.toList());
        } else {
            return context.playerParty.stream()
                    .filter(p -> p != null && !p.isDead())
                    .collect(Collectors.toList());
        }
    }
}
