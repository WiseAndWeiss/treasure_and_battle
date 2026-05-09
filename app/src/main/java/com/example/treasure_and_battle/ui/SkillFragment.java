package com.example.treasure_and_battle.ui;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;

import java.util.ArrayList;
import java.util.List;

public class SkillFragment extends Fragment {

    // 左侧 Tabs
    private TextView tabPassive;
    private TextView tabEvent;
    private TextView tabActive;

    // 技能列表
    private RecyclerView rvSkills;
    private SkillAdapter adapter;
    private boolean compactMode;

    // 临时模拟的数据池
    private List<SkillMockData> currentSkillList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skill, container, false);

        // 1. 绑定UI组件
        tabPassive = view.findViewById(R.id.tab_passive);
        tabEvent = view.findViewById(R.id.tab_event);
        tabActive = view.findViewById(R.id.tab_active);
        rvSkills = view.findViewById(R.id.rv_skills);
        compactMode = getResources().getConfiguration().smallestScreenWidthDp < 380;

        // 2. 初始化 RecyclerView
        rvSkills.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SkillAdapter(currentSkillList, compactMode);
        rvSkills.setAdapter(adapter);
        applyResponsiveUi(view);

        // 3. 绑定左侧 Tab 切换监听器
        tabPassive.setOnClickListener(v -> selectTab(0));
        tabEvent.setOnClickListener(v -> selectTab(1));
        tabActive.setOnClickListener(v -> selectTab(2));

        // 4. 绑定六维天赋加点按钮点击事件 (TODO)
        View.OnClickListener talentAddListener = v -> {
            Toast.makeText(getContext(), "TODO: 执行天赋点增加及数值重算", Toast.LENGTH_SHORT).show();
        };
        view.findViewById(R.id.btn_add_str).setOnClickListener(talentAddListener);
        view.findViewById(R.id.btn_add_agi).setOnClickListener(talentAddListener);
        view.findViewById(R.id.btn_add_int).setOnClickListener(talentAddListener);
        view.findViewById(R.id.btn_add_spr).setOnClickListener(talentAddListener);
        view.findViewById(R.id.btn_add_phy).setOnClickListener(talentAddListener);
        view.findViewById(R.id.btn_add_luc).setOnClickListener(talentAddListener);

        // 初始加载被动技能
        selectTab(0);

        return view;
    }

    private void applyResponsiveUi(View root) {
        if (!compactMode) return;
        setTextSizeSp(tabPassive, 13f);
        setTextSizeSp(tabEvent, 13f);
        setTextSizeSp(tabActive, 13f);

        TextView tvRemainingSkillPoints = root.findViewById(R.id.tv_remaining_skill_points);
        if (tvRemainingSkillPoints != null) {
            setTextSizeSp(tvRemainingSkillPoints, 11f);
        }
        rvSkills.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
    }

    private void setTextSizeSp(TextView textView, float sp) {
        if (textView == null) return;
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
        // 重置所有 Tab 样式
        resetTabStyle(tabPassive);
        resetTabStyle(tabEvent);
        resetTabStyle(tabActive);

        // 针对选中的 Tab 设为高亮且更替数据
        if (index == 0) {
            highlightTab(tabPassive);
            loadTempData("被动");
        } else if (index == 1) {
            highlightTab(tabEvent);
            loadTempData("事件");
        } else if (index == 2) {
            highlightTab(tabActive);
            loadTempData("主动");
        }
    }

    private void resetTabStyle(TextView tv) {
        if (tv == null) return;
        tv.setBackgroundResource(R.drawable.bg_tab_idle);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.tb_text_sub));
        tv.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void highlightTab(TextView tv) {
        if (tv == null) return;
        tv.setBackgroundResource(R.drawable.bg_tab_active);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.tb_bg_dark));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    // 临时加载对应的分类假数据用以进行布局展示
    private void loadTempData(String category) {
        currentSkillList.clear();
        int maxLevel = 5;
        for (int i = 1; i <= 10; i++) {
            int cur = i % maxLevel;
            if (cur == 0) cur = maxLevel;
            String name = category + "技能_" + i;
            String desc =
                    "这是一个非常厉害的" + category + "技能，拥有着独特的机制。\n"
                            + "它能大幅攀升属性、改变战斗结果等不可思议的作用。";
            String levelDisplay = cur + "/" + maxLevel;
            int cost = 1 + (i % 3);
            String tags;
            String cooldown;
            String range;
            if ("主动".equals(category)) {
                tags = "主动 · 施法";
                cooldown = (6 + i * 2) + " 秒";
                range = "单体敌方";
            } else if ("事件".equals(category)) {
                tags = "事件 · 战斗触发";
                cooldown = "无";
                range = "满足条件时自动触发";
            } else {
                tags = "被动 · 永久";
                cooldown = "无";
                range = "常驻（脱战亦生效）";
            }
            String effectCurrent =
                    "· 主要数值：强度系数 +" + (cur * 3 + i)
                            + "\n· 次要效果：与「" + category + "」流派协同，层数可叠加。";
            String effectNext =
                    cur >= maxLevel
                            ? null
                            : "· 主要数值：强度系数 +" + ((cur + 1) * 3 + i)
                                    + "\n· 解锁额外词条或缩短内置间隔。";
            currentSkillList.add(
                    new SkillMockData(
                            name,
                            desc,
                            levelDisplay,
                            category,
                            tags,
                            cost,
                            cooldown,
                            range,
                            effectCurrent,
                            effectNext));
        }
        adapter.notifyDataSetChanged();
    }

    // ================== Adapter & Mock Data ==================
    private static class SkillMockData {
        final String name;
        final String desc;
        final String levelDisplay;
        final String category;
        final String tags;
        final int skillPointPerLevel;
        final String cooldown;
        final String castRange;
        final String effectCurrent;
        /** 满级时为 null，详情窗显示「已满级」。 */
        final String effectNext;

        SkillMockData(
                String name,
                String desc,
                String levelDisplay,
                String category,
                String tags,
                int skillPointPerLevel,
                String cooldown,
                String castRange,
                String effectCurrent,
                String effectNext) {
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
        }
    }

    private static class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {
        private final List<SkillMockData> data;
        private final boolean compactMode;

        SkillAdapter(List<SkillMockData> data, boolean compactMode) {
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
            SkillMockData item = data.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.desc);
            holder.tvLevel.setText(item.levelDisplay);
            holder.applyCompactStyle(compactMode);

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
                Toast.makeText(v.getContext(), "TODO: 进行 [" + item.name + "] 技能学习/升级操作", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class SkillViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvLevel;
            ImageView btnAdd;
            private boolean compactApplied = false;
            SkillViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_skill_name);
                tvDesc = itemView.findViewById(R.id.tv_skill_desc);
                tvLevel = itemView.findViewById(R.id.tv_skill_level);
                btnAdd = itemView.findViewById(R.id.btn_skill_action);
            }

            void applyCompactStyle(boolean compactMode) {
                if (!compactMode || compactApplied) return;
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
}