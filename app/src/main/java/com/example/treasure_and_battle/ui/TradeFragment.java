package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.drawable.TreasureStyleDrawable;
import com.example.treasure_and_battle.utils.GameAssetIcons;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.material.MaterialItem;
import com.example.treasure_and_battle.model.merchant.MerchantConfig;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TradeFragment extends Fragment {

    public static final String TAG = "TradeFragment";

    private static final int MERCHANT_COLUMNS = 4;
    private static final int MERCHANT_SLOT_COUNT = MERCHANT_COLUMNS * 2;

    private TextView tvGold;
    private TradeBagBottomController tradeBagBottom;
    @Nullable
    private RecyclerView rvMerchant;
    private int merchantSquareSizePx;
    private boolean merchantGridReady;
    @Nullable
    private MerchantAdapter merchantAdapter;

    private final List<MerchantListing> listings = new ArrayList<>();
    private MerchantConfig.Type merchantType = null;
    private final Random merchantRng = new Random();

    @FunctionalInterface
    private interface PurchaseFactory {
        @Nullable
        Item createForPurchase(Context ctx);
    }

    private static final class MerchantListing {
        final Item sample;
        final int unitBuyPrice;
        final PurchaseFactory purchaseFactory;
        final boolean infiniteStock;
        int stockRemaining;

        private MerchantListing(Item sample, int unitBuyPrice, PurchaseFactory pf, boolean infinite, int startingStock) {
            this.sample = sample;
            this.unitBuyPrice = unitBuyPrice;
            this.purchaseFactory = pf;
            this.infiniteStock = infinite;
            this.stockRemaining = infinite ? 0 : startingStock;
        }

        static MerchantListing finiteStock(Item sample, int unitPrice, PurchaseFactory pf, int stock) {
            return new MerchantListing(sample, unitPrice, pf, false, stock);
        }

        static MerchantListing infiniteStock(Item sample, int unitPrice, PurchaseFactory pf) {
            return new MerchantListing(sample, unitPrice, pf, true, 0);
        }

        boolean isSoldOut() {
            return !infiniteStock && stockRemaining <= 0;
        }

        /** 仅可堆叠商品才弹出购买数量；装备/宝石等 maxStack≤1 时直接购 1 个 */
        boolean canPickQuantity() {
            return sample.canStack();
        }

        void consumeStock(int qty) {
            if (!infiniteStock) {
                stockRemaining = Math.max(0, stockRemaining - qty);
            }
        }

        @Nullable
        String stackBadgeTextForCell() {
            if (isSoldOut()) {
                return "售罄";
            }
            if (infiniteStock) {
                return canPickQuantity() ? "×∞" : null;
            }
            if (canPickQuantity()) {
                return "×" + stockRemaining;
            }
            return stockRemaining > 1 ? "×" + stockRemaining : null;
        }
    }

    void addGoldFromSell(int amount) {
        if (amount <= 0) {
            return;
        }
        tradeCharacter().addGold(amount);
        refreshGoldLabel();
    }

    @NonNull
    private Character tradeCharacter() {
        return PlayerCharacterHolder.getOrCreate(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trade, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvGold = view.findViewById(R.id.tv_trade_gold);
        view.findViewById(R.id.btn_trade_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        refreshGoldLabel();

        Bundle args = getArguments();
        if (args != null && args.containsKey("merchant_type")) {
            try {
                merchantType = MerchantConfig.Type.valueOf(args.getString("merchant_type"));
            } catch (IllegalArgumentException ignored) {
                merchantType = null;
            }
        }

        View bagSection = view.findViewById(R.id.trade_bag_bottom_section);
        tradeBagBottom = new TradeBagBottomController(this, bagSection);

        if (merchantType != null) {
            List<ItemType> filterTypes = MerchantConfig.getPlayerBagFilterTypes(merchantType);
            tradeBagBottom.setItemTypeFilter(filterTypes);
            tradeBagBottom.setCanSell(MerchantConfig.canSell(merchantType));
        }

        buildMerchantListings();

        rvMerchant = view.findViewById(R.id.rv_merchant);
        rvMerchant.setVisibility(View.INVISIBLE);
        rvMerchant.setHasFixedSize(true);
        rvMerchant.setLayoutManager(new GridLayoutManager(requireContext(), MERCHANT_COLUMNS));
        merchantAdapter = new MerchantAdapter();
        setupMerchantGridCellSizing();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tradeBagBottom != null) {
            tradeBagBottom.reloadFromInventory();
        }
        if (tvGold != null) {
            refreshGoldLabel();
        }
    }

    @Override
    public void onPause() {
        InventoryGridSync.flushSharedGridToManager(requireContext());
        super.onPause();
    }

    private static int unitSellPriceForListing(@NonNull Item item) {
        if (item.getRarity() == null) {
            return Math.max(0, item.getBaseValue());
        }
        return Math.round(item.getBaseValue() * item.getRarity().getSellPriceMultiplier());
    }

    private static int unitBuyPriceForListing(@NonNull Item sample) {
        return Math.max(1, Math.round(unitSellPriceForListing(sample) * 2.2f));
    }

    /** 固定 2×4 格：动态生成商人商品 */
    private void buildMerchantListings() {
        listings.clear();

        if (merchantType != null) {
            buildDynamicMerchantListings();
        } else {
            buildDefaultMerchantListings();
        }

        if (listings.size() > MERCHANT_SLOT_COUNT) {
            listings.subList(MERCHANT_SLOT_COUNT, listings.size()).clear();
        }
    }

    private void buildDynamicMerchantListings() {
        EquipmentManager em = EquipmentManager.getInstance(requireContext());
        ItemManager im = ItemManager.getInstance(requireContext());
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(merchantType, merchantRng);

        for (MerchantConfig.MerchantSlot slot : slots) {
            Rarity rarity = slot.rarity;
            ItemType type = slot.itemType;
            int level = 5 + merchantRng.nextInt(21);

            if (type == ItemType.EQUIPMENT) {
                EquipItem eq = em.generateRandomEquip(level, rarity);
                if (eq != null) {
                    int price = unitBuyPriceForListing(eq);
                    listings.add(MerchantListing.finiteStock(eq, price,
                            c -> eq.deepCopy(), 1));
                }
            } else if (type == ItemType.GEM) {
                GemItem gem = im.getRandomGemByRarity(rarity);
                if (gem != null) {
                    int price = unitBuyPriceForListing(gem);
                    int stock = 1 + merchantRng.nextInt(5);
                    final String gemId = gem.getId();
                    listings.add(MerchantListing.finiteStock(gem, price,
                            c -> ItemManager.getInstance(c).createGem(gemId), stock));
                }
            } else if (type == ItemType.CONSUMABLE) {
                ConsumableItem cons = im.getRandomConsumableByRarity(rarity);
                if (cons != null) {
                    int price = unitBuyPriceForListing(cons);
                    int stock = 1 + merchantRng.nextInt(8);
                    final String consId = cons.getId();
                    listings.add(MerchantListing.finiteStock(cons, price,
                            c -> ItemManager.getInstance(c).createConsumable(consId), stock));
                }
            }
        }
    }

    private void buildDefaultMerchantListings() {
        EquipmentManager em = EquipmentManager.getInstance(requireContext());

        // 装备（equip_config.json）
        addEquipListing(em, 1001, 2, Rarity.COMMON, 1);
        addEquipListing(em, 4001, 2, Rarity.COMMON, 1);
        addEquipListing(em, 3001, 2, Rarity.COMMON, 1);

        // 消耗品（consumable_config.json）
        addConsumableListing(im, "potion_hp_small", 30);
        addConsumableListing(im, "potion_mp_small", 30);
        addConsumableListing(im, "potion_hp_medium", 20);

        // 材料（material_config.json）
        addMaterialListing(im, "slime_gel_common", 99);
        addMaterialListing(im, "wolf_fang", 99);

        if (listings.size() > MERCHANT_SLOT_COUNT) {
            listings.subList(MERCHANT_SLOT_COUNT, listings.size()).clear();
        }
    }

    private void addConsumableListing(@NonNull ItemManager im, @NonNull String consumableId, int merchantStock) {
        ConsumableItem sample = im.createConsumable(consumableId);
        if (sample == null) {
            return;
        }
        sample.setCount(1);
        int price = unitBuyPriceForListing(sample);
        listings.add(MerchantListing.finiteStock(sample, price,
                c -> shopConsumableUnit(c, consumableId), merchantStock));
    }

    private void addMaterialListing(@NonNull ItemManager im, @NonNull String materialId, int merchantStock) {
        MaterialItem sample = im.createMaterial(materialId);
        if (sample == null) {
            return;
        }
        sample.setCount(1);
        int price = unitBuyPriceForListing(sample);
        listings.add(MerchantListing.finiteStock(sample, price,
                c -> shopMaterialUnit(c, materialId), merchantStock));
    }

    private void addEquipListing(EquipmentManager em, int templateId, int level, Rarity rarity, int shopStock) {
        EquipItem eq = em.generateEquip(templateId, level, rarity);
        if (eq == null) {
            return;
        }
        int price = unitBuyPriceForListing(eq);
        listings.add(MerchantListing.finiteStock(eq, price,
                c -> eq.deepCopy(), shopStock));
    }

    @Nullable
    private static MaterialItem shopMaterialUnit(@NonNull Context ctx, @NonNull String materialId) {
        MaterialItem m = ItemManager.getInstance(ctx).createMaterial(materialId);
        if (m != null) {
            m.setCount(1);
        }
        return m;
    }

    @Nullable
    private static ConsumableItem shopConsumableUnit(@NonNull Context ctx, @NonNull String consumableId) {
        ConsumableItem c = ItemManager.getInstance(ctx).createConsumable(consumableId);
        if (c != null) {
            c.setCount(1);
        }
        return c;
    }

    private void refreshGoldLabel() {
        if (tvGold == null) {
            return;
        }
        tvGold.setText("金币：" + tradeCharacter().getGold());
    }

    private void setupMerchantGridCellSizing() {
        if (rvMerchant == null) {
            return;
        }
        rvMerchant.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                applyMerchantGridCellSizeIfReady();
            }
        });
        scheduleMerchantGridCellSizeApply();
    }

    private void scheduleMerchantGridCellSizeApply() {
        if (rvMerchant == null) {
            return;
        }
        rvMerchant.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!applyMerchantGridCellSizeIfReady()) {
                    return true;
                }
                if (!merchantGridReady) {
                    merchantGridReady = true;
                    rvMerchant.getViewTreeObserver().removeOnPreDrawListener(this);
                    rvMerchant.requestLayout();
                    return false;
                }
                return true;
            }
        });
    }

    private boolean applyMerchantGridCellSizeIfReady() {
        if (rvMerchant == null || merchantAdapter == null) {
            return false;
        }
        int width = rvMerchant.getWidth()
                - rvMerchant.getPaddingLeft() - rvMerchant.getPaddingRight();
        int height = rvMerchant.getHeight()
                - rvMerchant.getPaddingTop() - rvMerchant.getPaddingBottom();
        if (width <= 0 || height <= 0) {
            return false;
        }

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int resolved = BagGridCellSizer.resolveMerchantSquareSizePx(dm, width, height);
        if (resolved <= 0) {
            return false;
        }

        boolean sizeChanged = resolved != merchantSquareSizePx;
        merchantSquareSizePx = resolved;
        merchantAdapter.setSquareSizePx(merchantSquareSizePx);

        if (rvMerchant.getAdapter() == null) {
            rvMerchant.setAdapter(merchantAdapter);
            rvMerchant.setVisibility(View.VISIBLE);
        } else if (sizeChanged) {
            merchantAdapter.notifyItemRangeChanged(0, merchantAdapter.getItemCount(), "CELL_SIZE");
        }
        return true;
    }

    private int dpToPx(int dp) {
        return BagGridCellSizer.dpToPx(getResources().getDisplayMetrics(), dp);
    }

    private int computeMaxPurchasableQty(@NonNull MerchantListing listing) {
        int maxByStock = listing.infiniteStock ? 999 : listing.stockRemaining;
        maxByStock = Math.min(maxByStock, 999);
        int unit = listing.unitBuyPrice;
        int maxByGold = unit <= 0 ? maxByStock : tradeCharacter().getGold() / unit;
        int empty = tradeBagBottom != null ? tradeBagBottom.countEmptySlots() : 0;
        int maxByBag;
        if (!listing.sample.canStack()) {
            maxByBag = empty;
        } else {
            int ms = Math.max(1, listing.sample.getMaxStack());
            maxByBag = empty * ms;
        }
        int q = Math.min(Math.min(maxByStock, maxByGold), maxByBag);
        return Math.max(0, q);
    }

    private boolean wouldFitInBag(@NonNull MerchantListing listing, int qty) {
        int empty = tradeBagBottom != null ? tradeBagBottom.countEmptySlots() : 0;
        if (qty <= 0) {
            return false;
        }
        if (!listing.sample.canStack()) {
            return qty <= empty;
        }
        int maxStack = Math.max(1, listing.sample.getMaxStack());
        int slotsNeeded = (qty + maxStack - 1) / maxStack;
        return slotsNeeded <= empty;
    }

    private boolean placePurchasedItems(@NonNull MerchantListing listing, int qty) {
        if (!listing.sample.canStack()) {
            for (int i = 0; i < qty; i++) {
                Item one = listing.purchaseFactory.createForPurchase(requireContext());
                if (one == null || tradeBagBottom == null || !tradeBagBottom.tryPutInFirstEmptySlot(one)) {
                    return false;
                }
            }
            return true;
        }
        int maxStack = Math.max(1, listing.sample.getMaxStack());
        int left = qty;
        while (left > 0) {
            int chunk = Math.min(left, maxStack);
            Item stack = listing.purchaseFactory.createForPurchase(requireContext());
            if (stack == null || tradeBagBottom == null) {
                return false;
            }
            stack.setCount(chunk);
            if (!tradeBagBottom.tryPutInFirstEmptySlot(stack)) {
                return false;
            }
            left -= chunk;
        }
        return true;
    }

    private void tryPurchaseWithQuantity(@NonNull MerchantListing listing, int qty) {
        if (listing.isSoldOut()) {
            showFloatMsg("已售罄");
            return;
        }
        if (qty <= 0) {
            return;
        }
        if (!listing.infiniteStock && qty > listing.stockRemaining) {
            showFloatMsg("库存不足");
            return;
        }
        long totalLong = (long) listing.unitBuyPrice * qty;
        if (totalLong > Integer.MAX_VALUE) {
            showFloatMsg("数量过多");
            return;
        }
        int total = (int) totalLong;
        Character ch = tradeCharacter();
        if (ch.getGold() < total) {
            showFloatMsg("金币不足");
            return;
        }
        if (!wouldFitInBag(listing, qty)) {
            showFloatMsg("背包空位不足");
            return;
        }
        if (!placePurchasedItems(listing, qty)) {
            showFloatMsg("背包已满");
            return;
        }
        if (!ch.spendGold(total)) {
            showFloatMsg("金币不足");
            return;
        }
        listing.consumeStock(qty);
        // 购买只改了共享网格，须立刻写回 InventoryManager；否则 Trade.onResume 的 reload 或切回背包会读到旧列表
        InventoryGridSync.flushSharedGridToManager(requireContext());
        refreshGoldLabel();
        if (merchantAdapter != null) {
            merchantAdapter.notifyDataSetChanged();
        }
        showFloatMsg("已购买 ×" + qty + "（合计 " + total + " 金）");
    }

    private void promptPurchase(@NonNull MerchantListing listing) {
        if (listing.isSoldOut()) {
            showFloatMsg("已售罄");
            return;
        }
        if (!listing.canPickQuantity()) {
            tryPurchaseWithQuantity(listing, 1);
            return;
        }
        int maxQ = computeMaxPurchasableQty(listing);
        if (maxQ <= 0) {
            showFloatMsg("金币不足或背包空位不足");
            return;
        }
        showBuyQuantityDialog(listing, maxQ);
    }

    private void showBuyQuantityDialog(@NonNull MerchantListing listing, int maxQty) {
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_merchant_buy_quantity, null);
        TextView tvName = root.findViewById(R.id.tv_buy_item_name);
        TextView tvInfo = root.findViewById(R.id.tv_buy_unit_and_stock);
        TextView tvQty = root.findViewById(R.id.tv_buy_quantity);
        TextView tvTotal = root.findViewById(R.id.tv_buy_total);
        SeekBar seek = root.findViewById(R.id.seek_buy_quantity);

        tvName.setText(listing.sample.getName());
        String stockStr = listing.infiniteStock ? "库存：充足（不限量）" : ("库存剩余：" + listing.stockRemaining);
        tvInfo.setText("单价 " + listing.unitBuyPrice + " 金/个 · " + stockStr + " · 单格最多堆叠 " + listing.sample.getMaxStack());

        int safeMax = Math.max(1, maxQty);
        seek.setMax(safeMax - 1);
        seek.setProgress(0);

        Runnable refresh = () -> {
            int qty = seek.getProgress() + 1;
            tvQty.setText(String.valueOf(qty));
            tvTotal.setText("合计 " + (listing.unitBuyPrice * qty) + " 金");
        };
        refresh.run();

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                refresh.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog);
        builder.setView(root);
        AlertDialog dialog = builder.create();
        root.findViewById(R.id.btn_merchant_buy_cancel).setOnClickListener(v -> dialog.dismiss());
        root.findViewById(R.id.btn_merchant_buy_confirm).setOnClickListener(v -> {
            tryPurchaseWithQuantity(listing, seek.getProgress() + 1);
            dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private class MerchantAdapter extends RecyclerView.Adapter<MerchantAdapter.Vh> {

        private int squareSizePx = ViewGroup.LayoutParams.WRAP_CONTENT;

        void setSquareSizePx(int squareSizePx) {
            this.squareSizePx = squareSizePx;
        }

        private int itemHeightPx() {
            if (squareSizePx <= 0) {
                return ViewGroup.LayoutParams.WRAP_CONTENT;
            }
            return BagGridCellSizer.merchantItemHeightPx(getResources().getDisplayMetrics(), squareSizePx);
        }

        private void applyCellLayoutParams(@NonNull Vh h) {
            if (squareSizePx <= 0) {
                return;
            }
            ViewGroup.LayoutParams itemLp = h.itemView.getLayoutParams();
            int targetHeight = itemHeightPx();
            if (itemLp instanceof RecyclerView.LayoutParams) {
                RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) itemLp;
                if (p.height != targetHeight) {
                    p.height = targetHeight;
                    h.itemView.setLayoutParams(p);
                }
            }
            ViewGroup.LayoutParams squareLp = h.merchantSlotSquare.getLayoutParams();
            if (squareLp.width != squareSizePx || squareLp.height != squareSizePx) {
                squareLp.width = squareSizePx;
                squareLp.height = squareSizePx;
                h.merchantSlotSquare.setLayoutParams(squareLp);
            }
        }

        @NonNull
        @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_merchant_cell, parent, false);
            int spacing = dpToPx(BagGridCellSizer.MERCHANT_CELL_SPACING_DP);
            int height = itemHeightPx();
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height > 0 ? height : ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(spacing, spacing, spacing, spacing);
            row.setLayoutParams(params);
            return new Vh(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh h, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                onBindViewHolder(h, position);
                return;
            }
            for (Object payload : payloads) {
                if ("CELL_SIZE".equals(payload)) {
                    applyCellLayoutParams(h);
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull Vh h, int position) {
            applyCellLayoutParams(h);
            MerchantListing listing = listings.get(position);
            Item item = listing.sample;
            boolean stackSale = listing.canPickQuantity();
            h.price.setText(stackSale ? (listing.unitBuyPrice + " 金/个") : (listing.unitBuyPrice + " 金"));

            if (item instanceof EquipItem) {
                EquipItem eq = (EquipItem) item;
                h.level.setVisibility(View.VISIBLE);
                h.level.setText("Lv." + eq.getLevel());
            } else {
                h.level.setVisibility(View.GONE);
            }

            h.name.setText(item.getName());
            GameAssetIcons.bindItem(requireContext(), h.icon, item);
            h.bg.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
            h.bg.setBackgroundTintList(null);
            Integer borderArgb = item.getRarity() != null ? item.getRarity().getColor() : null;
            h.merchantSlotSquare.setForeground(
                    TreasureStyleDrawable.newSlotStrokeOverlay(requireContext(), borderArgb));

            String stackBadge = listing.stackBadgeTextForCell();
            if (stackBadge == null) {
                h.stock.setVisibility(View.GONE);
            } else {
                h.stock.setVisibility(View.VISIBLE);
                h.stock.setText(stackBadge);
            }
            boolean sold = listing.isSoldOut();
            h.itemView.setAlpha(sold ? 0.42f : 1f);

            h.itemView.setOnClickListener(v -> showMerchantItemMenu(v, listing));
        }

        @Override
        public int getItemCount() {
            return listings.size();
        }

        class Vh extends RecyclerView.ViewHolder {
            final FrameLayout merchantSlotSquare;
            final RelativeLayout bg;
            final ImageView icon;
            final TextView name;
            final TextView level;
            final TextView price;
            final TextView stock;

            Vh(@NonNull View itemView) {
                super(itemView);
                merchantSlotSquare = itemView.findViewById(R.id.merchant_slot_square);
                bg = itemView.findViewById(R.id.bg_item_color);
                icon = itemView.findViewById(R.id.iv_item_icon);
                name = itemView.findViewById(R.id.tv_item_name);
                level = itemView.findViewById(R.id.tv_item_level);
                price = itemView.findViewById(R.id.tv_merchant_price);
                stock = itemView.findViewById(R.id.tv_merchant_stock);
            }
        }
    }

    private void showMerchantItemMenu(View anchor, MerchantListing listing) {
        if (listing.isSoldOut()) {
            showFloatMsg("已售罄");
            return;
        }
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, "查看详情");
        menu.getMenu().add(0, 2, 0, "购买");
        menu.setOnMenuItemClickListener(mi -> {
            if (mi.getItemId() == 1) {
                ItemDetailDialog.show(requireContext(), listing.sample);
                return true;
            }
            if (mi.getItemId() == 2) {
                promptPurchase(listing);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showFloatMsg(String text) {
        if (!isAdded() || getActivity() == null) return;
        FloatMsgOverlay.showFloatMsg(getActivity(), text);
    }
}
