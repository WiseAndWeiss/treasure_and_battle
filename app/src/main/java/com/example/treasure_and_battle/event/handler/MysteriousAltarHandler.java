package com.example.treasure_and_battle.event.handler;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.drawable.TreasureStyleDrawable;
import com.example.treasure_and_battle.event.NeutralEventResolver;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.ui.NeutralEventActivity;
import com.example.treasure_and_battle.utils.GameAssetIcons;

import java.util.ArrayList;
import java.util.List;

/**
 * 神秘祭坛 — 献祭3件同品质物品获得高一阶品质的奖励。
 */
public class MysteriousAltarHandler extends BaseEventHandler {

    public MysteriousAltarHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.clearButtons();
        Character ch = ui.getCharacter();
        List<Item> bag = ch.getBagItems();

        int eligibleCount = 0;
        for (Item item : bag) {
            if (item != null && item.getType() != ItemType.MATERIAL) eligibleCount += item.getCount();
        }

        if (eligibleCount >= 3) {
            boolean hasValidCombo = false;
            Rarity[] checkRarities = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE};
            for (Rarity r : checkRarities)
                if (NeutralEventResolver.countItemsByRarity(bag, r) >= 3) { hasValidCombo = true; break; }

            if (hasValidCombo) {
                ui.addButton("挑选献祭物品", 0xFFFF9800, v -> showSacrificeDialog());
            } else {
                ui.addButton("没有足够的同品质物品", 0xFF888888, v -> {
                    ui.showResult("祭坛需要3件相同品质的物品才能献祭。");
                    ui.switchToForwardButton();
                });
            }
        } else {
            ui.addButton("可献祭物品不足3件", 0xFF888888, v -> {
                ui.showResult("你背包中可献祭的物品不足3件（材料不可献祭）。");
                ui.switchToForwardButton();
            });
        }
        ui.addButton("转身离开", 0xFF888888, v -> {
            ui.showResult("你对祭坛默默祈祷，然后离开了。");
            ui.switchToForwardButton();
        });
    }

    private static String nameForType(ItemType t) {
        return t == ItemType.EQUIPMENT ? "装备"
                : t == ItemType.CONSUMABLE ? "药水"
                : t == ItemType.GEM ? "宝石"
                : "物品";
    }

    private void showSacrificeDialog() {
        Character ch = ui.getCharacter();
        Context ctx = ui.getContext();
        List<Item> bag = ch.getBagItems();
        List<Item> eligible = new ArrayList<>();
        for (Item item : bag) if (item != null && item.getType() != ItemType.MATERIAL) eligible.add(item);

        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_altar_sacrifice, null);
        int dialogHeight = (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.66);
        int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = Math.min((int) (screenWidth * 0.82f), NeutralEventActivity.dpToPx(ctx, 340));
        d.setContentView(content, new ViewGroup.LayoutParams(dialogWidth, dialogHeight));
        Window window = d.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = dialogWidth;
            lp.height = dialogHeight;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
        }

        TextView tvCounter = content.findViewById(R.id.tv_sacrifice_counter);
        TextView btnConfirm = content.findViewById(R.id.btn_sacrifice_confirm);
        TextView btnCancel = content.findViewById(R.id.btn_sacrifice_cancel);
        RecyclerView rv = content.findViewById(R.id.rv_sacrifice_grid);
        content.findViewById(R.id.btn_altar_close).setOnClickListener(v -> d.dismiss());
        btnCancel.setOnClickListener(v -> d.dismiss());

        tvCounter.setText(eligible.isEmpty()
                ? "无可献祭物品（材料不可选）"
                : "已选 0 / 3 — 请挑选同类型同品质的物品");
        tvCounter.setTextColor(ContextCompat.getColor(ctx,
                eligible.isEmpty() ? R.color.tb_text_sub : R.color.tb_gold));
        styleSacrificeConfirmButton(btnConfirm, false);

        int[] sacrificeCounts = new int[eligible.size()];
        int[] totalCount = {0};

        rv.setLayoutManager(new GridLayoutManager(ctx, 2));
        rv.setClipToPadding(true);
        rv.setClipChildren(true);
        rv.setNestedScrollingEnabled(true);
        btnConfirm.setOnClickListener(v -> {
            int total = totalCount[0];
            if (total != 3) return;
            if (!NeutralEventResolver.isSacCountsValid(eligible, sacrificeCounts)) {
                tvCounter.setText("选中的物品品质不一致！");
                tvCounter.setTextColor(0xFFE53935);
                return;
            }
            d.dismiss();
            NeutralEventResolver.AlterSacrificeResult ar =
                    NeutralEventResolver.resolveAlterSacrifice(bag, eligible, sacrificeCounts, ctx, ch);
            if (!ar.success) {
                ui.showResult(ar.errorMessage);
            } else if (ar.rewardAdded) {
                ui.showResult("祭坛散发出耀眼的光芒！\n 献祭" + total + "件→获得：" + ar.rewardName + "（" + ar.rewardRarityDisplay + "）");
            } else {
                ui.showResult("祭坛散发出耀眼的光芒！\n 献祭" + total + "件→但什么都没有发生...");
            }
            ui.switchToForwardButton();
        });

        TextView fBtnConfirm = btnConfirm;
        Runnable updateV = () -> {
            int t = totalCount[0];
            if (t == 0) {
                tvCounter.setText("已选 0 / 3 — 请挑选3件相同品质的物品");
                tvCounter.setTextColor(0xFFFF9800);
                fBtnConfirm.setEnabled(false);
                fBtnConfirm.setBackgroundTintList(ColorStateList.valueOf(0xFF888888));
            } else if (!NeutralEventResolver.isSacCountsValid(eligible, sacrificeCounts)) {
                tvCounter.setText("已选 " + t + " / 3 ❌ 品质不一致");
                tvCounter.setTextColor(0xFFE53935);
                fBtnConfirm.setEnabled(false);
                fBtnConfirm.setBackgroundTintList(ColorStateList.valueOf(0xFF888888));
            } else {
                Item fi = null;
                for (int i = 0; i < sacrificeCounts.length; i++)
                    if (sacrificeCounts[i] > 0) { fi = eligible.get(i); break; }
                Rarity toR = fi != null ? Rarity.fromId(fi.getRarity().getId() + 1) : null;
                if (toR == null) {
                    tvCounter.setText("已选 " + t + " / 3  已达最高品质，无法升阶");
                    tvCounter.setTextColor(0xFFE53935);
                    fBtnConfirm.setEnabled(false);
                    fBtnConfirm.setBackgroundTintList(ColorStateList.valueOf(0xFF888888));
                } else if (t != 3) {
                    tvCounter.setText("已选 " + t + " / 3 " + fi.getRarity().getDisplayName() + " — 需要恰好3件");
                    tvCounter.setTextColor(0xFFFF9800);
                    fBtnConfirm.setEnabled(false);
                    fBtnConfirm.setBackgroundTintList(ColorStateList.valueOf(0xFF888888));
                } else {
                    tvCounter.setText("已选 " + t + " / 3 " + fi.getRarity().getDisplayName() + " — 品质统一！");
                    tvCounter.setTextColor(0xFF4CAF50);
                    fBtnConfirm.setEnabled(true);
                    fBtnConfirm.setBackgroundTintList(ColorStateList.valueOf(0xFFFF9800));
                }
            }
        };

        AltarSacrificeAdapter[] ar = {null};
        ar[0] = new AltarSacrificeAdapter(eligible, sacrificeCounts, totalCount, (idx, isStacked) -> {
            Item item = eligible.get(idx);
            int cur = sacrificeCounts[idx];
            if (cur > 0) {
                sacrificeCounts[idx] = 0;
                totalCount[0] -= cur;
                updateV.run();
                ar[0].notifyItemChanged(idx);
                return;
            }
            int remaining = 3 - totalCount[0];
            if (remaining <= 0) return;
            int maxN = item.getCount();
            if (!isStacked || maxN <= 1) {
                sacrificeCounts[idx] = 1;
                totalCount[0]++;
                updateV.run();
                ar[0].notifyItemChanged(idx);
                return;
            }
            int n = Math.min(remaining, maxN);
            if (n == 1) {
                sacrificeCounts[idx] = 1;
                totalCount[0]++;
                updateV.run();
                ar[0].notifyItemChanged(idx);
                return;
            }
            showQuantityPicker(n, qty -> {
                sacrificeCounts[idx] = qty;
                totalCount[0] += qty;
                updateV.run();
                ar[0].notifyItemChanged(idx);
            });
        });
        d.setCancelable(true);
        d.show();
        NeutralEventActivity.applyTransparentDialogWindow(d);
        NeutralEventActivity.scheduleDialogItemGrid(rv, ar[0], ar[0]::setGridLayout);
    }

    private void showQuantityPicker(int maxQty, java.util.function.IntConsumer onPick) {
        Context ctx = ui.getContext();
        Dialog pick = new Dialog(ctx);
        pick.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_sacrifice_quantity, null);
        TextView tvTitle = content.findViewById(R.id.tv_quantity_title);
        LinearLayout llOptions = content.findViewById(R.id.ll_quantity_options);
        tvTitle.setText("选择献祭数量（最多" + maxQty + "件）");
        int margin = NeutralEventActivity.dpToPx(ctx, 6);
        for (int q = 1; q <= maxQty; q++) {
            TextView opt = new TextView(ctx);
            opt.setText(q + " 件");
            opt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            opt.setTypeface(opt.getTypeface(), android.graphics.Typeface.BOLD);
            opt.setTextColor(ContextCompat.getColor(ctx, R.color.tb_bg_dark));
            opt.setBackgroundResource(R.drawable.bg_tab_active);
            opt.setGravity(Gravity.CENTER);
            opt.setClickable(true);
            opt.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, NeutralEventActivity.dpToPx(ctx, 44));
            if (q > 1) lp.topMargin = margin;
            int qty = q;
            opt.setOnClickListener(v -> {
                pick.dismiss();
                onPick.accept(qty);
            });
            llOptions.addView(opt, lp);
        }
        TextView btnCancel = content.findViewById(R.id.btn_quantity_cancel);
        LinearLayout.LayoutParams cancelLp = (LinearLayout.LayoutParams) btnCancel.getLayoutParams();
        cancelLp.topMargin = margin;
        btnCancel.setLayoutParams(cancelLp);
        btnCancel.setOnClickListener(v -> pick.dismiss());
        pick.setContentView(content);
        pick.setCancelable(true);
        pick.show();
        NeutralEventActivity.applyTransparentDialogWindow(pick);
    }

    private void styleSacrificeConfirmButton(TextView btn, boolean enabled) {
        Context ctx = ui.getContext();
        btn.setEnabled(enabled);
        if (enabled) {
            btn.setBackgroundResource(R.drawable.bg_tab_active);
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.tb_bg_dark));
        } else {
            btn.setBackgroundResource(R.drawable.bg_tab_idle);
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.tb_text_sub));
        }
    }

    private static class AltarSacrificeAdapter extends RecyclerView.Adapter<AltarSacrificeAdapter.VH> {
        final List<Item> items;
        final int[] counts;
        final int[] totalCount;
        final OnSacrificeListener listener;
        private int recyclerWidthPx;
        private int squareSizePx;

        interface OnSacrificeListener { void onToggle(int index, boolean isStacked); }

        AltarSacrificeAdapter(List<Item> items, int[] counts, int[] totalCount, OnSacrificeListener l) {
            this.items = items; this.counts = counts; this.totalCount = totalCount; this.listener = l;
        }

        void setGridLayout(int recyclerWidthPx, int squareSizePx) {
            if (recyclerWidthPx > 0) {
                this.recyclerWidthPx = recyclerWidthPx;
            }
            if (squareSizePx > 0) {
                this.squareSizePx = squareSizePx;
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_equip_select, p, false);
            v.setLayoutParams(NeutralEventActivity.newSquareGridCellLp(p.getContext(), squareSizePx, recyclerWidthPx));
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int pos) {
            NeutralEventActivity.applySquareGridCellLayout(holder.itemView, squareSizePx, recyclerWidthPx);
            Item item = items.get(pos);
            GameAssetIcons.bindItem(holder.itemView.getContext(), holder.ivIcon, item);
            if (item instanceof EquipItem) {
                holder.tvLevel.setVisibility(View.VISIBLE);
                holder.tvLevel.setText("Lv." + ((EquipItem) item).getLevel());
            } else {
                holder.tvLevel.setVisibility(View.GONE);
            }
            holder.tvName.setVisibility(View.VISIBLE);
            holder.tvName.setText(item.getName());
            holder.tvName.setTextSize(10);
            int stackCount = item.getCount();
            int sel = counts[pos];
            if (stackCount > 1) {
                holder.tvCount.setVisibility(View.VISIBLE);
                holder.tvCount.setText("×" + stackCount);
            } else {
                holder.tvCount.setVisibility(View.GONE);
            }
            holder.itemView.setForeground(TreasureStyleDrawable.newSlotStrokeOverlay(
                    holder.itemView.getContext(), item.getRarity() != null ? item.getRarity().getColor() : null));
            holder.selectOverlay.setVisibility(sel > 0 ? View.VISIBLE : View.GONE);
            if (sel > 0) {
                holder.selBadge.setVisibility(View.VISIBLE);
                holder.selBadge.setText(String.valueOf(sel));
            } else {
                holder.selBadge.setVisibility(View.GONE);
            }
            boolean isStacked = stackCount > 1;
            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onToggle(pos, isStacked); });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivIcon; TextView tvLevel, tvName, tvCount, selBadge; View selectOverlay;
            VH(View v) {
                super(v);
                ivIcon = v.findViewById(R.id.iv_item_icon);
                tvLevel = v.findViewById(R.id.tv_item_level);
                tvName = v.findViewById(R.id.tv_item_name);
                tvName.setVisibility(View.GONE);
                tvName.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                ((RelativeLayout.LayoutParams) tvName.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                tvCount = v.findViewById(R.id.tv_bag_stack_count);
                selectOverlay = new View(v.getContext());
                selectOverlay.setBackgroundColor(0x66FFC107);
                selectOverlay.setVisibility(View.GONE);
                ((FrameLayout) v).addView(selectOverlay, new FrameLayout.LayoutParams(-1, -1));
                selBadge = new TextView(v.getContext());
                selBadge.setTextColor(ContextCompat.getColor(v.getContext(), R.color.tb_bg_dark));
                selBadge.setTextSize(11);
                selBadge.setTypeface(v.getContext().getResources().getFont(R.font.zpix));
                selBadge.setGravity(Gravity.CENTER);
                selBadge.setBackgroundResource(R.drawable.bg_tab_active);
                int sz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, v.getContext().getResources().getDisplayMetrics());
                FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(sz, sz);
                bp.gravity = Gravity.TOP | Gravity.START;
                selBadge.setLayoutParams(bp);
                selBadge.setVisibility(View.GONE);
                ((FrameLayout) v).addView(selBadge);
            }
        }
    }
}
