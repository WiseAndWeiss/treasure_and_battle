package com.example.treasure_and_battle.ui;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
import com.example.treasure_and_battle.manager.battle.MonsterManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.manager.skill.MonsterSkillManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.MonsterTemplate;
import com.example.treasure_and_battle.model.item.consumable.ConsumableTemplate;
import com.example.treasure_and_battle.model.item.gem.GemTemplate;
import com.example.treasure_and_battle.model.item.material.MaterialTemplate;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

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
    private static final int CATEGORY_MONSTER = 4;

    private static final String[] TAB_LABELS = {"宝石", "药水", "消耗品", "材料", "怪物"};

    private static final Map<String, String> RACE_NAMES = new HashMap<>();
    static {
        RACE_NAMES.put("SLIME", "史莱姆");
        RACE_NAMES.put("WOLF", "狼");
        RACE_NAMES.put("BANDIT", "土匪");
        RACE_NAMES.put("DRAGON", "龙");
        RACE_NAMES.put("FIRE_ELEMENTAL", "火元素");
        RACE_NAMES.put("ICE_ELEMENTAL", "冰元素");
        RACE_NAMES.put("CULTIST", "教徒");
        RACE_NAMES.put("GOBLIN", "哥布林");
        RACE_NAMES.put("SKELETON", "骷髅");
    }

    private RecyclerView rvItems;
    private LinearLayout llTabContainer;
    private EncyclopediaAdapter adapter;

    private List<GemTemplate> gemList;
    private List<ConsumableTemplate> potionList;
    private List<ConsumableTemplate> consumableList;
    private List<MaterialTemplate> materialList;
    private List<Object> monsterEntries;  // String (header) or MonsterTemplate

    private int currentCategory = -1;
    private GridLayoutManager gridLayoutManager;
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

        gridLayoutManager = new GridLayoutManager(this, 3);
        rvItems.setLayoutManager(gridLayoutManager);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (currentCategory == CATEGORY_MONSTER) {
                    int viewType = adapter.getItemViewType(position);
                    return viewType == EncyclopediaAdapter.VIEW_TYPE_MONSTER_HEADER ? 3 : 1;
                }
                return 1;
            }
        });
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

        // Monster data - grouped by race
        MonsterManager mm = MonsterManager.getInstance(this);
        Map<String, List<MonsterTemplate>> raceMap = mm.getTemplatesByRace();
        monsterEntries = new ArrayList<>();
        // Sort races by name
        List<String> sortedRaces = new ArrayList<>(raceMap.keySet());
        Collections.sort(sortedRaces);
        for (String raceId : sortedRaces) {
            String raceDisplayName = RACE_NAMES.containsKey(raceId) ? RACE_NAMES.get(raceId) : raceId;
            monsterEntries.add(raceDisplayName); // header
            List<MonsterTemplate> templates = raceMap.get(raceId);
            // Sort by rarity descending
            Collections.sort(templates, (a, b) -> Integer.compare(b.getRarityId(), a.getRarityId()));
            for (MonsterTemplate mt : templates) {
                monsterEntries.add(mt);
            }
        }
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
            case CATEGORY_MONSTER:
                adapter.setData(monsterEntries, category);
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

    private void showItemDetailDialog(String title, String description, String iconAssetPath) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        float density = getResources().getDisplayMetrics().density;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = screenWidth - (int) (64 * density);
        int iconSize = (int) (80 * density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.bg_panel_treasure_fill_only);
        layout.setForeground(getResources().getDrawable(R.drawable.bg_panel_treasure_stroke_only));
        layout.setPadding((int) (13 * density), (int) (13 * density), (int) (13 * density), (int) (13 * density));
        layout.setLayoutParams(new ViewGroup.LayoutParams(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.tb_gold));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (10 * density));
        layout.addView(tvTitle);

        // Icon
        ImageView ivIcon = new ImageView(this);
        ivIcon.setAdjustViewBounds(true);
        ivIcon.setMaxHeight(iconSize);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                iconSize, iconSize);
        iconParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconParams.bottomMargin = (int) (10 * density);
        ivIcon.setLayoutParams(iconParams);
        Bitmap bmp = loadFromAssets(iconAssetPath);
        if (bmp != null) {
            ivIcon.setImageBitmap(bmp);
        }
        layout.addView(ivIcon);

        // Description
        if (description != null && !description.isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(description);
            tvDesc.setTextColor(getColor(R.color.tb_text_main));
            tvDesc.setTextSize(14);
            tvDesc.setLineSpacing((int) (4 * density), 1f);
            tvDesc.setPadding(0, (int) (4 * density), 0, (int) (4 * density));
            layout.addView(tvDesc);
        }

        // OK Button
        TextView btnOk = new TextView(this);
        btnOk.setText("确定");
        btnOk.setTextColor(getColor(R.color.tb_text_main));
        btnOk.setTextSize(15);
        btnOk.setTypeface(btnOk.getTypeface(), android.graphics.Typeface.BOLD);
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setBackgroundResource(R.drawable.bg_tab_idle);
        btnOk.setClickable(true);
        btnOk.setFocusable(true);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * density));
        btnParams.topMargin = (int) (10 * density);
        btnOk.setLayoutParams(btnParams);
        btnOk.setOnClickListener(v -> dialog.dismiss());
        layout.addView(btnOk);

        dialog.setContentView(layout);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = dialogWidth;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
        }

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
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

        private static final int VIEW_TYPE_MONSTER_HEADER = 100;
        private static final int VIEW_TYPE_MONSTER_CARD = 101;

        @Override
        public int getItemViewType(int position) {
            if (category == CATEGORY_MONSTER) {
                Object obj = items.get(position);
                return (obj instanceof String) ? VIEW_TYPE_MONSTER_HEADER : VIEW_TYPE_MONSTER_CARD;
            }
            return super.getItemViewType(position);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_MONSTER_HEADER) {
                // Header view
                TextView tv = new TextView(parent.getContext());
                tv.setTextSize(16);
                tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                tv.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.tb_gold));
                tv.setPadding(
                        (int) (12 * parent.getContext().getResources().getDisplayMetrics().density),
                        (int) (16 * parent.getContext().getResources().getDisplayMetrics().density),
                        (int) (12 * parent.getContext().getResources().getDisplayMetrics().density),
                        (int) (8 * parent.getContext().getResources().getDisplayMetrics().density));
                tv.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                return new ViewHolder(tv);
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_encyclopedia_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object obj = items.get(position);

            // MONSTER: handle header and item
            if (category == CATEGORY_MONSTER) {
                if (obj instanceof String) {
                    // Race header
                    TextView tv = (TextView) holder.itemView;
                    tv.setText((String) obj);
                    return;
                }
                MonsterTemplate mt = (MonsterTemplate) obj;
                String name = mt.getName();
                int rarityId = mt.getRarityId();
                String assetPath = "icons/monster/" + mt.getEntityId() + ".png";

                ViewGroup.LayoutParams lp = holder.ivIcon.getLayoutParams();
                lp.height = (int) (50 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
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

                final String fName = name;
                final String fAssetPath = assetPath;
                final MonsterTemplate fMt = mt;
                holder.itemView.setOnClickListener(v -> {
                    StringBuilder sb = new StringBuilder();
                    List<MonsterTemplate.SkillReference> skills = fMt.getSkillPool();
                    MonsterSkillManager msm = MonsterSkillManager.getInstance(v.getContext());
                    if (skills != null && !skills.isEmpty()) {
                        sb.append("技能：\n");
                        for (MonsterTemplate.SkillReference sr : skills) {
                            SkillTemplate st = msm.getSkillTemplateBySkillId(sr.getSkillId());
                            if (st != null) {
                                sb.append("· ").append(st.getSkillName()).append("：").append(st.getSimpleDesc()).append("\n");
                            }
                        }
                    }
                    showItemDetailDialog(fName, sb.toString(), fAssetPath);
                });
                return;
            }

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
            lp.height = (int) (50 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
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

            final String fName = name;
            final String fAssetPath = assetPath;
            final Object fObj = obj;
            holder.itemView.setOnClickListener(v -> {
                String description = "";
                switch (category) {
                    case CATEGORY_GEM: {
                        GemTemplate gem = (GemTemplate) fObj;
                        StringBuilder sb = new StringBuilder();
                        List<GemTemplate.BonusEntry> bonuses = gem.getAccessoryBonuses();
                        if (bonuses != null && !bonuses.isEmpty()) {
                            sb.append("饰品增益：\n");
                            for (GemTemplate.BonusEntry b : bonuses) {
                                sb.append("· ").append(b.type).append(" +").append(b.value).append("\n");
                            }
                        }
                        description = sb.toString();
                        break;
                    }
                    case CATEGORY_POTION:
                    case CATEGORY_CONSUMABLE: {
                        ConsumableTemplate ct = (ConsumableTemplate) fObj;
                        description = ct.getDescription();
                        break;
                    }
                    case CATEGORY_MATERIAL: {
                        MaterialTemplate mt = (MaterialTemplate) fObj;
                        Rarity r = Rarity.fromId(mt.getRarityId());
                        description = "稀有度：" + (r != null ? r.getDisplayName() : "");
                        break;
                    }
                }
                showItemDetailDialog(fName, description, fAssetPath);
            });
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
