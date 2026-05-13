package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.utils.GameAssetIcons;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        ImageView icon = root.findViewById(R.id.iv_detail_icon);
        title.setText(item.getName());
        Rarity rarity = item.getRarity();
        if (rarity != null) {
            title.setTextColor(rarity.getColor());
        } else {
            title.setTextColor(ContextCompat.getColor(context, R.color.tb_gold_deep));
        }
        body.setText(HtmlCompat.fromHtml(buildDetailHtml(item), HtmlCompat.FROM_HTML_MODE_LEGACY));

        if (icon != null) {
            GameAssetIcons.bindItem(context, icon, item);
            icon.setVisibility(View.VISIBLE);
        }

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

    private static final Map<String, String> STAT_LABELS = new LinkedHashMap<>();
    static {
        STAT_LABELS.put("maxHp", "生命上限");
        STAT_LABELS.put("maxMp", "法力上限");
        STAT_LABELS.put("physicalAtk", "物理攻击");
        STAT_LABELS.put("magicalAtk", "法术攻击");
        STAT_LABELS.put("physicalDef", "物理防御");
        STAT_LABELS.put("magicalDef", "法术防御");
        STAT_LABELS.put("speed", "速度");
        STAT_LABELS.put("strength", "力量");
        STAT_LABELS.put("agility", "敏捷");
        STAT_LABELS.put("intelligence", "智力");
        STAT_LABELS.put("spirit", "精神");
        STAT_LABELS.put("physique", "体魄");
        STAT_LABELS.put("luck", "幸运");
        STAT_LABELS.put("physicalCritRate", "物理暴击率");
        STAT_LABELS.put("magicalCritRate", "魔法暴击率");
        STAT_LABELS.put("physicalCritDmg", "物理暴伤");
        STAT_LABELS.put("magicalCritDmg", "魔法暴伤");
        STAT_LABELS.put("hitRate", "命中率");
        STAT_LABELS.put("dodgeRate", "闪避率");
        STAT_LABELS.put("debuffResist", "异常抵抗");
        STAT_LABELS.put("mpCostReduction", "蓝耗减免");
        STAT_LABELS.put("damageReductionRate", "伤害减免");
        STAT_LABELS.put("lootRarityBonus", "掉落加成");
        STAT_LABELS.put("goldBonus", "金币加成");
        STAT_LABELS.put("expBonus", "经验加成");
        STAT_LABELS.put("maxActionPoints", "行动点上限");
    }

    @NonNull
    private static String buildDetailHtml(@NonNull Item item) {
        StringBuilder sb = new StringBuilder();

        appendHtmlLine(sb, "品质", item.getRarity() != null ? item.getRarity().getDisplayName() : null);
        appendHtmlLine(sb, "类型", itemTypeLabel(item.getType()));

        if (item instanceof EquipItem) {
            EquipItem eq = (EquipItem) item;
            appendHtmlLine(sb, "等级", "Lv." + eq.getLevel());
            appendHtmlLine(sb, "部位", equipSlotLabel(eq.getSlot()));
            appendEquipBaseStats(sb, eq);
            appendAffixes(sb, eq.getAffixes());
            appendGemSockets(sb, eq);
        }

        appendHtmlLine(sb, "描述", nonEmpty(item.getDescription()));

        if (item.getBaseValue() > 0) {
            appendHtmlLine(sb, "基准价值", String.valueOf(item.getBaseValue()));
        }
        if (item.getRarity() != null && item.getBaseValue() > 0) {
            int approxSell = Math.round(item.getBaseValue() * item.getRarity().getSellPriceMultiplier());
            if (approxSell > 0) {
                appendHtmlLine(sb, "回收价（约）", String.valueOf(approxSell));
            }
        }

        if (item.getMaxStack() > 1) {
            appendHtmlLine(sb, "堆叠上限", String.valueOf(item.getMaxStack()));
        }
        if (item.getCount() > 1 || item.getMaxStack() > 1) {
            appendHtmlLine(sb, "持有数量", String.valueOf(item.getCount()));
        }

        String out = sb.toString().trim();
        return out.isEmpty() ? "（暂无详情）" : out;
    }

    private static void appendEquipBaseStats(@NonNull StringBuilder sb, @NonNull EquipItem eq) {
        AttributeSet a = eq.getRawBaseAttributes();
        if (a == null) return;

        StringBuilder block = new StringBuilder();
        for (Field field : AttributeSet.class.getFields()) {
            if (!STAT_LABELS.containsKey(field.getName())) continue;
            try {
                Object val = field.get(a);
                if (val == null) continue;
                if (val instanceof Integer && (Integer) val != 0) {
                    appendStatLine(block, STAT_LABELS.get(field.getName()), (Integer) val);
                } else if (val instanceof Float && Math.abs((Float) val) > 0.0001f) {
                    appendFloatStatLine(block, STAT_LABELS.get(field.getName()), (Float) val);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        if (block.length() > 0) {
            sb.append("<b>【基础属性】</b><br>");
            sb.append(block);
        }
    }

    private static void appendStatLine(@NonNull StringBuilder sb, @NonNull String label, int value) {
        if (value == 0) return;
        sb.append(label).append("：").append(value).append("<br>");
    }

    private static void appendFloatStatLine(@NonNull StringBuilder sb, @NonNull String label, float value) {
        sb.append(label).append("：");
        sb.append(String.format(java.util.Locale.CHINA, "%.1f%%", value * 100f));
        sb.append("<br>");
    }

    private static void appendAffixes(@NonNull StringBuilder sb, @Nullable List<BaseAffix> affixes) {
        if (affixes == null || affixes.isEmpty()) return;
        sb.append("<b>【词缀】</b><br>");
        for (BaseAffix affix : affixes) {
            if (affix == null) continue;
            String line = affix.getDescription();
            if (TextUtils.isEmpty(line)) continue;
            String colorHex = colorToHex(affix.getRarity() != null
                    ? affix.getRarity().getColor() : 0xFF888888);
            sb.append("<font color=\"").append(colorHex).append("\">●</font> ");
            sb.append(android.text.TextUtils.htmlEncode(line)).append("<br>");
        }
    }

    private static void appendGemSockets(@NonNull StringBuilder sb, @NonNull EquipItem eq) {
        int total = eq.getMaxSockets();
        if (total <= 0) return;
        List<GemItem> gems = eq.getSocketedGems();
        sb.append("<b>【宝石槽】</b><br>");
        for (int i = 0; i < total; i++) {
            GemItem gem = i < gems.size() ? gems.get(i) : null;
            String colorHex;
            String label;
            if (gem != null) {
                colorHex = colorToHex(gem.getRarity() != null
                        ? gem.getRarity().getColor() : 0xFF888888);
                label = gem.getName() + "（" + gem.getRarity().getDisplayName() + "）";
            } else {
                colorHex = "#888888";
                label = "空槽位";
            }
            sb.append("<font color=\"").append(colorHex).append("\">●</font> ");
            sb.append(label).append("<br>");
        }
    }

    private static void appendHtmlLine(@NonNull StringBuilder sb, @NonNull String label, @Nullable String value) {
        if (TextUtils.isEmpty(value)) return;
        sb.append(label).append("：").append(android.text.TextUtils.htmlEncode(value.trim())).append("<br>");
    }

    @NonNull
    private static String colorToHex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
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
