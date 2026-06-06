package com.example.treasure_and_battle.event.handler;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.drawable.TreasureStyleDrawable;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.manager.affix.EquipAffixManager;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.ui.NeutralEventActivity;
import com.example.treasure_and_battle.utils.GameAssetIcons;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备重炼 — 选择一件装备，挑选一个词条重新随机。
 */
public class EquipmentReforgeHandler extends BaseEventHandler {

    public EquipmentReforgeHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.clearButtons();

        ui.addButton("选择装备重炼", 0xFF9C27B0, v -> showEquipmentSelectionDialog());
        ui.addButton("暂时不需要", 0xFF888888, v -> {
            ui.showResult("你婉拒了铁匠大师的好意。");
            ui.switchToForwardButton();
        });
    }

    private void showEquipmentSelectionDialog() {
        Character ch = ui.getCharacter();
        Context ctx = ui.getContext();
        List<Item> bag = ch.getBagItems();
        if (InventoryManager.isEmpty(bag)) {
            EquipmentManager em = EquipmentManager.getInstance(ctx);
            int equipLevel = ch.getLevel() + com.example.treasure_and_battle.utils.RandomUtils.getRandomInt(-3, 3);
            equipLevel = Math.max(1, equipLevel);
            InventoryManager.addItem(bag, em.generateEquip(em.getRandomTemplateId(), equipLevel, Rarity.COMMON));
            InventoryManager.addItem(bag, em.generateEquip(em.getRandomTemplateId(), equipLevel, Rarity.UNCOMMON));
            InventoryManager.addItem(bag, em.generateEquip(em.getRandomTemplateId(), equipLevel, Rarity.RARE));
            InventoryManager.addItem(bag, em.generateEquip(em.getRandomTemplateId(), equipLevel, Rarity.EPIC));
        }

        final List<EquipItem> equipItems = new ArrayList<>();
        for (Item item : bag) {
            if (item instanceof EquipItem) {
                equipItems.add((EquipItem) item);
            }
        }
        if (equipItems.isEmpty()) {
            ui.showResult("你的背包中没有可重炼的装备。");
            return;
        }

        final Dialog gridDialog = new Dialog(ctx);
        gridDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View gridView = LayoutInflater.from(ctx).inflate(R.layout.dialog_equip_grid, null);
        RecyclerView rv = gridView.findViewById(R.id.rv_equip_grid);
        gridView.findViewById(R.id.btn_grid_close).setOnClickListener(v -> gridDialog.dismiss());

        rv.setLayoutManager(new GridLayoutManager(ctx, 2));
        EquipGridAdapter equipAdapter = new EquipGridAdapter(equipItems, equip -> {
            gridDialog.dismiss();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> showReforgeDialog(equip));
        });

        gridDialog.setContentView(gridView);
        gridDialog.setCancelable(true);
        gridDialog.setCanceledOnTouchOutside(true);
        gridDialog.show();
        NeutralEventActivity.applyTransparentDialogWindow(gridDialog);
        NeutralEventActivity.scheduleDialogItemGrid(rv, equipAdapter, equipAdapter::setGridLayout);
    }

    private void showReforgeDialog(EquipItem equip) {
        Context ctx = ui.getContext();
        final EquipItem targetEquip = equip;
        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_equip_reforge, null);
        d.setContentView(content);
        final Window w = d.getWindow();
        if (w != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }
        View reforgeRoot = content.findViewById(R.id.reforge_dialog_root);
        ImageView ivPreview = content.findViewById(R.id.iv_equip_preview);
        TextView tvName = content.findViewById(R.id.tv_equip_name);
        TextView tvRarity = content.findViewById(R.id.tv_equip_rarity);
        TextView tvLevel = content.findViewById(R.id.tv_equip_level);
        LinearLayout llAffixList = content.findViewById(R.id.ll_affix_list);
        ScrollView svAffix = content.findViewById(R.id.sv_affix_container);
        TextView btnAction = content.findViewById(R.id.btn_reforge_action);
        TextView btnCancel = content.findViewById(R.id.btn_reforge_cancel);
        boolean[] hasReforged = {false};
        int[] selIdx = {-1};
        final BaseAffix[] previousAffixAtSelected = {null};
        GameAssetIcons.bindItem(ctx, ivPreview, targetEquip);
        tvName.setText(targetEquip.getName());
        tvRarity.setText(targetEquip.getRarity().getDisplayName());
        tvRarity.setTextColor(targetEquip.getRarity().getColor());
        tvLevel.setText("Lv." + targetEquip.getLevel());
        styleReforgeActionButton(btnAction, false, false);
        View.OnClickListener dismissIfNotDone = v -> {
            if (!hasReforged[0]) d.dismiss();
        };
        content.findViewById(R.id.btn_reforge_close).setOnClickListener(dismissIfNotDone);
        final RefineClick[] cbRef = {null};
        cbRef[0] = idx -> {
            selIdx[0] = idx;
            styleReforgeActionButton(btnAction, true, false);
            updateRefineList(llAffixList, targetEquip.getAffixes(), selIdx, hasReforged, cbRef[0],
                    previousAffixAtSelected[0]);
            fitReforgeAffixScrollHeight(svAffix, llAffixList, reforgeRoot, w);
        };
        updateRefineList(llAffixList, targetEquip.getAffixes(), selIdx, hasReforged, cbRef[0],
                previousAffixAtSelected[0]);
        fitReforgeAffixScrollHeight(svAffix, llAffixList, reforgeRoot, w);
        btnAction.setOnClickListener(v -> {
            if (hasReforged[0]) {
                d.dismiss();
                ui.showResult("装备重炼完成！");
                ui.switchToForwardButton();
                return;
            }
            int idx = selIdx[0];
            if (idx < 0 || idx >= targetEquip.getAffixes().size()) return;
            BaseAffix oldAffix = targetEquip.getAffixes().get(idx);
            BaseEquipAffix newAffix = EquipAffixManager.getInstance(ctx).generateSingleAffixForEquipment(targetEquip);
            if (newAffix != null) {
                previousAffixAtSelected[0] = oldAffix;
                targetEquip.getAffixes().set(idx, newAffix);
            }
            hasReforged[0] = true;
            updateRefineList(llAffixList, targetEquip.getAffixes(), selIdx, hasReforged, null,
                    previousAffixAtSelected[0]);
            fitReforgeAffixScrollHeight(svAffix, llAffixList, reforgeRoot, w);
            svAffix.post(() -> svAffix.fullScroll(View.FOCUS_DOWN));
            styleReforgeActionButton(btnAction, true, true);
            btnCancel.setVisibility(View.GONE);
            LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) btnAction.getLayoutParams();
            lp2.setMarginStart(0);
            lp2.setMarginEnd(0);
            btnAction.setLayoutParams(lp2);
        });
        btnCancel.setOnClickListener(dismissIfNotDone);
        d.show();
        NeutralEventActivity.applyTransparentDialogWindow(d);
    }

    private void fitReforgeAffixScrollHeight(ScrollView sv, LinearLayout affixList, View root, Window window) {
        if (sv == null || affixList == null || root == null) return;
        Context ctx = ui.getContext();
        final int maxDialogPx = (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.66f);
        root.post(() -> {
            int width = sv.getWidth() > 0 ? sv.getWidth() : root.getWidth();
            affixList.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int affixH = affixList.getMeasuredHeight() + sv.getPaddingTop() + sv.getPaddingBottom();

            ViewGroup.LayoutParams svLp = sv.getLayoutParams();
            svLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            sv.setLayoutParams(svLp);
            root.requestLayout();
            root.post(() -> {
                int otherH = root.getHeight() - sv.getHeight();
                int maxScrollH = Math.max(NeutralEventActivity.dpToPx(ctx, 48), maxDialogPx - otherH);
                if (affixH > maxScrollH) {
                    svLp.height = maxScrollH;
                } else {
                    svLp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                sv.setLayoutParams(svLp);
                root.requestLayout();
                if (window != null) {
                    root.post(() -> {
                        int h = root.getHeight();
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.height = h > maxDialogPx ? maxDialogPx : WindowManager.LayoutParams.WRAP_CONTENT;
                        window.setAttributes(lp);
                    });
                }
            });
        });
    }

    private void styleReforgeActionButton(TextView btn, boolean enabled, boolean confirm) {
        Context ctx = ui.getContext();
        if (confirm) {
            btn.setEnabled(true);
            btn.setText("确定");
            btn.setBackgroundResource(R.drawable.bg_tab_active);
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.tb_bg_dark));
        } else if (enabled) {
            btn.setEnabled(true);
            btn.setText("重炼此词条");
            btn.setBackgroundResource(R.drawable.bg_tab_active);
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.tb_bg_dark));
        } else {
            btn.setEnabled(false);
            btn.setText("请选择要重炼的词条");
            btn.setBackgroundResource(R.drawable.bg_tab_idle);
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.tb_text_sub));
        }
    }

    private interface RefineClick { void onClick(int index); }

    private static String formatAffixLine(BaseAffix affix) {
        return (affix != null ? affix.getAffixName() : "???") + "："
                + (affix != null ? affix.getDescription() : "...");
    }

    private static int affixDotColor(BaseAffix affix) {
        return (affix != null && affix.getRarity() != null)
                ? affix.getRarity().getColor()
                : 0xFF888888;
    }

    private LinearLayout createAffixLineRow(BaseAffix affix, int textColor, boolean strikethrough) {
        Context ctx = ui.getContext();
        LinearLayout lineRow = new LinearLayout(ctx);
        lineRow.setOrientation(LinearLayout.HORIZONTAL);
        lineRow.setGravity(Gravity.CENTER_VERTICAL);
        lineRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView dot = new TextView(ctx);
        dot.setText("●");
        dot.setTextColor(affixDotColor(affix));
        dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lineRow.addView(dot);

        TextView line = new TextView(ctx);
        line.setText(formatAffixLine(affix));
        line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        line.setTextColor(strikethrough ? ColorUtils.setAlphaComponent(textColor, 170) : textColor);
        if (strikethrough) {
            line.setPaintFlags(line.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lineLp.setMarginStart(NeutralEventActivity.dpToPx(ctx, 8));
        line.setLayoutParams(lineLp);
        lineRow.addView(line);
        return lineRow;
    }

    private void updateRefineList(LinearLayout container, List<? extends BaseAffix> affixes,
            int[] selIdx, boolean[] hasReforged, RefineClick cb, BaseAffix previousAffixAtSelected) {
        Context ctx = ui.getContext();
        container.removeAllViews();
        int pad = NeutralEventActivity.dpToPx(ctx, 10);
        if (affixes == null || affixes.isEmpty()) {
            TextView tv = new TextView(ctx);
            tv.setText("(无词条)");
            tv.setTextSize(13);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.tb_text_sub));
            tv.setPadding(pad, pad, pad, pad);
            container.addView(tv);
            return;
        }
        boolean done = hasReforged[0];
        int sel = selIdx[0];
        for (int i = 0; i < affixes.size(); i++) {
            int idx = i;
            BaseAffix a = affixes.get(i);
            boolean selected = idx == sel;
            boolean showReforgeCompare = done && selected && previousAffixAtSelected != null;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = NeutralEventActivity.dpToPx(ctx, 4);
            row.setLayoutParams(rowLp);
            row.setPadding(pad, pad, pad, pad);
            if (selected) {
                row.setBackgroundResource(R.drawable.bg_tab_active);
            } else {
                row.setBackgroundResource(R.drawable.bg_panel_treasure_fill_only);
                row.setForeground(ContextCompat.getDrawable(ctx, R.drawable.bg_panel_treasure_stroke_only));
            }
            int textColor = ContextCompat.getColor(ctx,
                    selected ? R.color.tb_bg_dark : R.color.tb_text_main);

            if (showReforgeCompare) {
                LinearLayout compareCol = new LinearLayout(ctx);
                compareCol.setOrientation(LinearLayout.VERTICAL);
                compareCol.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                compareCol.addView(createAffixLineRow(previousAffixAtSelected, textColor, true));
                LinearLayout newRow = createAffixLineRow(a, textColor, false);
                LinearLayout.LayoutParams newRowLp = (LinearLayout.LayoutParams) newRow.getLayoutParams();
                newRowLp.topMargin = NeutralEventActivity.dpToPx(ctx, 4);
                newRow.setLayoutParams(newRowLp);
                compareCol.addView(newRow);
                row.addView(compareCol);
            } else {
                int dotColor = affixDotColor(a);
                TextView dot = new TextView(ctx);
                dot.setText("●");
                dot.setTextColor(dotColor);
                dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                row.addView(dot);

                TextView line = new TextView(ctx);
                line.setText(formatAffixLine(a));
                line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                line.setTextColor(textColor);
                LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lineLp.setMarginStart(NeutralEventActivity.dpToPx(ctx, 8));
                line.setLayoutParams(lineLp);
                row.addView(line);
            }
            if (!done && cb != null) row.setOnClickListener(v -> cb.onClick(idx));
            container.addView(row);
        }
    }

    private static class EquipGridAdapter extends RecyclerView.Adapter<EquipGridAdapter.VH> {
        final List<EquipItem> items;
        final OnEquipClickListener listener;
        private int recyclerWidthPx;
        private int squareSizePx;

        interface OnEquipClickListener { void onClick(EquipItem item); }

        EquipGridAdapter(List<EquipItem> items, OnEquipClickListener l) { this.items = items; this.listener = l; }

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
            EquipItem equip = items.get(pos);
            holder.tvName.setText(equip.getName());
            holder.tvName.setTextSize(14);
            holder.tvLevel.setText("Lv." + equip.getLevel());
            holder.tvLevel.setTextSize(12);
            GameAssetIcons.bindItem(holder.itemView.getContext(), holder.ivIcon, equip);
            holder.bgColor.setBackgroundTintList(null);
            android.graphics.drawable.Drawable bg = holder.bgColor.getBackground();
            if (bg != null) bg.clearColorFilter();
            holder.itemView.setForeground(TreasureStyleDrawable.newSlotStrokeOverlay(holder.itemView.getContext(),
                    equip.getRarity() != null ? equip.getRarity().getColor() : null));
            holder.itemView.setOnClickListener(v -> listener.onClick(equip));
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            View bgColor; TextView tvName, tvLevel; ImageView ivIcon;
            VH(View v) { super(v); bgColor = v.findViewById(R.id.bg_item_color); tvName = v.findViewById(R.id.tv_item_name); tvLevel = v.findViewById(R.id.tv_item_level); ivIcon = v.findViewById(R.id.iv_item_icon); }
        }
    }
}
