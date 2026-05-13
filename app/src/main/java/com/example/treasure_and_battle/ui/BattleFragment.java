package com.example.treasure_and_battle.ui;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.utils.GameAssetIcons;
import com.example.treasure_and_battle.utils.LruBitmapCache;
import com.example.treasure_and_battle.utils.TachieManager;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.action.ActionIntent;
import com.example.treasure_and_battle.battle.action.BattleAction;
import com.example.treasure_and_battle.battle.BattleContext.RevealedIntent;
import com.example.treasure_and_battle.battle.log.BattleLogEntry;
import com.example.treasure_and_battle.battle.SkillTargetResolver;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.item.ConsumableManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.skill.SkillRangeType;
import com.example.treasure_and_battle.profession.Profession;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 战斗界面：新开局通过 {@link BattleManager#bootstrapBattleForUi} 进入与 {@link BattleManager#startBattle}
 * 相同的回合结构（意图揭示、速度行动条、怪物按意图池释放技能/普攻）；玩家在轮到己方时由 UI 操作，
 * 耗光行动点后由 {@link BattleManager#onPlayerTurnFullySpent} 继续后续单位与下一回合。
 * <p>
 * 进行中的战斗由 {@link BattleSessionHolder} 暂存：点返回或切换底栏后再进入会继续本场；
 * 战斗以胜利、失败或成功逃跑等结束后清除暂存，下次进入为新战斗。
 */
public class BattleFragment extends Fragment {

    public static final String TAG = "BattleFragment";

    private enum PendingMode {
        NONE,
        PICK_SINGLE_ATTACK,
        PICK_SINGLE_SKILL,
        AOE_HIGHLIGHT
    }

    private static final class BuffLine {
        final String title;
        final String detail;

        BuffLine(String title, String detail) {
            this.title = title;
            this.detail = detail;
        }
    }

    @FunctionalInterface
    private interface OnBattleItemUseListener {
        void onUse(ConsumableItem item);
    }

    @FunctionalInterface
    private interface OnSkillPickListener {
        void onPick(ActiveSkill skill);
    }

    private static final int[] MONSTER_TEMPLATE_IDS = {1001, 2002};
    private static final int[] MONSTER_SLOT_INDEX = {1, 2};
    private static final int[] MONSTER_ICONS = {
            R.drawable.ic_map,
            R.drawable.ic_map,
            R.drawable.ic_map,
            R.drawable.ic_map
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BattleManager battleManager;
    private BattleContext battleContext;
    private Player player;

    private ImageView ivBattleTachie;
    private TextView btnEndTurn;
    private boolean endTurnCooldown;
    private BottomSheetDialog statsSheet;

    private PendingMode pendingMode = PendingMode.NONE;
    private ActiveSkill pendingActiveSkill;

    private final List<BuffLine> buffLines = new ArrayList<>();

    private TextView tvHint;
    private TextView tvPlayerName;
    private TextView tvPlayerHpVal;
    private TextView tvPlayerMpVal;
    private TextView tvPlayerApVal;
    private ProgressBar pbHp;
    private ProgressBar pbMp;
    private ProgressBar pbAp;
    private RecyclerView rvBuffs;

    private View[] slotRoots = new View[5];
    private TextView[] slotMarkers = new TextView[5];
    private ImageView[] slotIcons = new ImageView[5];
    private TextView[] slotNames = new TextView[5];
    private ProgressBar[] slotHps = new ProgressBar[5];
    private TextView[] slotHpVals = new TextView[5];
    private TextView[] slotIntents = new TextView[5];

    private BuffListAdapter buffAdapter;

    private AlertDialog lootDialog;
    private View lootContentRoot;
    private List<Item> lootItems = new ArrayList<>();
    private int lootCurrentPage;
    private int lootMaxPage;
    private static final int LOOT_PAGE_SIZE = 8;
    private static final int LOOT_ITEMS_PER_ROW = 4;
    private GestureDetector lootGestureDetector;

    /** 本次进入是否为继续暂存战斗（用于恢复时补跑速度条上未完成的怪物回合）。 */
    private boolean openedWithResume;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_battle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        battleManager = BattleManager.getInstance(requireContext());
        initBattleSession();

        tvHint = view.findViewById(R.id.tv_battle_hint);
        tvPlayerName = view.findViewById(R.id.tv_battle_player_name);
        tvPlayerHpVal = view.findViewById(R.id.tv_battle_player_hp_val);
        tvPlayerMpVal = view.findViewById(R.id.tv_battle_player_mp_val);
        tvPlayerApVal = view.findViewById(R.id.tv_battle_player_ap_val);
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
            slotHpVals[i] = root.findViewById(R.id.tv_battle_monster_hp_val);
            slotIntents[i] = root.findViewById(R.id.tv_battle_monster_intent);
            int idx = i;
            root.setOnClickListener(v -> onMonsterSlotClick(idx));
        }

        view.findViewById(R.id.btn_battle_log_book).setOnClickListener(v -> showBattleLogDialog());

        ivBattleTachie = view.findViewById(R.id.iv_battle_tachie);
        if (ivBattleTachie != null && player != null && player.owner != null) {
            TachieManager.bind(requireContext(), ivBattleTachie,
                    player.owner.getProfessionType(), android.R.drawable.ic_menu_gallery);
            ivBattleTachie.setOnClickListener(v -> onTachieClick());
        }

        btnEndTurn = view.findViewById(R.id.btn_end_turn);
        if (btnEndTurn != null) {
            btnEndTurn.setOnClickListener(v -> onEndTurnClicked());
        }

        view.findViewById(R.id.btn_battle_back).setOnClickListener(v -> onBackPressed());
        view.findViewById(R.id.btn_battle_attack).setOnClickListener(v -> onAttackCommand());
        view.findViewById(R.id.btn_battle_skill).setOnClickListener(v -> onSkillCommand());
        view.findViewById(R.id.btn_battle_item).setOnClickListener(v -> onItemCommand());
        view.findViewById(R.id.btn_battle_escape).setOnClickListener(v -> onEscape());

        rvBuffs.setLayoutManager(new LinearLayoutManager(requireContext()));
        buffAdapter = new BuffListAdapter();
        rvBuffs.setAdapter(buffAdapter);

        refreshBuffLinesFromPlayer();
        bindPlayerPanel();
        bindAllMonsterSlots();
        refreshTargetMarkers();
        setHint("");

        if (openedWithResume && battleContext != null && !battleContext.isBattleEnded) {
            battleManager.runMonsterTurnsUntilPlayerTurn(battleContext);
            refreshBattleUi();
        }

        // 不得在 onViewCreated 同步路径里立刻 pop：会导致 Fragment 事务重入/状态错乱，多次进出战斗易闪退
        if (battleContext != null && battleContext.isBattleEnded) {
            scheduleFinishBattleAfterViewReady(view);
        }
    }

    private void scheduleFinishBattleAfterViewReady(@NonNull View root) {
        root.post(() -> {
            if (!isAdded() || battleContext == null || !battleContext.isBattleEnded) {
                return;
            }
            finishBattleAndExit();
        });
    }

    private void onBackPressed() {
        if (battleContext != null && !battleContext.isBattleEnded && player != null && player.owner != null) {
            player.owner.syncFromPlayer(player);
        }
        requireActivity().getSupportFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onResume() {
        super.onResume();
        InventoryGridSync.reloadSharedGridFromManager(requireContext());
    }

    /**
     * 若存在未结束的暂存战斗（返回或切 Tab 离开），则继续本场；否则从 {@link PlayerCharacterHolder} 开新局并写入
     * {@link BattleSessionHolder}。战斗以胜利、失败或成功逃跑等结束后会 {@link BattleSessionHolder#clear()}。
     */
    private void initBattleSession() {
        BattleSessionHolder.discardIfEnded();

        openedWithResume = BattleSessionHolder.hasResumableBattle();
        if (openedWithResume) {
            battleContext = BattleSessionHolder.getSuspended();
            player = battleContext != null ? battleContext.player : null;
            if (battleContext != null && player != null) {
                return;
            }
        }

        openedWithResume = false;
        BattleSessionHolder.clear();

        Character ch = PlayerCharacterHolder.getOrCreate(requireContext());
        player = ch.generatePlayer();
        player.resetActionPoints();

        MonsterManager mm = MonsterManager.getInstance(requireContext());
        List<Monster> monsters = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            monsters.add(null);
        }
        for (int k = 0; k < MONSTER_SLOT_INDEX.length && k < MONSTER_TEMPLATE_IDS.length; k++) {
            Monster m = mm.createMonsterByTemplateId(MONSTER_TEMPLATE_IDS[k]);
            if (m != null) {
                monsters.set(MONSTER_SLOT_INDEX[k], m);
            }
        }

        battleContext = battleManager.bootstrapBattleForUi(player, monsters, BattleContext.SurpriseDirection.NONE);
        BattleSessionHolder.setSuspended(battleContext);
    }

    @Nullable
    private Monster monsterAtSlot(int index) {
        if (battleContext == null || battleContext.monsters == null || index < 0 || index >= battleContext.monsters.size()) {
            return null;
        }
        return battleContext.monsters.get(index);
    }

    private boolean isSlotAliveMonster(int index) {
        Monster m = monsterAtSlot(index);
        return m != null && !m.isDead();
    }

    private void refreshBuffLinesFromPlayer() {
        buffLines.clear();
        if (player != null && player.getActiveBuffList() != null) {
            for (BaseBuff b : player.getActiveBuffList()) {
                if (b == null) {
                    continue;
                }
                int dur = b.getRemainingDuration();
                String title = b.getBuffName() + (dur > 0 ? " · " + dur + " 回合" : "");
                buffLines.add(new BuffLine(title, "层数 " + b.getStackCount()));
            }
        }
        if (buffLines.isEmpty()) {
            buffLines.add(new BuffLine("暂无 Buff", "使用技能或道具后可在此查看状态。"));
        }
        if (buffAdapter != null) {
            buffAdapter.notifyDataSetChanged();
        }
    }

    /** 与 {@link BagFragment} 共用 {@link InventoryGridSync}。 */
    private List<ConsumableItem> collectUsableBattleConsumables() {
        List<Item> grid = InventoryGridSync.getSharedBagGrid(requireContext());
        List<ConsumableItem> out = new ArrayList<>();
        for (Item it : grid) {
            if (it instanceof ConsumableItem) {
                ConsumableItem c = (ConsumableItem) it;
                if (c.isUsableInBattle() && c.getCount() > 0) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    private void consumeOneFromBag(ConsumableItem item, @Nullable List<ConsumableItem> dialogList) {
        List<Item> grid = InventoryGridSync.getSharedBagGrid(requireContext());
        for (int i = 0; i < grid.size(); i++) {
            if (grid.get(i) != item) {
                continue;
            }
            if (item.getCount() > 1) {
                item.setCount(item.getCount() - 1);
            } else {
                grid.set(i, null);
                if (dialogList != null) {
                    dialogList.remove(item);
                }
            }
            InventoryGridSync.flushSharedGridToManager(requireContext());
            return;
        }
    }

    private void applyFallbackBattleConsumable(ConsumableItem item) {
        String n = item.getName() != null ? item.getName() : "";
        int maxHp = Math.max(1, player.getFinalAttributes().maxHp);
        int maxMp = Math.max(1, player.getFinalAttributes().maxMp);
        if (n.contains("治疗") || n.contains("生命") || n.contains("血")) {
            player.healHp(Math.max(1, maxHp / 4));
        } else if (n.contains("法") || n.contains("魔") || n.contains("蓝") || n.contains("法力")) {
            player.healMp(Math.max(1, maxMp / 4));
        } else {
            player.healHp(Math.max(1, (int) (maxHp * 0.15f)));
        }
    }

    private void bindPlayerPanel() {
        if (player == null) {
            return;
        }
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
        if (tvPlayerHpVal != null) {
            tvPlayerHpVal.setText(formatCurMax(player.getCurrentHp(), maxHp));
        }
        if (tvPlayerMpVal != null) {
            tvPlayerMpVal.setText(formatCurMax(player.getCurrentMp(), maxMp));
        }
        if (tvPlayerApVal != null) {
            tvPlayerApVal.setText(formatCurMax(player.getCurrentActionPoints(), maxAp));
        }
    }

    private static String formatCurMax(int cur, int max) {
        return cur + " / " + max;
    }

    private void bindAllMonsterSlots() {
        for (int i = 0; i < 5; i++) {
            bindMonsterSlot(i);
        }
    }

    private void bindMonsterSlot(int i) {
        Monster m = monsterAtSlot(i);
        View root = slotRoots[i];
        if (m == null) {
            root.setAlpha(0.4f);
            root.setClickable(false);
            slotIcons[i].setVisibility(View.INVISIBLE);
            slotNames[i].setText("");
            slotHps[i].setMax(100);
            slotHps[i].setProgress(0);
            if (slotHpVals[i] != null) {
                slotHpVals[i].setVisibility(View.GONE);
            }
            if (slotIntents[i] != null) {
                slotIntents[i].setVisibility(View.GONE);
            }
            return;
        }

        int iconRes = iconForMonsterSlot(i);
        if (!m.isDead()) {
            root.setAlpha(1f);
            root.setClickable(true);
            slotIcons[i].setVisibility(View.VISIBLE);
            GameAssetIcons.bindMonster(requireContext(), slotIcons[i], m.getEntityId(), iconRes);
            slotNames[i].setText(m.getName());
            int maxHp = Math.max(1, m.getFinalAttributes().maxHp);
            slotHps[i].setMax(maxHp);
            slotHps[i].setProgress(m.getCurrentHp());
            if (slotHpVals[i] != null) {
                slotHpVals[i].setVisibility(View.VISIBLE);
                slotHpVals[i].setText(formatCurMax(m.getCurrentHp(), maxHp));
            }
            bindMonsterIntentBadge(slotIntents[i], m);
        } else {
            root.setAlpha(0.45f);
            root.setClickable(false);
            slotIcons[i].setVisibility(View.VISIBLE);
            GameAssetIcons.bindMonster(requireContext(), slotIcons[i], m.getEntityId(), iconRes);
            slotNames[i].setText("已击倒");
            int maxHpDead = Math.max(1, m.getFinalAttributes().maxHp);
            slotHps[i].setMax(maxHpDead);
            slotHps[i].setProgress(0);
            if (slotHpVals[i] != null) {
                slotHpVals[i].setVisibility(View.VISIBLE);
                slotHpVals[i].setText(formatCurMax(0, maxHpDead));
            }
            if (slotIntents[i] != null) {
                slotIntents[i].setVisibility(View.GONE);
            }
        }
    }

    private void bindMonsterIntentBadge(@Nullable TextView tv, @Nullable Monster m) {
        if (tv == null) {
            return;
        }
        if (m == null || battleContext == null || m.isDead()) {
            tv.setVisibility(View.GONE);
            return;
        }
        if (battleContext.monsterRevealedIntents == null) {
            tv.setVisibility(View.GONE);
            return;
        }
        List<RevealedIntent> list = battleContext.monsterRevealedIntents.get(m.getEntityId());
        if (list == null || list.isEmpty()) {
            tv.setVisibility(View.GONE);
            return;
        }
        RevealedIntent first = list.get(0);
        tv.setVisibility(View.VISIBLE);
        if (first.seenThrough && first.intent != null) {
            tv.setText(intentTypeShort(first.intent.getType()));
        } else {
            tv.setText("?");
        }
    }

    private static String intentTypeShort(ActionIntent.IntentType t) {
        if (t == null) {
            return "?";
        }
        switch (t) {
            case ATTACK:
                return "攻";
            case SKILL:
                return "技";
            case ESCAPE:
                return "逃";
            default:
                return "?";
        }
    }

    private static int iconForMonsterSlot(int slotIndex) {
        for (int k = 0; k < MONSTER_SLOT_INDEX.length; k++) {
            if (MONSTER_SLOT_INDEX[k] == slotIndex) {
                return MONSTER_ICONS[k % MONSTER_ICONS.length];
            }
        }
        return R.drawable.ic_map;
    }

    private void setHint(String s) {
        tvHint.setText(s);
    }

    private void clearPending() {
        pendingMode = PendingMode.NONE;
        pendingActiveSkill = null;
        refreshTargetMarkers();
    }

    private void onAttackCommand() {
        if (!ensureBattleActive()) {
            return;
        }
        clearPending();
        pendingMode = PendingMode.PICK_SINGLE_ATTACK;
        setHint("点击敌人普攻（消耗 1 AP）");
    }

    private void onSkillCommand() {
        if (!ensureBattleActive()) {
            return;
        }
        clearPending();
        Character ch = PlayerCharacterHolder.getOrCreate(requireContext());
        Profession prof = ch.getProfession();
        if (prof == null) {
            showFloatMsg("职业数据异常");
            return;
        }

        List<ActiveSkill> skills = new ArrayList<>();
        for (Skill s : prof.getLearnedActiveSkill()) {
            if (!(s instanceof ActiveSkill)) {
                continue;
            }
            ActiveSkill as = (ActiveSkill) s;
            if (!as.isLearned()) {
                continue;
            }
            skills.add(as);
        }

        if (skills.isEmpty()) {
            showFloatMsg("没有已学习的主动技能");
            return;
        }

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_battle_skills_pick, null, false);
        RecyclerView rv = content.findViewById(R.id.rv_battle_skills_pick);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setTitle("选择技能")
                .setView(content)
                .setNegativeButton("取消", null)
                .create();

        SkillPickAdapter adapter = new SkillPickAdapter(skills, skill -> {
            dialog.dismiss();
            SkillRangeType rt = skill.getSkillRangeType();
            if (rt == SkillRangeType.SINGLE_ENEMY) {
                pendingMode = PendingMode.PICK_SINGLE_SKILL;
                pendingActiveSkill = skill;
                setHint("点击目标：" + skill.getSkillName() + "（" + formatSkillCostLine(skill) + "）");
            } else {
                pendingMode = PendingMode.AOE_HIGHLIGHT;
                pendingActiveSkill = skill;
                setHint("正在释放：" + skill.getSkillName());
                refreshTargetMarkers();
                mainHandler.postDelayed(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    castPlayerSkill(skill);
                    clearPending();
                    setHint("");
                    refreshTargetMarkers();
                }, 350);
            }
        });
        rv.setAdapter(adapter);
        dialog.show();
    }

    private static String formatSkillCostLine(ActiveSkill skill) {
        if (skill == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int ap = skill.getActionPointCost();
        int mp = skill.getMpCost();
        int hp = skill.getHpCost();
        if (ap > 0) {
            sb.append("AP").append(ap);
        }
        if (mp > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("MP").append(mp);
        }
        if (hp > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("HP").append(hp);
        }
        return sb.length() > 0 ? sb.toString() : "无消耗";
    }

    private void castPlayerSkill(@NonNull ActiveSkill skill) {
        if (!ensureBattleActive() || battleManager == null || battleContext == null) {
            return;
        }
        if (!skill.canCast(player)) {
            showFloatMsg("无法释放（冷却/行动点/魔法/生命不足）");
            return;
        }
        try {
            List<BattleEntity> targets =
                    SkillTargetResolver.resolve(skill.getSkillRangeType(), player, battleContext);
            if (targets.isEmpty() && skill.getSkillRangeType() != SkillRangeType.NONE) {
                showFloatMsg("没有可选目标");
                return;
            }
            battleManager.executeSkill(player, skill, targets, battleContext);
        } catch (Exception e) {
            showFloatMsg("技能释放失败: " + e.getMessage());
            return;
        }

        battleManager.checkDeath(battleContext);
        afterPlayerActionUi();
    }

    private void onItemCommand() {
        if (!ensureBattleActive()) {
            return;
        }
        clearPending();
        setHint("");

        InventoryGridSync.reloadSharedGridFromManager(requireContext());
        List<ConsumableItem> usable = collectUsableBattleConsumables();
        if (usable.isEmpty()) {
            showFloatMsg("背包中没有可在战斗中使用的道具");
            return;
        }

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_battle_items, null, false);
        RecyclerView rv = content.findViewById(R.id.rv_battle_items);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        final ItemPickerAdapter[] adapterHolder = new ItemPickerAdapter[1];
        adapterHolder[0] = new ItemPickerAdapter(usable, item -> {
            if (!player.consumeActionPoints(1)) {
                showFloatMsg("行动点不足，无法使用道具");
                return;
            }
            boolean hasEffects = item.getEffects() != null && !item.getEffects().isEmpty();
            boolean ok;
            if (hasEffects) {
                ok = ConsumableManager.execute(player, battleContext, item, requireContext());
            } else {
                applyFallbackBattleConsumable(item);
                ok = true;
            }
            if (!ok) {
                player.setCurrentActionPoints(player.getCurrentActionPoints() + 1);
                showFloatMsg("无法使用该道具");
                return;
            }
            consumeOneFromBag(item, usable);
            if (adapterHolder[0] != null) {
                adapterHolder[0].notifyDataSetChanged();
            }
            afterPlayerActionUi();
            showFloatMsg("已使用：" + item.getName());
        });
        rv.setAdapter(adapterHolder[0]);

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setTitle("道具")
                .setView(content)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void onEscape() {
        if (!ensureBattleActive() || battleManager == null) {
            return;
        }
        boolean escaped = battleManager.executePlayerEscape(battleContext);
        battleManager.checkDeath(battleContext);
        refreshBuffLinesFromPlayer();
        bindPlayerPanel();
        bindAllMonsterSlots();

        if (battleContext.isBattleEnded) {
            battleManager.settleBattleResult(battleContext);
            if (player.owner != null) {
                player.owner.syncFromPlayer(player);
            }
            BattleSessionHolder.clear();
            showFloatMsg(escaped ? "逃跑成功" : "战斗结束");
            requireActivity().getSupportFragmentManager().popBackStackImmediate();
            return;
        }
        if (!escaped) {
            showFloatMsg("逃跑失败，遭到追击");
            if (battleContext.isBattleEnded) {
                finishBattleAndExit();
            } else {
                tryAdvanceIfPlayerOutOfAp();
            }
        }
    }

    private void onMonsterSlotClick(int index) {
        if (!ensureBattleActive()) {
            return;
        }
        Monster m = monsterAtSlot(index);

        if (pendingMode == PendingMode.PICK_SINGLE_ATTACK) {
            if (!isSlotAliveMonster(index) || m == null) {
                showFloatMsg("该位置没有可攻击目标");
                return;
            }
            refreshTargetMarkers();
            slotMarkers[index].setVisibility(View.VISIBLE);
            boolean ok = battleManager.submitBattleAction(battleContext, BattleAction.normalAttack(player, m));
            if (!ok) {
                clearPending();
                setHint("");
                refreshTargetMarkers();
                showFloatMsg("无法普攻（资源不足）");
                return;
            }
            battleManager.checkDeath(battleContext);
            mainHandler.postDelayed(() -> {
                if (!isAdded()) {
                    return;
                }
                clearPending();
                setHint("");
                refreshTargetMarkers();
                afterPlayerActionUi();
            }, 350);
            return;
        }

        if (pendingMode == PendingMode.PICK_SINGLE_SKILL && pendingActiveSkill != null) {
            if (!isSlotAliveMonster(index) || m == null) {
                showFloatMsg("该位置没有可攻击目标");

                return;
            }
            refreshTargetMarkers();
            slotMarkers[index].setVisibility(View.VISIBLE);
            battleContext.currentTarget = m;
            castPlayerSkill(pendingActiveSkill);
            mainHandler.postDelayed(() -> {
                if (!isAdded()) {
                    return;
                }
                clearPending();
                setHint("");
                refreshTargetMarkers();
            }, 350);
            return;
        }

        if (pendingMode == PendingMode.NONE && m != null) {
            showMonsterInspectDialog(m);
        }
    }

    private boolean ensureBattleActive() {
        if (battleContext == null || battleManager == null || player == null) {
            return false;
        }
        if (battleContext.isBattleEnded) {
            showFloatMsg("战斗已结束");
            return false;
        }
        return true;
    }

    /** 玩家行动后刷新 UI；需要玩家主动点击"结束回合"来推进。 */
    private void afterPlayerActionUi() {
        refreshBattleUi();
        if (battleContext.isBattleEnded) {
            finishBattleAndExit();
        }
    }

    private void refreshBattleUi() {
        refreshBuffLinesFromPlayer();
        bindPlayerPanel();
        bindAllMonsterSlots();
        updateEndTurnButton();
    }

    private void updateEndTurnButton() {
        if (btnEndTurn == null) return;
        boolean isPlayerTurn = battleContext != null
                && !battleContext.isBattleEnded
                && battleContext.isPlayerTurn;
        btnEndTurn.setEnabled(isPlayerTurn && !endTurnCooldown);
    }

    private void onEndTurnClicked() {
        if (endTurnCooldown || battleContext == null || battleContext.isBattleEnded) return;
        if (!battleContext.isPlayerTurn) return;
        endTurnCooldown = true;
        btnEndTurn.setEnabled(false);
        battleManager.onPlayerTurnFullySpent(battleContext);
        mainHandler.postDelayed(() -> {
            endTurnCooldown = false;
            refreshBattleUi();
            if (battleContext.isBattleEnded) {
                finishBattleAndExit();
            }
        }, 300);
    }

    private void onTachieClick() {
        if (player == null) return;
        if (statsSheet != null && statsSheet.isShowing()) return;

        ScaleAnimation anim = new ScaleAnimation(
                1.0f, 1.05f, 1.0f, 1.05f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(150);
        anim.setRepeatCount(1);
        anim.setRepeatMode(ScaleAnimation.REVERSE);
        ivBattleTachie.startAnimation(anim);

        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_battle_stats, null, false);
        populateStatsSheet(sheetView);

        statsSheet = new BottomSheetDialog(requireContext());
        statsSheet.setContentView(sheetView);
        statsSheet.setCanceledOnTouchOutside(true);
        statsSheet.show();
    }

    private void populateStatsSheet(View root) {
        if (player == null) return;
        AttributeSet fa = player.getFinalAttributes();

        LinearLayout baseContainer = root.findViewById(R.id.layout_stats_base);
        if (baseContainer != null) {
            addStatRow(baseContainer, "物理攻击", String.valueOf(fa.physicalAtk));
            addStatRow(baseContainer, "魔法攻击", String.valueOf(fa.magicalAtk));
            addStatRow(baseContainer, "物理防御", String.valueOf(fa.physicalDef));
            addStatRow(baseContainer, "魔法防御", String.valueOf(fa.magicalDef));
            addStatRow(baseContainer, "速度", String.valueOf(fa.speed));
            addStatRow(baseContainer, "暴击率",
                    String.format(java.util.Locale.CHINA, "%.1f%%", fa.physicalCritRate * 100f));
        }

        LinearLayout buffContainer = root.findViewById(R.id.layout_stats_buffs);
        if (buffContainer != null) {
            List<BaseBuff> buffs = player.getActiveBuffList();
            if (buffs != null && !buffs.isEmpty()) {
                for (BaseBuff b : buffs) {
                    String text = b.getBuffName() + " x" + b.getStackCount()
                            + " · 剩" + b.getRemainingDuration() + "回合";
                    addStatRow(buffContainer, text, "");
                }
            } else {
                addStatRow(buffContainer, "无Buff", "");
            }
        }

        LinearLayout equipContainer = root.findViewById(R.id.layout_stats_equip);
        if (equipContainer != null && player.owner != null) {
            for (com.example.treasure_and_battle.model.item.equip.EquipItem eq
                    : player.owner.getEquippedItems()) {
                if (eq == null) continue;
                String text = eq.getName() + " Lv." + eq.getLevel();
                addStatRowGreen(equipContainer, text, "");
            }
        }
    }

    private void addStatRow(LinearLayout parent, String label, String value) {
        TextView row = new TextView(requireContext());
        row.setTextSize(13);
        row.setTextColor(0xFFFFFFFF);
        row.setText(label + (value.isEmpty() ? "" : "：" + value));
        row.setPadding(0, 4, 0, 4);
        parent.addView(row);
    }

    private void addStatRowGreen(LinearLayout parent, String label, String value) {
        TextView row = new TextView(requireContext());
        row.setTextSize(13);
        row.setTextColor(0xFF4CAF50);
        row.setText(label + (value.isEmpty() ? "" : "：" + value));
        row.setPadding(0, 4, 0, 4);
        parent.addView(row);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        if (ivBattleTachie != null) {
            ivBattleTachie.setImageDrawable(null);
        }
        if (statsSheet != null && statsSheet.isShowing()) {
            statsSheet.dismiss();
        }
        dismissLootPanel();
    }

    private void dismissLootPanel() {
        if (lootDialog != null && lootDialog.isShowing()) {
            lootDialog.dismiss();
        }
        lootDialog = null;
        lootContentRoot = null;
        lootItems.clear();
        lootGestureDetector = null;
    }

    private void showFloatMsg(String text) {
        if (!isAdded() || getActivity() == null) return;
        FloatMsgOverlay.show(getActivity(), text);
    }

    private void tryAdvanceIfPlayerOutOfAp() {
        if (battleContext == null || battleContext.isBattleEnded || player == null) {
            return;
        }
        if (player.getCurrentActionPoints() > 0) {
            return;
        }
        battleManager.onPlayerTurnFullySpent(battleContext);
        refreshBattleUi();
        if (battleContext.isBattleEnded) {
            finishBattleAndExit();
        }
    }

    private void finishBattleAndExit() {
        if (battleContext == null || battleManager == null || !isAdded()) {
            return;
        }
        battleManager.settleBattleResult(battleContext);

        if (battleContext.battleResult == BattleContext.BattleResult.DEFEAT) {
            if (player != null && player.owner != null) {
                player.owner.syncFromPlayer(player);
            }
            BattleSessionHolder.clear();
            showFloatMsg(summarizeResult());
            new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                    .setTitle("战斗失败")
                    .setMessage("战斗失败！")
                    .setCancelable(false)
                    .setPositiveButton("确定", (d, w) ->
                            requireActivity().getSupportFragmentManager().popBackStackImmediate())
                    .show();
            return;
        }

        List<Item> lootCopy = new ArrayList<>();
        if (battleContext.battleResult == BattleContext.BattleResult.VICTORY && battleContext.pendingLoot != null) {
            lootCopy.addAll(battleContext.pendingLoot);
            battleContext.pendingLoot.clear();
        }
        if (player != null && player.owner != null) {
            player.owner.syncFromPlayer(player);
        }
        BattleSessionHolder.clear();

        int finalGold = battleContext.rewardGold;
        int finalExp = battleContext.rewardExp;
        String msg = summarizeResult();
        if (lootCopy.isEmpty()) {
            showFloatMsg(msg);
            requireActivity().getSupportFragmentManager().popBackStackImmediate();
            return;
        }
        showLootPanel(lootCopy, msg, finalGold, finalExp);
    }

    private void showLootPanel(List<Item> loot, String summaryMsg, int rewardGold, int rewardExp) {
        if (!isAdded()) return;

        lootItems.clear();
        lootItems.addAll(loot);
        lootCurrentPage = 0;
        lootMaxPage = (lootItems.size() + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_battle_loot, null, false);
        lootContentRoot = content;

        TextView tvSummary = content.findViewById(R.id.tv_loot_summary);
        tvSummary.setText(summaryMsg);

        LinearLayout rewardRow = content.findViewById(R.id.loot_reward_row);
        TextView tvGold = content.findViewById(R.id.tv_loot_gold);
        TextView tvExp = content.findViewById(R.id.tv_loot_exp);
        if (tvGold != null) tvGold.setText("获得金币：" + rewardGold);
        if (tvExp != null) tvExp.setText("获得经验：" + rewardExp);
        if (rewardRow != null) rewardRow.setVisibility(View.VISIBLE);

        LinearLayout layoutPager = content.findViewById(R.id.layout_loot_pager);
        TextView btnPrev = content.findViewById(R.id.btn_loot_prev_page);
        TextView btnNext = content.findViewById(R.id.btn_loot_next_page);

        if (lootMaxPage > 1) {
            layoutPager.setVisibility(View.VISIBLE);
            btnPrev.setOnClickListener(v -> {
                if (lootCurrentPage > 0) {
                    lootCurrentPage--;
                    renderLootPage();
                }
            });
            btnNext.setOnClickListener(v -> {
                if (lootCurrentPage < lootMaxPage - 1) {
                    lootCurrentPage++;
                    renderLootPage();
                }
            });
        }

        content.findViewById(R.id.btn_loot_claim_all).setOnClickListener(v -> onLootClaimAll());
        content.findViewById(R.id.btn_loot_leave).setOnClickListener(v -> onLootLeave());

        lootGestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX < -800 && lootCurrentPage < lootMaxPage - 1) {
                        lootCurrentPage++;
                        renderLootPage();
                        return true;
                    } else if (velocityX > 800 && lootCurrentPage > 0) {
                        lootCurrentPage--;
                        renderLootPage();
                        return true;
                    }
                }
                return false;
            }
        });

        renderLootPage();

        lootDialog = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .setCancelable(false)
                .create();

        lootDialog.show();
    }

    private void renderLootPage() {
        if (lootContentRoot == null) return;

        LinearLayout gridContainer = lootContentRoot.findViewById(R.id.grid_loot_container);
        if (gridContainer == null) return;
        gridContainer.removeAllViews();

        int start = lootCurrentPage * LOOT_PAGE_SIZE;
        int end = Math.min(start + LOOT_PAGE_SIZE, lootItems.size());
        int count = end - start;
        int rows = (count + LOOT_ITEMS_PER_ROW - 1) / LOOT_ITEMS_PER_ROW;

        float density = requireContext().getResources().getDisplayMetrics().density;
        int cellSizePx = (int) (68 * density);
        int itemSpacingPx = (int) (10 * density);
        int rowSpacingPx = (int) (16 * density);

        for (int row = 0; row < rows; row++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            if (row > 0) {
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLp.topMargin = rowSpacingPx;
                rowLayout.setLayoutParams(rowLp);
            }

            for (int col = 0; col < LOOT_ITEMS_PER_ROW; col++) {
                int idx = row * LOOT_ITEMS_PER_ROW + col;
                if (idx >= count) break;

                View cell = LayoutInflater.from(requireContext()).inflate(R.layout.item_bag_grid, rowLayout, false);
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(cellSizePx, cellSizePx);
                if (col > 0) {
                    cellLp.leftMargin = itemSpacingPx;
                }
                cell.setLayoutParams(cellLp);
                bindLootCell(cell, lootItems.get(start + idx), start + idx);
                rowLayout.addView(cell);
            }

            gridContainer.addView(rowLayout);
        }

        View gridContainerView = lootContentRoot.findViewById(R.id.grid_loot_container);
        if (gridContainerView != null) {
            gridContainerView.setOnTouchListener((v, event) -> {
                if (lootGestureDetector != null) {
                    lootGestureDetector.onTouchEvent(event);
                }
                return false;
            });
        }

        TextView tvPageInfo = lootContentRoot.findViewById(R.id.tv_page_info);
        if (tvPageInfo != null && lootMaxPage > 1) {
            tvPageInfo.setText("第 " + (lootCurrentPage + 1) + " / " + lootMaxPage + " 页");
        }

        TextView btnPrev = lootContentRoot.findViewById(R.id.btn_loot_prev_page);
        TextView btnNext = lootContentRoot.findViewById(R.id.btn_loot_next_page);
        if (btnPrev != null) btnPrev.setEnabled(lootCurrentPage > 0);
        if (btnNext != null) btnNext.setEnabled(lootCurrentPage < lootMaxPage - 1);

        updateLootLeaveButton();
    }

    private void bindLootCell(View cell, Item item, int globalIndex) {
        RelativeLayout bg = cell.findViewById(R.id.bg_item_color);
        ImageView icon = cell.findViewById(R.id.iv_item_icon);
        TextView level = cell.findViewById(R.id.tv_item_level);
        TextView name = cell.findViewById(R.id.tv_item_name);
        TextView stackCount = cell.findViewById(R.id.tv_bag_stack_count);

        if (bg != null) {
            bg.setBackgroundTintList(item.getRarity() != null
                    ? android.content.res.ColorStateList.valueOf(item.getRarity().getColor())
                    : android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.tb_slot_empty)));
        }
        if (icon != null) {
            icon.setVisibility(View.VISIBLE);
            bindLootItemIcon(icon, item);
        }
        if (name != null) {
            name.setText(item.getName());
        }
        if (level != null) {
            if (item instanceof EquipItem) {
                level.setVisibility(View.VISIBLE);
                level.setText("Lv." + ((EquipItem) item).getLevel());
            } else {
                level.setVisibility(View.GONE);
            }
        }
        if (stackCount != null) {
            if (item.getMaxStack() > 1 && item.getCount() > 1) {
                stackCount.setVisibility(View.VISIBLE);
                stackCount.setText("\u00d7" + item.getCount());
            } else {
                stackCount.setVisibility(View.GONE);
            }
        }

        cell.setOnClickListener(v -> onLootItemClick(globalIndex));
    }

    private void bindLootItemIcon(ImageView imageView, Item item) {
        if (item == null) return;

        String folder = folderForItemType(item.getType());
        if (folder == null) {
            GameAssetIcons.bindItem(requireContext(), imageView, item);
            return;
        }
        String path = GameAssetIcons.assetPath(folder, item.getId());
        if (path == null) {
            GameAssetIcons.bindItem(requireContext(), imageView, item);
            return;
        }

        Bitmap cached = LruBitmapCache.getInstance().get(path);
        if (cached != null && !cached.isRecycled()) {
            imageView.setImageBitmap(cached);
            return;
        }

        try (InputStream is = requireContext().getAssets().open(path)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp != null) {
                LruBitmapCache.getInstance().put(path, bmp);
                Bitmap cachedAgain = LruBitmapCache.getInstance().get(path);
                imageView.setImageBitmap(cachedAgain != null ? cachedAgain : bmp);
                return;
            }
        } catch (IOException ignored) {
        }

        GameAssetIcons.bindItem(requireContext(), imageView, item);
    }

    private static String folderForItemType(ItemType type) {
        if (type == null) return null;
        switch (type) {
            case MATERIAL: return GameAssetIcons.FOLDER_MATERIAL;
            case CONSUMABLE: return GameAssetIcons.FOLDER_CONSUMABLE;
            case GEM: return GameAssetIcons.FOLDER_GEM;
            case EQUIPMENT: return GameAssetIcons.FOLDER_EQUIP;
            default: return null;
        }
    }

    private void onLootItemClick(int globalIndex) {
        if (globalIndex < 0 || globalIndex >= lootItems.size()) return;
        Item item = lootItems.get(globalIndex);
        if (item == null) return;

        Character ch = PlayerCharacterHolder.getOrCreate(requireContext());
        List<Item> bag = ch.getBagItems();
        if (InventoryManager.addItem(bag, item)) {
            InventoryGridSync.reloadSharedGridFromManager(requireContext());
            lootItems.remove(globalIndex);
            lootMaxPage = (lootItems.size() + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
            if (lootCurrentPage >= lootMaxPage && lootMaxPage > 0) {
                lootCurrentPage = lootMaxPage - 1;
            }
            if (lootItems.isEmpty()) {
                dismissLootPanel();
                requireActivity().getSupportFragmentManager().popBackStackImmediate();
                return;
            }
            renderLootPage();
        } else {
            showFloatMsg("背包已满");
        }
    }

    private void onLootClaimAll() {
        Character ch = PlayerCharacterHolder.getOrCreate(requireContext());
        List<Item> bag = ch.getBagItems();
        int claimed = 0;
        int total = lootItems.size();

        int i = lootCurrentPage * LOOT_PAGE_SIZE;
        while (i < lootItems.size()) {
            Item item = lootItems.get(i);
            if (item != null && InventoryManager.addItem(bag, item)) {
                lootItems.remove(i);
                claimed++;
            } else {
                break;
            }
        }

        InventoryGridSync.reloadSharedGridFromManager(requireContext());

        if (claimed > 0) {
            lootMaxPage = (lootItems.size() + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
            if (lootCurrentPage >= lootMaxPage && lootMaxPage > 0) {
                lootCurrentPage = lootMaxPage - 1;
            }
        }

        if (lootItems.isEmpty()) {
            showFloatMsg("已领取 " + claimed + " / " + total + " 件");
            dismissLootPanel();
            requireActivity().getSupportFragmentManager().popBackStackImmediate();
            return;
        }

        if (claimed < total) {
            showFloatMsg("背包已满，已领取 " + claimed + " / " + total + " 件");
        }

        renderLootPage();
    }

    private void onLootLeave() {
        int remaining = lootItems.size();
        if (remaining > 0) {
            new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                    .setTitle("未领取掉落物")
                    .setMessage("还有 " + remaining + " 件掉落物未领取，确定要离开？未领取的道具将永久消失。")
                    .setPositiveButton("确定", (d, w) -> {
                        dismissLootPanel();
                        requireActivity().getSupportFragmentManager().popBackStackImmediate();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            dismissLootPanel();
            requireActivity().getSupportFragmentManager().popBackStackImmediate();
        }
    }

    private void updateLootLeaveButton() {
        if (lootContentRoot == null) return;
        TextView btnLeave = lootContentRoot.findViewById(R.id.btn_loot_leave);
        if (btnLeave == null) return;
        int remaining = lootItems.size();
        btnLeave.setText(remaining > 0 ? "离开（剩余" + remaining + "件）" : "离开");
    }

    private int addSelectedItemsToBag(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        Character ch = PlayerCharacterHolder.getOrCreate(requireContext());
        List<Item> bag = ch.getBagItems();
        int placed = 0;
        for (Item it : items) {
            if (it == null) {
                continue;
            }
            if (InventoryManager.addItem(bag, it)) {
                placed++;
            }
        }
        InventoryGridSync.reloadSharedGridFromManager(requireContext());
        return placed;
    }

    private void showMonsterInspectDialog(@NonNull Monster m) {
        StringBuilder msg = new StringBuilder();
        if (battleContext != null && battleContext.monsterRevealedIntents != null) {
            List<RevealedIntent> revealed = battleContext.monsterRevealedIntents.get(m.getEntityId());
            msg.append("【意图】\n");
            if (revealed == null || revealed.isEmpty()) {
                msg.append("本轮暂无揭示意图。\n");
            } else {
                for (RevealedIntent ri : revealed) {
                    if (ri == null) {
                        continue;
                    }
                    if (ri.seenThrough && ri.intent != null) {
                        ActionIntent in = ri.intent;
                        msg.append(String.format("· %s（%s） AP%d MP%d\n",
                                in.getName(), in.getType().name(), in.getApCost(), in.getMpCost()));
                    } else {
                        msg.append("· （未看破）\n");
                    }
                }
            }
        }
        msg.append("\n【Buff】\n");
        if (m.getActiveBuffList() == null || m.getActiveBuffList().isEmpty()) {
            msg.append("无");
        } else {
            for (BaseBuff b : m.getActiveBuffList()) {
                if (b == null) {
                    continue;
                }
                int dur = b.getRemainingDuration();
                msg.append(String.format("· %s 层%d 剩%d回合\n",
                        b.getBuffName(), b.getStackCount(), dur));
            }
        }
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setTitle(m.getName())
                .setMessage(msg.toString().trim())
                .setPositiveButton("关闭", null)
                .show();
    }

    private String summarizeResult() {
        if (battleContext == null || battleContext.battleResult == null) {
            return "战斗结束";
        }
        switch (battleContext.battleResult) {
            case VICTORY:
                return "胜利！经验与掉落已结算（见日志）";
            case DEFEAT:
                return "失败…";
            case ESCAPED:
                return "已逃离战斗";
            default:
                return "战斗结束";
        }
    }

    private void showBattleLogDialog() {
        if (battleContext == null) {
            showFloatMsg("战斗未初始化");
            return;
        }
        List<BattleLogEntry> src = battleContext.battleLogs;
        List<BattleLogEntry> forDisplay = new ArrayList<>(src);
        Collections.reverse(forDisplay);

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_battle_log, null, false);
        RecyclerView rv = content.findViewById(R.id.rv_battle_log);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new BattleLogListAdapter(forDisplay));

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setTitle("战斗记录")
                .setView(content)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void refreshTargetMarkers() {
        for (int i = 0; i < 5; i++) {
            TextView tri = slotMarkers[i];
            boolean show = pendingMode == PendingMode.AOE_HIGHLIGHT && isSlotAliveMonster(i);
            tri.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private final class SkillPickAdapter extends RecyclerView.Adapter<SkillPickAdapter.Vh> {

        private final List<ActiveSkill> skills;
        private final OnSkillPickListener listener;

        SkillPickAdapter(List<ActiveSkill> skills, OnSkillPickListener listener) {
            this.skills = skills;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_battle_skill_pick_row, parent, false);
            return new Vh(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh h, int position) {
            ActiveSkill skill = skills.get(position);
            SkillRangeType rt = skill.getSkillRangeType();
            String rangeHint = (rt == SkillRangeType.ALL_ENEMIES) ? "（群体）"
                    : (rt == SkillRangeType.SINGLE_ENEMY ? "（单体）" : "");
            h.name.setText(skill.getSkillName() + rangeHint);
            h.cost.setText(formatSkillCostLine(skill));
            h.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPick(skill);
                }
            });
        }

        @Override
        public int getItemCount() {
            return skills.size();
        }

        class Vh extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView cost;

            Vh(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_skill_pick_name);
                cost = itemView.findViewById(R.id.tv_skill_pick_cost);
            }
        }
    }

    private static final class BattleLogListAdapter extends RecyclerView.Adapter<BattleLogListAdapter.Vh> {

        private final List<BattleLogEntry> entries;

        BattleLogListAdapter(List<BattleLogEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_battle_log_row, parent, false);
            return new Vh(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Vh h, int position) {
            BattleLogEntry e = entries.get(position);
            h.tv.setText(e.toString());
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class Vh extends RecyclerView.ViewHolder {
            final TextView tv;

            Vh(@NonNull View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tv_battle_log_row);
            }
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
            GameAssetIcons.bindItem(h.itemView.getContext(), h.icon, it);
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
