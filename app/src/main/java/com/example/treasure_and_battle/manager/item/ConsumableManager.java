package com.example.treasure_and_battle.manager.item;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.battle.DamageManager;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.BuffEntry;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.DebuffEntry;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.Effect;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem.Target;

public class ConsumableManager {

    public static boolean execute(Player player, BattleContext ctx, ConsumableItem item, Context context) {
        if (player == null || item == null || item.getEffects() == null) return false;

        for (Effect effect : item.getEffects()) {
            boolean ok = dispatch(player, ctx, item, effect, context);
            if (!ok) return false;
        }
        return true;
    }

    private static boolean dispatch(Player player, BattleContext ctx,
                                     ConsumableItem item, Effect e, Context context) {
        switch (e.type) {
            case HEAL_HP: return heal(player, ctx, item, e, context, true);
            case HEAL_MP: return heal(player, ctx, item, e, context, false);
            case DAMAGE:  return damage(player, ctx, e);
            case BUFF:    return buff(player, ctx, e);
            case CLEANSE: return cleanse(player, ctx);
            case ESCAPE:  return escape(ctx);
            case UTILITY: return true;
            default:      return false;
        }
    }

    // ========== HEAL ==========
    private static boolean heal(Player player, BattleContext ctx, ConsumableItem item,
                                 Effect e, Context context, boolean isHp) {
        int maxVal = isHp ? player.getFinalAttributes().maxHp : player.getFinalAttributes().maxMp;
        int amount = e.isPercent ? (int)(maxVal * e.value / 100f) : (int)e.value;
        amount = Math.max(1, amount);

        if (isHp) player.healHp(amount); else player.healMp(amount);

        String label = isHp ? "HP" : "MP";
        if (ctx != null) ctx.addLog(LogType.ACTION, "使用[%s]，恢复 %d %s", item.getName(), amount, label);

        if (isHp && e.shieldDuration > 0) {
            BaseBuff shield = BuffManager.getInstance(context).createBuffByBuffId("buff_shield");
            if (shield != null) {
                shield.setStack(amount);
                shield.setRemainingDuration(e.shieldDuration);
                BuffManager.getInstance(context).addBuff(player, shield);
                if (ctx != null) ctx.addLog(LogType.BUFF,
                    "获得护盾 %d 点，持续%d回合", amount, e.shieldDuration);
            }
        }
        return true;
    }

    // ========== DAMAGE ==========
    private static boolean damage(Player player, BattleContext ctx, Effect e) {
        if (ctx == null) return false;
        AttributeSet attr = player.getFinalAttributes();
        int baseAtk = Math.max(attr.physicalAtk, attr.magicalAtk);
        int dmg = Math.max(1, e.isPercent ? (int)(baseAtk * e.value / 100f) : (int)e.value);

        if (e.target == Target.ALL_ENEMIES) {
            for (Monster m : ctx.getAliveMonsters()) {
                DamageManager.getInstance(player.getContext()).dealDamage(DamageConfig.itemDamage(), null, m, dmg, ctx);
                ctx.addLog(LogType.DAMAGE, "对[%s]造成 %d 伤害", m.getName(), dmg);
                applyDebuffs(ctx, m, dmg, e.debuffs);
            }
        } else {
            Monster target = ctx.getPrimaryMonsterTarget();
            if (target == null) return false;
            DamageManager.getInstance(player.getContext()).dealDamage(DamageConfig.itemDamage(), null, target, dmg, ctx);
            ctx.addLog(LogType.DAMAGE, "对[%s]造成 %d 伤害", target.getName(), dmg);
            applyDebuffs(ctx, target, dmg, e.debuffs);
        }
        return true;
    }

    private static void applyDebuffs(BattleContext ctx, Monster target, int dmg,
                                      java.util.List<DebuffEntry> debuffs) {
        if (debuffs == null || debuffs.isEmpty()) return;
        BuffManager bm = BuffManager.getInstance(target.getContext());
        for (DebuffEntry de : debuffs) {
            int stacks = Math.max(1, (int)(dmg * de.stackRatio));
            BaseBuff debuff = bm.createBuffByTemplateId(de.buffTemplateId);
            if (debuff == null) continue;
            debuff.setStack(stacks);
            bm.addBuff(target, debuff);
            ctx.addLog(LogType.BUFF, "对[%s]施加 [%s] x%d层",
                target.getName(), debuff.getBuffName(), stacks);
        }
    }

    // ========== BUFF ==========
    private static boolean buff(Player player, BattleContext ctx, Effect e) {
        if (ctx == null || e.buffs == null) return false;
        BuffManager bm = BuffManager.getInstance(player.getContext());
        for (BuffEntry be : e.buffs) {
            BaseBuff b = bm.createBuffByTemplateId(be.buffTemplateId);
            if (b == null) continue;
            if (be.stacks > 0) b.setStack(be.stacks);
            if (be.duration > 0) b.setRemainingDuration(be.duration);
            bm.addBuff(player, b);
            ctx.addLog(LogType.BUFF, "获得 [%s] x%d层 持续%d回合",
                b.getBuffName(), b.getStackCount(), b.getRemainingDuration());
        }
        return true;
    }

    // ========== CLEANSE ==========
    private static boolean cleanse(Player player, BattleContext ctx) {
        if (ctx == null) return false;
        BuffManager.getInstance(player.getContext()).dispelBuffs(player, false, true);
        ctx.addLog(LogType.ACTION, "负面效果已清除");
        return true;
    }

    // ========== ESCAPE ==========
    private static boolean escape(BattleContext ctx) {
        if (ctx == null) return false;
        ctx.isBattleEnded = true;
        ctx.battleResult = BattleContext.BattleResult.ESCAPED;
        ctx.addLog(LogType.ACTION, "使用道具逃跑成功！");
        return true;
    }
}
