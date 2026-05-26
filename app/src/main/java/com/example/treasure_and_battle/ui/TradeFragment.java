package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

        RecyclerView rv = view.findViewById(R.id.rv_merchant);
        rv.setHasFixedSize(true);
        rv.setNestedScrollingEnabled(false);
        GridLayoutManager glm = new GridLayoutManager(requireContext(), MERCHANT_COLUMNS) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        rv.setLayoutManager(glm);
        merchantAdapter = new MerchantAdapter();
        rv.setAdapter(merchantAdapter);
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

        addEquipListing(em, 3001, 2, Rarity.COMMON, 1);
        addEquipListing(em, 3002, 4, Rarity.COMMON, 1);
        addEquipListing(em, 3003, 5, Rarity.UNCOMMON, 1);

        addConsumableShop("shop_hp_s", "小型治疗药水", Rarity.COMMON, 15, 24,
                true, true, "恢复少量生命值。", android.R.drawable.ic_menu_day, 60);
        addConsumableShop("shop_mp_s", "小型法力药水", Rarity.COMMON, 18, 24,
                true, true, "恢复少量法力值。", android.R.drawable.ic_menu_compass, 60);
        addConsumableShop("shop_hp_m", "中型治疗药水", Rarity.UNCOMMON, 45, 16,
                true, true, "恢复中量生命值。", android.R.drawable.ic_menu_recent_history, 32);

        addMaterialShop("shop_iron_sand", "粗铁砂", Rarity.COMMON, 8, 99, "商店补给", android.R.drawable.ic_menu_edit, 99);
        addMaterialShop("shop_arcane_dust", "奥术粉尘", Rarity.UNCOMMON, 40, 50, "商店补给", R.drawable.ic_map, 48);
    }

    private void addConsumableShop(String id, String name, Rarity rarity, int baseValue, int maxStack,
                                   boolean inBattle, boolean outBattle, String description, int iconResId, int merchantStock) {
        ConsumableItem sample = new ConsumableItem(id, name, rarity, baseValue, maxStack,
                inBattle, outBattle, new ArrayList<>(), description);
        sample.setIconResId(iconResId);
        sample.setCount(1);
        int price = unitBuyPriceForListing(sample);
        listings.add(MerchantListing.finiteStock(sample, price, c -> consumableShopUnit(sample), merchantStock));
    }

    private void addMaterialShop(String id, String name, Rarity rarity, int baseValue, int maxStack,
                                 String dropFrom, int iconResId, int merchantStock) {
        MaterialItem sample = new MaterialItem(id, name, rarity, baseValue, maxStack, dropFrom);
        sample.setIconResId(iconResId);
        sample.setCount(1);
        int price = unitBuyPriceForListing(sample);
        listings.add(MerchantListing.finiteStock(sample, price, c -> materialShopUnit(sample), merchantStock));
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

    private static MaterialItem materialShopUnit(MaterialItem proto) {
        MaterialItem m = new MaterialItem(proto.getId(), proto.getName(), proto.getRarity(), proto.getBaseValue(),
                proto.getMaxStack(), proto.getDropFrom());
        m.setIconResId(proto.getIconResId());
        m.setCount(1);
        return m;
    }

    private static ConsumableItem consumableShopUnit(ConsumableItem proto) {
        ConsumableItem c = new ConsumableItem(proto.getId(), proto.getName(), proto.getRarity(), proto.getBaseValue(),
                proto.getMaxStack(), proto.isUsableInBattle(), proto.isUsableOutBattle(),
                new ArrayList<>(proto.getEffects()), proto.getDescription());
        c.setIconResId(proto.getIconResId());
        c.setCount(1);
        return c;
    }

    private void refreshGoldLabel() {
        if (tvGold == null) {
            return;
        }
        tvGold.setText("金币：" + tradeCharacter().getGold());
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
        View btnMinus = root.findViewById(R.id.btn_qty_minus);
        View btnPlus = root.findViewById(R.id.btn_qty_plus);

        tvName.setText(listing.sample.getName());
        String stockStr = listing.infiniteStock ? "库存：充足（不限量）" : ("库存剩余：" + listing.stockRemaining);
        tvInfo.setText("单价 " + listing.unitBuyPrice + " 金/个 · " + stockStr + " · 单格最多堆叠 " + listing.sample.getMaxStack());

        final int[] qtyHolder = {1};
        Runnable refresh = () -> {
            tvQty.setText(String.valueOf(qtyHolder[0]));
            int t = listing.unitBuyPrice * qtyHolder[0];
            tvTotal.setText("合计 " + t + " 金");
        };
        refresh.run();

        btnMinus.setOnClickListener(v -> {
            if (qtyHolder[0] > 1) {
                qtyHolder[0]--;
                refresh.run();
            }
        });
        btnPlus.setOnClickListener(v -> {
            if (qtyHolder[0] < maxQty) {
                qtyHolder[0]++;
                refresh.run();
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog);
        builder.setView(root);
        AlertDialog dialog = builder.create();
        root.findViewById(R.id.btn_merchant_buy_cancel).setOnClickListener(v -> dialog.dismiss());
        root.findViewById(R.id.btn_merchant_buy_confirm).setOnClickListener(v -> {
            tryPurchaseWithQuantity(listing, qtyHolder[0]);
            dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private class MerchantAdapter extends RecyclerView.Adapter<MerchantAdapter.Vh> {

        @NonNull
        @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_merchant_cell, parent, false);
            return new Vh(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh h, int position) {
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
        menu.getMenu().add(0, 1, 0, "查看描述");
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
