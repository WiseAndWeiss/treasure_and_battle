package com.example.treasure_and_battle.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.drawable.TreasureStyleDrawable;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.affix.EquipAffixManager;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.EventManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.utils.GameAssetIcons;

import java.util.ArrayList;
import java.util.List;

public class NeutralEventActivity extends AppCompatActivity {

    private String eventKey;
    private LinearLayout llActionArea;
    private LinearLayout llResultArea;
    private TextView tvResult;
    private Button btnContinue;
    private Button btnAction1;
    private Button btnAction2;

    private int caveStep = 0;
    private int[] caveHpLoss = new int[4];
    private String[] caveItemDesc = new String[2];
    private transient Item caveItem1;
    private transient Item caveItem2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neutral_event);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnContinue = findViewById(R.id.btn_continue);
        btnContinue.setOnClickListener(v -> finish());

        llActionArea = findViewById(R.id.ll_action_area);
        llResultArea = findViewById(R.id.ll_result_area);
        tvResult = findViewById(R.id.tv_result);

        Intent intent = getIntent();
        String name = intent.getStringExtra("event_name");
        String desc = intent.getStringExtra("event_desc");
        String reward = intent.getStringExtra("event_reward");
        String risk = intent.getStringExtra("event_risk");
        eventKey = intent.getStringExtra("event_key");

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(name != null ? name : "中性事件");
        ((TextView) findViewById(R.id.tv_event_name)).setText(name != null ? name : "未知事件");
        ((TextView) findViewById(R.id.tv_event_desc)).setText(desc != null ? desc : "暂无描述");
        ((TextView) findViewById(R.id.tv_event_reward)).setText(reward != null ? reward : "暂无");
        ((TextView) findViewById(R.id.tv_event_risk)).setText(risk != null ? risk : "无");

        buildActionButtons();
    }

    private void buildActionButtons() {
        llActionArea.removeAllViews();
        if (eventKey == null) return;

        switch (eventKey) {
            case "merchant":
                btnAction1 = addActionButton("进入商店交易", 0xFF2196F3, v -> {
                    Intent result = new Intent();
                    result.putExtra("open_trade", true);
                    setResult(RESULT_OK, result);
                    finish();
                });
                btnAction2 = addActionButton("拒绝交易", 0xFF888888, v -> {
                    showResult("你拒绝了商人的交易邀请。");
                    switchToForwardButton();
                });
                break;

            case "exploration":
                btnAction1 = addActionButton("深入探险", 0xFFE53935, v -> {
                    showResult("你鼓起勇气深入洞穴...\n（探险系统后续开发）");
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("谨慎离开", 0xFF888888, v -> {
                    showResult("你选择了安全离开，放弃了可能存在的宝藏。");
                    switchToForwardButton();
                });
                break;

            case "traveler":
                btnAction1 = addActionButton("帮助旅人", 0xFF4CAF50, v -> {
                    showResult("你帮助了迷路的旅人！\n\n✅ 获得补给品 ×3\n✅ 获得金币 ×200\n✅ 幸运值提升，持续1小时");
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("无视旅人", 0xFF888888, v -> {
                    showResult("你匆匆走过，没有理会旅人求助的目光。");
                    switchToForwardButton();
                });
                break;

            case "scholar":
                btnAction1 = addActionButton("洗点重置（消耗500金币）", 0xFFFF9800, v -> {
                    showResult("你消耗了500金币，天赋点和技能点已重置！");
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("离开", 0xFF888888, v -> {
                    showResult("你离开了学者，保持现有技能配置。");
                    switchToForwardButton();
                });
                break;

            case "statue_blessing":
                btnAction1 = addActionButton("接受雕像祝福", 0xFFFFC107, v -> {
                    showResult("雕像散发金色光芒...\n\n✅ 下3场战斗开始时获得随机Buff：\n  · 攻击力 +10%\n  · 防御力 +10%\n  · 最大生命 +15%");
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("绕道离开", 0xFF888888, v -> {
                    showResult("你绕过了雕像，没有接受祝福。");
                    switchToForwardButton();
                });
                break;

            case "monster_camp":
                btnAction1 = addActionButton("偷袭怪物", 0xFFE53935, v -> {
                    showResult("你悄悄靠近并发动偷袭！必定先手攻击！\n（战斗系统后续开发）");
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("悄悄离开", 0xFF888888, v -> {
                    showResult("你屏住呼吸，悄悄绕过了正在休息的怪物。");
                    switchToForwardButton();
                });
                break;

            case "cave_treasure":
                buildCaveTreasureActions();
                break;

            case "equipment_reforge":
                btnAction1 = addActionButton("选择装备重炼", 0xFF9C27B0, v -> {
                    showEquipmentSelectionDialog();
                });
                btnAction2 = addActionButton("暂时不需要", 0xFF888888, v -> {
                    showResult("你婉拒了铁匠大师的好意。");
                    switchToForwardButton();
                });
                break;

            case "casino_wagon":
                btnAction1 = addActionButton("下注100金币", 0xFFE53935, v -> {
                    int roll = (int) (Math.random() * 100);
                    if (roll < 40) {
                        showResult("🎉 恭喜！你赢了！\n\n✅ 获得双倍回报：200金币！");
                    } else if (roll < 70) {
                        showResult("😐 平局！你的100金币退还给你。");
                    } else {
                        showResult("😞 你输了...100金币血本无归。");
                    }
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("不参与赌博", 0xFF888888, v -> {
                    showResult("你收起好奇心，继续前行。");
                    switchToForwardButton();
                });
                break;

            case "divination_hut":
                if (EventManager.getInstance(getApplicationContext()).hasUnknownEvents()) {
                    btnAction1 = addActionButton("付300金币占卜", 0xFF7B1FA2, v -> {
                        String result = EventManager.getInstance(getApplicationContext()).revealUnknownEvent();
                        showResult(result);
                        switchToForwardButton();
                    });
                } else {
                    btnAction1 = addActionButton("付300金币占卜", 0xFFAAAAAA, v -> {});
                    btnAction1.setEnabled(false);
                }
                btnAction2 = addActionButton("不感兴趣", 0xFF888888, v -> {
                    showResult("你觉得占卜师只是在故弄玄虚，直接走开了。");
                    switchToForwardButton();
                });
                if (!EventManager.getInstance(getApplicationContext()).hasUnknownEvents()) {
                    showResult("占卜师遗憾地告诉你，当前地图上没有未知的迷雾需要揭示。");
                    switchToForwardButton();
                }
                break;

            case "mystery_box":
                btnAction1 = addActionButton("购买盲盒（200金币）", 0xFFFF9800, v -> {
                    ItemManager im = ItemManager.getInstance(NeutralEventActivity.this);
                    EquipmentManager em = EquipmentManager.getInstance(NeutralEventActivity.this);
                    Rarity[] rarities = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
                    Rarity rarity = rarities[(int) (Math.random() * rarities.length)];
                    Character ch = PlayerCharacterHolder.getOrCreate(NeutralEventActivity.this);

                    int roll = (int) (Math.random() * 3);
                    StringBuilder sb = new StringBuilder("🎁 打开盲盒！\n\n");

                    if (roll == 0) {
                        EquipItem equip = em.generateRandomEquip((int) (5 + Math.random() * 21), rarity);
                        if (equip != null) {
                            InventoryManager.addItem(ch.getBagItems(), equip);
                            sb.append("获得装备：").append(equip.getName())
                                    .append("\n品质：").append(equip.getRarity().getDisplayName());
                        } else {
                            sb.append("盲盒是空的...你被骗了！200金币打水漂。");
                        }
                    } else if (roll == 1) {
                        ConsumableItem consumable = im.getRandomConsumableByRarity(rarity);
                        if (consumable != null) {
                            InventoryManager.addItem(ch.getBagItems(), consumable);
                            sb.append("获得药水：").append(consumable.getName())
                                    .append("\n品质：").append(consumable.getRarity().getDisplayName());
                        } else {
                            sb.append("盲盒是空的...你被骗了！200金币打水漂。");
                        }
                    } else {
                        GemItem gem = im.getRandomGemByRarity(rarity);
                        if (gem != null) {
                            InventoryManager.addItem(ch.getBagItems(), gem);
                            sb.append("获得宝石：").append(gem.getName())
                                    .append("\n品质：").append(gem.getRarity().getDisplayName());
                        } else {
                            sb.append("盲盒是空的...你被骗了！200金币打水漂。");
                        }
                    }

                    showResult(sb.toString());
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("不相信盲盒", 0xFF888888, v -> {
                    showResult("你坚信便宜没好货，头也不回地走了。");
                    switchToForwardButton();
                });
                break;
        }
    }

    private Button addActionButton(String text, int bgColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(15);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundColor(bgColor);
        int pad = (int) (14 * getResources().getDisplayMetrics().density);
        btn.setPadding(pad, pad, pad, pad);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        params.setMargins(0, margin, 0, margin);
        btn.setLayoutParams(params);
        btn.setOnClickListener(listener);

        llActionArea.addView(btn);
        return btn;
    }

    private void showResult(String text) {
        llActionArea.setVisibility(View.GONE);
        tvResult.setText(text);
        llResultArea.setVisibility(View.VISIBLE);
    }

    private void switchToForwardButton() {
        if (btnContinue != null) {
            btnContinue.setText("前进");
            btnContinue.setBackgroundColor(0xFF4CAF50);
            btnContinue.setTextColor(0xFFFFFFFF);
            btnContinue.setOnClickListener(v -> finish());
        }
    }

    private void buildCaveTreasureActions() {
        llActionArea.removeAllViews();
        Character ch = PlayerCharacterHolder.getOrCreate(this);

        String btn1Text;
        View.OnClickListener btn1Listener;
        int btn1Color = 0xFFFF9800;

        if (caveStep == 0) {
            caveHpLoss[0] = 10 + (int) (Math.random() * 21);
            ItemManager im = ItemManager.getInstance(NeutralEventActivity.this);
            EquipmentManager em = EquipmentManager.getInstance(NeutralEventActivity.this);
            Rarity r = Math.random() < 0.5 ? Rarity.COMMON : Rarity.UNCOMMON;
            if (Math.random() < 0.4) {
                caveItem1 = em.generateRandomEquip(5 + (int) (Math.random() * 11), r);
            } else {
                caveItem1 = im.getRandomConsumableByRarity(r);
                if (caveItem1 == null) caveItem1 = im.getRandomGemByRarity(r);
            }
            caveItemDesc[0] = caveItem1 != null ? caveItem1.getName() : "一件宝物";
            if (caveItem1 != null) {
                InventoryManager.addItem(ch.getBagItems(), caveItem1);
            }
            btn1Text = "你不小心擦破了皮肤，但是你找到了一件宝物（-" + caveHpLoss[0] + "点生命，获得" + caveItemDesc[0] + "）";
            btn1Listener = v -> {
                caveStep = 1;
                ch.setCurrentHp(Math.max(1, ch.getCurrentHp() - caveHpLoss[0]));
                buildCaveTreasureActions();
            };
        } else if (caveStep == 1) {
            caveHpLoss[1] = 20 + (int) (Math.random() * 31);
            ItemManager im = ItemManager.getInstance(NeutralEventActivity.this);
            EquipmentManager em = EquipmentManager.getInstance(NeutralEventActivity.this);
            Rarity r = Math.random() < 0.6 ? Rarity.UNCOMMON : Rarity.RARE;
            if (Math.random() < 0.4) {
                caveItem2 = em.generateRandomEquip(10 + (int) (Math.random() * 11), r);
            } else {
                caveItem2 = im.getRandomConsumableByRarity(r);
                if (caveItem2 == null) caveItem2 = im.getRandomGemByRarity(r);
            }
            caveItemDesc[1] = caveItem2 != null ? caveItem2.getName() : "一件宝物";
            if (caveItem2 != null) {
                InventoryManager.addItem(ch.getBagItems(), caveItem2);
            }
            btn1Text = "你进一步深入探索，虽然受了点伤，但是你找到了一件宝物（-" + caveHpLoss[1] + "点生命，获得" + caveItemDesc[1] + "）";
            btn1Listener = v -> {
                caveStep = 2;
                ch.setCurrentHp(Math.max(1, ch.getCurrentHp() - caveHpLoss[1]));
                buildCaveTreasureActions();
            };
        } else if (caveStep == 2) {
            caveHpLoss[2] = 30 + (int) (Math.random() * 41);
            btn1Text = "你即将走到洞穴最深处，但仍坚持继续探索（-" + caveHpLoss[2] + "点生命）";
            btn1Listener = v -> {
                caveStep = 3;
                ch.setCurrentHp(Math.max(1, ch.getCurrentHp() - caveHpLoss[2]));
                buildCaveTreasureActions();
            };
        } else {
            int roll = (int) (Math.random() * 2);
            if (roll == 0) {
                btn1Text = "洞穴里有一只猛兽，你被迫与它战斗！";
                btn1Color = 0xFFE53935;
                btn1Listener = v -> {
                    Intent result = new Intent();
                    result.putExtra("open_battle", true);
                    setResult(RESULT_OK, result);
                    finish();
                };
            } else {
                int gold = 500 + (int) (Math.random() * 1001);
                ch.addGold(gold);
                ItemManager im = ItemManager.getInstance(NeutralEventActivity.this);
                EquipmentManager em = EquipmentManager.getInstance(NeutralEventActivity.this);
                Rarity r = Math.random() < 0.5 ? Rarity.RARE : Rarity.EPIC;
                Item bonusItem;
                if (Math.random() < 0.4) {
                    bonusItem = em.generateRandomEquip(15 + (int) (Math.random() * 11), r);
                } else {
                    bonusItem = im.getRandomGemByRarity(r);
                    if (bonusItem == null) bonusItem = im.getRandomConsumableByRarity(r);
                }
                String bonusName = bonusItem != null ? bonusItem.getName() : "一份神秘的战利品";
                if (bonusItem != null) {
                    InventoryManager.addItem(ch.getBagItems(), bonusItem);
                }
                btn1Text = "你找到了不知谁遗弃的珠宝，你发财了！（金币+" + gold + "，获得" + bonusName + "）";
                btn1Color = 0xFF4CAF50;
                final String finalText = btn1Text;
                btn1Listener = v -> {
                    showResult(finalText);
                    switchToForwardButton();
                };
            }
        }

        addActionButton(btn1Text, btn1Color, btn1Listener);
        addActionButton("你感到害怕，选择离开", 0xFF888888, v -> {
            StringBuilder sb = new StringBuilder("你感到害怕，转身离开了洞穴。\n\n");
            if (caveStep >= 1) {
                sb.append("本次探险获得：\n");
                sb.append("· ").append(caveItemDesc[0]).append("\n");
            }
            if (caveStep >= 2) {
                sb.append("· ").append(caveItemDesc[1]).append("\n");
            }
            if (caveStep >= 1) {
                int totalHp = caveHpLoss[0];
                if (caveStep >= 2) totalHp += caveHpLoss[1];
                if (caveStep >= 3) totalHp += caveHpLoss[2];
                sb.append("\n共损失生命：" + totalHp + "点");
            } else {
                sb.append("你什么都没得到。");
            }
            showResult(sb.toString());
            switchToForwardButton();
        });
    }

    private void showEquipmentSelectionDialog() {
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        List<Item> bag = ch.getBagItems();
        if (InventoryManager.isEmpty(bag)) {
            EquipmentManager em = EquipmentManager.getInstance(this);
            InventoryManager.addItem(bag, em.generateRandomEquip(5, Rarity.COMMON));
            InventoryManager.addItem(bag, em.generateRandomEquip(8, Rarity.UNCOMMON));
            InventoryManager.addItem(bag, em.generateRandomEquip(12, Rarity.RARE));
            InventoryManager.addItem(bag, em.generateRandomEquip(20, Rarity.EPIC));
        }

        final List<EquipItem> equipItems = new ArrayList<>();
        for (Item item : bag) {
            if (item instanceof EquipItem) {
                equipItems.add((EquipItem) item);
            }
        }
        if (equipItems.isEmpty()) {
            showResult("你的背包中没有可重炼的装备。");
            return;
        }

        final Dialog gridDialog = new Dialog(this);
        gridDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View gridView = LayoutInflater.from(this).inflate(R.layout.dialog_equip_grid, null);
        RecyclerView rv = gridView.findViewById(R.id.rv_equip_grid);
        gridView.findViewById(R.id.btn_grid_close).setOnClickListener(v -> gridDialog.dismiss());

        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(new EquipGridAdapter(equipItems, equip -> {
            gridDialog.dismiss();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> showReforgeDialog(equip));
        }));

        gridDialog.setContentView(gridView);
        gridDialog.setCancelable(true);
        gridDialog.setCanceledOnTouchOutside(true);
        gridDialog.show();
    }

    private void showReforgeDialog(EquipItem equip) {
        final EquipItem targetEquip = equip;
        Dialog reforgeDialog = new Dialog(this);
        reforgeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_equip_reforge, null);
        reforgeDialog.setContentView(content);

        Window window = reforgeDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.66);
            window.setAttributes(lp);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivPreview = content.findViewById(R.id.iv_equip_preview);
        TextView tvName = content.findViewById(R.id.tv_equip_name);
        TextView tvRarity = content.findViewById(R.id.tv_equip_rarity);
        TextView tvLevel = content.findViewById(R.id.tv_equip_level);
        LinearLayout llAffixList = content.findViewById(R.id.ll_affix_list);
        ScrollView svAffix = content.findViewById(R.id.sv_affix_container);
        Button btnAction = content.findViewById(R.id.btn_reforge_action);
        Button btnCancel = content.findViewById(R.id.btn_reforge_cancel);

        final boolean[] hasReforged = {false};

        GameAssetIcons.bindItem(NeutralEventActivity.this, ivPreview, targetEquip);
        tvName.setText(targetEquip.getName());
        tvRarity.setText(targetEquip.getRarity().name());
        tvRarity.setTextColor(targetEquip.getRarity().getColor());
        tvLevel.setText("Lv." + targetEquip.getLevel());

        updateAffixList(llAffixList, targetEquip.getAffixes());

        btnAction.setOnClickListener(v -> {
            if (hasReforged[0]) {
                reforgeDialog.dismiss();
                showResult("装备重炼完成！新的词缀已经附魔到装备上。");
                switchToForwardButton();
            } else {
                List<BaseAffix> oldAffixes = new ArrayList<>(targetEquip.getAffixes());
                List<BaseEquipAffix> newAffixes = EquipAffixManager.getInstance(NeutralEventActivity.this)
                        .generateAffixForEquipment(targetEquip);
                targetEquip.getAffixes().clear();
                targetEquip.getAffixes().addAll(newAffixes);

                updateAffixList(llAffixList, targetEquip.getAffixes());
                svAffix.fullScroll(View.FOCUS_DOWN);

                btnAction.setText("确定");
                btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
                hasReforged[0] = true;
            }
        });

        btnCancel.setOnClickListener(v -> reforgeDialog.dismiss());

        reforgeDialog.show();
    }

    private void updateAffixList(LinearLayout container, List<? extends BaseAffix> affixes) {
        container.removeAllViews();
        int padH = (int) (4 * getResources().getDisplayMetrics().density);
        int padV = (int) (10 * getResources().getDisplayMetrics().density);
        if (affixes == null || affixes.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("(无词条)");
            tv.setTextSize(13);
            tv.setTextColor(0xFF888888);
            tv.setPadding(padH, padV, padH, padV);
            container.addView(tv);
            return;
        }
        String[] colors = {"#E53935", "#FF9800", "#FDD835", "#4CAF50", "#2196F3"};
        for (int i = 0; i < affixes.size(); i++) {
            BaseAffix a = affixes.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(padH, padV, padH, padV);
            row.setBackgroundColor(0x0AFFFFFF);

            TextView tvDot = new TextView(this);
            tvDot.setText("●");
            tvDot.setTextColor(android.graphics.Color.parseColor(colors[i % colors.length]));
            tvDot.setTextSize(10);
            row.addView(tvDot);

            TextView tvLine = new TextView(this);
            String affixName = (a != null && a.getAffixName() != null) ? a.getAffixName() : "???";
            String desc = (a != null && a.getDescription() != null) ? a.getDescription() : "...";
            tvLine.setText(affixName + "：" + desc);
            tvLine.setTextSize(13);
            tvLine.setTextColor(0xFFDDDDDD);
            tvLine.setPadding((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
            row.addView(tvLine);

            container.addView(row);
        }
    }

    private static class EquipGridAdapter extends RecyclerView.Adapter<EquipGridAdapter.VH> {
        private final List<EquipItem> items;
        private final OnEquipClickListener listener;

        interface OnEquipClickListener {
            void onClick(EquipItem item);
        }

        EquipGridAdapter(List<EquipItem> items, OnEquipClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_equip_select, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            EquipItem equip = items.get(position);
            holder.tvName.setText(equip.getName());
            holder.tvLevel.setText("Lv." + equip.getLevel());
            GameAssetIcons.bindItem(holder.itemView.getContext(), holder.ivIcon, equip);
            holder.bgColor.setBackgroundTintList(null);
            android.graphics.drawable.Drawable bg = holder.bgColor.getBackground();
            if (bg != null) {
                bg.clearColorFilter();
            }
            Integer borderArgb = equip.getRarity() != null ? equip.getRarity().getColor() : null;
            holder.itemView.setForeground(
                    TreasureStyleDrawable.newSlotStrokeOverlay(holder.itemView.getContext(), borderArgb));
            holder.itemView.setOnClickListener(v -> listener.onClick(equip));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            View bgColor;
            TextView tvName, tvLevel;
            ImageView ivIcon;

            VH(View v) {
                super(v);
                bgColor = v.findViewById(R.id.bg_item_color);
                tvName = v.findViewById(R.id.tv_item_name);
                tvLevel = v.findViewById(R.id.tv_item_level);
                ivIcon = v.findViewById(R.id.iv_item_icon);
            }
        }
    }
}
