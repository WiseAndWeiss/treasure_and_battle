package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;

public class TriggerDispatcher {

    public static void dispatch(BattleContext ctx, TriggerType type, Context context) {
        if (ctx == null) return;

        for (BattleEntity e : ctx.playerParty) {
            if (e == null || e.isDead()) continue;
            dispatchTo(e, ctx, type, context);
        }

        for (Monster m : ctx.getAliveMonsters()) {
            if (m == null) continue;
            dispatchTo(m, ctx, type, context);
        }
    }

    public static void dispatch(BattleEntity entity, BattleContext ctx, TriggerType type, Context context) {
        if (entity == null || entity.isDead()) return;
        dispatchTo(entity, ctx, type, context);

        if (type == TriggerType.ON_ATTACK && ctx.currentTarget != null && !ctx.currentTarget.isDead()) {
            PassiveSkillManager.getInstance().trigger(ctx.currentTarget, entity, ctx, TriggerType.ON_ATTACKED);
        }
    }

    private static void dispatchTo(BattleEntity entity, BattleContext ctx, TriggerType type, Context context) {
        PassiveSkillManager.getInstance().trigger(entity, ctx, type);
        BuffManager.getInstance(context).triggerBuffs(entity, ctx, type);
        AffixManager.getInstance(context).triggerAffixes(entity, ctx, type);
    }
}
