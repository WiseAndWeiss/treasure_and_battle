package com.example.treasure_and_battle.event.handler;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.drawable.TreasureStyleDrawable;
import com.example.treasure_and_battle.event.NeutralEventResolver;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.ui.NeutralEventActivity;
import com.example.treasure_and_battle.utils.GameAssetIcons;

import java.util.ArrayList;
import java.util.List;

/**
 * 雕像祝福 — 选择一个稀有度低于传说的宝石进行升级。
 */
public class StatueBlessingHandler extends BaseEventHandler {

    public StatueBlessingHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.clearButtons();

        ui.addButton("接受雕像祝福", 0xFFFFC107, v -> showGemUpgradeDialog());
        ui.addButton("绕道离开", 0xFF888888, v -> {
            ui.showResult("你绕过了雕像，没有接受祝福。");
            ui.switchToForwardButton();
        });
    }

    private void showGemUpgradeDialog() {
        Character ch = ui.getCharacter();
        Context ctx = ui.getContext();
        List<Item> bag = ch.getBagItems();

        final List<GemItem> gemItems = new ArrayList<>();
        for (Item item : bag) {
            if (item instanceof GemItem) {
                GemItem gem = (GemItem) item;
                if (gem.getRarity().getId() < 4) {
                    gemItems.add(gem);
                }
            }
        }
        if (gemItems.isEmpty()) {
            ui.showResult("你的背包中没有可升级的宝石。\n（传说品质宝石已无法继续升级）");
            ui.switchToForwardButton();
            return;
        }

        final Dialog gridDialog = new Dialog(ctx);
        gridDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View gridView = LayoutInflater.from(ctx).inflate(R.layout.dialog_gem_grid, null);
        RecyclerView rv = gridView.findViewById(R.id.rv_gem_grid);
        gridView.findViewById(R.id.btn_grid_close).setOnClickListener(v -> gridDialog.dismiss());

        rv.setLayoutManager(new GridLayoutManager(ctx, 2));
        GemGridAdapter gemAdapter = new GemGridAdapter(gemItems, gem -> {
            gridDialog.dismiss();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                NeutralEventResolver.GemUpgradeResult r =
                        NeutralEventResolver.resolveGemUpgrade(gem, ch, ctx);
                if (!r.success) {
                    ui.showResult(r.errorMessage);
                    ui.switchToForwardButton();
                    return;
                }
                String resultText = "雕像散发出耀眼的金色光芒...\n\n"
                        + gem.getName() + "（" + gem.getRarity().getDisplayName()
                        + "）已升级为\n" + r.upgradedGem.getName() + "（"
                        + r.upgradedGem.getRarity().getDisplayName() + "）！";
                ui.showResult(resultText);
                ui.switchToForwardButton();
            });
        });

        gridDialog.setContentView(gridView);
        gridDialog.setCancelable(true);
        gridDialog.setCanceledOnTouchOutside(true);
        gridDialog.show();
        NeutralEventActivity.applyTransparentDialogWindow(gridDialog);
        NeutralEventActivity.scheduleDialogItemGrid(rv, gemAdapter, gemAdapter::setGridLayout);
    }

    private static class GemGridAdapter extends RecyclerView.Adapter<GemGridAdapter.VH> {
        private final List<GemItem> items;
        private final OnGemClickListener listener;
        private int recyclerWidthPx;
        private int squareSizePx;

        interface OnGemClickListener {
            void onClick(GemItem item);
        }

        GemGridAdapter(List<GemItem> items, OnGemClickListener listener) {
            this.items = items;
            this.listener = listener;
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
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gem_select, parent, false);
            v.setLayoutParams(NeutralEventActivity.newSquareGridCellLp(parent.getContext(), squareSizePx, recyclerWidthPx));
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            NeutralEventActivity.applySquareGridCellLayout(holder.itemView, squareSizePx, recyclerWidthPx);
            GemItem gem = items.get(position);
            holder.tvRarity.setText(gem.getRarity().getDisplayName());
            holder.tvRarity.setTextColor(gem.getRarity().getColor());
            GameAssetIcons.bindItem(holder.itemView.getContext(), holder.ivIcon, gem);
            int c = gem.getCount();
            if (c > 1) {
                holder.tvCount.setVisibility(View.VISIBLE);
                holder.tvCount.setText("×" + c);
            } else {
                holder.tvCount.setVisibility(View.GONE);
            }
            holder.bgColor.setBackgroundTintList(null);
            android.graphics.drawable.Drawable bg = holder.bgColor.getBackground();
            if (bg != null) {
                bg.clearColorFilter();
            }
            Integer borderArgb = gem.getRarity() != null ? gem.getRarity().getColor() : null;
            holder.itemView.setForeground(
                    TreasureStyleDrawable.newSlotStrokeOverlay(holder.itemView.getContext(), borderArgb));
            holder.itemView.setOnClickListener(v -> listener.onClick(gem));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            View bgColor;
            TextView tvRarity, tvCount;
            ImageView ivIcon;

            VH(View v) {
                super(v);
                bgColor = v.findViewById(R.id.bg_item_color);
                tvRarity = v.findViewById(R.id.tv_item_rarity);
                tvCount = v.findViewById(R.id.tv_bag_stack_count);
                ivIcon = v.findViewById(R.id.iv_item_icon);
            }
        }
    }
}
