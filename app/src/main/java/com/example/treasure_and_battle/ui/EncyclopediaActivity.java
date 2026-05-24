package com.example.treasure_and_battle.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.consumable.ConsumableTemplate;
import com.example.treasure_and_battle.model.item.gem.GemTemplate;
import com.example.treasure_and_battle.model.item.material.MaterialTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncyclopediaActivity extends AppCompatActivity {

    private static final int CATEGORY_GEM = 0;
    private static final int CATEGORY_POTION = 1;
    private static final int CATEGORY_CONSUMABLE = 2;
    private static final int CATEGORY_MATERIAL = 3;

    private static final String[] TAB_LABELS = {"宝石", "药水", "消耗品", "材料"};

    private RecyclerView rvItems;
    private LinearLayout llTabContainer;
    private EncyclopediaAdapter adapter;

    private List<GemTemplate> gemList;
    private List<ConsumableTemplate> potionList;
    private List<ConsumableTemplate> consumableList;
    private List<MaterialTemplate> materialList;

    private int currentCategory = -1;
    private List<Button> tabButtons = new ArrayList<>();
    private Map<String, Bitmap> imageCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encyclopedia);

        View header = findViewById(R.id.encyclopedia_header);
        View list = findViewById(R.id.rv_items);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.tb_bg_dark));

        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), status.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        final int listPaddingBottom = list.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(list, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    listPaddingBottom + nav.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        llTabContainer = findViewById(R.id.ll_tab_container);
        rvItems = findViewById(R.id.rv_items);

        loadData();
        createTabs();

        rvItems.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new EncyclopediaAdapter();
        rvItems.setAdapter(adapter);

        switchToCategory(CATEGORY_GEM);
    }

    private void loadData() {
        ItemManager im = ItemManager.getInstance(this);

        gemList = im.getAllGemTemplates();
        Collections.sort(gemList, (a, b) -> Integer.compare(b.getRarityId(), a.getRarityId()));

        List<ConsumableTemplate> allConsumables = im.getAllConsumableTemplates();
        potionList = new ArrayList<>();
        consumableList = new ArrayList<>();
        for (ConsumableTemplate ct : allConsumables) {
            String id = ct.getConsumableId();
            if (id.startsWith("potion_") || id.equals("antidote") || id.equals("crystal_mana")) {
                potionList.add(ct);
            } else {
                consumableList.add(ct);
            }
        }
        Collections.sort(potionList, (a, b) -> Integer.compare(b.getRarityId(), a.getRarityId()));
        Collections.sort(consumableList, (a, b) -> Integer.compare(b.getRarityId(), a.getRarityId()));

        materialList = im.getAllMaterialTemplates();
        Collections.sort(materialList, (a, b) -> Integer.compare(b.getRarityId(), a.getRarityId()));
    }

    private void createTabs() {
        tabButtons.clear();
        llTabContainer.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int tabHeight = (int) (40 * density);
        int paddingH = (int) (4 * density);
        int paddingV = (int) (4 * density);
        int marginH = (int) (4 * density);

        for (int i = 0; i < TAB_LABELS.length; i++) {
            Button btn = new Button(this);
            btn.setText(TAB_LABELS[i]);
            btn.setTextSize(13);
            btn.setAllCaps(false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, tabHeight, 1f);
            params.setMargins(marginH, 0, marginH, 0);
            btn.setLayoutParams(params);
            btn.setPadding(paddingH, paddingV, paddingH, paddingV);

            int categoryIndex = i;
            btn.setOnClickListener(v -> switchToCategory(categoryIndex));

            tabButtons.add(btn);
            llTabContainer.addView(btn);
        }
    }

    private void switchToCategory(int category) {
        if (currentCategory == category) return;
        currentCategory = category;

        for (int i = 0; i < tabButtons.size(); i++) {
            Button btn = tabButtons.get(i);
            if (i == category) {
                btn.setTextColor(getColor(R.color.tb_panel));
                btn.setBackgroundResource(R.drawable.bg_entry_primary_btn);
            } else {
                btn.setTextColor(getColor(R.color.tb_panel_soft));
                btn.setBackgroundResource(R.drawable.bg_entry_secondary_btn);
            }
        }

        switch (category) {
            case CATEGORY_GEM:
                adapter.setData(gemList, category);
                break;
            case CATEGORY_POTION:
                adapter.setData(potionList, category);
                break;
            case CATEGORY_CONSUMABLE:
                adapter.setData(consumableList, category);
                break;
            case CATEGORY_MATERIAL:
                adapter.setData(materialList, category);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Bitmap bmp : imageCache.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        imageCache.clear();
    }

    private Bitmap loadFromAssets(String path) {
        Bitmap cached = imageCache.get(path);
        if (cached != null) {
            return cached;
        }
        InputStream is = null;
        try {
            is = getAssets().open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp != null) {
                imageCache.put(path, bmp);
            }
            return bmp;
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private class EncyclopediaAdapter extends RecyclerView.Adapter<EncyclopediaAdapter.ViewHolder> {

        private List<?> items;
        private int category;

        void setData(List<?> items, int category) {
            this.items = items;
            this.category = category;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_encyclopedia_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object obj = items.get(position);

            String name;
            int rarityId;
            String assetPath;

            switch (category) {
                case CATEGORY_GEM: {
                    GemTemplate gem = (GemTemplate) obj;
                    name = gem.getName();
                    rarityId = gem.getRarityId();
                    assetPath = "icons/gem/" + gem.getGemId() + ".png";
                    break;
                }
                case CATEGORY_POTION:
                case CATEGORY_CONSUMABLE: {
                    ConsumableTemplate ct = (ConsumableTemplate) obj;
                    name = ct.getName();
                    rarityId = ct.getRarityId();
                    assetPath = "icons/consumable/" + ct.getConsumableId() + ".png";
                    break;
                }
                case CATEGORY_MATERIAL: {
                    MaterialTemplate mt = (MaterialTemplate) obj;
                    name = mt.getName();
                    rarityId = mt.getRarityId();
                    assetPath = "icons/material/" + mt.getMaterialId() + ".png";
                    break;
                }
                default:
                    return;
            }

            ViewGroup.LayoutParams lp = holder.ivIcon.getLayoutParams();
            if (category == CATEGORY_POTION || category == CATEGORY_CONSUMABLE) {
                lp.height = (int) (50 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            } else {
                lp.height = (int) (100 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            }
            holder.ivIcon.setLayoutParams(lp);

            Rarity rarity = Rarity.fromId(rarityId);
            holder.tvName.setText(name);
            holder.tvRarity.setText(rarity != null ? rarity.getDisplayName() : "");
            int rarityColor = rarity != null ? rarity.getColor() : 0xFFFFFFFF;
            holder.tvRarity.setTextColor(rarityColor);

            Bitmap bmp = imageCache.get(assetPath);
            if (bmp == null) {
                bmp = loadFromAssets(assetPath);
            }
            if (bmp != null) {
                holder.ivIcon.setImageBitmap(bmp);
            } else {
                holder.ivIcon.setImageResource(R.drawable.bg_item_placeholder);
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName;
            TextView tvRarity;

            ViewHolder(View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_item_icon);
                tvName = itemView.findViewById(R.id.tv_item_name);
                tvRarity = itemView.findViewById(R.id.tv_item_rarity);
            }
        }
    }
}
