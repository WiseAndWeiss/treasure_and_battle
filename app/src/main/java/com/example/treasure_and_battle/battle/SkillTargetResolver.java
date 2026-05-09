package com.example.treasure_and_battle.battle;

import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.skill.SkillRangeType;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能目标解析工具
 * 根据技能范围类型和施法者自动决定目标列表，后续扩展新范围类型只需加 case
 */
public class SkillTargetResolver {

    public static List<BattleEntity> resolve(SkillRangeType rangeType,
                                              BattleEntity caster,
                                              BattleContext ctx) {
        List<BattleEntity> targets = new ArrayList<>();

        switch (rangeType) {
            case SELF:
                targets.add(caster);
                break;

            case SINGLE_ENEMY:
                if (ctx.currentTarget != null && !ctx.currentTarget.isDead()
                        && ctx.currentTarget != caster) {
                    targets.add(ctx.currentTarget);
                } else if (caster instanceof Player) {
                    List<Monster> alive = ctx.getAliveMonsters();
                    if (!alive.isEmpty()) targets.add(alive.get(0));
                } else {
                    targets.add(ctx.player);
                }
                break;

            case ALL_ENEMIES:
                if (caster instanceof Player) {
                    targets.addAll(ctx.getAliveMonsters());
                } else {
                    targets.addAll(ctx.getAlivePlayerParty());
                }
                break;

            case ALL_ALLIES:
                if (caster instanceof Player) {
                    targets.addAll(ctx.getAlivePlayerParty());
                } else {
                    targets.addAll(ctx.getAliveMonsters());
                }
                break;

            case NONE:
                break;
        }

        return targets;
    }
}
