package com.example.treasure_and_battle.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.manager.EventManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;

public class BattleActivity extends Activity {

    private Monster mMonster;

    private TextView tvMonsterName;
    private TextView tvMonsterLevel;
    private TextView tvMonsterRarity;
    private TextView tvHp;
    private TextView tvAtk;
    private TextView tvDef;
    private TextView tvSpd;
    private TextView tvStrength;
    private TextView tvAgility;
    private TextView tvIntelligence;
    private TextView tvSpirit;
    private TextView tvPhysique;
    private TextView tvLuck;
    private TextView tvExpReward;
    private TextView tvGoldReward;
    private Button btnBattle;
    private TextView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_battle);

        mMonster = EventManager.getInstance(getApplicationContext()).getCurrentBattleMonster();

        bindViews();
        populateMonsterData();
        setupListeners();
    }

    private void bindViews() {
        tvMonsterName = findViewById(R.id.tv_monster_name);
        tvMonsterLevel = findViewById(R.id.tv_monster_level);
        tvMonsterRarity = findViewById(R.id.tv_monster_rarity);
        tvHp = findViewById(R.id.tv_hp);
        tvAtk = findViewById(R.id.tv_atk);
        tvDef = findViewById(R.id.tv_def);
        tvSpd = findViewById(R.id.tv_spd);
        tvStrength = findViewById(R.id.tv_strength);
        tvAgility = findViewById(R.id.tv_agility);
        tvIntelligence = findViewById(R.id.tv_intelligence);
        tvSpirit = findViewById(R.id.tv_spirit);
        tvPhysique = findViewById(R.id.tv_physique);
        tvLuck = findViewById(R.id.tv_luck);
        tvExpReward = findViewById(R.id.tv_exp_reward);
        tvGoldReward = findViewById(R.id.tv_gold_reward);
        btnBattle = findViewById(R.id.btn_battle);
        btnBack = findViewById(R.id.btn_back);
    }

    private void populateMonsterData() {
        if (mMonster == null) {
            tvMonsterName.setText("未知怪物");
            tvMonsterLevel.setText("Lv.?");
            tvMonsterRarity.setText("未知");
            tvMonsterRarity.setBackgroundColor(0xFF888888);
            Toast.makeText(this, "未找到怪物数据", Toast.LENGTH_SHORT).show();
            return;
        }

        tvMonsterName.setText(mMonster.getName());
        tvMonsterLevel.setText("Lv." + mMonster.getLevel());

        Rarity rarity = mMonster.getRarity();
        tvMonsterRarity.setText(rarity.getDisplayName());
        tvMonsterRarity.setBackgroundColor(rarity.getColor());

        AttributeSet attrs = mMonster.getFinalAttributes();

        tvHp.setText(String.valueOf(attrs.maxHp));
        tvAtk.setText(String.valueOf(attrs.physicalAtk));
        tvDef.setText(String.valueOf(attrs.physicalDef));
        tvSpd.setText(String.valueOf(attrs.speed));

        tvStrength.setText(String.valueOf(attrs.strength));
        tvAgility.setText(String.valueOf(attrs.agility));
        tvIntelligence.setText(String.valueOf(attrs.intelligence));
        tvSpirit.setText(String.valueOf(attrs.spirit));
        tvPhysique.setText(String.valueOf(attrs.physique));
        tvLuck.setText(String.valueOf(attrs.luck));

        tvExpReward.setText(String.valueOf(mMonster.getExpReward()));
        tvGoldReward.setText(String.valueOf(mMonster.getGoldReward()));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnBattle.setOnClickListener(v -> {
            if (mMonster == null) {
                Toast.makeText(this, "无法进入战斗：怪物数据缺失", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "战斗系统开发中...", Toast.LENGTH_SHORT).show();
        });
    }
}
