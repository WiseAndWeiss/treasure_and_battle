package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.treasure_and_battle.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 技能列表「查看详情」：与背包物品详情同一套面板风格。
 */
public final class SkillDetailDialog {

    private SkillDetailDialog() {}

    /** 展示用数据（可与后续真实 Skill 模型对齐）。 */
    public static final class Detail {
        @NonNull public final String name;
        @NonNull public final String category;
        @NonNull public final String tags;
        @NonNull public final String levelDisplay;
        public final int skillPointPerLevel;
        @NonNull public final String cooldown;
        @NonNull public final String castRange;
        @NonNull public final String effectCurrent;
        @Nullable public final String effectNext;
        @NonNull public final String fullDescription;

        public Detail(
                @NonNull String name,
                @NonNull String category,
                @NonNull String tags,
                @NonNull String levelDisplay,
                int skillPointPerLevel,
                @NonNull String cooldown,
                @NonNull String castRange,
                @NonNull String effectCurrent,
                @Nullable String effectNext,
                @NonNull String fullDescription
        ) {
            this.name = name;
            this.category = category;
            this.tags = tags;
            this.levelDisplay = levelDisplay;
            this.skillPointPerLevel = skillPointPerLevel;
            this.cooldown = cooldown;
            this.castRange = castRange;
            this.effectCurrent = effectCurrent;
            this.effectNext = effectNext;
            this.fullDescription = fullDescription;
        }
    }

    public static void show(@NonNull Context context, @Nullable Detail detail) {
        if (detail == null) return;

        View root = LayoutInflater.from(context).inflate(R.layout.dialog_item_detail, null, false);
        TextView title = root.findViewById(R.id.tv_detail_title);
        TextView body = root.findViewById(R.id.tv_detail_body);
        title.setText(detail.name);
        body.setText(buildDetailText(detail));

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Tb_ItemDetailDialog);
        builder.setView(root);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        View close = root.findViewById(R.id.btn_detail_close);
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    @NonNull
    private static String buildDetailText(@NonNull Detail d) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "分类", d.category);
        appendLine(sb, "标签", nonEmpty(d.tags));
        appendLine(sb, "等级进度", d.levelDisplay);
        if (d.skillPointPerLevel > 0) {
            appendLine(sb, "升级消耗", "每级 " + d.skillPointPerLevel + " 技能点");
        }
        appendLine(sb, "冷却", nonEmpty(d.cooldown));
        appendLine(sb, "作用范围", nonEmpty(d.castRange));

        sb.append("【当前效果】\n");
        sb.append(d.effectCurrent.trim()).append("\n");

        if (!TextUtils.isEmpty(d.effectNext)) {
            sb.append("\n【下一等级】\n");
            sb.append(d.effectNext.trim()).append("\n");
        } else {
            sb.append("\n【下一等级】\n已满级\n");
        }

        String desc = nonEmpty(d.fullDescription);
        if (desc != null) {
            sb.append("\n【技能说明】\n");
            sb.append(desc).append("\n");
        }

        String out = sb.toString().trim();
        return out.isEmpty() ? "（暂无详情）" : out;
    }

    private static void appendLine(@NonNull StringBuilder sb, @NonNull String label, @Nullable String value) {
        if (TextUtils.isEmpty(value)) return;
        sb.append(label).append("：").append(value.trim()).append("\n");
    }

    @Nullable
    private static String nonEmpty(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
