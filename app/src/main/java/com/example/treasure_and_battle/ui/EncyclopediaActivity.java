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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.consumable.ConsumableTemplate;
import com.example.treasure_and_battle.model.item.gem.GemTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncyclopediaActivity extends AppCompatActivity {

    private RecyclerView rvItems;
    private Button btnTabGem;
    private Button btnTabPotion;
    private EncyclopediaAdapter adapter;

    private List<GemTemplate> gemList;
    private List<ConsumableTemplate> potionList;

    private boolean showingGem = true;
    private Map<String, Bitmap> imageCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encyclopedia);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        btnTabGem = findViewById(R.id.btn_tab_gem);
        btnTabPotion = findViewById(R.id.btn_tab_potion);
        rvItems = findViewById(R.id.rv_items);

        loadData();

        rvItems.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new EncyclopediaAdapter();
        rvItems.setAdapter(adapter);

        btnTabGem.setOnClickListener(v -> switchToGem());
        btnTabPotion.setOnClickListener(v -> switchToPotion());

        adapter.setData(gemList, true);
    }

    private void loadData() {
        ItemManager im = ItemManager.getInstance(this);

        gemList = im.getAllGemTemplates();

        List<ConsumableTemplate> allConsumables = im.getAllConsumableTemplates();
        potionList = new ArrayList<>();
        for (ConsumableTemplate ct : allConsumables) {
            String id = ct.getConsumableId();
            if (id.startsWith("potion_") || id.equals("antidote") || id.equals("crystal_mana")) {
                potionList.add(ct);
            }
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

    private void switchToGem() {
        if (showingGem) return;
        showingGem = true;
        btnTabGem.setTextColor(getColor(R.color.tb_panel));
        btnTabGem.setBackgroundResource(R.drawable.bg_entry_primary_btn);
        btnTabPotion.setTextColor(getColor(R.color.tb_text_main));
        btnTabPotion.setBackgroundResource(R.drawable.bg_entry_secondary_btn);
        adapter.setData(gemList, true);
    }

    private void switchToPotion() {
        if (!showingGem) return;
        showingGem = false;
        btnTabPotion.setTextColor(getColor(R.color.tb_panel));
        btnTabPotion.setBackgroundResource(R.drawable.bg_entry_primary_btn);
        btnTabGem.setTextColor(getColor(R.color.tb_text_main));
        btnTabGem.setBackgroundResource(R.drawable.bg_entry_secondary_btn);
        adapter.setData(potionList, false);
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
        private boolean isGem;

        void setData(List<?> items, boolean isGem) {
            this.items = items;
            this.isGem = isGem;
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

            if (isGem) {
                GemTemplate gem = (GemTemplate) obj;
                name = gem.getName();
                rarityId = gem.getRarityId();
                assetPath = "icons/gem/" + gem.getGemId() + ".png";
            } else {
                ConsumableTemplate potion = (ConsumableTemplate) obj;
                name = potion.getName();
                rarityId = potion.getRarityId();
                assetPath = "icons/consumable/" + potion.getConsumableId() + ".png";
            }

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
