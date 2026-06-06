package com.example.treasure_and_battle.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.game.SaveManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SaveSelectDialog {

    public static final int MODE_LOAD = 0;
    public static final int MODE_SAVE = 1;

    private SaveSelectDialog() {}

    public interface OnSaveActionListener {
        void onLoadSave(Character character);
        void onSaveComplete();
    }

    public static void show(@NonNull Context context, int mode, OnSaveActionListener listener) {
        SaveManager sm = SaveManager.getInstance(context);
        List<SaveManager.SlotMeta> metas = sm.getAllSlotMetas();

        View content = LayoutInflater.from(context).inflate(R.layout.dialog_save_select, null, false);
        TextView title = content.findViewById(R.id.tv_save_select_title);
        title.setText(mode == MODE_LOAD ? "选择存档" : "保存游戏");

        RecyclerView rv = content.findViewById(R.id.rv_save_slots);
        rv.setLayoutManager(new GridLayoutManager(context, 2));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .setCancelable(true)
                .create();

        SaveSlotAdapter adapter = new SaveSlotAdapter(metas, mode, meta -> {
            if (meta.isEmpty) {
                if (mode == MODE_SAVE && !meta.isAuto) {
                    showConfirmDialog(context, dialog, meta, mode, sm, listener);
                }
                return;
            }

            if (mode == MODE_SAVE && meta.isAuto) {
                return;
            }

            showConfirmDialog(context, dialog, meta, mode, sm, listener);
        });

        rv.setAdapter(adapter);

        content.findViewById(R.id.btn_save_select_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private static void showConfirmDialog(Context context, AlertDialog parentDialog,
                                          SaveManager.SlotMeta meta, int mode,
                                          SaveManager sm, OnSaveActionListener listener) {
        View alertView = LayoutInflater.from(context).inflate(R.layout.dialog_treasure_alert, null, false);
        TextView alertTitle = alertView.findViewById(R.id.tv_treasure_alert_title);
        TextView alertMsg = alertView.findViewById(R.id.tv_treasure_alert_message);
        View negBtn = alertView.findViewById(R.id.btn_treasure_alert_negative);
        TextView posBtn = alertView.findViewById(R.id.btn_treasure_alert_positive);

        if (mode == MODE_LOAD) {
            alertTitle.setText("读取存档");
            alertMsg.setText("是否以 " + meta.displayName + " 开始游戏？\n\n"
                    + "角色：" + meta.characterName + "\n"
                    + "等级：Lv." + meta.level);
        } else {
            alertTitle.setText("覆盖存档");
            alertMsg.setText("是否要覆盖 " + meta.displayName + " ？\n\n"
                    + (meta.isEmpty ? "（空槽位）" : "当前：Lv." + meta.level + " " + meta.characterName));
        }

        negBtn.setVisibility(View.VISIBLE);
        ((TextView) negBtn).setText("取消");
        posBtn.setText("确认");

        AlertDialog confirmDialog = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(alertView)
                .setCancelable(true)
                .create();

        negBtn.setOnClickListener(v -> confirmDialog.dismiss());
        posBtn.setOnClickListener(v -> {
            confirmDialog.dismiss();
            parentDialog.dismiss();

            if (mode == MODE_LOAD) {
                Character ch = sm.loadGame(meta.slotId);
                if (ch != null && listener != null) {
                    listener.onLoadSave(ch);
                }
            } else {
                Activity activity = resolveActivity(context);
                if (activity != null) {
                    Character ch = PlayerCharacterHolder.getOrCreate(context);
                    boolean ok = sm.saveGame(ch, meta.slotId);
                    if (ok) {
                        FloatMsgOverlay.showFloatMsg(context, "存档成功");
                        if (listener != null) listener.onSaveComplete();
                    } else {
                        FloatMsgOverlay.showFloatMsg(context, "存档失败");
                    }
                }
            }
        });

        confirmDialog.show();
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private static Activity resolveActivity(Context context) {
        if (context instanceof Activity) return (Activity) context;
        return null;
    }

    private static class SaveSlotAdapter extends RecyclerView.Adapter<SaveSlotAdapter.VH> {
        private final List<SaveManager.SlotMeta> metas;
        private final int mode;
        private final OnSlotClickListener clickListener;

        interface OnSlotClickListener {
            void onClick(SaveManager.SlotMeta meta);
        }

        SaveSlotAdapter(List<SaveManager.SlotMeta> metas, int mode, OnSlotClickListener listener) {
            this.metas = metas;
            this.mode = mode;
            this.clickListener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_save_slot, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SaveManager.SlotMeta meta = metas.get(position);
            holder.tvName.setText(meta.displayName);

            if (meta.isEmpty) {
                holder.tvLevel.setText("（空）");
                holder.tvExp.setText("");
                holder.tvTime.setText("");
                holder.tvName.setTextColor(0xFF888888);
                holder.itemView.setAlpha(0.5f);
                if (mode == MODE_SAVE) {
                    holder.itemView.setAlpha(0.7f);
                }
            } else {
                holder.tvName.setTextColor(meta.isAuto
                        ? holder.itemView.getContext().getResources().getColor(R.color.tb_gold)
                        : holder.itemView.getContext().getResources().getColor(R.color.tb_text_main));
                holder.tvLevel.setText("Lv." + meta.level + "  " + meta.characterName);
                holder.tvExp.setText("EXP: " + meta.currentExp + " / " + meta.expToNextLevel);
                String timeStr = formatTime(meta.timestamp);
                holder.tvTime.setText(timeStr);
                holder.itemView.setAlpha(1f);
            }

            holder.itemView.setOnClickListener(v -> {
                if (meta.isEmpty && mode == MODE_LOAD) return;
                if (meta.isAuto && mode == MODE_SAVE) return;
                clickListener.onClick(meta);
            });
        }

        @Override
        public int getItemCount() {
            return metas.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvLevel, tvExp, tvTime;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_slot_name);
                tvLevel = itemView.findViewById(R.id.tv_slot_level);
                tvExp = itemView.findViewById(R.id.tv_slot_exp);
                tvTime = itemView.findViewById(R.id.tv_slot_time);
            }
        }

        private static String formatTime(long timestamp) {
            if (timestamp <= 0) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
