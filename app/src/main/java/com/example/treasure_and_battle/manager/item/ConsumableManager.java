package com.example.treasure_and_battle.manager.item;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.character.Character;
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
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.utils.AttributeUtils;

import java.util.List;

public class ConsumableManager {

    // ===================== 战斗内使用 =====================

    public static boolean execute(Player player, BattleContext ctx, ConsumableItem item, Context context) {
        if (player == null || item == null || item.getEffects() == null || item.getEffects().isEmpty()) return false;

        for (Effect effect : item.getEffects()) {
            boolean ok = dispatchInBattle(player, ctx, item, effect, context);
            if (!ok) return false;
        }
        return true;
    }

    private static boolean dispatchInBattle(Player player, BattleContext ctx,
                                             ConsumableItem item, Effect e, Context context) {
        if (e == null || e.type == null) return false;
        switch (e.type) {
            case HEAL_HP: return heal(player, ctx, item, e, context, true);
            case HEAL_MP: return heal(player, ctx, item, e, context, false);
            case DAMAGE:  return damage(player, ctx, e);
            case BUFF:    return buff(player, ctx, e);
            case CLEANSE: return cleanse(player, ctx);
            case ESCAPE:  return escape(ctx);
            case UTILITY: return utilityInBattle(ctx, e);
            default:      return false;
        }
    }

    // ===================== 战斗外使用 =====================

    public static boolean executeOutBattle(Character character, ConsumableItem item, Context context) {
        if (character == null || item == null || item.getEffects() == null || item.getEffects().isEmpty())
            return false;

        for (Effect effect : item.getEffects()) {
            boolean ok = dispatchOutBattle(character, item, effect, context);
            if (!ok) return false;
        }
        return true;
    }

    private static boolean dispatchOutBattle(Character character, ConsumableItem item, Effect e, Context context) {
        if (e == null || e.type == null) return false;
        switch (e.type) {
            case HEAL_HP: return healOutBattle(character, item, e, true);
            case HEAL_MP: return healOutBattle(character, item, e, false);
            case UTILITY: return utilityOutBattle(character, item, e, context);
            default:      return false;
        }
    }

    // ===================== 局外 HEAL =====================

    private static boolean healOutBattle(Character character, ConsumableItem item, Effect e, boolean isHp) {
        AttributeSet attr = AttributeUtils.calculateCharacterAttributes(character);
        int maxVal = isHp ? attr.maxHp : attr.maxMp;
        boolean isPercent = "PERCENTAGE".equals(e.valueType);
        int amount = isPercent ? (int) (maxVal * e.value / 100f) : (int) e.value;
        amount = Math.max(1, amount);

        if (isHp) {
            character.setCurrentHp(Math.min(character.getCurrentHp() + amount, maxVal));
        } else {
            character.setCurrentMp(Math.min(character.getCurrentMp() + amount, maxVal));
        }
        return true;
    }

    // ===================== 局外 UTILITY =====================

    private static boolean utilityOutBattle(Character character, ConsumableItem item, Effect e, Context context) {
        String uid = e.utilityId;
        if (uid == null) return false;

        switch (uid) {
            case "KEY_COPPER":
            case "KEY_SILVER":
            case "KEY_GOLD":
                return true;
            case "UNSOCKET_GEM":
                return true;
            case "POLISH_EQUIP":
                return true;
            case "RESET_TALENTS":
                character.resetAllTalentPoints();
                character.resetAllSkills();
                return true;
            case "REFRESH_EVENTS":
                return true;
            default:
                return false;
        }
    }

    // ===================== 局内 UTILITY =====================

    private static boolean utilityInBattle(BattleContext ctx, Effect e) {
        return true;
    }

    // ===================== 局内 HEAL =====================

    private static boolean heal(Player player, BattleContext ctx, ConsumableItem item,
                                 Effect e, Context context, boolean isHp) {
        int maxVal = isHp ? player.getFinalAttributes().maxHp : player.getFinalAttributes().maxMp;
        boolean isPercent = ValueType.PERCENTAGE.name().equals(e.valueType);
        int amount = isPercent ? (int)(maxVal * e.value / 100f) : (int)e.value;
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

    // ===================== 局内 DAMAGE =====================

    private static boolean damage(Player player, BattleContext ctx, Effect e) {
        if (ctx == null) return false;
        AttributeSet attr = player.getFinalAttributes();
        int baseAtk = Math.max(attr.physicalAtk, attr.magicalAtk);
        boolean isPercent = ValueType.PERCENTAGE.name().equals(e.valueType);
        int dmg = Math.max(1, isPercent ? (int)(baseAtk * e.value / 100f) : (int)e.value);

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
                                      List<DebuffEntry> debuffs) {
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

    // ===================== 局内 BUFF =====================

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

    // ===================== 局内 CLEANSE =====================

    private static boolean cleanse(Player player, BattleContext ctx) {
        if (ctx == null) return false;
        BuffManager.getInstance(player.getContext()).dispelBuffs(player, false, true);
        ctx.addLog(LogType.ACTION, "负面效果已清除");
        return true;
    }

    // ===================== 局内 ESCAPE =====================

    private static boolean escape(BattleContext ctx) {
        if (ctx == null) return false;
        ctx.isBattleEnded = true;
        ctx.battleResult = BattleContext.BattleResult.ESCAPED;
        ctx.addLog(LogType.ACTION, "使用道具逃跑成功！");
        return true;
    }
}
