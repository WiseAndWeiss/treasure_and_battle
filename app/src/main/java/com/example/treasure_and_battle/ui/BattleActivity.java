package com.example.treasure_and_battle.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.affix.BaseAffix;
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
import com.example.treasure_and_battle.manager.event.EventManager;
import com.example.treasure_and_battle.manager.game.GameManager;
import com.example.treasure_and_battle.manager.battle.MonsterManager;
import com.example.treasure_and_battle.ui.animation.BattleAnimationManager;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.item.ConsumableManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.skill.SkillRangeType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.utils.HtmlRenderUtils;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 战斗界面：新开局通过 {@link BattleManager#bootstrapBattleForUi} 进入与 {@link BattleManager#startBattle}
 * 相同的回合结构（意图揭示、速度行动条、怪物按意图池释放技能/普攻）；玩家在轮到己方时由 UI 操作，
 * 耗光行动点后由 {@link BattleManager#onPlayerTurnFullySpent} 继续后续单位与下一回合。
 */
public class BattleActivity extends AppCompatActivity {

    public static final String TAG = "BattleActivity";

    private enum PendingMode {
        NONE,
        PICK_SINGLE_ATTACK,
        PICK_SINGLE_SKILL,
        PICK_SINGLE_ITEM,
        AOE_HIGHLIGHT
    }

    private enum CommandHighlight {
        NONE,
        ATTACK,
        SKILL,
        ITEM
    }

    private static final class BuffLine {
        final String name;
        final int stacks;
        final String detailBody;
        final boolean placeholder;

        BuffLine(@NonNull BaseBuff buff) {
            name = buff.getBuffName();
            stacks = buff.getStackCount();
            detailBody = BuffUiText.detailBody(buff);
            placeholder = false;
        }

        BuffLine(@NonNull String emptyTitle, @NonNull String emptyDetail) {
            name = emptyTitle;
            stacks = 0;
            detailBody = emptyDetail;
            placeholder = true;
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

    private static final int[] MONSTER_SLOT_INDEX = {0, 1, 2, 3, 4};
    private static final int BATTLE_SLOT_COUNT = 5;
    private static final List<String> recentRaceIds = new ArrayList<>();
    private static final int RACE_COOLDOWN_SIZE = 3;
    private String debugRaceId = null;
    private int debugMaxRarity = -1;
    private int debugCount = -1;
    private static final int[] MONSTER_ICONS = {
            R.drawable.ic_map,
            R.drawable.ic_map,
            R.drawable.ic_map,
            R.drawable.ic_map
    };
    /** 仅放大立绘 ImageView，槽位/血条/名字布局尺寸不变 */
    private static final float MONSTER_ICON_DISPLAY_SCALE = 1.6f;
    private static final float MONSTER_ICON_DISPLAY_OFFSET_Y_DP = 32f;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BattleManager battleManager;
    private BattleContext battleContext;
    private Player player;

    private ImageView ivBattleTachie;
    private TextView btnEndTurn;
    private boolean endTurnCooldown;
    private TextView btnAttack;
    private TextView btnSkill;
    private TextView btnItem;
    private TextView btnEscape;
    private TextView btnBack;
    private CommandHighlight commandHighlight = CommandHighlight.NONE;
    private TextView btnConfirm;
    private TextView btnCancel;
    private AlertDialog statsDialog;

    private PendingMode pendingMode = PendingMode.NONE;
    private ActiveSkill pendingActiveSkill;
    private ConsumableItem pendingConsumableItem;
    private int selectedTargetIndex = -1;

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
    /** 避免 refresh 时重复 bind 立绘导致全体贴图抖动 */
    @Nullable
    private final String[] slotBoundEntityIds = new String[5];
    private final boolean[] slotDisplayScaleApplied = new boolean[5];
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

    private DamageNumberOverlay damageNumberOverlay;
    private FrameLayout monsterArea;
    @Nullable
    private View monsterFormation;
    private int battleSlotWidthPx;
    private int battleSlotHeightPx;
    private DamageNumberOverlay tachieDamageOverlay;
    private AlertDialog itemUseDialog;
    private boolean playerInputLocked;

    // 动画系统
    private BattleAnimationManager animationManager;

    /** 本次进入是否为继续暂存战斗（用于恢复时补跑速度条上未完成的怪物回合）。 */
    private boolean openedWithResume;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        View battleRoot = findViewById(R.id.battle_root);
        if (battleRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(battleRoot, (v, insets) -> {
                int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navBottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            debugRaceId = extras.getString("debugRaceId");
            if (debugRaceId != null) {
                debugMaxRarity = extras.getInt("debugMaxRarity", -1);
                debugCount = extras.getInt("debugCount", -1);
            }
        }

        battleManager = BattleManager.getInstance(this);
        battleManager.setMonsterActListener(new com.example.treasure_and_battle.manager.battle.BattleManager.MonsterActListener() {
            @Override
            public void onMonsterWillAct(Monster monster) {
                onMonsterWillActInUi(monster);
            }
            @Override
            public void onMonsterEscaped(Monster monster) {
                if (isFinishing() || isDestroyed()) return;
                mainHandler.post(() -> {
                    showFloatMsg(monster.getName() + " 逃跑了！");
                    refreshBattleUi();
                });
            }
            @Override
            public void onMonsterEscapeFailed(Monster monster) {
                if (isFinishing() || isDestroyed()) return;
                mainHandler.post(() -> {
                    showFloatMonsterText(monster, "逃跑失败");
                    playerInputLocked = true;
                    refreshBattleUi();
                    animatePlayerChase(monster, () -> {
                        playerInputLocked = false;
                        battleManager.executeNormalAttack(battleContext, player, monster);
                        refreshBattleUi();
                    });
                });
            }
        });
        battleManager.setShieldAbsorbListener((target, amount) -> {
            if (isFinishing() || isDestroyed()) return;
            mainHandler.post(() -> {
                if (target == player) {
                    if (tachieDamageOverlay == null || ivBattleTachie == null) return;
                    FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                    int[] loc = new int[2];
                    ivBattleTachie.getLocationOnScreen(loc);
                    int[] parentLoc = new int[2];
                    decor.getLocationOnScreen(parentLoc);
                    int cx = loc[0] - parentLoc[0] + ivBattleTachie.getWidth() / 2;
                    int cy = loc[1] - parentLoc[1];
                    tachieDamageOverlay.showShieldAbsorbOffset(cx, cy, amount);
                } else if (target instanceof Monster) {
                    if (damageNumberOverlay == null || monsterArea == null) return;
                    int slotIdx = findMonsterSlotIndex((Monster) target);
                    View sprite = monsterSpriteAnimTarget(slotIdx);
                    if (sprite == null) return;
                    int[] loc = new int[2];
                    sprite.getLocationOnScreen(loc);
                    int[] parentLoc = new int[2];
                    monsterArea.getLocationOnScreen(parentLoc);
                    int cx = loc[0] - parentLoc[0] + sprite.getWidth() / 2;
                    int cy = loc[1] - parentLoc[1] + sprite.getHeight() / 2;
                    damageNumberOverlay.showShieldAbsorbOffset(cx, cy, amount);
                }
            });
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        });
        initBattleSession();

        tvPlayerName = findViewById(R.id.tv_battle_player_name);
        tvPlayerHpVal = findViewById(R.id.tv_battle_player_hp_val);
        tvPlayerMpVal = findViewById(R.id.tv_battle_player_mp_val);
        tvPlayerApVal = findViewById(R.id.tv_battle_player_ap_val);
        pbHp = findViewById(R.id.pb_battle_player_hp);
        pbMp = findViewById(R.id.pb_battle_player_mp);
        pbAp = findViewById(R.id.pb_battle_player_ap);
        rvBuffs = findViewById(R.id.rv_battle_buffs);
        tvHint = findViewById(R.id.tv_battle_hint);

        int[] slotIds = {
                R.id.battle_slot_0, R.id.battle_slot_1, R.id.battle_slot_2,
                R.id.battle_slot_3, R.id.battle_slot_4
        };
        for (int i = 0; i < 5; i++) {
            View root = findViewById(slotIds[i]);
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

        findViewById(R.id.btn_battle_log_book).setOnClickListener(v -> showBattleLogDialog());

        ivBattleTachie = findViewById(R.id.iv_battle_tachie);
        if (ivBattleTachie != null && player != null && player.owner != null) {
            TachieManager.bind(this, ivBattleTachie,
                    player.owner.getProfessionType(), android.R.drawable.ic_menu_gallery);
            ivBattleTachie.setOnClickListener(v -> onTachieClick());
        }

        btnEndTurn = findViewById(R.id.btn_end_turn);
        if (btnEndTurn != null) {
            btnEndTurn.setOnClickListener(v -> onEndTurnClicked());
        }

        btnAttack = findViewById(R.id.btn_battle_attack);
        btnSkill = findViewById(R.id.btn_battle_skill);
        btnItem = findViewById(R.id.btn_battle_item);
        btnEscape = findViewById(R.id.btn_battle_escape);
        btnBack = findViewById(R.id.btn_battle_back);
        btnAttack.setOnClickListener(v -> onAttackCommand());
        btnSkill.setOnClickListener(v -> onSkillCommand());
        btnItem.setOnClickListener(v -> onItemCommand());
        btnEscape.setOnClickListener(v -> onEscape());
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
        applyCommandButtonStyles();
        btnConfirm = findViewById(R.id.btn_battle_confirm);
        btnCancel = findViewById(R.id.btn_battle_cancel);
        btnConfirm.setOnClickListener(v -> onConfirmClick());
        btnCancel.setOnClickListener(v -> onCancelClick());

        rvBuffs.setLayoutManager(new LinearLayoutManager(this));
        buffAdapter = new BuffListAdapter();
        rvBuffs.setAdapter(buffAdapter);

        monsterArea = findViewById(R.id.monster_area);
        monsterFormation = findViewById(R.id.monster_formation);
        setupMonsterSlotSizing();
        loadRandomBattleBackground();
        disableViewGroupClipping(monsterArea);
        for (View slotRoot : slotRoots) {
            disableViewGroupClipping(slotRoot);
            if (slotRoot != null) {
                View spriteHost = slotRoot.findViewById(R.id.fl_monster_sprite);
                disableViewGroupClipping(spriteHost);
            }
        }
        damageNumberOverlay = new DamageNumberOverlay(monsterArea);
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        tachieDamageOverlay = new DamageNumberOverlay(decor);

        // 初始化动画管理器
        animationManager = new BattleAnimationManager(this);
        setupEntityViewMappings();

        setupResourceChangeListeners();
        setupTriggerListeners();

        refreshBuffLinesFromPlayer();
        bindPlayerPanel();
        bindAllMonsterSlots();
        refreshTargetMarkers();
        setHint("");

        if (battleContext != null && !battleContext.isBattleEnded) {
            playerInputLocked = true;
            refreshBattleUi();
            showBattleStartBanner(() -> {
                if (isFinishing() || isDestroyed() || battleContext == null || battleContext.isBattleEnded) {
                    playerInputLocked = false;
                    return;
                }
                battleManager.dispatchOnBattleStart(battleContext);
                showRoundBanner(1, () -> {
                    if (isFinishing() || isDestroyed() || battleContext == null || battleContext.isBattleEnded) {
                        playerInputLocked = false;
                        return;
                    }
                    playerInputLocked = false;
                    beginSteppedBattleExecution();
                });
            });
        }

        if (battleContext != null && battleContext.isBattleEnded) {
            scheduleFinishBattleAfterViewReady();
        }
    }

    private void setupMonsterSlotSizing() {
        if (monsterFormation == null) {
            return;
        }
        scheduleMonsterSlotSizeApply();
        monsterFormation.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                applyMonsterSlotSizesIfReady();
            }
        });
    }

    private void scheduleMonsterSlotSizeApply() {
        if (monsterFormation == null) {
            return;
        }
        if (applyMonsterSlotSizesIfReady()) {
            return;
        }
        monsterFormation.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (applyMonsterSlotSizesIfReady()) {
                    monsterFormation.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    private boolean applyMonsterSlotSizesIfReady() {
        if (monsterFormation == null) {
            return false;
        }
        int width = monsterFormation.getWidth()
                - monsterFormation.getPaddingLeft() - monsterFormation.getPaddingRight();
        int height = monsterFormation.getHeight()
                - monsterFormation.getPaddingTop() - monsterFormation.getPaddingBottom();
        if (width <= 0 || height <= 0) {
            return false;
        }
        BattleMonsterSlotSizer.SlotSize size = BattleMonsterSlotSizer.resolve(
                getResources().getDisplayMetrics(), width, height);
        if (size == null) {
            return false;
        }
        if (size.widthPx == battleSlotWidthPx && size.heightPx == battleSlotHeightPx) {
            return true;
        }
        battleSlotWidthPx = size.widthPx;
        battleSlotHeightPx = size.heightPx;
        for (int i = 0; i < slotRoots.length; i++) {
            View root = slotRoots[i];
            if (root == null) {
                continue;
            }
            ViewGroup.LayoutParams lp = root.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.width = battleSlotWidthPx;
            lp.height = battleSlotHeightPx;
            root.setLayoutParams(lp);
            View spriteHost = root.findViewById(R.id.fl_monster_sprite);
            if (spriteHost != null) {
                ViewGroup.LayoutParams slp = spriteHost.getLayoutParams();
                if (slp != null) {
                    slp.height = size.spriteHeightPx;
                    spriteHost.setLayoutParams(slp);
                }
            }
            slotDisplayScaleApplied[i] = false;
        }
        monsterFormation.post(this::reapplyMonsterIconDisplayScalesAfterLayout);
        return true;
    }

    private void reapplyMonsterIconDisplayScalesAfterLayout() {
        for (int i = 0; i < slotRoots.length; i++) {
            if (slotRoots[i] == null || monsterAtSlot(i) == null) {
                continue;
            }
            slotDisplayScaleApplied[i] = false;
            applyMonsterIconDisplayScale(i);
            slotDisplayScaleApplied[i] = true;
        }
    }

    private void showBattleStartBanner(Runnable onDone) {
        if (monsterArea == null) {
            onDone.run();
            return;
        }
        float density = monsterArea.getResources().getDisplayMetrics().density;
        int pad = (int) (12 * density);
        int bannerH = (int) (48 * density);

        TextView banner = new TextView(monsterArea.getContext());
        banner.setText("战斗开始");
        banner.setTextColor(0xFFFFFFFF);
        banner.setTextSize(20);
        banner.setGravity(android.view.Gravity.CENTER);
        banner.setBackgroundColor(0xDD000000);
        banner.setPadding(pad, pad, pad, pad);
        banner.setAlpha(0f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bannerH);
        params.gravity = android.view.Gravity.CENTER;
        banner.setLayoutParams(params);

        monsterArea.addView(banner);
        banner.animate().alpha(1f).setDuration(300).withEndAction(() ->
            banner.animate().alpha(0f).setDuration(400).setStartDelay(800).withEndAction(() -> {
                if (banner.getParent() != null) {
                    ((ViewGroup) banner.getParent()).removeView(banner);
                }
                onDone.run();
            }).start()
        ).start();
    }

    private void showRoundBanner(int round, Runnable onDone) {
        if (monsterArea == null) {
            onDone.run();
            return;
        }
        float density = monsterArea.getResources().getDisplayMetrics().density;
        int pad = (int) (12 * density);
        int bannerH = (int) (48 * density);

        TextView banner = new TextView(monsterArea.getContext());
        banner.setText("第" + round + "回合开始");
        banner.setTextColor(0xFFFFFFFF);
        banner.setTextSize(20);
        banner.setGravity(android.view.Gravity.CENTER);
        banner.setBackgroundColor(0xDD000000);
        banner.setPadding(pad, pad, pad, pad);
        banner.setAlpha(0f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bannerH);
        params.gravity = android.view.Gravity.CENTER;
        banner.setLayoutParams(params);

        monsterArea.addView(banner);
        banner.animate().alpha(1f).setDuration(300).withEndAction(() ->
            banner.animate().alpha(0f).setDuration(400).setStartDelay(500).withEndAction(() -> {
                if (banner.getParent() != null) {
                    ((ViewGroup) banner.getParent()).removeView(banner);
                }
                onDone.run();
            }).start()
        ).start();
    }

    private void scheduleFinishBattleAfterViewReady() {
        View root = findViewById(android.R.id.content);
        if (root == null) return;
        root.post(() -> {
            if (isFinishing() || isDestroyed() || battleContext == null || !battleContext.isBattleEnded) {
                return;
            }
            finishBattleAndExit();
        });
    }

    @Override
    public void onBackPressed() {
        if (battleContext == null || battleContext.isBattleEnded) {
            super.onBackPressed();
            return;
        }
        if (isFinishing() || isDestroyed()) return;

        int currentGold = (player != null && player.owner != null) ? player.owner.getGold() : 0;
        int estimatedLoss = Math.max(1, currentGold / 2);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_treasure_alert, null, false);
        TextView title = dialogView.findViewById(R.id.tv_treasure_alert_title);
        TextView msg = dialogView.findViewById(R.id.tv_treasure_alert_message);
        title.setText("强行退出");
        msg.setText("确定要强行退出战斗吗？\n将视为战斗失败，损失 " + estimatedLoss + " 金币。");
        View neg = dialogView.findViewById(R.id.btn_treasure_alert_negative);
        TextView pos = dialogView.findViewById(R.id.btn_treasure_alert_positive);
        neg.setVisibility(View.VISIBLE);
        ((TextView) neg).setText("取消");
        pos.setText("确定退出");
        AlertDialog dlg = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        neg.setOnClickListener(v -> dlg.dismiss());
        pos.setOnClickListener(v -> {
            dlg.dismiss();
            if (battleContext != null && !battleContext.isBattleEnded) {
                battleContext.isBattleEnded = true;
                battleContext.battleResult = BattleContext.BattleResult.DEFEAT;
            }
            finishBattleAndExit();
        });
        dlg.show();
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        InventoryGridSync.reloadSharedGridFromManager(this);
    }

    /**
     * 开始新战斗：从 {@link PlayerCharacterHolder} 开新局。
     */
    private void initBattleSession() {
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        player = ch.generatePlayer();
        player.resetActionPoints();

        EventManager em = EventManager.getInstance(this);
        Monster reservedMonster = em.getCurrentBattleMonster();
        BattleContext.SurpriseDirection surprise = em.getCurrentBattleSurprise();
        List<Monster> preGeneratedMonsters = em.getCurrentBattleMonsters();

        List<Monster> monsters = new ArrayList<>();
        if (preGeneratedMonsters != null && !preGeneratedMonsters.isEmpty()) {
            for (int i = 0; i < BATTLE_SLOT_COUNT; i++) monsters.add(null);
            for (int k = 0; k < MONSTER_SLOT_INDEX.length && k < preGeneratedMonsters.size(); k++) {
                monsters.set(MONSTER_SLOT_INDEX[k], preGeneratedMonsters.get(k));
            }
            em.setCurrentBattleMonsters(null);
        } else if (reservedMonster != null && surprise != BattleContext.SurpriseDirection.NONE) {
            for (int i = 0; i < 5; i++) monsters.add(null);
            monsters.set(1, reservedMonster);
            em.setCurrentBattleMonster(null);
            em.setCurrentBattleSurprise(BattleContext.SurpriseDirection.NONE);
        } else {
            MonsterManager mm = MonsterManager.getInstance(this);
            for (int i = 0; i < BATTLE_SLOT_COUNT; i++) monsters.add(null);

            int playerLevel = ch.getLevel();
            Random rng = new Random();
            List<Integer> batch;

            if (debugRaceId != null) {
                batch = mm.generateDebugBatch(debugRaceId, debugMaxRarity, debugCount, rng);
            } else {
                batch = mm.generateMonsterBatch(playerLevel, BATTLE_SLOT_COUNT,
                        rng, recentRaceIds);
            }

            List<Monster> generated = mm.createMonstersFromTemplateIds(batch, playerLevel);

            if (!batch.isEmpty() && !generated.isEmpty()) {
                String selectedRace = mm.getTemplate(batch.get(0)) != null
                        ? mm.getTemplate(batch.get(0)).getRaceId() : null;
                if (selectedRace != null) {
                    recentRaceIds.add(selectedRace);
                    while (recentRaceIds.size() > RACE_COOLDOWN_SIZE) {
                        recentRaceIds.remove(0);
                    }
                }
            }

            for (int k = 0; k < MONSTER_SLOT_INDEX.length && k < generated.size(); k++) {
                Monster m = generated.get(k);
                if (m != null) monsters.set(MONSTER_SLOT_INDEX[k], m);
            }
        }

        battleContext = battleManager.bootstrapBattleForUi(player, monsters,
                surprise, false);
        clearMonsterSlotBindCache();
    }

    private void clearMonsterSlotBindCache() {
        for (int i = 0; i < 5; i++) {
            slotBoundEntityIds[i] = null;
            slotDisplayScaleApplied[i] = false;
        }
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
                if (b == null || b.isExpired() || b.getStackCount() <= 0) {
                    continue;
                }
                buffLines.add(new BuffLine(b));
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
        List<Item> grid = InventoryGridSync.getSharedBagGrid(this);
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
        List<Item> grid = InventoryGridSync.getSharedBagGrid(this);
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
            InventoryGridSync.flushSharedGridToManager(this);
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

    /**
     * 战斗中使用道具时，仅当存在「对单体敌人」的伤害类效果时才需要点选怪物；
     * 治疗/增益/净化/逃跑、全体伤害、无配置效果（走名称兜底治疗）等均直接作用于己方或全体，无需选怪。
     */
    private static boolean battleConsumableNeedsMonsterTarget(@NonNull ConsumableItem item) {
        List<ConsumableItem.Effect> effects = item.getEffects();
        if (effects == null || effects.isEmpty()) {
            return false;
        }
        for (ConsumableItem.Effect e : effects) {
            if (e.type == ConsumableItem.EffectType.DAMAGE) {
                if (e.target == ConsumableItem.Target.ALL_ENEMIES) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 使用道具前需已按需设置 {@link BattleContext#currentTarget}（单体敌伤需要，否则可为 null）。
     */
    private boolean applyBattleConsumableUseCore(@NonNull ConsumableItem consumable) {
        if (!player.consumeActionPoints(1)) {
            showFloatMsg("行动点不足");
            return false;
        }
        boolean hasEffects = consumable.getEffects() != null
                && !consumable.getEffects().isEmpty();
        consumeOneFromBag(consumable, null);
        mainHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            clearPending();
            setHint("");
            afterPlayerActionUi(() -> {
                boolean ok;
                if (hasEffects) {
                    ok = ConsumableManager.execute(player, battleContext, consumable, this);
                } else {
                    applyFallbackBattleConsumable(consumable);
                    ok = true;
                }
                if (!ok) {
                    showFloatMsg("无法使用该道具");
                    return;
                }
                battleManager.checkDeath(battleContext);
            });
        }, 350);
        showFloatMsg("已使用：" + consumable.getName());
        return true;
    }

    private void bindPlayerPanel() {
        if (player == null) {
            return;
        }
        tvPlayerName.setText(player.getName());
        tvPlayerName.setVisibility(View.GONE);
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

    private static void disableViewGroupClipping(@Nullable View view) {
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        group.setClipChildren(false);
        group.setClipToPadding(false);
    }

    private void loadRandomBattleBackground() {
        ImageView ivBg = findViewById(R.id.iv_battle_bg);
        if (ivBg == null) return;
        try {
            String[] files = this.getAssets().list("icons/battle_bg");
            if (files == null || files.length == 0) return;
            int idx = new Random().nextInt(files.length);
            InputStream is = null;
            try {
                is = this.getAssets().open("icons/battle_bg/" + files[idx]);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    ivBg.setImageBitmap(bmp);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void bindAllMonsterSlots() {
        for (int i = 0; i < 5; i++) {
            bindMonsterSlot(i);
        }
    }

    @Nullable
    private View monsterSpriteAnimTarget(int slotIdx) {
        if (slotIdx < 0 || slotIdx >= slotIcons.length) {
            return null;
        }
        ImageView icon = slotIcons[slotIdx];
        return icon != null ? icon : slotRoots[slotIdx];
    }

    private void resetMonsterSlotTransforms(int i, boolean clearIconDisplayTransform) {
        View root = slotRoots[i];
        if (root == null) {
            return;
        }
        root.animate().cancel();
        root.setTranslationX(0f);
        root.setTranslationY(0f);
        root.setScaleX(1f);
        root.setScaleY(1f);
        if (slotIcons[i] != null) {
            slotIcons[i].animate().cancel();
            slotIcons[i].setTranslationX(0f);
            if (clearIconDisplayTransform) {
                slotIcons[i].setTranslationY(0f);
                slotIcons[i].setScaleX(1f);
                slotIcons[i].setScaleY(1f);
            }
            slotIcons[i].setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    private void applyMonsterIconDisplayScale(int i) {
        ImageView icon = slotIcons[i];
        if (icon == null) {
            return;
        }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        View root = slotRoots[i];
        View host = root != null ? root.findViewById(R.id.fl_monster_sprite) : null;
        View layoutProbe = host != null ? host : icon;
        Runnable apply = () -> applyMonsterIconDisplayScaleNow(i);
        if (layoutProbe.getWidth() > 0 && layoutProbe.getHeight() > 0) {
            apply.run();
        } else {
            layoutProbe.post(apply);
        }
    }

    /** 须在 ImageView / 立绘容器完成 layout 后调用，否则 pivot 会偏导致立绘看起来偏右/偏左。 */
    private void applyMonsterIconDisplayScaleNow(int i) {
        ImageView icon = slotIcons[i];
        if (icon == null) {
            return;
        }
        int w = icon.getWidth();
        int h = icon.getHeight();
        if (w <= 0 || h <= 0) {
            View root = slotRoots[i];
            View host = root != null ? root.findViewById(R.id.fl_monster_sprite) : null;
            if (host != null) {
                w = host.getWidth();
                h = host.getHeight();
            }
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        icon.setPivotX(w * 0.5f);
        icon.setPivotY(h);
        icon.setScaleX(MONSTER_ICON_DISPLAY_SCALE);
        icon.setScaleY(MONSTER_ICON_DISPLAY_SCALE);
        float offsetPx = MONSTER_ICON_DISPLAY_OFFSET_Y_DP
                * icon.getResources().getDisplayMetrics().density;
        icon.setTranslationX(0f);
        icon.setTranslationY(offsetPx);
    }

    private void bindMonsterSlot(int i) {
        Monster m = monsterAtSlot(i);
        View root = slotRoots[i];
        if (root == null || slotIcons[i] == null) {
            return;
        }
        if (m == null) {
            slotBoundEntityIds[i] = null;
            slotDisplayScaleApplied[i] = false;
            resetMonsterSlotTransforms(i, true);
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

        String entityId = m.getEntityId();
        boolean sameMonster = entityId != null && entityId.equals(slotBoundEntityIds[i]);
        if (!sameMonster) {
            resetMonsterSlotTransforms(i, false);
            slotBoundEntityIds[i] = entityId;
            slotDisplayScaleApplied[i] = false;
        }

        int iconRes = iconForMonsterSlot(i);
        if (!m.isDead()) {
            root.setClickable(true);
            slotIcons[i].setVisibility(View.VISIBLE);
            slotIcons[i].setAlpha(1f);
            View infoLayout = root.findViewById(R.id.layout_monster_info);
            if (infoLayout != null) infoLayout.setAlpha(1f);
            if (!sameMonster) {
                slotIcons[i].setScaleType(ImageView.ScaleType.FIT_CENTER);
                GameAssetIcons.bindMonster(this, slotIcons[i], entityId, iconRes);
            }
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
            root.setClickable(false);
            slotIcons[i].setVisibility(View.VISIBLE);
            if (!sameMonster) {
                slotIcons[i].setScaleType(ImageView.ScaleType.FIT_CENTER);
                GameAssetIcons.bindMonster(this, slotIcons[i], entityId, iconRes);
            }
            slotIcons[i].setAlpha(0.45f);
            View infoLayout = root.findViewById(R.id.layout_monster_info);
            if (infoLayout != null) infoLayout.setAlpha(0.45f);
            slotNames[i].setText(m.isEscaped() ? "已逃跑" : "已击倒");
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

        if (!slotDisplayScaleApplied[i]) {
            applyMonsterIconDisplayScale(i);
            slotDisplayScaleApplied[i] = true;
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
        List<RevealedIntent> list = battleContext.monsterRevealedIntents.get(m.getBattleKey());
        if (list == null || list.isEmpty()) {
            tv.setVisibility(View.GONE);
            return;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (int i = 0; i < list.size(); i++) {
            RevealedIntent ri = list.get(i);
            if (ri == null) continue;
            if (i > 0) ssb.append("\n");

            String label;
            if (ri.executed) {
                label = intentLabel(ri.intent, m);
            } else if (ri.seenThrough && ri.intent != null) {
                label = intentLabel(ri.intent, m);
            } else {
                label = "未知意图";
            }

            int start = ssb.length();
            ssb.append(label);
            if (ri.executed) {
                ssb.setSpan(new StrikethroughSpan(), start, ssb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (ssb.length() == 0) {
            tv.setVisibility(View.GONE);
            return;
        }
        tv.setVisibility(View.VISIBLE);
        tv.setText(ssb);
    }

    private static String intentLabel(ActionIntent intent, Monster m) {
        if (intent == null) return "未知意图";
        switch (intent.getType()) {
            case ATTACK:
                return "攻击";
            case SKILL: {
                String skillId = intent.getActionRefId();
                if (skillId != null && m != null) {
                    ActiveSkill skill = m.getMonsterSkill(skillId);
                    if (skill != null) {
                        return "技能：" + skill.getSkillName();
                    }
                }
                return "技能：" + intent.getName();
            }
            case ESCAPE:
                return "逃跑";
            default:
                return "未知意图";
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
        if (tvHint == null) {
            return;
        }
        if (s == null || s.trim().isEmpty()) {
            tvHint.setText("");
            tvHint.setVisibility(View.INVISIBLE);
            return;
        }
        tvHint.setText(s.trim());
        tvHint.setVisibility(View.VISIBLE);
    }

    private void showBuffDetailDialog(@NonNull String title, @NonNull String detail) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_battle_buff_detail, null, false);
        TextView tTitle = content.findViewById(R.id.tv_battle_buff_detail_title);
        TextView tBody = content.findViewById(R.id.tv_battle_buff_detail_body);
        tTitle.setText(title);
        tBody.setText(detail);
        AlertDialog d = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .create();
        d.setCanceledOnTouchOutside(true);
        content.findViewById(R.id.btn_battle_buff_detail_ok).setOnClickListener(v -> d.dismiss());
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private void clearPending() {
        pendingMode = PendingMode.NONE;
        pendingActiveSkill = null;
        pendingConsumableItem = null;
        selectedTargetIndex = -1;
        hideConfirmCancelButtons();
        refreshTargetMarkers();
        setHint("");
        setCommandHighlight(CommandHighlight.NONE);
    }

    private void setCommandHighlight(@NonNull CommandHighlight highlight) {
        commandHighlight = highlight;
        applyCommandButtonStyles();
    }

    private void applyCommandButtonStyles() {
        applyCommandButtonStyle(btnAttack, commandHighlight == CommandHighlight.ATTACK);
        applyCommandButtonStyle(btnSkill, commandHighlight == CommandHighlight.SKILL);
        applyCommandButtonStyle(btnItem, commandHighlight == CommandHighlight.ITEM);
        applyCommandButtonStyle(btnEscape, false);
        applyCommandButtonStyle(btnBack, false);
    }

    private void applyCommandButtonStyle(@Nullable TextView button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setBackgroundResource(selected ? R.drawable.bg_tab_active : R.drawable.bg_tab_idle);
        int colorRes = selected ? R.color.tb_bg_dark : R.color.tb_text_main;
        button.setTextColor(this.getColor(colorRes));
    }

    private void showConfirmCancelButtons() {
        if (btnConfirm != null) btnConfirm.setVisibility(View.VISIBLE);
        if (btnCancel != null) btnCancel.setVisibility(View.VISIBLE);
    }

    private void hideConfirmCancelButtons() {
        if (btnConfirm != null) btnConfirm.setVisibility(View.GONE);
        if (btnCancel != null) btnCancel.setVisibility(View.GONE);
    }

    private void onAttackCommand() {
        if (!ensureBattleActive()) {
            return;
        }
        clearPending();
        pendingMode = PendingMode.PICK_SINGLE_ATTACK;
        setCommandHighlight(CommandHighlight.ATTACK);
        setHint("点击敌人普攻（消耗 1 AP）");
    }

    private void onSkillCommand() {
        if (!ensureBattleActive()) {
            return;
        }
        clearPending();
        setCommandHighlight(CommandHighlight.SKILL);

        List<ActiveSkill> skills = new ArrayList<>();
        if (player != null && player.getActiveSkillList() != null) {
            skills.addAll(player.getActiveSkillList());
        }

        if (skills.isEmpty()) {
            showFloatMsg("没有已学习的主动技能");
            setCommandHighlight(CommandHighlight.NONE);
            return;
        }

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_battle_skills_pick, null, false);
        RecyclerView rv = content.findViewById(R.id.rv_battle_skills_pick);
        rv.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .create();
        content.findViewById(R.id.btn_battle_skills_pick_cancel).setOnClickListener(v -> dialog.dismiss());

        SkillPickAdapter adapter = new SkillPickAdapter(skills, skill -> {
            dialog.dismiss();
            SkillRangeType rt = skill.getSkillRangeType();
            setCommandHighlight(CommandHighlight.SKILL);
            if (rt == SkillRangeType.SINGLE_ENEMY) {
                pendingMode = PendingMode.PICK_SINGLE_SKILL;
                pendingActiveSkill = skill;
                setHint("点击目标：" + skill.getSkillName() + "（" + formatSkillCostLine(skill) + "）");
            } else {
                pendingMode = PendingMode.AOE_HIGHLIGHT;
                pendingActiveSkill = skill;
                setHint("群体技能：" + skill.getSkillName() + " - 请确认释放");
                refreshTargetMarkers();
                showConfirmCancelButtons();
            }
        });
        rv.setAdapter(adapter);
        dialog.setOnDismissListener(d -> {
            if (pendingMode == PendingMode.NONE) {
                setCommandHighlight(CommandHighlight.NONE);
            }
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
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
        List<BattleEntity> targets;
        try {
            targets = SkillTargetResolver.resolve(skill.getSkillRangeType(), player, battleContext);
        } catch (Exception e) {
            showFloatMsg("技能释放失败: " + e.getMessage());
            return;
        }
        if (targets.isEmpty() && skill.getSkillRangeType() != SkillRangeType.NONE) {
            showFloatMsg("没有可选目标");
            return;
        }

        afterPlayerActionUi(() -> {
            battleManager.executeSkill(player, skill, targets, battleContext);
            battleManager.checkDeath(battleContext);
        });
    }

    private void onItemCommand() {
        if (!ensureBattleActive()) {
            return;
        }
        clearPending();
        setHint("");
        setCommandHighlight(CommandHighlight.ITEM);

        InventoryGridSync.reloadSharedGridFromManager(this);
        List<ConsumableItem> usable = collectUsableBattleConsumables();
        if (usable.isEmpty()) {
            showFloatMsg("背包中没有可在战斗中使用的道具");
            setCommandHighlight(CommandHighlight.NONE);
            return;
        }

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_battle_items, null, false);
        RecyclerView rv = content.findViewById(R.id.rv_battle_items);
        rv.setLayoutManager(new LinearLayoutManager(this));
        final ItemPickerAdapter[] adapterHolder = new ItemPickerAdapter[1];
        adapterHolder[0] = new ItemPickerAdapter(usable, item -> {
            if (itemUseDialog != null && itemUseDialog.isShowing()) {
                itemUseDialog.dismiss();
                itemUseDialog = null;
            }
            clearPending();
            if (battleConsumableNeedsMonsterTarget(item)) {
                pendingMode = PendingMode.PICK_SINGLE_ITEM;
                pendingConsumableItem = item;
                setCommandHighlight(CommandHighlight.ITEM);
                setHint("点击目标使用道具：" + item.getName());
            } else {
                battleContext.currentTarget = null;
                if (!applyBattleConsumableUseCore(item)) {
                    setHint("");
                }
            }
        });
        rv.setAdapter(adapterHolder[0]);

        itemUseDialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .create();
        content.findViewById(R.id.btn_battle_items_close).setOnClickListener(v -> {
            if (itemUseDialog != null) {
                itemUseDialog.dismiss();
            }
            itemUseDialog = null;
        });
        itemUseDialog.setOnDismissListener(d -> {
            itemUseDialog = null;
            if (pendingMode == PendingMode.NONE) {
                setCommandHighlight(CommandHighlight.NONE);
            }
        });
        itemUseDialog.show();
        if (itemUseDialog.getWindow() != null) {
            itemUseDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private void onEscape() {
        if (!ensureBattleActive() || battleManager == null) {
            return;
        }
        boolean escaped = battleManager.executePlayerEscape(battleContext, false);
        battleManager.checkDeath(battleContext);
        refreshBuffLinesFromPlayer();
        bindPlayerPanel();
        bindAllMonsterSlots();

        if (battleContext.isBattleEnded) {
            battleManager.settleBattleResult(battleContext);
            if (player.owner != null) {
                player.owner.syncFromPlayer(player);
            }
            GameManager.getInstance(this).triggerAutoSave();
            showFloatMsg("逃跑成功");
            playerInputLocked = true;
            refreshBattleUi();
            animateEscapeSuccess(this::finish);
            return;
        }
        showFloatMsg("逃跑失败，遭到追击！");
        if (ivBattleTachie != null) {
            ivBattleTachie.animate().rotationBy(360f).setDuration(500).start();
        }
        showPlayerEscapeFailText();
        playerInputLocked = true;
        refreshBattleUi();
        List<Monster> chasers = battleManager.getAliveMonstersBySpeed(battleContext);
        animateChaseByMonsters(0, chasers, () -> {
            playerInputLocked = false;
            battleManager.checkDeath(battleContext);
            refreshBattleUi();
            if (battleContext.isBattleEnded) {
                finishBattleAndExit();
            } else {
                tryAdvanceIfPlayerOutOfAp();
            }
        });
    }

    private void onConfirmClick() {
        if (!ensureBattleActive()) return;

        switch (pendingMode) {
            case PICK_SINGLE_ATTACK:
                executeConfirmedAttack();
                break;
            case PICK_SINGLE_SKILL:
                executeConfirmedSkill();
                break;
            case PICK_SINGLE_ITEM:
                executeConfirmedItem();
                break;
            case AOE_HIGHLIGHT:
                executeConfirmedAoESkill();
                break;
            default:
                break;
        }
    }

    private void onCancelClick() {
        clearPending();
        setHint("");
    }

    private void executeConfirmedAttack() {
        if (selectedTargetIndex < 0) return;
        Monster m = monsterAtSlot(selectedTargetIndex);
        if (m == null || m.isDead()) {
            showFloatMsg("目标无效");
            clearPending();
            setHint("");
            return;
        }
        hideConfirmCancelButtons();
        mainHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            clearPending();
            setHint("");
            afterPlayerActionUi(() -> {
                boolean ok = battleManager.submitBattleAction(battleContext, BattleAction.normalAttack(player, m));
                if (!ok) {
                    showFloatMsg("无法普攻（资源不足）");
                    return;
                }
                battleManager.checkDeath(battleContext);
            });
        }, 350);
    }

    private void executeConfirmedSkill() {
        if (pendingActiveSkill == null || selectedTargetIndex < 0) return;
        Monster m = monsterAtSlot(selectedTargetIndex);
        if (m == null || m.isDead()) {
            showFloatMsg("目标无效");
            clearPending();
            setHint("");
            return;
        }
        battleContext.currentTarget = m;
        hideConfirmCancelButtons();
        castPlayerSkill(pendingActiveSkill);
        mainHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            clearPending();
            setHint("");
        }, 350);
    }

    private void executeConfirmedAoESkill() {
        if (pendingActiveSkill == null) return;
        hideConfirmCancelButtons();
        castPlayerSkill(pendingActiveSkill);
        mainHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            clearPending();
            setHint("");
        }, 350);
    }

    private void executeConfirmedItem() {
        if (pendingConsumableItem == null || selectedTargetIndex < 0) return;
        Monster m = monsterAtSlot(selectedTargetIndex);
        if (m == null || m.isDead()) {
            showFloatMsg("目标无效");
            clearPending();
            setHint("");
            return;
        }
        battleContext.currentTarget = m;
        boolean ok = applyBattleConsumableUseCore(pendingConsumableItem);
        if (!ok) {
            clearPending();
            setHint("");
            return;
        }
        hideConfirmCancelButtons();
    }

    private void onMonsterSlotClick(int index) {
        Monster m = monsterAtSlot(index);

        if (pendingMode == PendingMode.NONE && m != null && !m.isDead()) {
            showMonsterInspectDialog(m);
            return;
        }

        if (!ensureBattleActive()) {
            return;
        }

        if (pendingMode == PendingMode.AOE_HIGHLIGHT) {
            return;
        }

        if (pendingMode == PendingMode.PICK_SINGLE_ATTACK
                || pendingMode == PendingMode.PICK_SINGLE_SKILL
                || pendingMode == PendingMode.PICK_SINGLE_ITEM) {
            if (!isSlotAliveMonster(index) || m == null) {
                showFloatMsg("该位置没有可攻击目标");
                return;
            }
            selectedTargetIndex = index;
            battleContext.currentTarget = m;
            refreshTargetMarkers();
            showConfirmCancelButtons();
            setHint("已选中【" + m.getName() + "】，请点击确定或取消");
            return;
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
        if (playerInputLocked) {
            return false;
        }
        return true;
    }

    private void afterPlayerActionUi(@Nullable Runnable applyDamage) {
        playerInputLocked = true;
        refreshBattleUi();
        animatePlayerAttack(() -> {
            if (applyDamage != null) {
                applyDamage.run();
            }
            feedbackMonsterDodgeAfterPlayerHit();
        }, () -> {
            playerInputLocked = false;
            refreshBattleUi();
            if (battleContext.isBattleEnded) {
                finishBattleAndExit();
            }
        });
    }

    /** 玩家攻击/技能等对怪物未命中时：怪物槽位上跳回落 + MISS 浮字 */
    private void feedbackMonsterDodgeAfterPlayerHit() {
        if (battleContext == null || !battleContext.isDodged) {
            return;
        }
        BattleEntity target = battleContext.currentTarget;
        if (!(target instanceof Monster)) {
            return;
        }
        Monster m = (Monster) target;
        int slotIdx = findMonsterSlotIndex(m);
        if (slotIdx < 0) {
            return;
        }
        showMonsterMissAtSlot(slotIdx);
        animateMonsterDodge(slotIdx, null);
    }

    private void showMonsterMissAtSlot(int slotIdx) {
        if (isFinishing() || isDestroyed() || damageNumberOverlay == null || monsterArea == null) {
            return;
        }
        View sprite = monsterSpriteAnimTarget(slotIdx);
        if (sprite == null) {
            return;
        }
        int[] loc = new int[2];
        sprite.getLocationOnScreen(loc);
        int[] parentLoc = new int[2];
        monsterArea.getLocationOnScreen(parentLoc);
        int cx = loc[0] - parentLoc[0] + sprite.getWidth() / 2;
        int cy = loc[1] - parentLoc[1] + sprite.getHeight() / 2;
        damageNumberOverlay.showMissOffset(cx, cy);
    }

    /** 怪物攻击未命中玩家时：立绘先下沉再回位 + 白色 MISS 浮字 */
    private void feedbackPlayerDodgeAfterMonsterHit() {
        if (battleContext == null || !battleContext.isDodged || player == null) {
            return;
        }
        BattleEntity target = battleContext.currentTarget;
        if (target != player) {
            return;
        }
        showPlayerMiss();
        animatePlayerDodge(null);
    }

    private void showPlayerMiss() {
        if (isFinishing() || isDestroyed() || tachieDamageOverlay == null || ivBattleTachie == null) {
            return;
        }
        BattleActivity act = BattleActivity.this;
        if (act == null || act.isFinishing() || act.isDestroyed()) {
            return;
        }
        int[] center = tachieOverlayCenterInDecor(act);
        if (center == null) {
            return;
        }
        tachieDamageOverlay.showMissOffset(center[0], center[1]);
    }

    @Nullable
    private int[] tachieOverlayCenterInDecor(@NonNull android.app.Activity act) {
        FrameLayout decor = (FrameLayout) act.getWindow().getDecorView();
        int[] loc = new int[2];
        ivBattleTachie.getLocationOnScreen(loc);
        int[] parentLoc = new int[2];
        decor.getLocationOnScreen(parentLoc);
        int cx = loc[0] - parentLoc[0] + ivBattleTachie.getWidth() / 2;
        int cy = loc[1] - parentLoc[1] + ivBattleTachie.getHeight() / 3;
        return new int[]{cx, cy};
    }

    /** 玩家闪避：先下沉再回位（与怪物进攻动画同向，表示蹲闪） */
    private void animatePlayerDodge(@Nullable Runnable onDone) {
        if (ivBattleTachie == null) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        ivBattleTachie.animate().cancel();
        ivBattleTachie.setTranslationY(0f);
        float density = ivBattleTachie.getResources().getDisplayMetrics().density;
        float downOffset = 24f * density;

        android.animation.ObjectAnimator down = android.animation.ObjectAnimator.ofFloat(
                ivBattleTachie, "translationY", 0f, downOffset);
        down.setDuration(250);
        down.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                android.animation.ObjectAnimator up = android.animation.ObjectAnimator.ofFloat(
                        ivBattleTachie, "translationY", downOffset, 0f);
                up.setDuration(250);
                up.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (onDone != null) {
                            onDone.run();
                        }
                    }
                });
                up.start();
            }
        });
        down.start();
    }

    private void animatePlayerAttack(@Nullable Runnable onPeak, @NonNull Runnable onDone) {
        if (ivBattleTachie == null) {
            if (onPeak != null) onPeak.run();
            onDone.run();
            return;
        }
        float density = ivBattleTachie.getResources().getDisplayMetrics().density;
        float upOffset = 24f * density;

        android.animation.ObjectAnimator up = android.animation.ObjectAnimator.ofFloat(
                ivBattleTachie, "translationY", 0f, -upOffset);
        up.setDuration(250);
        up.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onPeak != null) onPeak.run();
                android.animation.ObjectAnimator down = android.animation.ObjectAnimator.ofFloat(
                        ivBattleTachie, "translationY", -upOffset, 0f);
                down.setDuration(250);
                down.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        onDone.run();
                    }
                });
                down.start();
            }
        });
        up.start();
    }

    private void animateEscapeSuccess(Runnable onDone) {
        if (ivBattleTachie == null) {
            if (onDone != null) onDone.run();
            return;
        }
        float targetY = ivBattleTachie.getHeight() * 1.2f;
        ivBattleTachie.animate()
                .translationY(targetY)
                .setDuration(600)
                .withEndAction(onDone)
                .start();
    }

    private void animatePlayerChase(Monster target, Runnable onDone) {
        int slotIdx = findMonsterSlotIndex(target);
        animatePlayerAttack(() -> {}, () -> {
            showFloatMonsterText(target, "追击！");
            if (onDone != null) onDone.run();
        });
    }

    private void animateChaseByMonsters(int index, List<Monster> chasers, Runnable onAllDone) {
        if (chasers == null || index >= chasers.size()) {
            if (onAllDone != null) onAllDone.run();
            return;
        }
        Monster m = chasers.get(index);
        int slotIdx = findMonsterSlotIndex(m);
        animateMonsterAttack(m, slotIdx,
            () -> {
                battleManager.executeNormalAttack(battleContext, m, player);
                feedbackPlayerDodgeAfterMonsterHit();
                showFloatMonsterText(m, "追击！");
                battleManager.checkDeath(battleContext);
            },
            () -> {
                refreshBattleUi();
                if (battleContext.isBattleEnded) {
                    if (onAllDone != null) onAllDone.run();
                    return;
                }
                mainHandler.postDelayed(() ->
                    animateChaseByMonsters(index + 1, chasers, onAllDone), 150);
            }
        );
    }

    private void refreshBattleUi() {
        refreshBuffLinesFromPlayer();
        bindPlayerPanel();
        bindAllMonsterSlots();
        updateEndTurnButton();
        updateActionButtons();
    }

    /**
     * 根据当前行动力 (AP) 实时更新左侧行动区按钮的可用/灰色状态：
     * - AP=0 时攻击/道具/逃跑按钮变灰不可点击
     * - 技能按钮始终可点击（面板内各技能独立校验可用性）
     */
    private void updateActionButtons() {
        if (btnAttack == null || btnSkill == null || btnItem == null || btnEscape == null || player == null) return;

        boolean hasAp = player.getCurrentActionPoints() > 0;
        boolean locked = playerInputLocked || (battleContext != null && battleContext.isBattleEnded);

        if (locked) {
            btnAttack.setAlpha(0.38f);
            btnAttack.setEnabled(false);
            btnSkill.setAlpha(0.38f);
            btnSkill.setEnabled(false);
            btnItem.setAlpha(0.38f);
            btnItem.setEnabled(false);
            btnEscape.setAlpha(0.38f);
            btnEscape.setEnabled(false);
            btnEndTurn.setAlpha(0.38f);
            btnEndTurn.setEnabled(false);
        } else if (hasAp) {
            btnAttack.setAlpha(1f);
            btnAttack.setEnabled(true);
            btnItem.setAlpha(1f);
            btnItem.setEnabled(true);
            btnEscape.setAlpha(1f);
            btnEscape.setEnabled(true);
            btnSkill.setAlpha(1f);
            btnSkill.setEnabled(true);
            btnEndTurn.setAlpha(1f);
            btnEndTurn.setEnabled(true);
        } else {
            btnAttack.setAlpha(0.38f);
            btnAttack.setEnabled(false);
            btnItem.setAlpha(0.38f);
            btnItem.setEnabled(false);
            btnEscape.setAlpha(0.38f);
            btnEscape.setEnabled(false);
            btnSkill.setAlpha(1f);
            btnSkill.setEnabled(true);
            btnEndTurn.setAlpha(1f);
            btnEndTurn.setEnabled(true);
        }

        if (btnBack != null) {
            btnBack.setAlpha(1f);
            btnBack.setEnabled(true);
        }

        applyCommandButtonStyles();
    }

    private void updateEndTurnButton() {
        if (btnEndTurn == null) return;
        boolean canAct = battleContext != null
                && !battleContext.isBattleEnded
                && !playerInputLocked;
        btnEndTurn.setEnabled(canAct && !endTurnCooldown);
    }

    private void onEndTurnClicked() {
        if (endTurnCooldown || battleContext == null || battleContext.isBattleEnded) return;
        if (playerInputLocked) return;
        clearPending();
        setHint("");
        endTurnCooldown = true;
        btnEndTurn.setEnabled(false);
        playerInputLocked = true;
        refreshBattleUi();
        battleContext.actionOrderIndex++;
        runMonsterTurnsStepped();
    }

    private void beginSteppedBattleExecution() {
        if (battleContext == null || battleContext.isBattleEnded) {
            refreshBattleUi();
            if (battleContext != null && battleContext.isBattleEnded) finishBattleAndExit();
            return;
        }
        playerInputLocked = true;
        refreshBattleUi();
        runMonsterTurnsSteppedInternal(false);
    }

    private void runMonsterTurnsStepped() {
        runMonsterTurnsSteppedInternal(true);
    }

    private void runMonsterTurnsSteppedInternal(boolean isEndTurn) {
        if (battleContext == null) {
            if (isEndTurn) endTurnCooldown = false;
            return;
        }
        if (battleContext.isBattleEnded) {
            if (isEndTurn) endTurnCooldown = false;
            refreshBattleUi();
            finishBattleAndExit();
            return;
        }

        playerInputLocked = true;

        int roundBefore = battleContext.currentRound;
        battleContext.resetDamageData();
        Monster m = battleManager.stepOneMonsterAction(battleContext);
        int roundAfter = battleContext.currentRound;
        final boolean playerDead = battleContext.isBattleEnded;

        if (roundAfter != roundBefore && !battleContext.isBattleEnded) {
            if (isEndTurn) endTurnCooldown = false;
            if (m != null) {
                refreshBattleUi();
                showRoundBanner(roundAfter, () -> {
                    if (isFinishing() || isDestroyed() || battleContext == null || battleContext.isBattleEnded) {
                        playerInputLocked = false;
                        if (battleContext != null && battleContext.isBattleEnded) finishBattleAndExit();
                        return;
                    }
                    int slotIdx = findMonsterSlotIndex(m);
                    animateMonsterAttack(m, slotIdx,
                        () -> {
                            battleManager.executePendingMonsterAction(battleContext);
                            feedbackPlayerDodgeAfterMonsterHit();
                        },
                        () -> {
                            refreshBattleUi();
                            if (battleContext.isBattleEnded) {
                                playerInputLocked = false;
                                finishBattleAndExit();
                                return;
                            }
                            mainHandler.postDelayed(this::runMonsterTurnsStepped, 1000);
                        });
                });
            } else {
                refreshBattleUi();
                showRoundBanner(roundAfter, () -> {
                    if (isFinishing() || isDestroyed() || battleContext == null || battleContext.isBattleEnded) {
                        playerInputLocked = false;
                        if (battleContext != null && battleContext.isBattleEnded) finishBattleAndExit();
                        return;
                    }
                    playerInputLocked = false;
                    refreshBattleUi();
                });
            }
            return;
        }

        if (m == null) {
            if (isEndTurn) endTurnCooldown = false;
            playerInputLocked = false;
            refreshBattleUi();
            if (battleContext.isBattleEnded) finishBattleAndExit();
            return;
        }
        int slotIdx = findMonsterSlotIndex(m);
        animateMonsterAttack(m, slotIdx,
            () -> {
                battleManager.executePendingMonsterAction(battleContext);
                feedbackPlayerDodgeAfterMonsterHit();
            },
            () -> {
                refreshBattleUi();
                if (playerDead) {
                    mainHandler.postDelayed(() -> finishBattleAndExit(), 400);
                    return;
                }
                if (battleContext.isBattleEnded) {
                    mainHandler.postDelayed(() -> finishBattleAndExit(), 400);
                    return;
                }
                mainHandler.postDelayed(this::runMonsterTurnsStepped, 1000);
            });
    }

    /** 怪物闪避：先上跳再落回（与玩家立绘攻击动画同向，表示躲开） */
    private void animateMonsterDodge(int slotIdx, @Nullable Runnable onDone) {
        View sprite = monsterSpriteAnimTarget(slotIdx);
        if (sprite == null) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        float baseTranslationY = sprite.getTranslationY();
        sprite.animate().cancel();
        sprite.setTranslationY(baseTranslationY);
        float density = sprite.getResources().getDisplayMetrics().density;
        float upOffset = 24f * density;

        android.animation.ObjectAnimator up = android.animation.ObjectAnimator.ofFloat(
                sprite, "translationY", baseTranslationY, baseTranslationY - upOffset);
        up.setDuration(250);
        up.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                android.animation.ObjectAnimator down = android.animation.ObjectAnimator.ofFloat(
                        sprite, "translationY", baseTranslationY - upOffset, baseTranslationY);
                down.setDuration(250);
                down.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (onDone != null) {
                            onDone.run();
                        }
                    }
                });
                down.start();
            }
        });
        up.start();
    }

    private void animateMonsterAttack(Monster m, int slotIdx, @Nullable Runnable onPeak, @NonNull Runnable onDone) {
        View sprite = monsterSpriteAnimTarget(slotIdx);
        if (sprite == null) {
            if (onPeak != null) onPeak.run();
            onDone.run();
            return;
        }
        float baseTranslationY = sprite.getTranslationY();
        sprite.animate().cancel();
        sprite.setTranslationY(baseTranslationY);
        float density = sprite.getResources().getDisplayMetrics().density;
        float downOffset = 24f * density;

        android.animation.ObjectAnimator down = android.animation.ObjectAnimator.ofFloat(
                sprite, "translationY", baseTranslationY, baseTranslationY + downOffset);
        down.setDuration(250);
        down.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onPeak != null) onPeak.run();
                android.animation.ObjectAnimator up = android.animation.ObjectAnimator.ofFloat(
                        sprite, "translationY", baseTranslationY + downOffset, baseTranslationY);
                up.setDuration(250);
                up.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        onDone.run();
                    }
                });
                up.start();
            }
        });
        down.start();
    }

    private void onMonsterWillActInUi(Monster monster) {
    }

    private int findMonsterSlotIndex(Monster m) {
        for (int i = 0; i < 5; i++) {
            Monster slotM = monsterAtSlot(i);
            if (slotM != null && slotM == m) return i;
        }
        return -1;
    }

    private boolean isLastHitCritical() {
        return battleContext != null && battleContext.isCriticalHit;
    }

    /**
     * 受击反馈：暴击时单行暴击数字 + 受击脉冲，否则普通伤害数字。
     */
    private void showHpDamageFeedback(
            @NonNull DamageNumberOverlay overlay,
            @NonNull View pulseTarget,
            int cx,
            int cy,
            int damage) {
        if (isLastHitCritical()) {
            overlay.showCritDamageOffset(cx, cy, damage);
            BattleCritVfx.playHitPulse(pulseTarget);
        } else {
            overlay.showDamageOffset(cx, cy, damage);
        }
    }

    /**
     * 为玩家和所有怪物注册 HP/MP/AP 资源变更监听。
     * 任何来源（普攻/技能/道具/词缀/buff/被动）造成的资源变化都会自动弹出数字提示。
     */
    private void setupResourceChangeListeners() {
        if (player == null || battleContext == null) return;

        // 启动动画管理器
        if (animationManager != null) {
            animationManager.start();
        }

        player.setResourceChangeListener(new BattleEntity.OnResourceChangeListener() {
            @Override
            public void onHpChanged(int delta, int newHp) {
                if (isFinishing() || isDestroyed() || tachieDamageOverlay == null || ivBattleTachie == null) return;
                BattleActivity act = BattleActivity.this;
                if (act == null || act.isFinishing() || act.isDestroyed()) return;
                FrameLayout decor = (FrameLayout) act.getWindow().getDecorView();
                int[] loc = new int[2];
                ivBattleTachie.getLocationOnScreen(loc);
                int[] parentLoc = new int[2];
                decor.getLocationOnScreen(parentLoc);
                int cx = loc[0] - parentLoc[0] + ivBattleTachie.getWidth() / 2;
                int cy = loc[1] - parentLoc[1];
                if (delta > 0) {
                    tachieDamageOverlay.showHealOffset(cx, cy, delta);
                } else {
                    showHpDamageFeedback(tachieDamageOverlay, ivBattleTachie, cx, cy, -delta);
                }
            }

            @Override
            public void onMpChanged(int delta, int newMp) {
                if (isFinishing() || isDestroyed() || tachieDamageOverlay == null || ivBattleTachie == null) return;
                BattleActivity act = BattleActivity.this;
                if (act == null || act.isFinishing() || act.isDestroyed()) return;
                FrameLayout decor = (FrameLayout) act.getWindow().getDecorView();
                int[] loc = new int[2];
                ivBattleTachie.getLocationOnScreen(loc);
                int[] parentLoc = new int[2];
                decor.getLocationOnScreen(parentLoc);
                int cx = loc[0] - parentLoc[0] + ivBattleTachie.getWidth() / 2;
                int cy = loc[1] - parentLoc[1];
                tachieDamageOverlay.showMpChange(cx, cy, delta);
            }

            @Override
            public void onApChanged(int delta, int newAp) {
                if (isFinishing() || isDestroyed() || tachieDamageOverlay == null || ivBattleTachie == null) return;
                BattleActivity act = BattleActivity.this;
                if (act == null || act.isFinishing() || act.isDestroyed()) return;
                FrameLayout decor = (FrameLayout) act.getWindow().getDecorView();
                int[] loc = new int[2];
                ivBattleTachie.getLocationOnScreen(loc);
                int[] parentLoc = new int[2];
                decor.getLocationOnScreen(parentLoc);
                int cx = loc[0] - parentLoc[0] + ivBattleTachie.getWidth() / 2;
                int cy = loc[1] - parentLoc[1];
                tachieDamageOverlay.showApChange(cx, cy, delta);
            }
        });

        java.util.List<Monster> monsters = battleContext.monsters;
        if (monsters != null) {
            for (Monster m : monsters) {
                if (m == null) continue;
                m.setResourceChangeListener(new BattleEntity.OnResourceChangeListener() {
                    @Override
                    public void onHpChanged(int delta, int newHp) {
                        if (isFinishing() || isDestroyed() || damageNumberOverlay == null || monsterArea == null) return;
                        int slotIdx = findMonsterSlotIndex(m);
                        View sprite = monsterSpriteAnimTarget(slotIdx);
                        if (sprite == null) return;
                        int[] loc = new int[2];
                        sprite.getLocationOnScreen(loc);
                        int[] parentLoc = new int[2];
                        monsterArea.getLocationOnScreen(parentLoc);
                        int cx = loc[0] - parentLoc[0] + sprite.getWidth() / 2;
                        int cy = loc[1] - parentLoc[1] + sprite.getHeight() / 2;
                        if (delta > 0) {
                            damageNumberOverlay.showHealOffset(cx, cy, delta);
                        } else {
                            showHpDamageFeedback(damageNumberOverlay, sprite, cx, cy, -delta);
                        }
                    }

                    @Override
                    public void onMpChanged(int delta, int newMp) { }

                    @Override
                    public void onApChanged(int delta, int newAp) { }
                });
            }
        }
    }

    private void setupTriggerListeners() {
        if (battleContext == null) return;

        com.example.treasure_and_battle.manager.affix.AffixManager affixMgr =
                com.example.treasure_and_battle.manager.affix.AffixManager.getInstance(this.getApplicationContext());
        affixMgr.setOnAffixTriggerListener((entity, affixName) -> {
            if (isFinishing() || isDestroyed() || battleContext == null) return;
            showTriggerTextForEntity(entity, affixName, true);
        });

        com.example.treasure_and_battle.manager.battle.BuffManager buffMgr =
                com.example.treasure_and_battle.manager.battle.BuffManager.getInstance(this.getApplicationContext());
        buffMgr.setOnBuffTriggerListener((entity, buffName) -> {
            if (isFinishing() || isDestroyed() || battleContext == null) return;
            showTriggerTextForEntity(entity, buffName, false);
        });
        buffMgr.setOnBuffApplyListener(new com.example.treasure_and_battle.manager.battle.BuffManager.OnBuffApplyListener() {
            @Override
            public void onBuffApplied(BattleEntity entity, String buffName) {
                if (isFinishing() || isDestroyed() || battleContext == null) return;
                showTriggerTextFixed(entity, "施加 " + buffName);
            }
            @Override
            public void onBuffAddFailed(BattleEntity entity, String buffName, boolean resisted) {
                if (isFinishing() || isDestroyed() || battleContext == null) return;
                showTriggerTextFixed(entity, "抵抗 " + buffName);
            }
        });

        com.example.treasure_and_battle.manager.skill.PassiveSkillManager passiveMgr =
                com.example.treasure_and_battle.manager.skill.PassiveSkillManager.getInstance();
        passiveMgr.setOnPassiveSkillTriggerListener((entity, skillName) -> {
            if (isFinishing() || isDestroyed() || battleContext == null) return;
            showTriggerTextForEntity(entity, skillName, false);
        });
    }

    private int triggerTextSequenceY = 0;

    private void showTriggerTextForEntity(BattleEntity entity, String name, boolean isAffix) {
        if (isFinishing() || isDestroyed() || tachieDamageOverlay == null || damageNumberOverlay == null) return;

        final int cyOffset = triggerTextSequenceY * 50;
        triggerTextSequenceY++;

        mainHandler.postDelayed(() -> {
            if (triggerTextSequenceY > 0) triggerTextSequenceY--;
        }, 600);

        showTriggerTextAt(entity, name, cyOffset, isAffix);
    }

    private void showTriggerTextFixed(BattleEntity entity, String name) {
        if (isFinishing() || isDestroyed() || tachieDamageOverlay == null || damageNumberOverlay == null) return;

        final int cyOffset = triggerTextSequenceY * 50;

        showTriggerTextAt(entity, name, cyOffset, false);
    }

    private void showTriggerTextAt(BattleEntity entity, String name, int cyOffset, boolean isAffix) {
        if (entity == player) {
            BattleActivity act = BattleActivity.this;
            if (act == null || act.isFinishing() || act.isDestroyed()) return;
            int[] loc = new int[2];
            ivBattleTachie.getLocationOnScreen(loc);
            FrameLayout decor = (FrameLayout) act.getWindow().getDecorView();
            int[] parentLoc = new int[2];
            decor.getLocationOnScreen(parentLoc);
            int cx = loc[0] - parentLoc[0] + ivBattleTachie.getWidth() / 2;
            int cy = loc[1] - parentLoc[1] + ivBattleTachie.getHeight() / 4 - cyOffset;
            cy = Math.max(cy, (int)(50 * decor.getResources().getDisplayMetrics().density));
            if (isAffix) {
                tachieDamageOverlay.showAffixTrigger(cx, cy, name);
            } else {
                tachieDamageOverlay.showBuffTrigger(cx, cy, name);
            }
        } else {
            int slotIdx = findMonsterSlotIndex((Monster) entity);
            View sprite = monsterSpriteAnimTarget(slotIdx);
            if (sprite == null) return;
            int[] loc = new int[2];
            sprite.getLocationOnScreen(loc);
            int[] parentLoc = new int[2];
            monsterArea.getLocationOnScreen(parentLoc);
            int cx = loc[0] - parentLoc[0] + sprite.getWidth() / 2;
            int cy = loc[1] - parentLoc[1] - cyOffset;
            cy = Math.max(cy, (int)(30 * monsterArea.getResources().getDisplayMetrics().density));
            damageNumberOverlay.showAffixTrigger(cx, cy, name);
        }
    }

    private void showFloatMonsterText(Monster monster, String text) {
        if (isFinishing() || isDestroyed() || damageNumberOverlay == null || monsterArea == null) return;
        int slotIdx = findMonsterSlotIndex(monster);
        View sprite = monsterSpriteAnimTarget(slotIdx);
        if (sprite == null) return;
        int[] loc = new int[2];
        sprite.getLocationOnScreen(loc);
        int[] parentLoc = new int[2];
        monsterArea.getLocationOnScreen(parentLoc);
        int cx = loc[0] - parentLoc[0] + sprite.getWidth() / 2;
        int cy = loc[1] - parentLoc[1];
        cy = Math.max(cy, (int)(30 * monsterArea.getResources().getDisplayMetrics().density));
        damageNumberOverlay.show(cx, cy, text, 0xFFE53935);
    }

    private void showPlayerEscapeFailText() {
        if (isFinishing() || isDestroyed() || tachieDamageOverlay == null || ivBattleTachie == null) return;
        int[] loc = new int[2];
        ivBattleTachie.getLocationOnScreen(loc);
        int cx = loc[0] + ivBattleTachie.getWidth() / 2;
        int cy = loc[1] + ivBattleTachie.getHeight() / 3;
        tachieDamageOverlay.show(cx, cy, "逃跑失败", 0xFFE53935);
    }

    private void onTachieClick() {
        if (player == null) return;
        if (statsDialog != null && statsDialog.isShowing()) return;

        ScaleAnimation anim = new ScaleAnimation(
                1.0f, 1.05f, 1.0f, 1.05f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(150);
        anim.setRepeatCount(1);
        anim.setRepeatMode(ScaleAnimation.REVERSE);
        ivBattleTachie.startAnimation(anim);

        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_battle_stats, null, false);
        populateStatsSheet(sheetView);

        statsDialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(sheetView)
                .create();
        statsDialog.setCanceledOnTouchOutside(true);
        sheetView.findViewById(R.id.btn_battle_stats_close).setOnClickListener(v -> statsDialog.dismiss());
        statsDialog.show();
        if (statsDialog.getWindow() != null) {
            statsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
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
                    if (b != null) {
                        addBuffStatBlock(buffContainer, b);
                    }
                }
            } else {
                addStatRow(buffContainer, "无 Buff", "");
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
        TextView row = new TextView(this);
        row.setTextSize(13);
        row.setTextColor(0xFFFFFFFF);
        row.setText(label + (value.isEmpty() ? "" : "：" + value));
        row.setPadding(0, 4, 0, 4);
        parent.addView(row);
    }

    private void addBuffStatBlock(@NonNull LinearLayout parent, @NonNull BaseBuff buff) {
        TextView header = new TextView(this);
        header.setTextSize(13);
        header.setTextColor(0xFFFFD54F);
        header.setText(buff.getBuffName() + "  ×" + Math.max(1, buff.getStackCount())
                + BuffUiText.durationSuffix(buff));
        header.setPadding(0, 6, 0, 2);
        parent.addView(header);

        TextView effect = new TextView(this);
        effect.setTextSize(12);
        effect.setTextColor(0xFFE0E0E0);
        effect.setText(BuffUiText.effectDescription(buff));
        effect.setPadding(0, 0, 0, 2);
        parent.addView(effect);
    }

    private void addStatRowGreen(LinearLayout parent, String label, String value) {
        TextView row = new TextView(this);
        row.setTextSize(13);
        row.setTextColor(0xFF4CAF50);
        row.setText(label + (value.isEmpty() ? "" : "：" + value));
        row.setPadding(0, 4, 0, 4);
        parent.addView(row);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        if (ivBattleTachie != null) {
            ivBattleTachie.setImageDrawable(null);
        }
        if (statsDialog != null && statsDialog.isShowing()) {
            statsDialog.dismiss();
        }
        statsDialog = null;
        if (battleManager != null) {
            battleManager.setMonsterActListener(null);
        }
        dismissLootPanel();

        // 销毁动画管理器
        if (animationManager != null) {
            animationManager.destroy();
        }
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
        if (isFinishing() || isDestroyed()) return;
        FloatMsgOverlay.showFloatMsg(this, text);
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
        if (battleContext == null || battleManager == null || isFinishing() || isDestroyed()) {
            return;
        }
        battleManager.settleBattleResult(battleContext);

        if (battleContext.battleResult == BattleContext.BattleResult.DEFEAT) {
            if (player != null && player.owner != null) {
                player.owner.syncFromPlayer(player);
            }
                        GameManager.getInstance(this).triggerAutoSave();
            showFloatMsg(summarizeResult());
            int goldLost = -battleContext.rewardGold;
            View defeatRoot = LayoutInflater.from(this).inflate(R.layout.dialog_treasure_alert, null, false);
            TextView defeatTitle = defeatRoot.findViewById(R.id.tv_treasure_alert_title);
            TextView defeatMsg = defeatRoot.findViewById(R.id.tv_treasure_alert_message);
            defeatTitle.setText("战斗失败");
            defeatMsg.setText("战斗失败！你损失 " + goldLost + " 金币");
            View defeatNeg = defeatRoot.findViewById(R.id.btn_treasure_alert_negative);
            TextView defeatPos = defeatRoot.findViewById(R.id.btn_treasure_alert_positive);
            defeatNeg.setVisibility(View.GONE);
            defeatPos.setText("确定");
            AlertDialog defeatDlg = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                    .setView(defeatRoot)
                    .setCancelable(false)
                    .create();
            defeatPos.setOnClickListener(v -> {
                defeatDlg.dismiss();
                finish();
            });
            defeatDlg.show();
            if (defeatDlg.getWindow() != null) {
                defeatDlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
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
                GameManager.getInstance(this).triggerAutoSave();

        int finalGold = battleContext.rewardGold;
        int finalExp = battleContext.rewardExp;
        String msg = summarizeResult();
        if (lootCopy.isEmpty()) {
            showFloatMsg(msg);
            finish();
            return;
        }
        showLootPanel(lootCopy, msg, finalGold, finalExp);
    }

    private void showLootPanel(List<Item> loot, String summaryMsg, int rewardGold, int rewardExp) {
        if (isFinishing() || isDestroyed()) return;

        lootItems.clear();
        lootItems.addAll(loot);
        lootCurrentPage = 0;
        lootMaxPage = (lootItems.size() + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_battle_loot, null, false);
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

        lootGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
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

        lootDialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .setCancelable(false)
                .create();

        lootDialog.show();
        if (lootDialog.getWindow() != null) {
            lootDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
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

        float density = this.getResources().getDisplayMetrics().density;
        int cellSizePx = (int) (68 * density);
        int itemSpacingPx = (int) (10 * density);
        int rowSpacingPx = (int) (16 * density);

        for (int row = 0; row < rows; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
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

                View cell = LayoutInflater.from(this).inflate(R.layout.item_bag_grid, rowLayout, false);
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

        TextView tvPageInfo = lootContentRoot.findViewById(R.id.tv_loot_page_info);
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

        if (item.getRarity() != null) {
            int rarityColor = item.getRarity().getColor();
            if (bg != null) {
                bg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(rarityColor));
            }
            cell.setForeground(
                com.example.treasure_and_battle.drawable.TreasureStyleDrawable.newSlotStrokeOverlay(
                    this, rarityColor));
        } else {
            if (bg != null) {
                bg.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.tb_slot_empty)));
            }
            cell.setForeground(this.getResources().getDrawable(R.drawable.bg_slot_treasure_stroke));
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
            GameAssetIcons.bindItem(this, imageView, item);
            return;
        }
        String path = GameAssetIcons.assetPath(folder, item.getId());
        if (path == null) {
            GameAssetIcons.bindItem(this, imageView, item);
            return;
        }

        Bitmap cached = LruBitmapCache.getInstance().get(path);
        if (cached != null && !cached.isRecycled()) {
            imageView.setImageBitmap(cached);
            return;
        }

        try (InputStream is = this.getAssets().open(path)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp != null) {
                LruBitmapCache.getInstance().put(path, bmp);
                Bitmap cachedAgain = LruBitmapCache.getInstance().get(path);
                imageView.setImageBitmap(cachedAgain != null ? cachedAgain : bmp);
                return;
            }
        } catch (IOException ignored) {
        }

        GameAssetIcons.bindItem(this, imageView, item);
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

        Character ch = PlayerCharacterHolder.getOrCreate(this);
        List<Item> bag = ch.getBagItems();
        if (InventoryManager.addItem(bag, item)) {
            InventoryGridSync.reloadSharedGridFromManager(this);
            lootItems.remove(globalIndex);
            lootMaxPage = (lootItems.size() + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
            if (lootCurrentPage >= lootMaxPage && lootMaxPage > 0) {
                lootCurrentPage = lootMaxPage - 1;
            }
            if (lootItems.isEmpty()) {
                dismissLootPanel();
                finish();
                return;
            }
            renderLootPage();
        } else {
            showFloatMsg("背包已满");
        }
    }

    private void onLootClaimAll() {
        Character ch = PlayerCharacterHolder.getOrCreate(this);
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

        InventoryGridSync.reloadSharedGridFromManager(this);

        if (claimed > 0) {
            lootMaxPage = (lootItems.size() + LOOT_PAGE_SIZE - 1) / LOOT_PAGE_SIZE;
            if (lootCurrentPage >= lootMaxPage && lootMaxPage > 0) {
                lootCurrentPage = lootMaxPage - 1;
            }
        }

        if (lootItems.isEmpty()) {
            showFloatMsg("已领取 " + claimed + " / " + total + " 件");
            dismissLootPanel();
            finish();
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
            View leaveRoot = LayoutInflater.from(this).inflate(R.layout.dialog_treasure_alert, null, false);
            TextView leaveTitle = leaveRoot.findViewById(R.id.tv_treasure_alert_title);
            TextView leaveMsg = leaveRoot.findViewById(R.id.tv_treasure_alert_message);
            leaveTitle.setText("未领取掉落物");
            leaveMsg.setText("还有 " + remaining + " 件掉落物未领取，确定要离开？未领取的道具将永久消失。");
            View leaveNeg = leaveRoot.findViewById(R.id.btn_treasure_alert_negative);
            TextView leavePos = leaveRoot.findViewById(R.id.btn_treasure_alert_positive);
            leaveNeg.setVisibility(View.VISIBLE);
            ((TextView) leaveNeg).setText("取消");
            leavePos.setText("确定");
            AlertDialog leaveDlg = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                    .setView(leaveRoot)
                    .create();
            leaveNeg.setOnClickListener(v -> leaveDlg.dismiss());
            leavePos.setOnClickListener(v -> {
                leaveDlg.dismiss();
                dismissLootPanel();
                finish();
            });
            leaveDlg.show();
            if (leaveDlg.getWindow() != null) {
                leaveDlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
        } else {
            dismissLootPanel();
            finish();
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
        Character ch = PlayerCharacterHolder.getOrCreate(this);
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
        InventoryGridSync.reloadSharedGridFromManager(this);
        return placed;
    }

    private void showMonsterInspectDialog(@NonNull Monster m) {
        View root = LayoutInflater.from(this).inflate(R.layout.dialog_item_detail, null, false);
        TextView title = root.findViewById(R.id.tv_detail_title);
        TextView body = root.findViewById(R.id.tv_detail_body);
        ImageView icon = root.findViewById(R.id.iv_detail_icon);

        title.setText(m.getName());
        Rarity rarity = m.getRarity();
        if (rarity != null) {
            title.setTextColor(rarity.getColor());
        } else {
            title.setTextColor(ContextCompat.getColor(this, R.color.tb_gold_deep));
        }

        HtmlRenderUtils.setHtmlText(body, buildMonsterDetailHtml(m));

        if (icon != null) {
            GameAssetIcons.bindMonster(this, icon, m.getEntityId(), R.drawable.ic_map);
            icon.setVisibility(View.VISIBLE);
        }

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog);
        builder.setView(root);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        View close = root.findViewById(R.id.btn_detail_close);
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private String buildMonsterDetailHtml(@NonNull Monster m) {
        StringBuilder sb = new StringBuilder();

        Rarity rarity = m.getRarity();
        appendHtmlLine(sb, "等级", "Lv." + m.getLevel());
        appendHtmlLine(sb, "品质", rarity != null ? rarity.getDisplayName() : "?");
        AttributeSet attr = m.getFinalAttributes();
        if (attr != null) {
            sb.append("<b>【基础属性】</b><br>");
            appendStatIfNonZero(sb, "生命上限", attr.maxHp);
            appendStatIfNonZero(sb, "法力上限", attr.maxMp);
            appendStatIfNonZero(sb, "物理攻击", attr.physicalAtk);
            appendStatIfNonZero(sb, "魔法攻击", attr.magicalAtk);
            appendStatIfNonZero(sb, "物理防御", attr.physicalDef);
            appendStatIfNonZero(sb, "魔法防御", attr.magicalDef);
            appendStatIfNonZero(sb, "速度", attr.speed);
            appendFloatStatIfNonZero(sb, "物理暴击率", attr.physicalCritRate);
            appendFloatStatIfNonZero(sb, "闪避率", attr.dodgeRate);
        }

        List<BaseAffix> affixes = m.getEntityAffixList();
        if (affixes != null && !affixes.isEmpty()) {
            sb.append("<b>【词缀】</b><br>");
            for (BaseAffix affix : affixes) {
                if (affix == null) continue;
                String desc = affix.getAffixName() + "：" + affix.getDescription();
                if (desc == null || desc.isEmpty()) continue;
                String colorHex = HtmlRenderUtils.colorToHex(affix.getRarity() != null
                        ? affix.getRarity().getColor() : 0xFF888888);
                sb.append("<font color=\"").append(colorHex).append("\">●</font> ");
                sb.append(android.text.TextUtils.htmlEncode(desc)).append("<br>");
            }
        }

        List<BaseBuff> buffs = m.getActiveBuffList();
        sb.append("<b>【Buff】</b><br>");
        if (buffs == null || buffs.isEmpty()) {
            sb.append("无<br>");
        } else {
            for (BaseBuff b : buffs) {
                if (b == null) continue;
                int dur = b.getRemainingDuration();
                if (dur > 0) {
                sb.append("· ").append(b.getBuffName())
                        .append(" 层").append(b.getStackCount())
                        .append(" 剩").append(dur).append("回合<br>");
                } else {
                    sb.append("· ").append(b.getBuffName()).append(" 层").append(b.getStackCount()).append("<br>");
                }
            }
        }

        if (battleContext != null && battleContext.monsterRevealedIntents != null) {
            List<RevealedIntent> revealed = battleContext.monsterRevealedIntents.get(m.getBattleKey());
            sb.append("<b>【意图】</b><br>");
            if (revealed == null || revealed.isEmpty()) {
                sb.append("本轮暂无揭示意图。<br>");
            } else {
                for (RevealedIntent ri : revealed) {
                    if (ri == null) continue;
                    if (ri.seenThrough && ri.intent != null) {
                        ActionIntent in = ri.intent;
                        sb.append(String.format("· %s（%s） AP%d MP%d<br>",
                                in.getName(), in.getType().name(), in.getApCost(), in.getMpCost()));
                        if (in.getType() == ActionIntent.IntentType.SKILL) {
                            ActiveSkill skill = m.getMonsterSkill(in.getActionRefId());
                            if (skill != null) {
                                String formattedDesc = skill.getFormattedDetailedDesc();
                                if (formattedDesc != null && !formattedDesc.isEmpty()) {
                                    sb.append("  <small><i>")
                                            .append(android.text.TextUtils.htmlEncode(formattedDesc))
                                            .append("</i></small><br>");
                                }
                            }
                        }
                    } else {
                        sb.append("· （未看破）<br>");
                    }
                }
            }
        }

        return sb.toString();
    }

    private static void appendHtmlLine(StringBuilder sb, String label, String value) {
        if (value == null || value.isEmpty()) return;
        sb.append("<b>").append(label).append("：</b>").append(value).append("<br>");
    }

    private static void appendStatIfNonZero(StringBuilder sb, String label, int v) {
        if (v != 0) sb.append(label).append("：").append(v).append("<br>");
    }

    private static void appendFloatStatIfNonZero(StringBuilder sb, String label, float v) {
        if (Math.abs(v) > 0.0001f) sb.append(label).append("：").append(String.format("%.1f%%", v * 100f)).append("<br>");
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

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_battle_log, null, false);
        RecyclerView rv = content.findViewById(R.id.rv_battle_log);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new BattleLogListAdapter(forDisplay));

        AlertDialog logDlg = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Tb_ItemDetailDialog)
                .setView(content)
                .create();
        content.findViewById(R.id.btn_battle_log_close).setOnClickListener(v -> logDlg.dismiss());
        logDlg.show();
        if (logDlg.getWindow() != null) {
            logDlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private void refreshTargetMarkers() {
        for (int i = 0; i < 5; i++) {
            TextView tri = slotMarkers[i];
            boolean show = false;
            if (pendingMode == PendingMode.AOE_HIGHLIGHT) {
                show = isSlotAliveMonster(i);
            } else if ((pendingMode == PendingMode.PICK_SINGLE_ATTACK
                    || pendingMode == PendingMode.PICK_SINGLE_SKILL
                    || pendingMode == PendingMode.PICK_SINGLE_ITEM)
                    && selectedTargetIndex == i
                    && isSlotAliveMonster(i)) {
                show = true;
            }
            tri.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * 设置实体View映射 - 为动画系统提供目标定位
     */
    private void setupEntityViewMappings() {
        if (animationManager == null) return;

        // 注册玩家立绘
        if (player != null && ivBattleTachie != null) {
            animationManager.registerEntityView(
                player.getEntityId(),
                ivBattleTachie,
                (ViewGroup) ivBattleTachie.getParent()
            );
        }

        // 注册所有怪物槽位
        for (int i = 0; i < 5; i++) {
            Monster m = monsterAtSlot(i);
            if (m != null && slotIcons[i] != null) {
                // 获取合适的容器View
                ViewGroup container = null;
                if (slotRoots[i] instanceof ViewGroup) {
                    container = (ViewGroup) slotRoots[i];
                } else if (slotRoots[i].getParent() instanceof ViewGroup) {
                    container = (ViewGroup) slotRoots[i].getParent();
                }

                if (container != null) {
                    // 使用动画专用ID，而不是通用entityId
                    animationManager.registerEntityView(
                        m.getAnimationId(),
                        slotIcons[i],
                        container
                    );
                }
            }
        }

        // 注册全局场景容器
        if (monsterArea != null) {
            animationManager.registerSceneContainer(monsterArea);
        }
    }

    /**
     * 发送动画信号到管道
     */
    private void emitAnimationSignal(String signalId, String activeEntityId,
                                  List<String> targetEntityIds) {
        if (animationManager != null) {
            AnimationSignal signal = AnimationSignal.createMultiTarget(
                signalId, activeEntityId, targetEntityIds
            );
            AnimationSignalPipeline.getInstance().emitSignal(signal);
        }
    }

    /**
     * 发送无目标动画信号
     */
    private void emitAnimationSignal(String signalId, String activeEntityId) {
        if (animationManager != null) {
            AnimationSignal signal = AnimationSignal.createNoTarget(
                signalId, activeEntityId
            );
            AnimationSignalPipeline.getInstance().emitSignal(signal);
        }
    }

    /**
     * 调试方法 - 测试动画系统
     * 在设置界面或通过特殊操作调用
     */
    public void debugTestAnimationSystem() {
        if (player == null || animationManager == null) {
            android.util.Log.w("BattleFragment", "无法测试动画系统：player或animationManager为null");
            return;
        }

        android.util.Log.i("BattleFragment", "=== 开始测试动画系统 ===");

        // 构建目标实体列表 - 包含所有怪物
        java.util.List<String> targetEntityIds = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Monster m = monsterAtSlot(i);
            if (m != null) {
                targetEntityIds.add(m.getEntityId());
            }
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

            int cd = skill.getCurrentCooldown();
            if (cd > 0) {
                h.cooldown.setText("冷却：剩余 " + cd + " 回合");
                h.cooldown.setVisibility(View.VISIBLE);
            } else {
                h.cooldown.setVisibility(View.GONE);
            }

            // 基于 canCast 校验技能可用性：资源不足或冷却中 → 变灰不可点击
            boolean canCast = player != null && skill.canCast(player);
            if (canCast) {
                h.name.setAlpha(1f);
                h.cost.setAlpha(1f);
                h.itemView.setAlpha(1f);
                h.itemView.setEnabled(true);
            } else {
                h.name.setAlpha(0.38f);
                h.cost.setAlpha(0.38f);
                h.itemView.setAlpha(0.38f);
                h.itemView.setEnabled(false);
            }

            h.itemView.setOnClickListener(v -> {
                if (listener != null && canCast) {
                    listener.onPick(skill);
                }
            });
            h.itemView.setOnLongClickListener(v -> {
                SkillDetailDialog.show(v.getContext(), SkillDetailDialog.fromActiveSkill(skill));
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return skills.size();
        }

        class Vh extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView cost;
            final TextView cooldown;

            Vh(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_skill_pick_name);
                cost = itemView.findViewById(R.id.tv_skill_pick_cost);
                cooldown = itemView.findViewById(R.id.tv_skill_pick_cooldown);
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
            h.name.setText(line.name);
            if (line.placeholder) {
                h.stacks.setVisibility(View.GONE);
            } else {
                h.stacks.setVisibility(View.VISIBLE);
                h.stacks.setText(String.valueOf(line.stacks));
            }
            View.OnClickListener openBuff = v -> showBuffDetailDialog(line.name, line.detailBody);
            h.itemView.setOnClickListener(line.placeholder ? null : openBuff);
            h.name.setOnClickListener(line.placeholder ? null : openBuff);
            h.stacks.setOnClickListener(line.placeholder ? null : openBuff);
        }

        @Override
        public int getItemCount() {
            return buffLines.size();
        }

        class Vh extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView stacks;

            Vh(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_battle_buff_name);
                stacks = itemView.findViewById(R.id.tv_battle_buff_stacks);
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
            h.itemView.setOnLongClickListener(v -> {
                ItemDetailDialog.show(v.getContext(), it);
                return true;
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
 