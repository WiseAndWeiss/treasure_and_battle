package com.example.treasure_and_battle.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.treasure_and_battle.R;

public class BenefitEventActivity extends AppCompatActivity {

    private LinearLayout llChoiceArea;
    private LinearLayout llResultArea;
    private TextView tvResult;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benefit_hub);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        llChoiceArea = findViewById(R.id.ll_choice_area);
        llResultArea = findViewById(R.id.ll_result_area);
        tvResult = findViewById(R.id.tv_result);
        btnContinue = findViewById(R.id.btn_continue);

        findViewById(R.id.card_rest).setOnClickListener(v -> {
            showResult("你选择休息，回复了80点生命值！\n\n✅ 生命值 +80");
            switchToForwardButton();
        });

        findViewById(R.id.card_training).setOnClickListener(v -> {
            showResult("你进行了锻炼，感觉力量增强了！\n\n✅ 力量 +5");
            switchToForwardButton();
        });

        findViewById(R.id.card_treasure).setOnClickListener(v -> {
            showResult("你发现了隐藏宝藏！\n\n✅ 获得金币 ×300~800\n✅ 获得随机装备 ×1\n✅ 获得随机材料 ×3\n✅ 获得随机宝石 ×1");
            switchToForwardButton();
        });
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
