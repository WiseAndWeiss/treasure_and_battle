package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.GameManager;
import com.example.treasure_and_battle.manager.SaveManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE);

        SwitchCompat switchMapPoi = view.findViewById(R.id.switch_show_map_poi);
        boolean isShowPoi = sharedPrefs.getBoolean("showMapPoi", false);
        switchMapPoi.setChecked(isShowPoi);
        switchMapPoi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("showMapPoi", isChecked).apply();
        });

        SwitchCompat switchDebug = view.findViewById(R.id.switch_debug);
        boolean isDebug = sharedPrefs.getBoolean("debugMode", false);
        switchDebug.setChecked(isDebug);
        switchDebug.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("debugMode", isChecked).apply();
        });
        
        View btnTrade = view.findViewById(R.id.btn_open_trade);
        btnTrade.setOnClickListener(v -> {
            FragmentManager fm = requireActivity().getSupportFragmentManager();
            fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container, new TradeFragment(), TradeFragment.TAG)
                    .hide(this)
                    .addToBackStack("trade")
                    .commit();
        });

        View btnBattle = view.findViewById(R.id.btn_open_battle);
        btnBattle.setOnClickListener(v -> showPoolSelectionDialog());

        View btnProfession = view.findViewById(R.id.btn_profession_select);
        btnProfession.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ProfessionSelectActivity.class));
        });

        View btnBackToMain = view.findViewById(R.id.btn_back_to_main);
        btnBackToMain.setOnClickListener(v -> {
            Character ch = PlayerCharacterHolder.getOrCreate(requireContext());
            GameManager.getInstance(requireContext()).triggerAutoSave();
            PlayerCharacterHolder.clear();
            startActivity(new Intent(requireActivity(), EntryActivity.class));
            requireActivity().finish();
        });

        View btnSaveGame = view.findViewById(R.id.btn_save_game);
        if (btnSaveGame != null) {
            btnSaveGame.setOnClickListener(v -> {
                SaveSelectDialog.show(requireContext(), SaveSelectDialog.MODE_SAVE,
                        new SaveSelectDialog.OnSaveActionListener() {
                            @Override
                            public void onLoadSave(Character character) {}

                            @Override
                            public void onSaveComplete() {
                                FloatMsgOverlay.showFloatMsg(requireContext(), "存档成功");
                            }
                        });
            });
        }

        View btnLoadGame = view.findViewById(R.id.btn_load_game);
        if (btnLoadGame != null) {
            btnLoadGame.setOnClickListener(v -> {
                SaveSelectDialog.show(requireContext(), SaveSelectDialog.MODE_LOAD,
                        new SaveSelectDialog.OnSaveActionListener() {
                            @Override
                            public void onLoadSave(Character character) {
                                PlayerCharacterHolder.restoreFrom(character);
                                FloatMsgOverlay.showFloatMsg(requireContext(), "读档成功");
                            }

                            @Override
                            public void onSaveComplete() {}
                        });
            });
        }

        View btnDebugExp = view.findViewById(R.id.btn_debug_gain_exp);
        if (btnDebugExp != null) {
            btnDebugExp.setOnClickListener(v ->
                    DebugCharacterGrants.grantExp(requireContext(), 500_000));
        }
        View btnDebugTalent = view.findViewById(R.id.btn_debug_gain_talent);
        if (btnDebugTalent != null) {
            btnDebugTalent.setOnClickListener(v ->
                    DebugCharacterGrants.grantTalentPoints(requireContext(), 20));
        }
        View btnDebugSkill = view.findViewById(R.id.btn_debug_gain_skill);
        if (btnDebugSkill != null) {
            btnDebugSkill.setOnClickListener(v ->
                    DebugCharacterGrants.grantSkillPoints(requireContext(), 20));
        }

        View btnDevSeedItems = view.findViewById(R.id.btn_dev_seed_items);
        if (btnDevSeedItems != null) {
            btnDevSeedItems.setOnClickListener(v -> {
                InventoryManager.clearAll(PlayerCharacterHolder.getOrCreate(requireContext()).getBagItems());
                InventoryGridSync.seedDemoItems(requireContext());
                InventoryGridSync.reloadSharedGridFromManager(requireContext());
                FloatMsgOverlay.showFloatMsg(requireContext(), "已填充测试物品");
            });
        }

        return view;
    }

    private int checkedId2Rate(int checkedId) {
        if (checkedId == R.id.rb_rate_fast) return 0;
        if (checkedId == R.id.rb_rate_normal) return 1;
        return 2;
    }

    private void selectRadioSilently(int rate) {
        rgRate.setOnCheckedChangeListener(null);
        if (rate == 1) rbNormal.setChecked(true);
        else if (rate == 2) rbSlow.setChecked(true);
        else rbFast.setChecked(true);
        rgRate.setOnCheckedChangeListener((group, checkedId) -> {
            int newRate = checkedId2Rate(checkedId);
            if (newRate == savedRate) return;
            String msg;
            if (newRate == 0) msg = "确认为快速（10秒/批）？\n将清空当前地图事件并重新生成。";
            else if (newRate == 1) msg = "确认为普通（1分钟/批）？\n将清空当前地图事件并重新生成。";
            else msg = "确认为慢速（30分钟/批）？\n将清空当前地图事件并重新生成。";
            new AlertDialog.Builder(requireContext())
                    .setTitle("是否确认更换速率")
                    .setMessage(msg)
                    .setPositiveButton("确认", (d, which) -> {
                        savedRate = newRate;
                        sharedPrefs.edit().putInt("eventRate", savedRate)
                                .putBoolean("pendingRateClear", true).apply();
                    })
                    .setNegativeButton("取消", (d, which) -> selectRadioSilently(savedRate))
                    .setOnCancelListener(d -> selectRadioSilently(savedRate))
                    .show();
        });
    }

    private void showPoolSelectionDialog() {
        ScrollView sv = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(12);
        root.setPadding(pad, pad, pad, pad);

        String[] categories = {"简单", "一般", "挑战", "困难", "灾难", "试炼"};
        int[] catColors = {0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFFE53935, 0xFF9C27B0, 0xFF000000};

        int poolCount = MonsterManager.getPoolCount();
        String lastCat = "";
        for (int i = 0; i < poolCount; i++) {
            String cat = MonsterManager.getPoolDifficultyCategory(i);
            if (!cat.equals(lastCat)) {
                lastCat = cat;
                int catIdx = java.util.Arrays.asList(categories).indexOf(cat);
                TextView tvCat = new TextView(requireContext());
                tvCat.setText("━━━ " + cat + " ━━━");
                tvCat.setTextSize(16);
                tvCat.setTextColor(catIdx >= 0 ? catColors[catIdx] : 0xFFFFFFFF);
                tvCat.setPadding(0, dpToPx(10), 0, dpToPx(4));
                root.addView(tvCat);
            }
            final int idx = i;
            TextView tvPool = new TextView(requireContext());
            tvPool.setText(MonsterManager.getPoolDescription(i));
            tvPool.setTextSize(14);
            tvPool.setTextColor(0xFFFFFFFF);
            tvPool.setBackgroundResource(R.drawable.bg_tab_idle);
            tvPool.setClickable(true);
            tvPool.setFocusable(true);
            tvPool.setGravity(android.view.Gravity.CENTER);
            tvPool.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dpToPx(3), 0, dpToPx(3));
            tvPool.setLayoutParams(lp);
            tvPool.setOnClickListener(v -> openBattleWithPool(idx));
            root.addView(tvPool);
        }
        sv.addView(root);
        new AlertDialog.Builder(requireContext())
                .setTitle("选择怪物阵容")
                .setView(sv)
                .setNegativeButton("取消", null)
                .show();
    }

    private void openBattleWithPool(int idx) {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        fm.popBackStackImmediate("battle", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment orphan = fm.findFragmentByTag(BattleFragment.TAG);
        FragmentTransaction ft = fm.beginTransaction().setReorderingAllowed(true);
        if (orphan != null) ft.remove(orphan);
        BattleFragment frag = new BattleFragment();
        Bundle args = new Bundle();
        args.putInt("poolIndex", idx);
        frag.setArguments(args);
        ft.add(R.id.fragment_container, frag, BattleFragment.TAG)
                .hide(this).addToBackStack("battle").commit();
    }

    private int dpToPx(int dp) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }
}
