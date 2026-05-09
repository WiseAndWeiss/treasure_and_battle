package com.example.treasure_and_battle.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.manager.EquipmentManager;
import com.example.treasure_and_battle.manager.InventoryManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.model.item.Item;

import java.util.ArrayList;
import java.util.List;

public class TradeActivity extends AppCompatActivity {

    private TextView tabSell;
    private TextView tabBuy;
    private TextView tvItemListTitle;
    private View tabIndicator;
    private TextView tvPageInfo;
    private LinearLayout gridShopContainer;

    private RecyclerView recyclerView;
    private ShopAdapter adapter;

    private boolean isSellTab = true;
    private int currentPage = 1;
    private final int itemsPerPage = 30;

    private List<Item> sellItems;
    private List<Item> buyItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trade);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tabSell = findViewById(R.id.tab_sell);
        tabBuy = findViewById(R.id.tab_buy);
        tvItemListTitle = findViewById(R.id.tv_item_list_title);
        tabIndicator = findViewById(R.id.tab_indicator);
        tvPageInfo = findViewById(R.id.tv_page_info);
        gridShopContainer = findViewById(R.id.grid_shop_container);

        initData();
        setupRecyclerView();
        setupTabs();

        updateTabState();
        updatePageUI();
    }

    private void initData() {
        sellItems = new ArrayList<>();
        List<Item> invItems = InventoryManager.getInstance().getItems();
        for (Item item : invItems) {
            sellItems.add(item);
        }
        while (sellItems.size() < 30) {
            sellItems.add(null);
        }

        buyItems = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            if (i == 0) {
                buyItems.add(EquipmentManager.getInstance(this).generateEquip(3003, 5, Rarity.RARE));
            } else if (i == 1) {
                buyItems.add(EquipmentManager.getInstance(this).generateEquip(3001, 3, Rarity.COMMON));
            } else if (i == 2) {
                buyItems.add(EquipmentManager.getInstance(this).generateEquip(3004, 8, Rarity.UNCOMMON));
            } else {
                buyItems.add(null);
            }
        }
        while (buyItems.size() < 30) {
            buyItems.add(null);
        }
    }

    private void setupRecyclerView() {
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 6));

        adapter = new ShopAdapter();
        recyclerView.setAdapter(adapter);
        gridShopContainer.addView(recyclerView);
    }

    private void setupTabs() {
        tabSell.setOnClickListener(v -> {
            if (!isSellTab) {
                isSellTab = true;
                currentPage = 1;
                updateTabState();
                updatePageUI();
            }
        });

        tabBuy.setOnClickListener(v -> {
            if (isSellTab) {
                isSellTab = false;
                currentPage = 1;
                updateTabState();
                updatePageUI();
            }
        });
    }

    private void updateTabState() {
        if (isSellTab) {
            tabSell.setTextColor(Color.parseColor("#FF9800"));
            tabBuy.setTextColor(Color.parseColor("#888888"));
            tvItemListTitle.setText("我的物品");
        } else {
            tabSell.setTextColor(Color.parseColor("#888888"));
            tabBuy.setTextColor(Color.parseColor("#FF9800"));
            tvItemListTitle.setText("商店商品");
        }
    }

    private int getTotalPages() {
        List<Item> items = isSellTab ? sellItems : buyItems;
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    private void updatePageUI() {
        tvPageInfo.setText("第" + currentPage + "页 / 共" + getTotalPages() + "页");
        adapter.notifyDataSetChanged();
    }

    private class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bag_grid, parent, false);
            int height = parent.getMeasuredHeight() / 5;
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, height > 0 ? height : ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(2, 2, 2, 2);
            view.setLayoutParams(params);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            List<Item> items = isSellTab ? sellItems : buyItems;
            int realPosition = (currentPage - 1) * itemsPerPage + position;
            Item item = realPosition < items.size() ? items.get(realPosition) : null;

            if (item == null) {
                holder.tvItemName.setText("");
                holder.tvItemLevel.setVisibility(View.GONE);
                holder.ivItemIcon.setVisibility(View.INVISIBLE);
                holder.bgItemColor.setBackgroundColor(Color.parseColor("#EAEAEA"));
            } else {
                holder.tvItemName.setText(item.getName());
                holder.ivItemIcon.setVisibility(View.VISIBLE);
                holder.ivItemIcon.setImageResource(item.getIconResId());
                holder.bgItemColor.setBackgroundColor(item.getRarity().getColor());

                if (item instanceof EquipItem) {
                    holder.tvItemLevel.setVisibility(View.VISIBLE);
                    holder.tvItemLevel.setText("Lv." + ((EquipItem) item).getLevel());
                } else {
                    holder.tvItemLevel.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (item != null) {
                    String action = isSellTab ? "出售" : "购买";
                    Toast.makeText(TradeActivity.this, action + ": " + item.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemsPerPage;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            RelativeLayout bgItemColor;
            ImageView ivItemIcon;
            TextView tvItemLevel;
            TextView tvItemName;

            ViewHolder(View itemView) {
                super(itemView);
                bgItemColor = itemView.findViewById(R.id.bg_item_color);
                ivItemIcon = itemView.findViewById(R.id.iv_item_icon);
                tvItemLevel = itemView.findViewById(R.id.tv_item_level);
                tvItemName = itemView.findViewById(R.id.tv_item_name);
            }
        }
    }
}
