package com.example.treasure_and_battle.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.model.skill.SkillEffectParams;
import com.example.treasure_and_battle.model.skill.SkillRangeType;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.model.profession.Profession;
import com.example.treasure_and_battle.manager.character.ProfessionManager;
import com.example.treasure_and_battle.model.profession.ProfessionType;
import com.example.treasure_and_battle.skill.SkillTree;
import com.example.treasure_and_battle.utils.GameAssetIcons;
import com.example.treasure_and_battle.utils.TachieManager;

import java.util.ArrayList;
import java.util.List;

public class ProfessionSelectActivity extends AppCompatActivity {

    private static final ProfessionType[] PROFESSIONS = {
            ProfessionType.WARRIOR,
            ProfessionType.MAGE,
            ProfessionType.RANGER
    };

    private int currentIndex;
    private Profession currentPreviewProfession;

    private ImageView ivTachie;
    private TextView tvName;
    private TextView tabPassive;
    private TextView tabActive;
    private RecyclerView rvSkills;
    private SkillPreviewAdapter adapter;
    private int currentTabIndex;
    private EditText etCharacterName;
    private boolean isNewGame;

    private final List<SkillPreviewRow> currentSkillList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profession_select);

        isNewGame = "new_game".equals(getIntent().getStringExtra("mode"));

        View header = findViewById(R.id.profession_header);
        View body = findViewById(R.id.profession_body);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.tb_bg_dark));

        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), status.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        final int bodyPaddingBottom = body.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(body, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    bodyPaddingBottom + nav.bottom);
            return insets;
        });

        Typeface zpix = ResourcesCompat.getFont(this, R.font.zpix);
        TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);

        ivTachie = findViewById(R.id.iv_profession_tachie);
        tvName = findViewById(R.id.tv_profession_name);
        tabPassive = findViewById(R.id.tab_passive);
        tabActive = findViewById(R.id.tab_active);
        rvSkills = findViewById(R.id.rv_skills);
        etCharacterName = findViewById(R.id.et_character_name);
        TextView btnLeft = findViewById(R.id.btn_arrow_left);
        TextView btnRight = findViewById(R.id.btn_arrow_right);
        TextView btnConfirm = findViewById(R.id.btn_confirm_profession);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnLeft.setTypeface(zpix);
        btnRight.setTypeface(zpix);
        if (zpix != null && tvToolbarTitle != null) {
            tvToolbarTitle.setTypeface(zpix, Typeface.BOLD);
        }

        Character ch = PlayerCharacterHolder.getOrCreate(this);
        etCharacterName.setText(ch.getName());

        btnBack.setOnClickListener(v -> finish());

        currentIndex = 0;
        rvSkills.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SkillPreviewAdapter(currentSkillList);
        rvSkills.setAdapter(adapter);

        btnLeft.setOnClickListener(v -> {
            currentIndex = (currentIndex - 1 + PROFESSIONS.length) % PROFESSIONS.length;
            loadProfession();
        });
        btnRight.setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % PROFESSIONS.length;
            loadProfession();
        });

        tabPassive.setOnClickListener(v -> selectTab(0));
        tabActive.setOnClickListener(v -> selectTab(1));

        btnConfirm.setOnClickListener(v -> {
            String name = etCharacterName.getText() != null ? etCharacterName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                FloatMsgOverlay.showFloatMsg(this, "角色名不能为空");
                return;
            }
            int chineseCount = 0;
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c >= '\u4e00' && c <= '\u9fff') chineseCount++;
            }
            if (chineseCount < 2 || chineseCount > 8) {
                FloatMsgOverlay.showFloatMsg(this, "角色名需包含2~8个中文字符");
                return;
            }

            ProfessionType chosenType = PROFESSIONS[currentIndex];

            if (isNewGame) {
                Character newCh = new Character(1, name, chosenType, getApplicationContext());
                PlayerCharacterHolder.restoreFrom(newCh);
                FloatMsgOverlay.showFloatMsg(this, "冒险开始！职业: " + chosenType.name());
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }

            ch.setName(name);
            Profession newProfession = ProfessionManager.getInstance(this)
                    .createProfession(chosenType);
            ch.switchProfession(chosenType, newProfession);
            FloatMsgOverlay.showFloatMsg(this, "已切换职业: " + newProfession.getProfessionName());
            finish();
        });

        selectTab(0);
        loadProfession();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ivTachie != null) {
            ivTachie.setImageDrawable(null);
        }
        TachieManager.recycle();
    }

    private void loadProfession() {
        ProfessionType type = PROFESSIONS[currentIndex];
        currentPreviewProfession = ProfessionManager.getInstance(this).createProfession(type);
        if (currentPreviewProfession == null) return;

        tvName.setText(currentPreviewProfession.getProfessionName());

        int tachieRes = android.R.drawable.ic_menu_gallery;
        TachieManager.bind(this, ivTachie, type, tachieRes);

        reloadSkillsForCurrentTab();
    }

    private void selectTab(int index) {
        currentTabIndex = index;
        resetTabStyle(tabPassive);
        resetTabStyle(tabActive);
        if (index == 0) {
            highlightTab(tabPassive);
        } else {
            highlightTab(tabActive);
        }
        reloadSkillsForCurrentTab();
    }

    private void reloadSkillsForCurrentTab() {
        currentSkillList.clear();
        if (currentPreviewProfession == null) {
            adapter.notifyDataSetChanged();
            return;
        }

        SkillTree tree;
        String categoryLabel;
        if (currentTabIndex == 0) {
            tree = currentPreviewProfession.getPassiveSkillTree();
            categoryLabel = "被动";
        } else {
            tree = currentPreviewProfession.getActiveSkillTree();
            categoryLabel = "主动";
        }

        SkillManager sm = SkillManager.getInstance(this);
        for (String skillId : tree.getAllSkillIds()) {
            SkillTemplate template = sm.getSkillTemplateBySkillId(skillId);
            if (template == null) continue;

            String effectLv1 = formatEffectBlock(template, 1);
            String effectLv5 = template.getMaxLevel() >= 5
                    ? formatEffectBlock(template, 5) : formatEffectBlock(template, template.getMaxLevel());

            String cooldown = template.getCooldown() <= 0 ? "无" : template.getCooldown() + " 回合";
            String range = rangeLabel(template.getSkillRangeType());

            currentSkillList.add(new SkillPreviewRow(
                    skillId,
                    template.getSkillName(),
                    template.getSimpleDesc(),
                    categoryLabel,
                    effectLv1,
                    effectLv5,
                    template.getMaxLevel(),
                    cooldown,
                    range));
        }

        adapter.notifyDataSetChanged();
    }

    private static String formatEffectBlock(SkillTemplate template, int level) {
        if (level <= 0) return "（未学习）";
        SkillEffectParams p = template.getEffectParamsWithLevel(level);
        String raw = template.getDetailedDesc() != null ? template.getDetailedDesc() : template.getSimpleDesc();
        return raw.replace("{x}", String.valueOf(p.x))
                .replace("{y}", String.valueOf(p.y))
                .replace("{z}", String.valueOf(p.z))
                .replace("{w}", String.valueOf(p.w));
    }

    private static String rangeLabel(@Nullable SkillRangeType r) {
        if (r == null) return "无目标";
        switch (r) {
            case SINGLE_ENEMY:  return "单体敌方";
            case ALL_ENEMIES:   return "全体敌方";
            case SELF:          return "自身";
            case ALL_ALLIES:    return "全体友方";
            case NONE:
            default:            return "无目标";
        }
    }

    private void resetTabStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_tab_idle);
        tv.setTextColor(ContextCompat.getColor(this, R.color.tb_text_sub));
        Typeface zpix = ResourcesCompat.getFont(this, R.font.zpix);
        tv.setTypeface(zpix, Typeface.NORMAL);
    }

    private void highlightTab(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_tab_active);
        tv.setTextColor(ContextCompat.getColor(this, R.color.tb_bg_dark));
        Typeface zpix = ResourcesCompat.getFont(this, R.font.zpix);
        tv.setTypeface(zpix, Typeface.BOLD);
    }

    static class SkillPreviewRow {
        final String skillId;
        final String name;
        final String desc;
        final String category;
        final String effectLv1;
        final String effectLv5;
        final int maxLevel;
        final String cooldown;
        final String range;

        SkillPreviewRow(String skillId, String name, String desc, String category,
                        String effectLv1, String effectLv5, int maxLevel,
                        String cooldown, String range) {
            this.skillId = skillId;
            this.name = name;
            this.desc = desc;
            this.category = category;
            this.effectLv1 = effectLv1;
            this.effectLv5 = effectLv5;
            this.maxLevel = maxLevel;
            this.cooldown = cooldown;
            this.range = range;
        }
    }

    private class SkillPreviewAdapter extends RecyclerView.Adapter<SkillPreviewAdapter.Vh> {
        private final List<SkillPreviewRow> data;

        SkillPreviewAdapter(List<SkillPreviewRow> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_skill, parent, false);
            return new Vh(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh holder, int position) {
            SkillPreviewRow item = data.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.desc);
            holder.tvLevel.setText("1/" + item.maxLevel);
            GameAssetIcons.bindSkill(ProfessionSelectActivity.this, holder.ivIcon,
                    item.skillId, android.R.drawable.ic_menu_gallery);

            holder.btnAdd.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                SkillDetailDialog.Detail detail = new SkillDetailDialog.Detail(
                        item.name,
                        item.category,
                        "",
                        "1~" + item.maxLevel + "级",
                        1,
                        item.cooldown,
                        item.range,
                        "Lv1: " + item.effectLv1,
                        "Lv" + item.maxLevel + ": " + item.effectLv5,
                        item.desc);
                SkillDetailDialog.show(ProfessionSelectActivity.this, detail);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Vh extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvDesc;
            final TextView tvLevel;
            final ImageView ivIcon;
            final ImageView btnAdd;

            Vh(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_skill_name);
                tvDesc = itemView.findViewById(R.id.tv_skill_desc);
                tvLevel = itemView.findViewById(R.id.tv_skill_level);
                ivIcon = itemView.findViewById(R.id.iv_skill_icon);
                btnAdd = itemView.findViewById(R.id.btn_skill_action);
            }
        }
    }
}
