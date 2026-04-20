package com.example.treasure_and_battle.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        // 2. 初始化 RecyclerView
        rvSkills.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SkillAdapter(currentSkillList);
        rvSkills.setAdapter(adapter);

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
        for (int i = 1; i <= 10; i++) {
            currentSkillList.add(new SkillMockData(
                    category + "技能_" + i, 
                    "这是一个非常厉害的" + category + "技能，拥有着独特的机制。\n它能大幅攀升属性、改变战斗结果等不可思议的作用。",
                    (i % 5) + "/" + 5
            ));
        }
        adapter.notifyDataSetChanged();
    }

    // ================== Adapter & Mock Data ==================
    private static class SkillMockData {
        String name, desc, levelDisplay;
        SkillMockData(String name, String desc, String levelDisplay) {
            this.name = name;
            this.desc = desc;
            this.levelDisplay = levelDisplay;
        }
    }

    private static class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {
        private List<SkillMockData> data;

        SkillAdapter(List<SkillMockData> data) {
            this.data = data;
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

            // 点击整个条目或加点按钮的事件 (TODO)
            View.OnClickListener clickDetail = v -> {
                Toast.makeText(v.getContext(), "TODO: 显示 [" + item.name + "] 详细介绍面板与当前等级的效果", Toast.LENGTH_SHORT).show();
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
            SkillViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_skill_name);
                tvDesc = itemView.findViewById(R.id.tv_skill_desc);
                tvLevel = itemView.findViewById(R.id.tv_skill_level);
                btnAdd = itemView.findViewById(R.id.btn_skill_action);
            }
        }
    }
}