package com.example.treasure_and_battle.ui;

import androidx.annotation.Nullable;

import com.example.treasure_and_battle.battle.BattleContext;

/**
 * 暂存「进行中的战斗」：从战斗页返回或切换底栏时不销毁上下文，下次进入可继续；
 * 仅在战斗以胜利、失败、成功逃跑等方式结束后 {@link #clear()}，下次进入开启新战斗。
 */
public final class BattleSessionHolder {

    private static BattleContext suspended;

    private BattleSessionHolder() {}

    @Nullable
    public static BattleContext getSuspended() {
        return suspended;
    }

    public static void setSuspended(@Nullable BattleContext ctx) {
        suspended = ctx;
    }

    public static void clear() {
        suspended = null;
    }

    /** 已结束但仍占位的上下文一律丢弃，避免漏调 clear 导致异常状态。 */
    public static void discardIfEnded() {
        if (suspended != null && suspended.isBattleEnded) {
            suspended = null;
        }
    }

    public static boolean hasResumableBattle() {
        return suspended != null && !suspended.isBattleEnded;
    }
}
