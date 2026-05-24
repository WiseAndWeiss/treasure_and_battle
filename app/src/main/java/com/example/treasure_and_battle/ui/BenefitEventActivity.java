package com.example.treasure_and_battle.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

import java.util.List;

public class BenefitEventActivity extends AppCompatActivity {

    private LinearLayout llChoiceArea;
    private LinearLayout llResultArea;
    private TextView tvResult;
    private Button btnContinue;
    private TextView tvToolbarTitle;
    private String eventKey;
    private String chestName;
    private String keyId;
    private Rarity chestRarity;
    private int goldMin, goldMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benefit_hub);

        eventKey = getIntent().getStringExtra("event_key");
        if (eventKey == null) eventKey = "rest";

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        llChoiceArea = findViewById(R.id.ll_choice_area);
        llResultArea = findViewById(R.id.ll_result_area);
        tvResult = findViewById(R.id.tv_result);
        btnContinue = findViewById(R.id.btn_continue);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);

        View cardRest = findViewById(R.id.card_rest);
        View cardChest = findViewById(R.id.card_chest);

        if ("chest".equals(eventKey)) {
            tvToolbarTitle.setText("营地宝箱");
            cardRest.setVisibility(View.GONE);
            rollChestType();
            TextView tvChestTitle = findViewById(R.id.tv_chest_title);
            tvChestTitle.setText("📦 " + chestName);
            TextView tvChestDesc = findViewById(R.id.tv_chest_desc);
            tvChestDesc.setText("营地角落里有一只" + chestName + "！需要" + getKeyName(keyId) + "才能开启。");
            cardChest.setOnClickListener(v -> doChest());
        } else {
            tvToolbarTitle.setText("安全营地");
            cardChest.setVisibility(View.GONE);
            cardRest.setOnClickListener(v -> doRest());
        }
    }

    private void rollChestType() {
        double roll = Math.random();
        if (roll < 0.10) {
            chestName = "金宝箱";
            keyId = "key_gold";
            chestRarity = Rarity.EPIC;
            goldMin = 1000;
            goldMax = 2000;
        } else if (roll < 0.30) {
            chestName = "银宝箱";
            keyId = "key_silver";
            chestRarity = Rarity.RARE;
            goldMin = 500;
            goldMax = 1000;
        } else {
            chestName = "铜宝箱";
            keyId = "key_copper";
            chestRarity = Rarity.UNCOMMON;
            goldMin = 200;
            goldMax = 500;
        }
    }

    private void doRest() {
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        int heal = (int) (ch.getBaseMaxHp() * 0.3);
        int newHp = Math.min(ch.getCurrentHp() + heal, ch.getBaseMaxHp());
        ch.setCurrentHp(newHp);
        showResult("你靠在篝火旁休息，伤势恢复了。\n\n✅ 生命值 +" + heal + "（当前：" + ch.getCurrentHp() + "/" + ch.getBaseMaxHp() + "）");
        switchToForwardButton();
    }

    private void doChest() {
        Character ch = PlayerCharacterHolder.getOrCreate(this);
        List<Item> bag = ch.getBagItems();

        ConsumableItem keyItem = findConsumableById(bag, keyId);
        if (keyItem == null) {
            showResult("营地中有一只" + chestName + "！\n\n❌ 你没有" + getKeyName(keyId) + "，无法打开宝箱。");
            switchToForwardButton();
            return;
        }

        keyItem.setCount(keyItem.getCount() - 1);
        if (keyItem.getCount() <= 0) {
            InventoryManager.removeItem(bag, keyItem);
        }

        EquipmentManager em = EquipmentManager.getInstance(this);
        ItemManager im = ItemManager.getInstance(this);
        int level = 5 + chestRarity.getId() * 5 + (int) (Math.random() * 11);
        EquipItem equip = em.generateRandomEquip(level, chestRarity);
        String equipName = equip != null ? equip.getName() + "（" + equip.getRarity().getDisplayName() + "）" : "一件装备";
        if (equip != null) InventoryManager.addItem(bag, equip);

        int gold = goldMin + (int) (Math.random() * (goldMax - goldMin + 1));
        ch.addGold(gold);

        StringBuilder sb = new StringBuilder();
        sb.append("营地中有一只").append(chestName).append("！\n");
        sb.append("你使用").append(getKeyName(keyId)).append("打开了宝箱！\n\n");
        sb.append("✅ 获得：").append(equipName).append("\n");
        sb.append("✅ 金币 +").append(gold).append("\n");

        GemItem gem = im.getRandomGemByRarity(chestRarity == Rarity.EPIC ? Rarity.RARE : Rarity.UNCOMMON);
        if (gem != null) {
            InventoryManager.addItem(bag, gem);
            sb.append("✅ 获得：").append(gem.getName()).append("（").append(gem.getRarity().getDisplayName()).append("）");
        }

        showResult(sb.toString());
        switchToForwardButton();
    }

    private ConsumableItem findConsumableById(List<Item> bag, String id) {
        for (Item item : bag) {
            if (item instanceof ConsumableItem && id.equals(item.getId())) {
                return (ConsumableItem) item;
            }
        }
        return null;
    }

    private String getKeyName(String keyId) {
        if ("key_gold".equals(keyId)) return "金钥匙";
        if ("key_silver".equals(keyId)) return "银钥匙";
        return "铜钥匙";
    }

    private void showResult(String text) {
        llChoiceArea.setVisibility(View.GONE);
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
}
