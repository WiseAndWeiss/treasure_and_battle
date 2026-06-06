package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.handler.BaseEventHandler;
import com.example.treasure_and_battle.event.handler.CampRestHandler;
import com.example.treasure_and_battle.event.handler.CasinoHandler;
import com.example.treasure_and_battle.event.handler.CaveTreasureHandler;
import com.example.treasure_and_battle.event.handler.ChestHandler;
import com.example.treasure_and_battle.event.handler.CursedChestHandler;
import com.example.treasure_and_battle.event.handler.DivinationHandler;
import com.example.treasure_and_battle.event.handler.EquipmentReforgeHandler;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.event.handler.MonsterCampHandler;
import com.example.treasure_and_battle.event.handler.MysteriousAltarHandler;
import com.example.treasure_and_battle.event.handler.MysteryBoxHandler;
import com.example.treasure_and_battle.event.handler.ScholarHandler;
import com.example.treasure_and_battle.event.handler.StatueBlessingHandler;
import com.example.treasure_and_battle.event.handler.TravelerHandler;
import com.example.treasure_and_battle.event.handler.WishingWellHandler;

import java.io.InputStream;

public class NeutralEventActivity extends AppCompatActivity {

    private String eventKey;
    private LinearLayout llActionArea;
    private LinearLayout llResultArea;
    private TextView tvResult;
    private TextView btnContinue;
    private TextView btnAction1;
    private TextView btnAction2;

    private BaseEventHandler currentHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neutral_event);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentHandler != null) {
            currentHandler.dispose();
            currentHandler = null;
        }
    }

    private static final java.util.Set<String> MERCHANT_KEYS = new java.util.HashSet<>(
            java.util.Arrays.asList("wandering_vendor", "equipment_merchant", "caravan", "material_merchant"));
    private static final java.util.Set<String> BENEFIT_KEYS = new java.util.HashSet<>(
            java.util.Arrays.asList("rest", "chest"));

    private void loadEventBorder() {
        if (eventKey == null) return;
        String mappedKey;
        if (MERCHANT_KEYS.contains(eventKey)) {
            mappedKey = "merchant";
        } else if (BENEFIT_KEYS.contains(eventKey)) {
            mappedKey = "scholar";
        } else {
            mappedKey = eventKey;
        }
        String fileName = mappedKey.replace("equipment_reforge", "equipment_reforce") + ".png";
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
            case "wandering_vendor":
                btnAction1 = addActionButton("查看商品", 0xFF2196F3, v -> {
                    Intent result = new Intent();
                    result.putExtra("open_trade", true);
                    result.putExtra("merchant_type", "WANDERING_VENDOR");
                    setResult(RESULT_OK, result);
                    finish();
                });
                btnAction2 = addActionButton("离开", 0xFF888888, v -> {
                    showResult("你离开了流浪商贩的摊位。");
                    switchToForwardButton();
                });
                break;

            case "equipment_merchant":
                btnAction1 = addActionButton("查看装备", 0xFF2196F3, v -> {
                    Intent result = new Intent();
                    result.putExtra("open_trade", true);
                    result.putExtra("merchant_type", "EQUIPMENT_MERCHANT");
                    setResult(RESULT_OK, result);
                    finish();
                });
                btnAction2 = addActionButton("离开", 0xFF888888, v -> {
                    showResult("你离开了装备商人的店铺。");
                    switchToForwardButton();
                });
                break;

            case "caravan":
                btnAction1 = addActionButton("查看商队货物", 0xFF2196F3, v -> {
                    Intent result = new Intent();
                    result.putExtra("open_trade", true);
                    result.putExtra("merchant_type", "CARAVAN");
                    setResult(RESULT_OK, result);
                    finish();
                });
                btnAction2 = addActionButton("离开", 0xFF888888, v -> {
                    showResult("你目送商队继续赶路。");
                    switchToForwardButton();
                });
                break;

            case "material_merchant":
                btnAction1 = addActionButton("查看货物", 0xFF2196F3, v -> {
                    Intent result = new Intent();
                    result.putExtra("open_trade", true);
                    result.putExtra("merchant_type", "MATERIAL_MERCHANT");
                    setResult(RESULT_OK, result);
                    finish();
                });
                btnAction2 = addActionButton("离开", 0xFF888888, v -> {
                    showResult("你离开了材料商人的摊位。");
                    switchToForwardButton();
                });
                break;

            case "traveler":
                currentHandler = new TravelerHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "scholar":
                currentHandler = new ScholarHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "statue_blessing":
                currentHandler = new StatueBlessingHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "monster_camp":
                currentHandler = new MonsterCampHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "cave_treasure":
                currentHandler = new CaveTreasureHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "rest":
                currentHandler = new CampRestHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "chest":
                currentHandler = new ChestHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "equipment_reforge":
                currentHandler = new EquipmentReforgeHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "casino_wagon":
                currentHandler = new CasinoHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "divination_hut":
                currentHandler = new DivinationHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "mystery_box":
                currentHandler = new MysteryBoxHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "mysterious_altar":
                currentHandler = new MysteriousAltarHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "wishing_well":
                currentHandler = new WishingWellHandler(new ActivityUICallback());
                currentHandler.build();
                break;

            case "cursed_chest":
                currentHandler = new CursedChestHandler(new ActivityUICallback());
                currentHandler.build();
                break;
        }
    }

    private TextView addActionButton(String text, int bgColor, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btn.setTypeface(btn.getTypeface(), android.graphics.Typeface.BOLD);
        btn.setTextColor(ContextCompat.getColor(this, isSecondaryActionColor(bgColor)
                ? R.color.tb_text_main : R.color.tb_bg_dark));
        btn.setBackgroundResource(isSecondaryActionColor(bgColor)
                ? R.drawable.bg_panel_treasure : R.drawable.bg_tab_active);
        btn.setGravity(Gravity.CENTER);
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setAllCaps(false);
        btn.setLineSpacing(dpToPx(4f), 1f);

        int minH = dpToPx(48);
        int hPad = dpToPx(12);
        int vPad = dpToPx(14);
        btn.setMinHeight(minH);
        btn.setPadding(hPad, vPad, hPad, vPad);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = dpToPx(4);
        params.setMargins(0, margin, 0, margin);
        btn.setLayoutParams(params);
        btn.setOnClickListener(listener);

        llActionArea.addView(btn);
        return btn;
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private static boolean isSecondaryActionColor(int bgColor) {
        return bgColor == 0xFF888888 || bgColor == 0xFFAAAAAA;
    }

    private void showResult(String text) {
        llActionArea.setVisibility(View.GONE);
        tvResult.setText(text);
        llResultArea.setVisibility(View.VISIBLE);
    }

    private void switchToForwardButton() {
        if (btnContinue != null) {
            btnContinue.setText("前进");
            btnContinue.setBackgroundResource(R.drawable.bg_tab_active);
            btnContinue.setTextColor(ContextCompat.getColor(this, R.color.tb_bg_dark));
            btnContinue.setOnClickListener(v -> finish());
        }
    }

    private void switchToBattleButton() {
        if (btnContinue != null) {
            btnContinue.setText("被迫进入战斗");
            btnContinue.setBackgroundResource(R.drawable.bg_tab_active);
            btnContinue.setTextColor(ContextCompat.getColor(this, R.color.tb_bg_dark));
            btnContinue.setOnClickListener(v -> {
                Intent r = new Intent();
                r.putExtra("open_battle", true);
                setResult(RESULT_OK, r);
                finish();
            });
        }
    }

    // ====================== ActivityUICallback ======================

    private class ActivityUICallback implements EventUICallback {
        @Override public void showResult(String text) {
            NeutralEventActivity.this.showResult(text);
        }
        @Override public void switchToForwardButton() {
            NeutralEventActivity.this.switchToForwardButton();
        }
        @Override public void switchToBattleButton() {
            NeutralEventActivity.this.switchToBattleButton();
        }
        @Override public void finishEvent() {
            finish();
        }
        @Override public void launchBattle() {
            Intent result = new Intent();
            result.putExtra("open_battle", true);
            setResult(RESULT_OK, result);
            finish();
        }
        @Override public View addButton(String text, int bgColor, View.OnClickListener listener) {
            return NeutralEventActivity.this.addActionButton(text, bgColor, listener);
        }
        @Override public void clearButtons() {
            llActionArea.removeAllViews();
        }
        @Override public View addInfoText(String text) {
            TextView tv = new TextView(NeutralEventActivity.this);
            tv.setText(text);
            tv.setTextColor(0xFF333333);
            tv.setTextSize(14);
            tv.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            tv.setBackgroundResource(R.drawable.bg_event_popup);
            llActionArea.addView(tv);
            return tv;
        }
        @Override public Context getContext() {
            return NeutralEventActivity.this;
        }
        @Override public Character getCharacter() {
            return PlayerCharacterHolder.getOrCreate(NeutralEventActivity.this);
        }
    }

    // ====================== 公共 UI 工具方法 ======================

    public static int dpToPx(Context ctx, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }

    public static void applyTransparentDialogWindow(Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public static RecyclerView.LayoutParams newSquareGridCellLp(
            Context context, int squarePx, int recyclerWidthPx) {
        int spacing = BagGridCellSizer.dpToPx(
                context.getResources().getDisplayMetrics(), BagGridCellSizer.CELL_SPACING_DP);
        int inset = recyclerWidthPx > 0
                ? BagGridCellSizer.dialogGridCellHorizontalInsetPx(recyclerWidthPx, squarePx)
                : spacing;
        int columnWidth = recyclerWidthPx > 0
                ? BagGridCellSizer.dialogGridColumnWidthPx(recyclerWidthPx)
                : squarePx + spacing * 2;
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(squarePx, squarePx);
        lp.leftMargin = inset;
        lp.rightMargin = Math.max(0, columnWidth - squarePx - inset);
        lp.topMargin = spacing;
        lp.bottomMargin = spacing;
        return lp;
    }

    public static void applySquareGridCellLayout(View itemView, int squarePx, int recyclerWidthPx) {
        if (squarePx <= 0) return;
        Context context = itemView.getContext();
        int spacing = BagGridCellSizer.dpToPx(
                context.getResources().getDisplayMetrics(), BagGridCellSizer.CELL_SPACING_DP);
        int inset = recyclerWidthPx > 0
                ? BagGridCellSizer.dialogGridCellHorizontalInsetPx(recyclerWidthPx, squarePx)
                : spacing;
        int columnWidth = recyclerWidthPx > 0
                ? BagGridCellSizer.dialogGridColumnWidthPx(recyclerWidthPx)
                : squarePx + spacing * 2;
        int rightInset = Math.max(0, columnWidth - squarePx - inset);
        ViewGroup.LayoutParams raw = itemView.getLayoutParams();
        if (!(raw instanceof RecyclerView.LayoutParams)) return;
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) raw;
        if (lp.width == squarePx && lp.height == squarePx
                && lp.leftMargin == inset && lp.rightMargin == rightInset
                && lp.topMargin == spacing && lp.bottomMargin == spacing) return;
        lp.width = squarePx;
        lp.height = squarePx;
        lp.leftMargin = inset;
        lp.rightMargin = rightInset;
        lp.topMargin = spacing;
        lp.bottomMargin = spacing;
        itemView.setLayoutParams(lp);
    }

    public static void scheduleDialogItemGrid(RecyclerView rv, RecyclerView.Adapter<?> adapter,
            java.util.function.BiConsumer<Integer, Integer> setGridLayout) {
        rv.setVisibility(View.INVISIBLE);
        rv.setHasFixedSize(true);
        rv.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                int contentWidth = rv.getWidth() - rv.getPaddingLeft() - rv.getPaddingRight();
                if (contentWidth <= 0) return true;
                int sizePx = BagGridCellSizer.resolveDialogGridSquareSizePx(
                        rv.getResources().getDisplayMetrics(), contentWidth);
                if (sizePx <= 0) return true;
                setGridLayout.accept(contentWidth, sizePx);
                if (rv.getAdapter() == null) {
                    rv.setAdapter(adapter);
                    rv.getViewTreeObserver().removeOnPreDrawListener(this);
                    rv.setVisibility(View.VISIBLE);
                    return false;
                }
                return true;
            }
        });
    }
}
