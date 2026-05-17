package com.example.treasure_and_battle.ui;

import androidx.annotation.NonNull;

import com.example.treasure_and_battle.buff.BaseBuff;

/**
 * 战斗 UI 用 Buff 文案（效果描述、层数、持续时间）。
 */
public final class BuffUiText {

    private BuffUiText() {
    }

    @NonNull
    public static String effectDescription(@NonNull BaseBuff buff) {
        try {
            String text = buff.getDescription();
            if (text != null && !text.isEmpty()) {
                return text;
            }
        } catch (Exception ignored) {
            // format 与数值不匹配时回退
        }
        return buff.getBuffName();
    }

    @NonNull
    public static String detailBody(@NonNull BaseBuff buff) {
        StringBuilder sb = new StringBuilder();
        sb.append("效果：").append(effectDescription(buff));
        sb.append("\n层数：").append(Math.max(0, buff.getStackCount()));
        int dur = buff.getRemainingDuration();
        if (dur > 0) {
            sb.append("\n剩余：").append(dur).append(" 回合");
        } else if (dur < 0) {
            sb.append("\n持续：永久");
        }
        return sb.toString();
    }

    @NonNull
    public static String durationSuffix(@NonNull BaseBuff buff) {
        int dur = buff.getRemainingDuration();
        if (dur > 0) {
            return " · " + dur + " 回合";
        }
        if (dur < 0) {
            return " · 永久";
        }
        return "";
    }
}
