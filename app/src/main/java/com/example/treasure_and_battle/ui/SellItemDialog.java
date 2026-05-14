package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.model.item.Item;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public final class SellItemDialog {

    private SellItemDialog() {}

    private static int unitSellPrice(@NonNull Item item) {
        if (item.getRarity() == null) {
            return Math.max(0, item.getBaseValue());
        }
        return Math.round(item.getBaseValue() * item.getRarity().getSellPriceMultiplier());
    }

    public static void show(
            @NonNull Context context,
            @NonNull List<Item> bagSlots,
            int bagIndex,
            @NonNull Item item,
            @Nullable Runnable onSold,
            @Nullable java.util.function.IntConsumer onGoldEarned) {
        if (bagIndex < 0 || bagIndex >= bagSlots.size()) {
            return;
        }
        if (bagSlots.get(bagIndex) != item) {
            FloatMsgOverlay.showFloatMsg(context, "物品已变化，请重试");
            return;
        }

        int unit = unitSellPrice(item);
        if (unit <= 0) {
            FloatMsgOverlay.showFloatMsg(context, "该物品无法出售");
            return;
        }

        boolean needsQuantityPicker = item.canStack() && item.getCount() > 1;
        if (!needsQuantityPicker) {
            int qty = item.getCount();
            int pay = unit * qty;
            MaterialAlertDialogBuilder simple = new MaterialAlertDialogBuilder(
                    context, R.style.ThemeOverlay_Tb_ItemDetailDialog);
            simple.setTitle("出售");
            simple.setMessage("以 " + pay + " 金币出售「" + item.getName() + "」？");
            simple.setNegativeButton("取消", null);
            simple.setPositiveButton("出售", (d, w) ->
                    trySellQuantity(context, bagSlots, bagIndex, item, qty, onSold, onGoldEarned));
            simple.show();
            return;
        }

        int maxQty = Math.max(1, item.getCount());
        android.view.View root = LayoutInflater.from(context).inflate(R.layout.dialog_sell_item, null, false);
        TextView tvName = root.findViewById(R.id.tv_sell_item_name);
        TextView tvUnit = root.findViewById(R.id.tv_sell_unit_price);
        TextView tvQtyVal = root.findViewById(R.id.tv_sell_quantity_value);
        TextView tvTotal = root.findViewById(R.id.tv_sell_total_price);
        SeekBar seek = root.findViewById(R.id.seek_sell_quantity);
        TextView btnCancel = root.findViewById(R.id.btn_sell_cancel);
        TextView btnOk = root.findViewById(R.id.btn_sell_confirm);

        tvName.setText(item.getName());
        tvUnit.setText("单价：" + unit + " 金币");

        seek.setMax(maxQty - 1);
        seek.setProgress(0);

        Runnable updateTotal = () -> {
            int qty = seek.getProgress() + 1;
            tvQtyVal.setText(String.valueOf(qty));
            long total = (long) unit * qty;
            tvTotal.setText("总价：" + total + " 金币");
        };
        updateTotal.run();

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTotal.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Tb_ItemDetailDialog);
        builder.setView(root);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            int qty = seek.getProgress() + 1;
            if (trySellQuantity(context, bagSlots, bagIndex, item, qty, onSold, onGoldEarned)) {
                dialog.dismiss();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    /**
     * 不可堆叠或格内仅 1 个时使用简单确认；可堆叠且多个时由 {@link #show} 内 SeekBar 选择数量。
     */
    private static boolean trySellQuantity(
            @NonNull Context context,
            @NonNull List<Item> bagSlots,
            int bagIndex,
            @NonNull Item expectedItem,
            int qty,
            @Nullable Runnable onSold,
            @Nullable java.util.function.IntConsumer onGoldEarned) {
        if (bagIndex < 0 || bagIndex >= bagSlots.size()) {
            return false;
        }
        Item item = bagSlots.get(bagIndex);
        if (item != expectedItem) {
            FloatMsgOverlay.showFloatMsg(context, "物品已变化");
            return false;
        }
        if (qty <= 0 || qty > item.getCount()) {
            FloatMsgOverlay.showFloatMsg(context, "数量无效");
            return false;
        }
        int unit = unitSellPrice(item);
        if (unit <= 0) {
            FloatMsgOverlay.showFloatMsg(context, "该物品无法出售");
            return false;
        }
        int pay = unit * qty;
        if (item.getCount() > qty) {
            item.setCount(item.getCount() - qty);
        } else {
            bagSlots.set(bagIndex, null);
        }
        if (onGoldEarned != null) {
            onGoldEarned.accept(pay);
        }
        FloatMsgOverlay.showFloatMsg(context, "已出售，获得 " + pay + " 金币");
        if (onSold != null) {
            onSold.run();
        }
        return true;
    }
}
