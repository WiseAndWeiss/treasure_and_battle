package com.example.treasure_and_battle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.treasure_and_battle.R;

public class NeutralEventActivity extends AppCompatActivity {

    private String eventKey;
    private LinearLayout llActionArea;
    private LinearLayout llResultArea;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neutral_event);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_continue).setOnClickListener(v -> finish());

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
                addActionButton("进入商店交易", 0xFF2196F3, v -> {
                    startActivity(new Intent(this, TradeActivity.class));
                    finish();
                });
                addActionButton("拒绝交易", 0xFF888888, v -> {
                    showResult("你拒绝了商人的交易邀请。");
                });
                break;

            case "exploration":
                addActionButton("深入探险", 0xFFE53935, v -> {
                    showResult("你鼓起勇气深入洞穴...\n（探险系统后续开发）");
                });
                addActionButton("谨慎离开", 0xFF888888, v -> {
                    showResult("你选择了安全离开，放弃了可能存在的宝藏。");
                });
                break;

            case "traveler":
                addActionButton("帮助旅人", 0xFF4CAF50, v -> {
                    showResult("你帮助了迷路的旅人！\n\n✅ 获得补给品 ×3\n✅ 获得金币 ×200\n✅ 幸运值提升，持续1小时");
                });
                addActionButton("无视旅人", 0xFF888888, v -> {
                    showResult("你匆匆走过，没有理会旅人求助的目光。");
                });
                break;

            case "scholar":
                addActionButton("洗点重置（消耗500金币）", 0xFFFF9800, v -> {
                    showResult("你消耗了500金币，天赋点和技能点已重置！");
                });
                addActionButton("离开", 0xFF888888, v -> {
                    showResult("你离开了学者，保持现有技能配置。");
                });
                break;

            case "statue_blessing":
                addActionButton("接受雕像祝福", 0xFFFFC107, v -> {
                    showResult("雕像散发金色光芒...\n\n✅ 下3场战斗开始时获得随机Buff：\n  · 攻击力 +10%\n  · 防御力 +10%\n  · 最大生命 +15%");
                });
                addActionButton("绕道离开", 0xFF888888, v -> {
                    showResult("你绕过了雕像，没有接受祝福。");
                });
                break;

            case "monster_camp":
                addActionButton("偷袭怪物", 0xFFE53935, v -> {
                    showResult("你悄悄靠近并发动偷袭！必定先手攻击！\n（战斗系统后续开发）");
                });
                addActionButton("悄悄离开", 0xFF888888, v -> {
                    showResult("你屏住呼吸，悄悄绕过了正在休息的怪物。");
                });
                break;

            case "cave_treasure":
                addActionButton("开启宝箱", 0xFFFF9800, v -> {
                    int roll = (int) (Math.random() * 100);
                    if (roll < 30) {
                        showResult("打开宝箱的瞬间，一只怪物从背后偷袭！\n⚔️ 进入战斗（战斗系统后续开发）");
                    } else {
                        showResult("宝箱顺利打开！\n\n✅ 获得金币 ×500\n✅ 获得随机装备 ×1\n✅ 获得随机宝石 ×1");
                    }
                });
                addActionButton("放弃宝箱", 0xFF888888, v -> {
                    showResult("你选择了谨慎行事，放弃了眼前的宝箱。");
                });
                break;

            case "equipment_reforge":
                addActionButton("选择装备重炼", 0xFF9C27B0, v -> {
                    showResult("铁匠大师为你重炼了装备词条...\n（装备重炼系统后续开发）");
                });
                addActionButton("暂时不需要", 0xFF888888, v -> {
                    showResult("你婉拒了铁匠大师的好意。");
                });
                break;
        }
    }

    private void addActionButton(String text, int bgColor, View.OnClickListener listener) {
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
    }

    private void showResult(String text) {
        llActionArea.setVisibility(View.GONE);
        tvResult.setText(text);
        llResultArea.setVisibility(View.VISIBLE);
    }
}
