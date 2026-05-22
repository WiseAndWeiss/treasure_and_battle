package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.PlayerManager;
import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.skill.SkillEffectParams;
import com.example.treasure_and_battle.model.skill.SkillRangeType;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.model.skill.SkillType;
import com.example.treasure_and_battle.profession.Profession;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.SkillTree;
import com.example.treasure_and_battle.utils.AttributeUtils;
import com.example.treasure_and_battle.utils.GameAssetIcons;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkillFragment extends Fragment {

    private TextView tabPassive;
    private TextView tabActive;

    private RecyclerView rvSkills;
    private SkillAdapter adapter;
    private boolean compactMode;

    private LinearLayout layoutStatsLeft;
    private LinearLayout layoutStatsRight;
    private TextView tvRemainingTalent;
    private TextView tvRemainingSkillPoints;

    private TextView tvTalentStr;
    private TextView tvTalentAgi;
    private TextView tvTalentInt;
    private TextView tvTalentSpr;
    private TextView tvTalentPhy;
    private TextView tvTalentLuc;

    private Character character;
    private int currentTabIndex;

    private final List<SkillListRow> currentSkillList = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        character = PlayerCharacterHolder.getOrCreate(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skill, container, false);

        tabPassive = view.findViewById(R.id.tab_passive);
        tabActive = view.findViewById(R.id.tab_active);
        rvSkills = view.findViewById(R.id.rv_skills);
        compactMode = getResources().getConfiguration().smallestScreenWidthDp < 380;

        layoutStatsLeft = view.findViewById(R.id.layout_stats_left);
        layoutStatsRight = view.findViewById(R.id.layout_stats_right);
        tvRemainingTalent = view.findViewById(R.id.tv_remaining_points);
        tvRemainingSkillPoints = view.findViewById(R.id.tv_remaining_skill_points);

        tvTalentStr = view.findViewById(R.id.tv_talent_str);
        tvTalentAgi = view.findViewById(R.id.tv_talent_agi);
        tvTalentInt = view.findViewById(R.id.tv_talent_int);
        tvTalentSpr = view.findViewById(R.id.tv_talent_spr);
        tvTalentPhy = view.findViewById(R.id.tv_talent_phy);
        tvTalentLuc = view.findViewById(R.id.tv_talent_luc);

        rvSkills.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SkillAdapter(this, currentSkillList, compactMode);
        rvSkills.setAdapter(adapter);
        applyResponsiveUi(view);

        tabPassive.setOnClickListener(v -> selectTab(0));
        tabActive.setOnClickListener(v -> selectTab(1));

        PlayerManager pm = PlayerManager.getInstance(requireContext());
        view.findViewById(R.id.btn_add_str).setOnClickListener(v -> tryAllocateTalent(pm, "STRENGTH"));
        view.findViewById(R.id.btn_add_agi).setOnClickListener(v -> tryAllocateTalent(pm, "AGILITY"));
        view.findViewById(R.id.btn_add_int).setOnClickListener(v -> tryAllocateTalent(pm, "INTELLIGENCE"));
        view.findViewById(R.id.btn_add_spr).setOnClickListener(v -> tryAllocateTalent(pm, "SPIRIT"));
        view.findViewById(R.id.btn_add_phy).setOnClickListener(v -> tryAllocateTalent(pm, "PHYSIQUE"));
        view.findViewById(R.id.btn_add_luc).setOnClickListener(v -> tryAllocateTalent(pm, "LUCK"));

        view.findViewById(R.id.btn_view_bonuses).setOnClickListener(v -> showBonusDialog());

        selectTab(0);
        refreshCharacterPanels();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshWhenVisible();
    }

    /**
     * 主界面用 {@link androidx.fragment.app.FragmentTransaction#hide} / {@code show} 切换 Tab 时，
     * 被隐藏的 Fragment 往往不会再次走 {@link #onResume()}，从交易等页面返回后切回技能页需在变为可见时刷新。
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshWhenVisible();
        }
    }

    private void refreshWhenVisible() {
        if (getContext() != null) {
            character = PlayerCharacterHolder.getOrCreate(requireContext());
        }
        refreshCharacterPanels();
        reloadSkillsForCurrentTab();
    }

    private void tryAllocateTalent(PlayerManager pm, String attributeName) {
        if (character == null) {
            return;
        }
        if (!pm.allocateTalentPoint(character, attributeName)) {
            showFloatMsg("天赋点不足或分配失败");
            return;
        }
        refreshCharacterPanels();
    }

    private void refreshCharacterPanels() {
        if (character == null || layoutStatsLeft == null || layoutStatsRight == null) {
            return;
        }
        Profession profession = character.getProfession();
        String jobName = profession != null ? profession.getProfessionName() : "—";

        AttributeSet fa = AttributeUtils.calculateCharacterAttributes(character);

        layoutStatsLeft.removeAllViews();
        layoutStatsRight.removeAllViews();

        addStatRow(layoutStatsLeft, "职业", jobName, R.color.tb_gold);
        addStatRow(layoutStatsLeft, "等级", String.valueOf(character.getLevel()), R.color.tb_text_main);
        addStatRow(layoutStatsLeft, "名称", character.getName(), R.color.tb_text_main);
        addStatRow(layoutStatsLeft, "血量",
                character.getCurrentHp() + " / " + fa.maxHp, R.color.tb_battle);
        addStatRow(layoutStatsLeft, "魔力",
                character.getCurrentMp() + " / " + fa.maxMp, R.color.tb_battle);
        addStatRow(layoutStatsLeft, "金币", String.valueOf(character.getGold()), R.color.tb_gold);
        addSeparator(layoutStatsLeft, "非战斗属性");
        addStatRow(layoutStatsLeft, "金币加成", percent1d(fa.goldBonus), R.color.tb_gold);
        addStatRow(layoutStatsLeft, "经验加成", percent1d(fa.expBonus), R.color.tb_text_main);
        addStatRow(layoutStatsLeft, "掉落加成", "+" + (int) fa.lootRarityBonus, R.color.tb_text_main);
        addStatRow(layoutStatsLeft, "蓝耗减免", percent0d(fa.mpCostReduction), R.color.tb_text_main);
        addStatRow(layoutStatsLeft, "减伤", percent1d(fa.damageReductionRate), R.color.tb_battle);
        addStatRow(layoutStatsLeft, "速度", String.valueOf(fa.speed), R.color.tb_text_main);

        addSeparator(layoutStatsRight, "战斗属性");
        addStatRow(layoutStatsRight, "物攻", String.valueOf(fa.physicalAtk), R.color.tb_battle);
        addStatRow(layoutStatsRight, "魔攻", String.valueOf(fa.magicalAtk), R.color.tb_battle);
        addStatRow(layoutStatsRight, "物防", String.valueOf(fa.physicalDef), R.color.tb_text_main);
        addStatRow(layoutStatsRight, "魔防", String.valueOf(fa.magicalDef), R.color.tb_text_main);
        addStatRow(layoutStatsRight, "物理暴击率", percent1d(fa.physicalCritRate), R.color.tb_battle);
        addStatRow(layoutStatsRight, "魔法暴击率", percent1d(fa.magicalCritRate), R.color.tb_battle);
        addStatRow(layoutStatsRight, "物理暴伤", percent1d(fa.physicalCritDmg), R.color.tb_battle);
        addStatRow(layoutStatsRight, "魔法暴伤", percent1d(fa.magicalCritDmg), R.color.tb_battle);
        addStatRow(layoutStatsRight, "命中率", percent1d(fa.hitRate), R.color.tb_text_main);
        addStatRow(layoutStatsRight, "闪避率", percent1d(fa.dodgeRate), R.color.tb_text_main);
        addStatRow(layoutStatsRight, "异常抵抗", percent0d(fa.debuffResist), R.color.tb_text_main);

        if (tvTalentStr != null) {
            tvTalentStr.setText("力量 " + fa.strength);
            tvTalentAgi.setText("敏捷 " + fa.agility);
            tvTalentInt.setText("智力 " + fa.intelligence);
            tvTalentSpr.setText("精神 " + fa.spirit);
            tvTalentPhy.setText("体魄 " + fa.physique);
            tvTalentLuc.setText("幸运 " + fa.luck);
        }
        if (tvRemainingTalent != null) {
            tvRemainingTalent.setText("剩余天赋点: " + character.getTalentPoints());
        }
        if (tvRemainingSkillPoints != null) {
            tvRemainingSkillPoints.setText("剩余技能点: " + character.getSkillPoints());
        }
    }

    private void addStatRow(LinearLayout parent, String label, String value, int valueColorRes) {
        if (parent == null) return;
        TextView row = new TextView(requireContext());
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, compactMode ? 11f : 13f);
        row.setText(label + "：");
        row.append(applyColor(value, valueColorRes));
        row.setLineSpacing(dpToPx(2), 1f);
        parent.addView(row);
    }

    private CharSequence applyColor(String text, int colorRes) {
        int color = ContextCompat.getColor(requireContext(), colorRes);
        android.text.SpannableString ss = new android.text.SpannableString(text);
        ss.setSpan(new android.text.style.ForegroundColorSpan(color), 0, text.length(), 0);
        return ss;
    }

    private void addSeparator(LinearLayout parent, String title) {
        if (parent == null) return;
        View sep = new View(requireContext());
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)));
        sep.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tb_divider));
        parent.addView(sep);

        TextView label = new TextView(requireContext());
        label.setText(title);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, compactMode ? 10f : 12f);
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.tb_gold));
        label.setGravity(Gravity.CENTER);
        parent.addView(label);
    }

    private static String percent0d(float rate01) {
        return String.format(Locale.CHINA, "%.0f%%", rate01 * 100f);
    }

    private static String percent1d(float rate01) {
        return String.format(Locale.CHINA, "%.1f%%", rate01 * 100f);
    }

    private static String percentLabel(float rate01) {
        return String.format(Locale.CHINA, "%.0f%%", rate01 * 100f);
    }

    private void applyResponsiveUi(View root) {
        if (!compactMode) {
            return;
        }
        setTextSizeSp(tabPassive, 13f);
        setTextSizeSp(tabActive, 13f);

        TextView tvRemainingSkill = root.findViewById(R.id.tv_remaining_skill_points);
        if (tvRemainingSkill != null) {
            setTextSizeSp(tvRemainingSkill, 11f);
        }
        rvSkills.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
    }

    private void setTextSizeSp(TextView textView, float sp) {
        if (textView == null) {
            return;
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
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
        if (character == null) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return;
        }
        Profession profession = character.getProfession();
        if (profession == null) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return;
        }

        SkillTree tree;
        String categoryLabel;
        if (currentTabIndex == 0) {
            tree = profession.getPassiveSkillTree();
            categoryLabel = "被动";
        } else {
            tree = profession.getActiveSkillTree();
            categoryLabel = "主动";
        }

        SkillManager sm = SkillManager.getInstance(requireContext());
        for (String skillId : tree.getAllSkillIds()) {
            SkillTemplate template = sm.getSkillTemplateBySkillId(skillId);
            if (template == null) {
                continue;
            }
            Skill learned = profession.getLearnedSkillById(skillId);
            int curLevel = learned != null ? learned.getLevel() : 0;
            int maxLevel = template.getMaxLevel();

            boolean canUpgrade = character.getSkillPoints() > 0 && profession.canLevelUpSkill(skillId);

            String levelDisplay = curLevel + "/" + maxLevel;
            String tags = buildTags(template);
            String cooldown = template.getCooldown() <= 0 ? "无" : template.getCooldown() + " 回合";
            String range = rangeLabel(template.getSkillRangeType());

            String effectCurrent = formatEffectBlock(template, curLevel);
            String effectNext;
            if (curLevel >= maxLevel) {
                effectNext = null;
            } else {
                effectNext = formatEffectBlock(template, curLevel + 1);
            }

            currentSkillList.add(new SkillListRow(
                    skillId,
                    template.getSkillName(),
                    template.getSimpleDesc(),
                    levelDisplay,
                    categoryLabel,
                    tags,
                    1,
                    cooldown,
                    range,
                    effectCurrent,
                    effectNext,
                    canUpgrade));
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (tvRemainingSkillPoints != null && character != null) {
            tvRemainingSkillPoints.setText("剩余技能点: " + character.getSkillPoints());
        }
    }

    void onSkillUpgradeClicked(@NonNull SkillListRow row) {
        if (character == null) {
            return;
        }
        Profession profession = character.getProfession();
        if (profession == null) {
            return;
        }
        if (character.getSkillPoints() <= 0) {
            showFloatMsg("技能点不足");
            return;
        }
        if (!profession.canLevelUpSkill(row.skillId)) {
            showFloatMsg("当前无法学习或升级该技能");
            return;
        }
        if (!profession.levelUpSkill(row.skillId)) {
            showFloatMsg("升级失败");
            return;
        }
        if (!SkillUiBridge.trySpendOneSkillPoint(character)) {
            showFloatMsg("技能点扣减异常，请重进游戏");
        }
        showFloatMsg("已升级：" + row.name);
        refreshCharacterPanels();
        reloadSkillsForCurrentTab();
    }

    private static String buildTags(SkillTemplate template) {
        SkillType type = template.getSkillType();
        String typePart = type == SkillType.ACTIVE ? "主动" : type == SkillType.PASSIVE ? "被动" : "事件";
        String triggerPart = "—";
        if (template.getSkillTriggerTypes() != null && !template.getSkillTriggerTypes().isEmpty()) {
            triggerPart = String.valueOf(template.getSkillTriggerTypes().get(0));
        }
        return typePart + " · " + triggerPart;
    }

    private static String rangeLabel(@Nullable SkillRangeType r) {
        if (r == null) {
            return "—";
        }
        switch (r) {
            case SINGLE_ENEMY:
                return "单体敌方";
            case ALL_ENEMIES:
                return "全体敌方";
            case SELF:
                return "自身";
            case ALL_ALLIES:
                return "全体友方";
            case NONE:
            default:
                return "无目标";
        }
    }

    private static String formatEffectBlock(SkillTemplate template, int level) {
        if (level <= 0) {
            return "（未学习）";
        }
        SkillEffectParams p = template.getEffectParamsWithLevel(level);
        String raw = template.getDetailedDesc() != null ? template.getDetailedDesc() : template.getSimpleDesc();
        return applyEffectPlaceholders(raw, p);
    }

    private static String applyEffectPlaceholders(String templateText, SkillEffectParams p) {
        if (templateText == null) {
            return "";
        }
        return templateText
                .replace("{x}", String.valueOf(p.x))
                .replace("{y}", String.valueOf(p.y))
                .replace("{z}", String.valueOf(p.z))
                .replace("{w}", String.valueOf(p.w));
    }

    private void resetTabStyle(TextView tv) {
        if (tv == null) {
            return;
        }
        tv.setBackgroundResource(R.drawable.bg_tab_idle);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.tb_text_sub));
        Typeface zpix = ResourcesCompat.getFont(requireContext(), R.font.zpix);
        tv.setTypeface(zpix, Typeface.NORMAL);
    }

    private void highlightTab(TextView tv) {
        if (tv == null) {
            return;
        }
        tv.setBackgroundResource(R.drawable.bg_tab_active);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.tb_bg_dark));
        Typeface zpix = ResourcesCompat.getFont(requireContext(), R.font.zpix);
        tv.setTypeface(zpix, Typeface.BOLD);
    }

    // ================== 列表数据 ==================

    static final class SkillListRow {
        final String skillId;
        final String name;
        final String desc;
        final String levelDisplay;
        final String category;
        final String tags;
        final int skillPointPerLevel;
        final String cooldown;
        final String castRange;
        final String effectCurrent;
        @Nullable
        final String effectNext;
        final boolean canPressAction;

        SkillListRow(
                String skillId,
                String name,
                String desc,
                String levelDisplay,
                String category,
                String tags,
                int skillPointPerLevel,
                String cooldown,
                String castRange,
                String effectCurrent,
                @Nullable String effectNext,
                boolean canPressAction) {
            this.skillId = skillId;
            this.name = name;
            this.desc = desc;
            this.levelDisplay = levelDisplay;
            this.category = category;
            this.tags = tags;
            this.skillPointPerLevel = skillPointPerLevel;
            this.cooldown = cooldown;
            this.castRange = castRange;
            this.effectCurrent = effectCurrent;
            this.effectNext = effectNext;
            this.canPressAction = canPressAction;
        }
    }

    private static class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {
        private final SkillFragment host;
        private final List<SkillListRow> data;
        private final boolean compactMode;

        SkillAdapter(SkillFragment host, List<SkillListRow> data, boolean compactMode) {
            this.host = host;
            this.data = data;
            this.compactMode = compactMode;
        }

        @NonNull
        @Override
        public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_skill, parent, false);
            return new SkillViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
            SkillListRow item = data.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.desc);
            holder.tvLevel.setText(item.levelDisplay);
            holder.applyCompactStyle(compactMode);
            GameAssetIcons.bindSkill(host.requireContext(), holder.ivSkillIcon, item.skillId,
                    android.R.drawable.ic_menu_gallery);

            float alpha = item.canPressAction ? 1f : 0.38f;
            holder.btnAdd.setAlpha(alpha);
            holder.btnAdd.setEnabled(item.canPressAction);

            View.OnClickListener clickDetail =
                    v -> {
                        SkillDetailDialog.Detail detail =
                                new SkillDetailDialog.Detail(
                                        item.name,
                                        item.category,
                                        item.tags,
                                        item.levelDisplay,
                                        item.skillPointPerLevel,
                                        item.cooldown,
                                        item.castRange,
                                        item.effectCurrent,
                                        item.effectNext,
                                        item.desc);
                        SkillDetailDialog.show(v.getContext(), detail);
                    };
            holder.itemView.setOnClickListener(clickDetail);
            holder.btnAdd.setOnClickListener(v -> {
                if (!item.canPressAction) {
                    host.showFloatMsg("技能点不足或已达上限/未满足前置");
                    return;
                }
                host.onSkillUpgradeClicked(item);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class SkillViewHolder extends RecyclerView.ViewHolder {
            ImageView ivSkillIcon;
            TextView tvName, tvDesc, tvLevel;
            ImageView btnAdd;
            private boolean compactApplied = false;

            SkillViewHolder(View itemView) {
                super(itemView);
                ivSkillIcon = itemView.findViewById(R.id.iv_skill_icon);
                tvName = itemView.findViewById(R.id.tv_skill_name);
                tvDesc = itemView.findViewById(R.id.tv_skill_desc);
                tvLevel = itemView.findViewById(R.id.tv_skill_level);
                btnAdd = itemView.findViewById(R.id.btn_skill_action);
            }

            void applyCompactStyle(boolean compactMode) {
                if (!compactMode || compactApplied) {
                    return;
                }
                tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                tvLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);

                ViewGroup.LayoutParams btnLayout = btnAdd.getLayoutParams();
                if (btnLayout != null) {
                    int compactButtonSize = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            36,
                            itemView.getResources().getDisplayMetrics()
                    );
                    btnLayout.width = compactButtonSize;
                    btnLayout.height = compactButtonSize;
                    btnAdd.setLayoutParams(btnLayout);
                }

                ViewGroup.LayoutParams itemLayout = itemView.getLayoutParams();
                if (itemLayout instanceof RecyclerView.LayoutParams) {
                    int marginBottom = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            6,
                            itemView.getResources().getDisplayMetrics()
                    );
                    ((RecyclerView.LayoutParams) itemLayout).bottomMargin = marginBottom;
                    itemView.setLayoutParams(itemLayout);
                }
                compactApplied = true;
            }
        }
    }

    private void showBonusDialog() {
        AttributeSet fa = AttributeUtils.calculateCharacterAttributes(character);

        View content = View.inflate(requireContext(), R.layout.dialog_treasure_alert, null);
        ((TextView) content.findViewById(R.id.tv_treasure_alert_title)).setText("属性百分比加成");
        View neg = content.findViewById(R.id.btn_treasure_alert_negative);
        TextView pos = content.findViewById(R.id.btn_treasure_alert_positive);
        neg.setVisibility(View.GONE);
        pos.setText("关闭");

        TextView msg = content.findViewById(R.id.tv_treasure_alert_message);
        StringBuilder sb = new StringBuilder();
        sb.append("力量加成: ").append(percentLabel(fa.percentStrength)).append("\n");
        sb.append("敏捷加成: ").append(percentLabel(fa.percentAgility)).append("\n");
        sb.append("智力加成: ").append(percentLabel(fa.percentIntelligence)).append("\n");
        sb.append("精神加成: ").append(percentLabel(fa.percentSpirit)).append("\n");
        sb.append("体魄加成: ").append(percentLabel(fa.percentPhysique)).append("\n");
        sb.append("幸运加成: ").append(percentLabel(fa.percentLuck)).append("\n");
        sb.append("生命上限加成: ").append(percentLabel(fa.percentMaxHp)).append("\n");
        sb.append("魔力上限加成: ").append(percentLabel(fa.percentMaxMp)).append("\n");
        sb.append("物攻加成: ").append(percentLabel(fa.percentPhysicalAtk)).append("\n");
        sb.append("物防加成: ").append(percentLabel(fa.percentPhysicalDef)).append("\n");
        sb.append("法攻加成: ").append(percentLabel(fa.percentMagicalAtk)).append("\n");
        sb.append("法防加成: ").append(percentLabel(fa.percentMagicalDef)).append("\n");
        sb.append("速度加成: ").append(percentLabel(fa.percentSpeed));
        msg.setText(sb.toString());

        AlertDialog d = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .create();
        pos.setOnClickListener(v -> d.dismiss());
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private void showFloatMsg(String text) {
        if (!isAdded() || getActivity() == null) return;
        FloatMsgOverlay.showFloatMsg(getActivity(), text);
    }
}
