package com.example.treasure_and_battle.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.drawable.TreasureStyleDrawable;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.manager.affix.EquipAffixManager;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.EventManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.utils.GameAssetIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.InputStream;

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

    private int travelerRequiredRarityId;
    private ItemType travelerRequiredType;
    private String travelerRequestDesc;

    private int wishingRound;
    private String lastWishResult;
    private static final int[] WISHING_AMOUNTS = {1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neutral_event);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            View btnBack = findViewById(R.id.btn_back);
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            android.widget.FrameLayout.LayoutParams lp =
                    (android.widget.FrameLayout.LayoutParams) btnBack.getLayoutParams();
            lp.topMargin = topInset + (int)(12 * getResources().getDisplayMetrics().density);
            btnBack.setLayoutParams(lp);
            return WindowInsetsCompat.CONSUMED;
        });

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

        ((TextView) findViewById(R.id.tv_event_name)).setText(name != null ? name : "未知事件");
        ((TextView) findViewById(R.id.tv_event_desc)).setText(desc != null ? desc : "暂无描述");
        ((TextView) findViewById(R.id.tv_event_reward)).setText(reward != null ? reward : "暂无");
        ((TextView) findViewById(R.id.tv_event_risk)).setText(risk != null ? risk : "无");

        loadEventBorder();

        buildActionButtons();
    }

    private void loadEventBorder() {
        if (eventKey == null) return;
        String fileName = eventKey.replace("equipment_reforge", "equipment_reforce") + ".png";
        ImageView ivBorder = findViewById(R.id.iv_event_border);
        try (InputStream is = getAssets().open("border/" + fileName)) {
            Bitmap raw = BitmapFactory.decodeStream(is);
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int padPx = (int) (12 * getResources().getDisplayMetrics().density * 2);
            int targetW = screenW - padPx;
            float ratio = (float) targetW / raw.getWidth();
            int targetH = (int) (raw.getHeight() * ratio);
            Bitmap scaled = Bitmap.createScaledBitmap(raw, targetW, targetH, false);
            raw.recycle();
            ivBorder.setImageBitmap(scaled);
            ivBorder.setVisibility(android.view.View.VISIBLE);
        } catch (Exception e) {
            ivBorder.setVisibility(android.view.View.GONE);
        }
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

            case "traveler":
                buildTravelerActions();
                break;

            case "scholar":
                btnAction1 = addActionButton("洗点重置（消耗500金币）", 0xFFFF9800, v -> {
                    Character ch = PlayerCharacterHolder.getOrCreate(NeutralEventActivity.this);
                    if (!ch.spendGold(500)) {
                        showResult("你的金币不足500，无法支付洗点费用。");
                        switchToForwardButton();
                        return;
                    }
                    int oldTalent = ch.getTalentPoints();
                    int oldSkill = ch.getSkillPoints();
                    ch.resetAllTalentPoints();
                    int skillRefund = 0;
                    if (ch.getProfession() != null) {
                        skillRefund += ch.getProfession().getActiveSkillTree().resetAllSkills();
                        skillRefund += ch.getProfession().getPassiveSkillTree().resetAllSkills();
                        skillRefund += ch.getProfession().getEventSkillTree().resetAllSkills();
                    }
                    ch.addSkillPoints(skillRefund);
                    int newTalent = ch.getTalentPoints();
                    int newSkill = ch.getSkillPoints();
                    showResult("你消耗了500金币，天赋点和技能点已重置！\n\n返还天赋点：+" + (newTalent - oldTalent) + "\n返还技能点：+" + (newSkill - oldSkill) + "\n当前金币：" + ch.getGold());
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("离开", 0xFF888888, v -> {
                    showResult("你离开了学者，保持现有技能配置。");
                    switchToForwardButton();
                });
                break;

            case "statue_blessing":
                btnAction1 = addActionButton("接受雕像祝福", 0xFFFFC107, v -> showGemUpgradeDialog());
                btnAction2 = addActionButton("绕道离开", 0xFF888888, v -> {
                    showResult("你绕过了雕像，没有接受祝福。");
                    switchToForwardButton();
                });
                break;

            case "monster_camp":
                btnAction1 = addActionButton("偷袭怪物", 0xFFE53935, v -> {
                    Monster monster = MonsterManager.getInstance(this).createRandomMonster();
                    EventManager em = EventManager.getInstance(getApplicationContext());
                    em.setCurrentBattleMonster(monster);
                    em.setCurrentBattleSurprise(BattleContext.SurpriseDirection.PLAYER_SURPRISE);
                    Intent result = new Intent();
                    result.putExtra("open_battle", true);
                    setResult(RESULT_OK, result);
                    finish();
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
                    Character ch = PlayerCharacterHolder.getOrCreate(NeutralEventActivity.this);
                    if (!ch.spendGold(100)) {
                        showResult("你的金币不足100，无法下注。");
                        switchToForwardButton();
                        return;
                    }
                    int roll = (int) (Math.random() * 100);
                    if (roll < 40) {
                        ch.addGold(200);
                        showResult("🎉 恭喜！你赢了！\n\n✅ 获得双倍回报：200金币！（当前金币：" + ch.getGold() + "）");
                    } else if (roll < 70) {
                        ch.addGold(100);
                        showResult("😐 平局！你的100金币退还给你。（当前金币：" + ch.getGold() + "）");
                    } else {
                        showResult("😞 你输了...100金币血本无归。（当前金币：" + ch.getGold() + "）");
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
                        Character ch = PlayerCharacterHolder.getOrCreate(NeutralEventActivity.this);
                        if (!ch.spendGold(300)) {
                            showResult("你的金币不足300，无法支付占卜费用。");
                            switchToForwardButton();
                            return;
                        }
                        String result = EventManager.getInstance(getApplicationContext()).revealUnknownEvent();
                        showResult(result + "\n（当前金币：" + ch.getGold() + "）");
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
                    Character ch = PlayerCharacterHolder.getOrCreate(NeutralEventActivity.this);
                    if (!ch.spendGold(200)) {
                        showResult("你的金币不足200，无法购买盲盒。");
                        switchToForwardButton();
                        return;
                    }
                    ItemManager im = ItemManager.getInstance(NeutralEventActivity.this);
                    EquipmentManager em = EquipmentManager.getInstance(NeutralEventActivity.this);
                    Rarity[] rarities = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
                    Rarity rarity = rarities[(int) (Math.random() * rarities.length)];

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

                    sb.append("\n（当前金币：" + ch.getGold() + "）");
                    showResult(sb.toString());
                    switchToForwardButton();
                });
                btnAction2 = addActionButton("不相信盲盒", 0xFF888888, v -> {
                    showResult("你坚信便宜没好货，头也不回地走了。");
                    switchToForwardButton();
                });
                break;

            case "mysterious_altar":
                buildAltarActions();
                break;

            case "phantom_maze":
                buildPhantomMazeActions();
                break;

            case "wishing_well":
                wishingRound = 0;
                lastWishResult = null;
                buildWishingWellActions();
                break;

            case "cursed_chest":
                buildCursedChestActions();
                break;
        }
    }

    private Button addActionButton(String text, int bgColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(10);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundResource(R.drawable.bg_neutral_action_btn);
        btn.getBackground().setTintList(ColorStateList.valueOf(bgColor));
        int pad = (int) (6 * getResources().getDisplayMetrics().density);
        btn.setPadding(pad, pad, pad, pad);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (2 * getResources().getDisplayMetrics().density);
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

    private void buildTravelerActions() {
        llActionArea.removeAllViews();

        Rarity[] rarities = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
        Rarity reqRarity = rarities[(int) (Math.random() * rarities.length)];
        travelerRequiredRarityId = reqRarity.getId();

        ItemType[] types = {ItemType.EQUIPMENT, ItemType.CONSUMABLE, ItemType.GEM};
        travelerRequiredType = types[(int) (Math.random() * types.length)];

        String typeName;
        switch (travelerRequiredType) {
            case EQUIPMENT: typeName = "装备"; break;
            case CONSUMABLE: typeName = "药水"; break;
            case GEM: typeName = "宝石"; break;
            default: typeName = "物品"; break;
        }

        travelerRequestDesc = "一件" + reqRarity.getDisplayName() + "品质的" + typeName;

        Character ch = PlayerCharacterHolder.getOrCreate(this);
        List<Item> bag = ch.getBagItems();
        Item match = findMatchingItem(bag, travelerRequiredRarityId, travelerRequiredType);

        if (match != null) {
            btnAction1 = addActionButton("帮助旅人", 0xFF4CAF50, v -> {
                InventoryManager.removeItem(bag, match);
                int goldReward = 50 + (travelerRequiredRarityId + 1) * 50
                        + (int) (Math.random() * ((travelerRequiredRarityId + 1) * 100 + 1));
                ch.addGold(goldReward);

                String resultText = "你慷慨地赠送了" + match.getName()
                        + "，旅人感激不尽！\n\n✅ 获得金币 ×" + goldReward;
                showResult(resultText);
                switchToForwardButton();
            });
            btnAction2 = addActionButton("无视旅人", 0xFF888888, v -> {
                showResult("你匆匆走过，没有理会旅人求助的目光。");
                switchToForwardButton();
            });
        } else {
            btnAction1 = addActionButton("很可惜，你无法帮助他", 0xFF888888, v -> {
                showResult("你的背包中没有" + travelerRequestDesc + "，旅人失望地离开了。");
                switchToForwardButton();
            });
        }
    }

    private Item findMatchingItem(List<Item> bag, int minRarityId, ItemType type) {
        for (Item item : bag) {
            if (item != null && item.getType() == type && item.getRarity().getId() >= minRarityId) {
                return item;
            }
        }
        return null;
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

    private void showGemUpgradeDialog() {
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        List<Item> bag = ch.getBagItems();

        final List<GemItem> gemItems = new ArrayList<>();
        for (Item item : bag) {
            if (item instanceof GemItem) {
                GemItem gem = (GemItem) item;
                if (gem.getRarity().getId() < 4) {
                    gemItems.add(gem);
                }
            }
        }
        if (gemItems.isEmpty()) {
            showResult("你的背包中没有可升级的宝石。\n（传说品质宝石已无法继续升级）");
            switchToForwardButton();
            return;
        }

        final Dialog gridDialog = new Dialog(this);
        gridDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View gridView = LayoutInflater.from(this).inflate(R.layout.dialog_gem_grid, null);
        RecyclerView rv = gridView.findViewById(R.id.rv_gem_grid);
        gridView.findViewById(R.id.btn_grid_close).setOnClickListener(v -> gridDialog.dismiss());

        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(new GemGridAdapter(gemItems, gem -> {
            gridDialog.dismiss();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                int currentRarityId = gem.getRarity().getId();
                Rarity nextRarity = Rarity.fromId(currentRarityId + 1);
                String upgradedGemId = gem.getGemType().toLowerCase() + "_"
                        + nextRarity.name().toLowerCase();

                GemItem upgraded = ItemManager.getInstance(NeutralEventActivity.this)
                        .createGem(upgradedGemId);
                if (upgraded == null) {
                    showResult("宝石升级失败：无法找到对应模板。");
                    switchToForwardButton();
                    return;
                }

                gem.setCount(gem.getCount() - 1);
                if (gem.getCount() <= 0) InventoryManager.removeItem(bag, gem);
                InventoryManager.addItem(bag, upgraded);

                String resultText = "雕像散发出耀眼的金色光芒...\n\n✅ "
                        + gem.getName() + "（" + gem.getRarity().getDisplayName()
                        + "）已升级为\n" + upgraded.getName() + "（"
                        + upgraded.getRarity().getDisplayName() + "）！";
                showResult(resultText);
                switchToForwardButton();
            });
        }));

        gridDialog.setContentView(gridView);
        gridDialog.setCancelable(true);
        gridDialog.setCanceledOnTouchOutside(true);
        gridDialog.show();
    }

    private void buildAltarActions() {
        llActionArea.removeAllViews();
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        List<Item> bag = ch.getBagItems();

        int eligibleCount = 0;
        for (Item item : bag) {
            if (item != null && item.getType() != ItemType.MATERIAL) eligibleCount += item.getCount();
        }

        if (eligibleCount >= 3) {
            boolean hasValidCombo = false;
            Rarity[] checkRarities = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE};
            for (Rarity r : checkRarities)
                if (countItemsByRarity(bag, r) >= 3) { hasValidCombo = true; break; }

            if (hasValidCombo) addActionButton("挑选献祭物品", 0xFFFF9800, v -> showAltarSacrificeDialog());
            else addActionButton("没有足够的同品质物品", 0xFF888888, v -> { showResult("祭坛需要3件相同品质的物品才能献祭。"); switchToForwardButton(); });
        } else {
            addActionButton("可献祭物品不足3件", 0xFF888888, v -> { showResult("你背包中可献祭的物品不足3件（材料不可献祭）。"); switchToForwardButton(); });
        }
        addActionButton("转身离开", 0xFF888888, v -> { showResult("你对祭坛默默祈祷，然后离开了。"); switchToForwardButton(); });
    }

    private int countItemsByRarity(List<Item> bag, Rarity rarity) {
        int count = 0;
        for (Item item : bag)
            if (item != null && item.getType() != ItemType.MATERIAL && item.getRarity() == rarity)
                count += item.getCount();
        return count;
    }

    private void showAltarSacrificeDialog() {
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        List<Item> bag = ch.getBagItems();
        List<Item> eligible = new ArrayList<>();
        for (Item item : bag) if (item != null && item.getType() != ItemType.MATERIAL) eligible.add(item);

        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_panel_treasure);
        int pad = dpToPx(12);
        root.setPadding(pad, pad, pad, pad);

        FrameLayout header = new FrameLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        header.setMinimumHeight(dpToPx(40));
        TextView tvTitle = new TextView(this);
        tvTitle.setText("挑选物品献祭（总数≤3件，材料不可选）");
        tvTitle.setTextColor(getResources().getColor(R.color.tb_gold_deep, null));
        tvTitle.setTextSize(16);
        tvTitle.setPadding(0, 0, dpToPx(36), 0);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2);
        tp.gravity = android.view.Gravity.CENTER_VERTICAL;
        header.addView(tvTitle, tp);
        TextView btnClose = new TextView(this);
        btnClose.setText("×");
        btnClose.setTextColor(getResources().getColor(R.color.tb_gold_deep, null));
        btnClose.setTextSize(24);
        btnClose.setGravity(android.view.Gravity.CENTER);
        TypedValue ov = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ov, true);
        btnClose.setBackgroundResource(ov.resourceId);
        btnClose.setWidth(dpToPx(36));
        btnClose.setHeight(dpToPx(36));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2);
        cp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        btnClose.setOnClickListener(v -> d.dismiss());
        header.addView(btnClose, cp);
        root.addView(header);

        TextView tvCounter = new TextView(this);
        tvCounter.setText(eligible.isEmpty() ? "⚠ 无可献祭物品" : "已选 0 / 3 — 请挑选≤3件同类型同品质的物品");
        tvCounter.setTextColor(eligible.isEmpty() ? 0xFFE53935 : 0xFFFF9800);
        tvCounter.setTextSize(14);
        tvCounter.setPadding(0, 0, 0, dpToPx(8));
        tvCounter.setGravity(android.view.Gravity.CENTER);
        root.addView(tvCounter);

        int[] sacrificeCounts = new int[eligible.size()];
        int[] totalCount = {0};

        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bb = new LinearLayout(this);
        bb.setOrientation(LinearLayout.HORIZONTAL);
        bb.setGravity(android.view.Gravity.CENTER);
        bb.setPadding(0, dpToPx(12), 0, 0);
        bb.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        Button btnConfirm = new Button(this);
        btnConfirm.setText("献祭物品");
        btnConfirm.setTextColor(0xFFFFFFFF);
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
        btnConfirm.setEnabled(false);
        btnConfirm.setTextSize(15);
        btnConfirm.setOnClickListener(v -> {
            int total = totalCount[0];
            if (total != 3) return;
            if (!isSacCountsValid(eligible, sacrificeCounts)) {
                tvCounter.setText("选中的物品品质不一致！");
                tvCounter.setTextColor(0xFFE53935);
                return;
            }
            Item first = null;
            for (int i = 0; i < sacrificeCounts.length; i++)
                if (sacrificeCounts[i] > 0) { first = eligible.get(i); break; }
            if (first == null) return;
            Rarity fromR = first.getRarity();
            Rarity toR = Rarity.fromId(fromR.getId() + 1);
            if (toR == null) {
                tvCounter.setText("已达到最高品质，无法升阶！");
                tvCounter.setTextColor(0xFFE53935);
                return;
            }
            d.dismiss();
            for (int i = 0; i < sacrificeCounts.length; i++) {
                int n = sacrificeCounts[i];
                if (n <= 0) continue;
                Item item = eligible.get(i);
                item.setCount(item.getCount() - n);
                if (item.getCount() <= 0) InventoryManager.removeItem(bag, item);
            }
            ItemType rewardType = pickRewardTypeByWeight(eligible, sacrificeCounts);
            Item reward = genReward(rewardType, toR);
            String rd = reward != null ? (reward.getName() + "（" + reward.getRarity().getDisplayName() + "）") : "什么都没有";
            if (reward != null) InventoryManager.addItem(bag, reward);
            showResult("祭坛散发出耀眼的光芒！\n✅ 献祭" + total + "件→获得：" + rd);
            switchToForwardButton();
        });
        btnConfirm.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        bb.addView(btnConfirm);

        Button btnCancel = new Button(this);
        btnCancel.setText("取消");
        btnCancel.setTextColor(0xFFFFFFFF);
        btnCancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF666666));
        btnCancel.setTextSize(15);
        LinearLayout.LayoutParams canP = new LinearLayout.LayoutParams(0, -2, 1);
        canP.setMargins(dpToPx(6), 0, 0, 0);
        btnCancel.setLayoutParams(canP);
        btnCancel.setOnClickListener(v -> d.dismiss());
        bb.addView(btnCancel);

        Button fBtnConfirm = btnConfirm;
        Runnable updateV = () -> {
            int t = totalCount[0];
            if (t == 0) {
                tvCounter.setText("已选 0 / 3 — 请挑选3件相同品质的物品");
                tvCounter.setTextColor(0xFFFF9800);
                fBtnConfirm.setEnabled(false);
                fBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
            } else if (!isSacCountsValid(eligible, sacrificeCounts)) {
                tvCounter.setText("已选 " + t + " / 3 ❌ 品质不一致");
                tvCounter.setTextColor(0xFFE53935);
                fBtnConfirm.setEnabled(false);
                fBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
            } else {
                Item fi = null;
                for (int i = 0; i < sacrificeCounts.length; i++)
                    if (sacrificeCounts[i] > 0) { fi = eligible.get(i); break; }
                Rarity toR = fi != null ? Rarity.fromId(fi.getRarity().getId() + 1) : null;
                if (toR == null) {
                    tvCounter.setText("已选 " + t + " / 3 ⚠ 已达最高品质，无法升阶");
                    tvCounter.setTextColor(0xFFE53935);
                    fBtnConfirm.setEnabled(false);
                    fBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
                } else if (t != 3) {
                    tvCounter.setText("已选 " + t + " / 3 ✅ " + fi.getRarity().getDisplayName() + " — 需要恰好3件");
                    tvCounter.setTextColor(0xFFFF9800);
                    fBtnConfirm.setEnabled(false);
                    fBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
                } else {
                    tvCounter.setText("已选 " + t + " / 3 ✅ " + fi.getRarity().getDisplayName() + " — 品质统一！");
                    tvCounter.setTextColor(0xFF4CAF50);
                    fBtnConfirm.setEnabled(true);
                    fBtnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800));
                }
            }
        };

        AltarSacrificeAdapter[] ar = {null};
        ar[0] = new AltarSacrificeAdapter(eligible, sacrificeCounts, totalCount, (idx, isStacked) -> {
            Item item = eligible.get(idx);
            int cur = sacrificeCounts[idx];
            if (cur > 0) {
                sacrificeCounts[idx] = 0;
                totalCount[0] -= cur;
                updateV.run();
                ar[0].notifyItemChanged(idx);
                return;
            }
            int remaining = 3 - totalCount[0];
            if (remaining <= 0) return;
            int maxN = item.getCount();
            if (!isStacked || maxN <= 1) {
                sacrificeCounts[idx] = 1;
                totalCount[0]++;
                updateV.run();
                ar[0].notifyItemChanged(idx);
                return;
            }
            int n = Math.min(remaining, maxN);
            if (n == 1) {
                sacrificeCounts[idx] = 1;
                totalCount[0]++;
                updateV.run();
                ar[0].notifyItemChanged(idx);
                return;
            }
            String[] opts = new String[n];
            for (int k = 0; k < n; k++) opts[k] = String.valueOf(k + 1);
            new android.app.AlertDialog.Builder(NeutralEventActivity.this)
                    .setTitle("选择献祭数量（最多" + n + "件）")
                    .setItems(opts, (dia, which) -> {
                        sacrificeCounts[idx] = which + 1;
                        totalCount[0] += which + 1;
                        updateV.run();
                        ar[0].notifyItemChanged(idx);
                    })
                    .show();
        });
        rv.setAdapter(ar[0]);
        root.addView(rv);
        root.addView(bb);
        d.setContentView(root);
        d.setCancelable(true);
        d.show();
    }

    private boolean isSacCountsValid(List<Item> items, int[] counts) {
        Item first = null;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] <= 0) continue;
            if (first == null) { first = items.get(i); continue; }
            Item t = items.get(i);
            if (t.getRarity() != first.getRarity()) return false;
        }
        return first != null;
    }

    private String nameForType(ItemType t) { return t == ItemType.EQUIPMENT ? "装备" : t == ItemType.CONSUMABLE ? "药水" : t == ItemType.GEM ? "宝石" : "物品"; }

    private ItemType pickRewardTypeByWeight(List<Item> items, int[] counts) {
        int equipCount = 0, consumableCount = 0, gemCount = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] <= 0) continue;
            Item item = items.get(i);
            if (item.getType() == ItemType.EQUIPMENT) equipCount += counts[i];
            else if (item.getType() == ItemType.CONSUMABLE) consumableCount += counts[i];
            else if (item.getType() == ItemType.GEM) gemCount += counts[i];
        }
        int totalWeight = equipCount + consumableCount + gemCount;
        if (totalWeight == 0) return ItemType.EQUIPMENT;
        int roll = new Random().nextInt(totalWeight);
        if (roll < equipCount) return ItemType.EQUIPMENT;
        if (roll < equipCount + consumableCount) return ItemType.CONSUMABLE;
        return ItemType.GEM;
    }

    private Item genReward(ItemType type, Rarity r) {
        if (type == ItemType.EQUIPMENT) return EquipmentManager.getInstance(this).generateRandomEquip(5 + r.getId() * 8, r);
        if (type == ItemType.CONSUMABLE) return ItemManager.getInstance(this).getRandomConsumableByRarity(r);
        return ItemManager.getInstance(this).getRandomGemByRarity(r);
    }

    private void buildPhantomMazeActions() {
        llActionArea.removeAllViews();
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        int str = ch.getAllocatedStrength(), agi = ch.getAllocatedAgility(), inte = ch.getAllocatedIntelligence();
        int[][] ranked = {{str, 0}, {agi, 1}, {inte, 2}};
        java.util.Arrays.sort(ranked, (a, b) -> Integer.compare(b[0], a[0]));
        String[] titles = {"角斗场（力量）", "密林迷宫（敏捷）", "元素试炼（智力）"};
        for (int i = 0; i < 2; i++) {
            int attrIdx = ranked[i][1];
            int[] vals = {str, agi, inte};
            int color = attrIdx == 0 ? 0xFFE53935 : attrIdx == 1 ? 0xFF4CAF50 : 0xFF2196F3;
            String label = titles[attrIdx] + "（当前" + vals[attrIdx] + "）";
            int ai = attrIdx;
            addActionButton(label, color, v -> {
                if (ai == 0) { Intent r = new Intent(); r.putExtra("open_battle", true); setResult(RESULT_OK, r); finish(); }
                else if (ai == 1) {
                    int g = 300 + (int)(Math.random() * 701); ch.addGold(g);
                    GemItem gm = ItemManager.getInstance(this).getRandomGemByRarity(Math.random() < 0.6 ? Rarity.UNCOMMON : Rarity.RARE);
                    if (gm != null) InventoryManager.addItem(ch.getBagItems(), gm);
                    showResult("密林迷宫：灵活穿梭，发现宝箱！\n✅ 金币+" + g + "\n✅ 获得：" + (gm != null ? gm.getName() : "宝石"));
                    switchToForwardButton();
                } else {
                    int pts = 2 + (int)(Math.random() * 3); ch.addSkillPoints(pts);
                    ConsumableItem pt = ItemManager.getInstance(this).getRandomConsumableByRarity(Rarity.UNCOMMON);
                    if (pt != null) InventoryManager.addItem(ch.getBagItems(), pt);
                    showResult("元素试炼：符文亮起，破解成功！\n✅ 技能点+" + pts + "\n✅ 获得：" + (pt != null ? pt.getName() : "药水"));
                    switchToForwardButton();
                }
            });
        }
        addActionButton("离开迷宫", 0xFF888888, v -> { showResult("雾气散去，迷宫入口消失了。"); switchToForwardButton(); });
    }

    private void buildWishingWellActions() {
        llActionArea.removeAllViews();
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        int coins = WISHING_AMOUNTS[Math.min(wishingRound, WISHING_AMOUNTS.length - 1)];

        if (lastWishResult != null && !lastWishResult.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(lastWishResult);
            tv.setTextColor(0xFF333333);
            tv.setTextSize(14);
            tv.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            tv.setBackgroundResource(R.drawable.bg_event_popup);
            llActionArea.addView(tv);
            lastWishResult = null;
        }

        if (ch.getGold() < coins) {
            addActionButton("金币不足" + coins + "，无法继续许愿", 0xFF888888, v -> {});
            addActionButton("离开古井", 0xFF888888, v -> { showResult("许愿之旅到此结束。\n（当前金币：" + ch.getGold() + "）"); switchToForwardButton(); });
        } else {
            addActionButton("投入" + coins + "金币许愿（第" + (wishingRound + 1) + "次）", 0xFF448AFF, v -> {
                if (!ch.spendGold(coins)) return;
                lastWishResult = rollWish(ch, coins);
                wishingRound++;
                buildWishingWellActions();
            });
            addActionButton("离开古井", 0xFF888888, v -> { showResult("你对着古井默默祈祷。\n（当前金币：" + ch.getGold() + "）"); switchToForwardButton(); });
        }
    }

    private String rollWish(Character ch, int n) {
        double cp = Math.min(n * 0.005, 0.70), up = Math.min(n * 0.0001, 0.04), rp = Math.min(n * 0.0001, 0.04);
        double ep = Math.min(n * 0.000005, 0.002), lp = Math.min(n * 0.000005, 0.002);
        double roll = Math.random();
        Rarity rr = null;
        double cum = cp; if (roll < cum) rr = Rarity.COMMON;
        cum += up; if (rr == null && roll < cum) rr = Rarity.UNCOMMON;
        cum += rp; if (rr == null && roll < cum) rr = Rarity.RARE;
        cum += ep; if (rr == null && roll < cum) rr = Rarity.EPIC;
        cum += lp; if (rr == null && roll < cum) rr = Rarity.LEGENDARY;
        if (rr == null) return "水面泛起涟漪，什么都没发生...\n（当前金币：" + ch.getGold() + "）";
        int lv = 5 + wishingRound * 3 + (int)(Math.random() * 11);
        Item reward;
        int tr = (int)(Math.random() * 3);
        if (tr == 0) reward = EquipmentManager.getInstance(this).generateRandomEquip(lv, rr);
        else if (tr == 1) reward = ItemManager.getInstance(this).getRandomConsumableByRarity(rr);
        else reward = ItemManager.getInstance(this).getRandomGemByRarity(rr);
        if (reward == null) reward = EquipmentManager.getInstance(this).generateRandomEquip(lv, rr);
        if (reward == null) return "水面泛起涟漪，什么都没发生...\n（当前金币：" + ch.getGold() + "）";
        InventoryManager.addItem(ch.getBagItems(), reward);
        return "古井涌出" + rr.getDisplayName() + "光芒！\n✅ 获得：" + reward.getName() + "（" + rr.getDisplayName() + "）\n（当前金币：" + ch.getGold() + "）";
    }

    private void buildCursedChestActions() {
        llActionArea.removeAllViews();
        addActionButton("打开宝箱", 0xFF9C27B0, v -> {
            Monster monster = MonsterManager.getInstance(this).createRandomMonster();
            EventManager.getInstance(getApplicationContext()).setCurrentBattleMonster(monster);
            EventManager.getInstance(getApplicationContext()).setCurrentBattleSurprise(BattleContext.SurpriseDirection.MONSTER_SURPRISE);
            showResult("紫黑色雾气喷涌而出！\n\n" + monster.getName() + "从暗处扑来，怪物获得了先手攻击！");
            if (btnContinue != null) {
                btnContinue.setText("被迫进入战斗");
                btnContinue.setBackgroundColor(0xFFE53935);
                btnContinue.setOnClickListener(v2 -> { Intent r = new Intent(); r.putExtra("open_battle", true); setResult(RESULT_OK, r); finish(); });
            }
        });
        addActionButton("就此离开", 0xFF888888, v -> { showResult("你绕过了散发不祥气息的宝箱。"); switchToForwardButton(); });
    }

    private void showReforgeDialog(EquipItem equip) {
        final EquipItem targetEquip = equip;
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_equip_reforge, null);
        d.setContentView(content);
        Window w = d.getWindow();
        if (w != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());
            lp.width = -1;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.66);
            w.setAttributes(lp);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
        ImageView ivPreview = content.findViewById(R.id.iv_equip_preview);
        TextView tvName = content.findViewById(R.id.tv_equip_name);
        TextView tvRarity = content.findViewById(R.id.tv_equip_rarity);
        TextView tvLevel = content.findViewById(R.id.tv_equip_level);
        LinearLayout llAffixList = content.findViewById(R.id.ll_affix_list);
        ScrollView svAffix = content.findViewById(R.id.sv_affix_container);
        Button btnAction = content.findViewById(R.id.btn_reforge_action);
        Button btnCancel = content.findViewById(R.id.btn_reforge_cancel);
        boolean[] hasReforged = {false};
        int[] selIdx = {-1};
        GameAssetIcons.bindItem(this, ivPreview, targetEquip);
        tvName.setText(targetEquip.getName());
        tvName.setTextSize(18);
        tvRarity.setText(targetEquip.getRarity().name());
        tvRarity.setTextColor(targetEquip.getRarity().getColor());
        tvLevel.setText("Lv." + targetEquip.getLevel());
        tvLevel.setTextSize(14);
        btnAction.setText("请选择要重炼的词条");
        btnAction.setEnabled(false);
        btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
        final RefineClick[] cbRef = {null};
        cbRef[0] = idx -> { selIdx[0] = idx; btnAction.setEnabled(true); btnAction.setText("重炼此词条"); btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF9C27B0)); updateRefineList(llAffixList, targetEquip.getAffixes(), selIdx, hasReforged, cbRef[0]); };
        updateRefineList(llAffixList, targetEquip.getAffixes(), selIdx, hasReforged, cbRef[0]);
        btnAction.setOnClickListener(v -> {
            if (hasReforged[0]) { d.dismiss(); showResult("装备重炼完成！"); switchToForwardButton(); return; }
            int idx = selIdx[0];
            if (idx < 0 || idx >= targetEquip.getAffixes().size()) return;
            BaseEquipAffix newAffix = EquipAffixManager.getInstance(this).generateSingleAffixForEquipment(targetEquip);
            if (newAffix != null) targetEquip.getAffixes().set(idx, newAffix);
            updateRefineList(llAffixList, targetEquip.getAffixes(), selIdx, hasReforged, null);
            svAffix.fullScroll(View.FOCUS_DOWN);
            btnAction.setText("确定");
            btnAction.setEnabled(true);
            btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            btnCancel.setVisibility(View.GONE);
            LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) btnAction.getLayoutParams();
            lp2.setMargins(0, lp2.topMargin, 0, lp2.bottomMargin);
            btnAction.setLayoutParams(lp2);
            hasReforged[0] = true;
        });
        btnCancel.setOnClickListener(v -> { if (!hasReforged[0]) d.dismiss(); });
        d.show();
    }

    private interface RefineClick { void onClick(int index); }

    private void updateRefineList(LinearLayout container, List<? extends BaseAffix> affixes,
            int[] selIdx, boolean[] hasReforged, RefineClick cb) {
        container.removeAllViews();
        int padH = dpToPx(4), padV = dpToPx(10);
        if (affixes == null || affixes.isEmpty()) {
            TextView tv = new TextView(this); tv.setText("(无词条)"); tv.setTextSize(13); tv.setTextColor(0xFF888888);
            tv.setPadding(padH, padV, padH, padV); container.addView(tv); return;
        }
        boolean done = hasReforged[0];
        int sel = selIdx[0];
        String[] colors = {"#E53935", "#FF9800", "#FDD835", "#4CAF50", "#2196F3"};
        for (int i = 0; i < affixes.size(); i++) {
            int idx = i;
            BaseAffix a = affixes.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(padH, padV, padH, padV);
            row.setBackgroundColor(idx == sel ? (done ? 0x334CAF50 : 0x33FFC107) : 0x0AFFFFFF);
            TextView dot = new TextView(this);
            dot.setText(idx == sel && done ? "★" : "●");
            dot.setTextColor(android.graphics.Color.parseColor(colors[i % colors.length]));
            dot.setTextSize(10);
            row.addView(dot);
            TextView line = new TextView(this);
            line.setText((a != null ? a.getAffixName() : "???") + "：" + (a != null ? a.getDescription() : "..."));
            line.setTextSize(14);
            line.setTextColor(0xFFDDDDDD);
            line.setPadding(dpToPx(8), 0, 0, 0);
            row.addView(line);
            if (!done && cb != null) row.setOnClickListener(v -> cb.onClick(idx));
            container.addView(row);
        }
    }

    static class EquipGridAdapter extends RecyclerView.Adapter<EquipGridAdapter.VH> {
        final List<EquipItem> items;
        final OnEquipClickListener listener;

        interface OnEquipClickListener { void onClick(EquipItem item); }

        EquipGridAdapter(List<EquipItem> items, OnEquipClickListener l) { this.items = items; this.listener = l; }

        @Override
        public VH onCreateViewHolder(ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_equip_select, p, false);
            int sz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, p.getContext().getResources().getDisplayMetrics());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, sz);
            int sp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, p.getContext().getResources().getDisplayMetrics());
            lp.setMargins(sp, sp, sp, sp);
            v.setLayoutParams(lp);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int pos) {
            EquipItem equip = items.get(pos);
            holder.tvName.setText(equip.getName());
            holder.tvName.setTextSize(14);
            holder.tvLevel.setText("Lv." + equip.getLevel());
            holder.tvLevel.setTextSize(12);
            GameAssetIcons.bindItem(holder.itemView.getContext(), holder.ivIcon, equip);
            holder.bgColor.setBackgroundTintList(null);
            android.graphics.drawable.Drawable bg = holder.bgColor.getBackground();
            if (bg != null) bg.clearColorFilter();
            holder.itemView.setForeground(TreasureStyleDrawable.newSlotStrokeOverlay(holder.itemView.getContext(), equip.getRarity() != null ? equip.getRarity().getColor() : null));
            holder.itemView.setOnClickListener(v -> listener.onClick(equip));
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            View bgColor; TextView tvName, tvLevel; ImageView ivIcon;
            VH(View v) { super(v); bgColor = v.findViewById(R.id.bg_item_color); tvName = v.findViewById(R.id.tv_item_name); tvLevel = v.findViewById(R.id.tv_item_level); ivIcon = v.findViewById(R.id.iv_item_icon); }
        }
    }

    private static class GemGridAdapter extends RecyclerView.Adapter<GemGridAdapter.VH> {
        private final List<GemItem> items;
        private final OnGemClickListener listener;

        interface OnGemClickListener {
            void onClick(GemItem item);
        }

        GemGridAdapter(List<GemItem> items, OnGemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gem_select, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            GemItem gem = items.get(position);
            holder.tvRarity.setText(gem.getRarity().getDisplayName());
            holder.tvRarity.setTextColor(gem.getRarity().getColor());
            GameAssetIcons.bindItem(holder.itemView.getContext(), holder.ivIcon, gem);
            int c = gem.getCount();
            if (c > 1) {
                holder.tvCount.setVisibility(View.VISIBLE);
                holder.tvCount.setText("×" + c);
            } else {
                holder.tvCount.setVisibility(View.GONE);
            }
            holder.bgColor.setBackgroundTintList(null);
            android.graphics.drawable.Drawable bg = holder.bgColor.getBackground();
            if (bg != null) {
                bg.clearColorFilter();
            }
            Integer borderArgb = gem.getRarity() != null ? gem.getRarity().getColor() : null;
            holder.itemView.setForeground(
                    TreasureStyleDrawable.newSlotStrokeOverlay(holder.itemView.getContext(), borderArgb));
            holder.itemView.setOnClickListener(v -> listener.onClick(gem));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            View bgColor;
            TextView tvRarity, tvCount;
            ImageView ivIcon;

            VH(View v) {
                super(v);
                bgColor = v.findViewById(R.id.bg_item_color);
                tvRarity = v.findViewById(R.id.tv_item_rarity);
                tvCount = v.findViewById(R.id.tv_bag_stack_count);
                ivIcon = v.findViewById(R.id.iv_item_icon);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private static class AltarSacrificeAdapter extends RecyclerView.Adapter<AltarSacrificeAdapter.VH> {
        final List<Item> items;
        final int[] counts;
        final int[] totalCount;
        final OnSacrificeListener listener;

        interface OnSacrificeListener { void onToggle(int index, boolean isStacked); }

        AltarSacrificeAdapter(List<Item> items, int[] counts, int[] totalCount, OnSacrificeListener l) {
            this.items = items; this.counts = counts; this.totalCount = totalCount; this.listener = l;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_equip_select, p, false);
            int sz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, p.getContext().getResources().getDisplayMetrics());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, sz);
            int sp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, p.getContext().getResources().getDisplayMetrics());
            lp.setMargins(sp, sp, sp, sp);
            v.setLayoutParams(lp);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int pos) {
            Item item = items.get(pos);
            GameAssetIcons.bindItem(holder.itemView.getContext(), holder.ivIcon, item);
            if (item instanceof EquipItem) {
                holder.tvLevel.setVisibility(View.VISIBLE);
                holder.tvLevel.setText("Lv." + ((EquipItem) item).getLevel());
            } else {
                holder.tvLevel.setVisibility(View.GONE);
            }
            holder.tvName.setVisibility(View.VISIBLE);
            holder.tvName.setText(item.getName());
            holder.tvName.setTextSize(10);
            int stackCount = item.getCount();
            int sel = counts[pos];
            if (stackCount > 1) {
                holder.tvCount.setVisibility(View.VISIBLE);
                holder.tvCount.setText("×" + stackCount);
            } else {
                holder.tvCount.setVisibility(View.GONE);
            }
            holder.itemView.setForeground(TreasureStyleDrawable.newSlotStrokeOverlay(
                    holder.itemView.getContext(), item.getRarity() != null ? item.getRarity().getColor() : null));
            holder.selectOverlay.setVisibility(sel > 0 ? View.VISIBLE : View.GONE);
            if (sel > 0) {
                holder.selBadge.setVisibility(View.VISIBLE);
                holder.selBadge.setText(String.valueOf(sel));
            } else {
                holder.selBadge.setVisibility(View.GONE);
            }
            boolean isStacked = stackCount > 1;
            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onToggle(pos, isStacked); });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivIcon; TextView tvLevel, tvName, tvCount, selBadge; View selectOverlay;
            VH(View v) {
                super(v);
                ivIcon = v.findViewById(R.id.iv_item_icon);
                tvLevel = v.findViewById(R.id.tv_item_level);
                tvName = v.findViewById(R.id.tv_item_name);
                tvName.setVisibility(View.GONE);
                tvName.setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                ((RelativeLayout.LayoutParams) tvName.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                tvCount = v.findViewById(R.id.tv_bag_stack_count);
                selectOverlay = new View(v.getContext());
                selectOverlay.setBackgroundColor(0x66FFC107);
                selectOverlay.setVisibility(View.GONE);
                ((FrameLayout) v).addView(selectOverlay, new FrameLayout.LayoutParams(-1, -1));
                selBadge = new TextView(v.getContext());
                selBadge.setTextColor(0xFFFFFFFF);
                selBadge.setTextSize(12);
                selBadge.setTypeface(v.getContext().getResources().getFont(R.font.zpix));
                selBadge.setGravity(android.view.Gravity.CENTER);
                selBadge.setBackgroundColor(0xFFE53935);
                int sz = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, v.getContext().getResources().getDisplayMetrics());
                FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(sz, sz);
                bp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                selBadge.setLayoutParams(bp);
                selBadge.setVisibility(View.GONE);
                ((FrameLayout) v).addView(selBadge);
            }
        }
    }
}
