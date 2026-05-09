package com.example.treasure_and_battle.ui;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.ConsumableItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗界面（演示）：5 怪物槽、指令栏、角色状态、可滚动 Buff 列表。
 * 未接入 {@link com.example.treasure_and_battle.manager.BattleManager} 主循环，便于单独迭代 UI。
 */
public class BattleFragment extends Fragment {

    public static final String TAG = "BattleFragment";

    private enum PendingMode {
        NONE,
        PICK_SINGLE_ATTACK,
        PICK_SINGLE_SKILL,
        AOE_HIGHLIGHT
    }

    private static final class MonsterCell {
        boolean occupied;
        String name = "";
        int hp;
        int maxHp;
        int iconResId = R.drawable.ic_map;

        MonsterCell empty() {
            occupied = false;
            name = "";
            hp = 0;
            maxHp = 1;
            return this;
        }

        MonsterCell fill(String name, int maxHp, int iconResId) {
            occupied = true;
            this.name = name;
            this.maxHp = Math.max(1, maxHp);
            this.hp = this.maxHp;
            this.iconResId = iconResId;
            return this;
        }

        boolean isAlive() {
            return occupied && hp > 0;
        }
    }

    private static final class SkillOption {
        final String name;
        final boolean aoe;
        final String description;

        SkillOption(String name, boolean aoe, String description) {
            this.name = name;
            this.aoe = aoe;
            this.description = description;
        }
    }

    private static final class BuffLine {
        final String title;
        final String detail;

        BuffLine(String title, String detail) {
            this.title = title;
            this.detail = detail;
        }
    }

    /** 不可放在非 static 内部类中声明 interface（部分 Java/Android 工具链会报错） */
    @FunctionalInterface
    private interface OnBattleItemUseListener {
        void onUse(ConsumableItem item);
    }

    private final MonsterCell[] monsters = new MonsterCell[5];
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Player player;
    private PendingMode pendingMode = PendingMode.NONE;
    private String pendingSkillName = "";
    private final List<ConsumableItem> battleInventory = new ArrayList<>();
    private final List<BuffLine> buffLines = new ArrayList<>();

    private TextView tvHint;
    private TextView tvPlayerName;
    private ProgressBar pbHp;
    private ProgressBar pbMp;
    private ProgressBar pbAp;
    private RecyclerView rvBuffs;

    private View[] slotRoots = new View[5];
    private TextView[] slotMarkers = new TextView[5];
    private ImageView[] slotIcons = new ImageView[5];
    private TextView[] slotNames = new TextView[5];
    private ProgressBar[] slotHps = new ProgressBar[5];

    private BuffListAdapter buffAdapter;

    private static final SkillOption[] SKILL_OPTIONS = {
            new SkillOption("火球术", false, "单体魔法，演示用。"),
            new SkillOption("烈焰风暴", true, "群体魔法，对所有敌人造成伤害（演示）。"),
            new SkillOption("重击", false, "单体物理，演示用。"),
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_battle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        for (int i = 0; i < monsters.length; i++) {
            monsters[i] = new MonsterCell();
        }
        initDemoMonsters();
        player = new Player("冒险者", requireContext());
        buildDemoInventory();
        buildDemoBuffs();

        tvHint = view.findViewById(R.id.tv_battle_hint);
        tvPlayerName = view.findViewById(R.id.tv_battle_player_name);
        pbHp = view.findViewById(R.id.pb_battle_player_hp);
        pbMp = view.findViewById(R.id.pb_battle_player_mp);
        pbAp = view.findViewById(R.id.pb_battle_player_ap);
        rvBuffs = view.findViewById(R.id.rv_battle_buffs);

        int[] slotIds = {
                R.id.battle_slot_0, R.id.battle_slot_1, R.id.battle_slot_2,
                R.id.battle_slot_3, R.id.battle_slot_4
        };
        for (int i = 0; i < 5; i++) {
            View root = view.findViewById(slotIds[i]);
            slotRoots[i] = root;
            slotMarkers[i] = root.findViewById(R.id.tv_battle_target_marker);
            slotIcons[i] = root.findViewById(R.id.iv_battle_monster);
            slotNames[i] = root.findViewById(R.id.tv_battle_monster_name);
            slotHps[i] = root.findViewById(R.id.pb_battle_monster_hp);
            int idx = i;
            root.setOnClickListener(v -> onMonsterSlotClick(idx));
        }

        view.findViewById(R.id.btn_battle_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btn_battle_attack).setOnClickListener(v -> onAttackCommand());
        view.findViewById(R.id.btn_battle_skill).setOnClickListener(v -> onSkillCommand());
        view.findViewById(R.id.btn_battle_item).setOnClickListener(v -> onItemCommand());
        view.findViewById(R.id.btn_battle_escape).setOnClickListener(v -> onEscape());

        rvBuffs.setLayoutManager(new LinearLayoutManager(requireContext()));
        buffAdapter = new BuffListAdapter();
        rvBuffs.setAdapter(buffAdapter);

        bindPlayerPanel();
        bindAllMonsterSlots();
        refreshTargetMarkers();
        setHint("");
    }

    private void initDemoMonsters() {
        monsters[0].fill("史莱姆", 28, R.drawable.ic_map);
        monsters[1].empty();
        monsters[2].fill("哥布林", 40, R.drawable.ic_config);
        monsters[3].empty();
        monsters[4].fill("骷髅兵", 35, R.drawable.ic_skill);
    }

    private void buildDemoInventory() {
        battleInventory.clear();
        ConsumableItem hp = new ConsumableItem("demo_hp", "治疗药水", Rarity.COMMON, 10, 20,
                true, true, new ArrayList<>(), "恢复生命（演示）。");
        hp.setIconResId(android.R.drawable.ic_menu_day);
        hp.setCount(6);
        battleInventory.add(hp);

        ConsumableItem mp = new ConsumableItem("demo_mp", "法力药水", Rarity.COMMON, 12, 20,
                true, true, new ArrayList<>(), "恢复法力（演示）。");
        mp.setIconResId(android.R.drawable.ic_menu_compass);
        mp.setCount(3);
        battleInventory.add(mp);

        ConsumableItem outOnly = new ConsumableItem("demo_bread", "干粮", Rarity.COMMON, 4, 40,
                false, true, new ArrayList<>(), "仅非战斗可用，不应出现在下列表中。");
        outOnly.setCount(5);
        // 故意加入列表外：筛选时排除 !usableInBattle
    }

    private void buildDemoBuffs() {
        buffLines.clear();
        buffLines.add(new BuffLine("防御提升 · 3 回合", "来自技能/装备：受到物理伤害降低（演示数据）。"));
        buffLines.add(new BuffLine("攻击提升", "物理攻击力临时提高。"));
        buffLines.add(new BuffLine("中毒 ×2", "每回合受到持续伤害，可叠加。"));
        buffLines.add(new BuffLine("流血", "受到攻击时额外损失生命。"));
        buffLines.add(new BuffLine("护盾", "可吸收一定伤害，耗尽后消失。"));
    }

    private void bindPlayerPanel() {
        tvPlayerName.setText(player.getName());
        int maxHp = Math.max(1, player.getFinalAttributes().maxHp);
        int maxMp = Math.max(1, player.getFinalAttributes().maxMp);
        int maxAp = Math.max(1, player.getFinalAttributes().maxActionPoints);
        pbHp.setMax(maxHp);
        pbHp.setProgress(player.getCurrentHp());
        pbMp.setMax(maxMp);
        pbMp.setProgress(player.getCurrentMp());
        pbAp.setMax(maxAp);
        pbAp.setProgress(player.getCurrentActionPoints());
    }

    private void bindAllMonsterSlots() {
        for (int i = 0; i < 5; i++) {
            bindMonsterSlot(i);
        }
    }

    private void bindMonsterSlot(int i) {
        MonsterCell m = monsters[i];
        View root = slotRoots[i];
        if (m.occupied && m.isAlive()) {
            root.setAlpha(1f);
            root.setClickable(true);
            slotIcons[i].setVisibility(View.VISIBLE);
            bindIcon(slotIcons[i], m.iconResId);
            slotNames[i].setText(m.name);
            slotHps[i].setMax(m.maxHp);
            slotHps[i].setProgress(m.hp);
        } else if (m.occupied) {
            root.setAlpha(0.45f);
            root.setClickable(false);
            slotIcons[i].setVisibility(View.VISIBLE);
            bindIcon(slotIcons[i], m.iconResId);
            slotNames[i].setText("已击倒");
            slotHps[i].setMax(m.maxHp);
            slotHps[i].setProgress(0);
        } else {
            root.setAlpha(0.4f);
            root.setClickable(false);
            slotIcons[i].setVisibility(View.INVISIBLE);
            slotNames[i].setText("");
            slotHps[i].setMax(100);
            slotHps[i].setProgress(0);
        }
    }

    private static void bindIcon(@Nullable ImageView iv, int resId) {
        if (iv == null) return;
        iv.setImageResource(resId);
        Drawable d = iv.getDrawable();
        if (d != null) {
            d.mutate();
            if (d instanceof BitmapDrawable) {
                ((BitmapDrawable) d).setFilterBitmap(false);
            }
        }
    }

    private void setHint(String s) {
        tvHint.setText(s);
    }

    private void clearPending() {
        pendingMode = PendingMode.NONE;
        pendingSkillName = "";
        refreshTargetMarkers();
    }

    private void onAttackCommand() {
        clearPending();
        pendingMode = PendingMode.PICK_SINGLE_ATTACK;
        setHint("点击一个敌人进行普通攻击");
    }

    private void onSkillCommand() {
        clearPending();
        String[] labels = new String[SKILL_OPTIONS.length];
        for (int i = 0; i < SKILL_OPTIONS.length; i++) {
            SkillOption o = SKILL_OPTIONS[i];
            labels[i] = o.name + (o.aoe ? "（群体）" : "（单体）");
        }
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setTitle("选择技能")
                .setItems(labels, (d, which) -> {
                    SkillOption opt = SKILL_OPTIONS[which];
                    if (opt.aoe) {
                        pendingMode = PendingMode.AOE_HIGHLIGHT;
                        pendingSkillName = opt.name;
                        setHint("群体技能：全体敌人已标记");
                        refreshTargetMarkers();
                        mainHandler.postDelayed(() -> {
                            performAoeSkillDemo(opt.name);
                            clearPending();
                            setHint("");
                        }, 400);
                    } else {
                        pendingMode = PendingMode.PICK_SINGLE_SKILL;
                        pendingSkillName = opt.name;
                        setHint("点击目标释放：" + opt.name);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void onItemCommand() {
        clearPending();
        setHint("");

        List<ConsumableItem> usable = new ArrayList<>();
        for (ConsumableItem c : battleInventory) {
            if (c.isUsableInBattle()) {
                usable.add(c);
            }
        }
        if (usable.isEmpty()) {
            Toast.makeText(requireContext(), "没有可在战斗中使用的道具", Toast.LENGTH_SHORT).show();
            return;
        }

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_battle_items, null, false);
        RecyclerView rv = content.findViewById(R.id.rv_battle_items);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        final ItemPickerAdapter[] adapterHolder = new ItemPickerAdapter[1];
        adapterHolder[0] = new ItemPickerAdapter(usable, item -> {
            Toast.makeText(requireContext(),
                    "使用（演示）：" + item.getName() + " ×1",
                    Toast.LENGTH_SHORT).show();
            if (item.getCount() > 1) {
                item.setCount(item.getCount() - 1);
            } else {
                battleInventory.remove(item);
                usable.remove(item);
            }
            if (adapterHolder[0] != null) {
                adapterHolder[0].notifyDataSetChanged();
            }
        });
        rv.setAdapter(adapterHolder[0]);

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setTitle("道具")
                .setView(content)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void onEscape() {
        Toast.makeText(requireContext(), "逃跑成功（演示）", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void onMonsterSlotClick(int index) {
        MonsterCell m = monsters[index];
        if (pendingMode == PendingMode.NONE || pendingMode == PendingMode.AOE_HIGHLIGHT) {
            return;
        }
        if (!m.isAlive()) {
            Toast.makeText(requireContext(), "该位置没有可攻击目标", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pendingMode == PendingMode.PICK_SINGLE_ATTACK) {
            refreshTargetMarkers();
            slotMarkers[index].setVisibility(View.VISIBLE);
            performNormalAttack(index);
            mainHandler.postDelayed(() -> {
                clearPending();
                setHint("");
                refreshTargetMarkers();
            }, 350);
        } else if (pendingMode == PendingMode.PICK_SINGLE_SKILL) {
            refreshTargetMarkers();
            slotMarkers[index].setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(),
                    pendingSkillName + " → " + m.name + "（演示）",
                    Toast.LENGTH_SHORT).show();
            dealDamageToMonster(index, 12);
            mainHandler.postDelayed(() -> {
                clearPending();
                setHint("");
                refreshTargetMarkers();
            }, 350);
        }
    }

    private void performNormalAttack(int index) {
        MonsterCell m = monsters[index];
        int dmg = 4 + (int) (Math.random() * 5);
        Toast.makeText(requireContext(), "普通攻击 → " + m.name + "（-" + dmg + "）", Toast.LENGTH_SHORT).show();
        dealDamageToMonster(index, dmg);
    }

    private void performAoeSkillDemo(String skillName) {
        Toast.makeText(requireContext(), skillName + "：对全体敌人造成伤害（演示）", Toast.LENGTH_SHORT).show();
        for (int i = 0; i < 5; i++) {
            if (monsters[i].isAlive()) {
                dealDamageToMonster(i, 8);
            }
        }
        bindAllMonsterSlots();
    }

    private void dealDamageToMonster(int index, int dmg) {
        MonsterCell m = monsters[index];
        if (!m.occupied) return;
        m.hp = Math.max(0, m.hp - dmg);
        bindMonsterSlot(index);
    }

    private void refreshTargetMarkers() {
        for (int i = 0; i < 5; i++) {
            TextView tri = slotMarkers[i];
            boolean show = false;
            if (pendingMode == PendingMode.AOE_HIGHLIGHT && monsters[i].isAlive()) {
                show = true;
            }
            tri.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private final class BuffListAdapter extends RecyclerView.Adapter<BuffListAdapter.Vh> {

        @NonNull
        @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_battle_buff_row, parent, false);
            return new Vh(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh h, int position) {
            BuffLine line = buffLines.get(position);
            h.tv.setText(line.title);
            h.itemView.setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                            .setTitle(line.title)
                            .setMessage(line.detail)
                            .setPositiveButton("知道了", null)
                            .show());
        }

        @Override
        public int getItemCount() {
            return buffLines.size();
        }

        class Vh extends RecyclerView.ViewHolder {
            final TextView tv;

            Vh(@NonNull View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tv_battle_buff_row);
            }
        }
    }

    private final class ItemPickerAdapter extends RecyclerView.Adapter<ItemPickerAdapter.Vh> {

        private final List<ConsumableItem> items;
        private final OnBattleItemUseListener onUse;

        ItemPickerAdapter(List<ConsumableItem> items, OnBattleItemUseListener onUse) {
            this.items = items;
            this.onUse = onUse;
        }

        @NonNull
        @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_battle_inventory_row, parent, false);
            return new Vh(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh h, int position) {
            ConsumableItem it = items.get(position);
            bindIcon(h.icon, it.getIconResId());
            h.name.setText(it.getName());
            h.count.setText("×" + it.getCount());
            h.itemView.setOnClickListener(v -> {
                if (onUse != null) {
                    onUse.onUse(it);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Vh extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView count;

            Vh(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.iv_battle_item_icon);
                name = itemView.findViewById(R.id.tv_battle_item_name);
                count = itemView.findViewById(R.id.tv_battle_item_count);
            }
        }
    }
}
