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
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * 背包 / 装备栏「查看描述」：展示模型中已有字段，无内容则整行省略。
 */
public final class ItemDetailDialog {

    private ItemDetailDialog() {}

    public static void show(@NonNull Context context, @Nullable Item item) {
        if (item == null) return;

        View root = LayoutInflater.from(context).inflate(R.layout.dialog_item_detail, null, false);
        TextView title = root.findViewById(R.id.tv_detail_title);
        TextView body = root.findViewById(R.id.tv_detail_body);
        title.setText(item.getName());
        body.setText(buildDetailText(item));

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
    private static String buildDetailText(@NonNull Item item) {
        StringBuilder sb = new StringBuilder();

        appendLine(sb, "品质", item.getRarity() != null ? item.getRarity().getDisplayName() : null);
        appendLine(sb, "类型", itemTypeLabel(item.getType()));

        if (item instanceof EquipItem) {
            EquipItem eq = (EquipItem) item;
            appendLine(sb, "等级", "Lv." + eq.getLevel());
            appendLine(sb, "部位", equipSlotLabel(eq.getSlot()));
            if (eq.getMaxSockets() > 0) {
                appendLine(sb, "宝石槽", String.valueOf(eq.getMaxSockets()));
            }
            appendEquipBaseStats(sb, eq);
            appendAffixes(sb, eq.getAffixes());
        }

        appendLine(sb, "描述", nonEmpty(item.getDescription()));

        if (item.getBaseValue() > 0) {
            appendLine(sb, "基准价值", String.valueOf(item.getBaseValue()));
        }
        if (item.getRarity() != null && item.getBaseValue() > 0) {
            int approxSell = Math.round(item.getBaseValue() * item.getRarity().getSellPriceMultiplier());
            if (approxSell > 0) {
                appendLine(sb, "回收价（约）", String.valueOf(approxSell));
            }
        }

        if (item.getMaxStack() > 1) {
            appendLine(sb, "堆叠上限", String.valueOf(item.getMaxStack()));
        }
        if (item.getCount() > 1 || item.getMaxStack() > 1) {
            appendLine(sb, "持有数量", String.valueOf(item.getCount()));
        }

        String out = sb.toString().trim();
        return out.isEmpty() ? "（暂无详情）" : out;
    }

    private static void appendEquipBaseStats(@NonNull StringBuilder sb, @NonNull EquipItem eq) {
        EquipSlot slot = eq.getSlot();
        AttributeSet a = eq.getBaseAttributes();
        if (slot == null || a == null) return;

        StringBuilder block = new StringBuilder();
        switch (slot) {
            case WEAPON:
                appendStatLine(block, "物理攻击", a.physicalAtk);
                appendStatLine(block, "法术攻击", a.magicalAtk);
                break;
            case HELMET:
            case CHEST:
            case LEGGINGS:
            case BOOTS:
                appendStatLine(block, "生命上限", a.maxHp);
                appendStatLine(block, "物理防御", a.physicalDef);
                appendStatLine(block, "法术防御", a.magicalDef);
                break;
            case NECKLACE:
            case RING:
            case BRACELET:
                appendStatLine(block, "生命上限", a.maxHp);
                appendStatLine(block, "力量", a.strength);
                appendStatLine(block, "体魄", a.physique);
                break;
            default:
                break;
        }
        if (block.length() > 0) {
            sb.append("【基础属性】\n");
            sb.append(block);
        }
    }

    private static void appendStatLine(@NonNull StringBuilder sb, @NonNull String label, int value) {
        if (value == 0) return;
        sb.append(label).append("：").append(value).append("\n");
    }

    private static void appendAffixes(@NonNull StringBuilder sb, @Nullable List<BaseAffix> affixes) {
        if (affixes == null || affixes.isEmpty()) return;
        sb.append("【词缀】\n");
        for (BaseAffix affix : affixes) {
            if (affix == null) continue;
            String line = affix.getDescription();
            if (TextUtils.isEmpty(line)) continue;
            sb.append("· ").append(line).append("\n");
        }
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

    @Nullable
    private static String itemTypeLabel(@Nullable ItemType type) {
        if (type == null) return null;
        switch (type) {
            case EQUIPMENT:
                return "装备";
            case CONSUMABLE:
                return "消耗品";
            case MATERIAL:
                return "材料";
            default:
                return null;
        }
    }

    @Nullable
    private static String equipSlotLabel(@Nullable EquipSlot slot) {
        if (slot == null) return null;
        switch (slot) {
            case WEAPON:
                return "武器";
            case HELMET:
                return "头盔";
            case CHEST:
                return "胸甲";
            case LEGGINGS:
                return "护腿";
            case BOOTS:
                return "鞋子";
            case NECKLACE:
                return "项链";
            case BRACELET:
                return "手镯";
            case RING:
                return "戒指";
            default:
                return null;
        }
    }
}
