package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.GameManager;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.manager.SaveManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.common.Rarity;

import java.util.List;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPrefs;
    private RadioGroup rgRate;
    private RadioButton rbFast, rbNormal, rbSlow;
    private int savedRate;

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

        SwitchCompat switchToast = view.findViewById(R.id.switch_event_toast);
        boolean showToast = sharedPrefs.getBoolean("showEventToast", true);
        switchToast.setChecked(showToast);
        switchToast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("showEventToast", isChecked).apply();
        });

        rbFast = view.findViewById(R.id.rb_rate_fast);
        rbNormal = view.findViewById(R.id.rb_rate_normal);
        rbSlow = view.findViewById(R.id.rb_rate_slow);
        rgRate = view.findViewById(R.id.rg_event_rate);

        savedRate = sharedPrefs.getInt("eventRate", 0);
        selectRadioSilently(savedRate);

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
        btnBattle.setOnClickListener(v -> showDebugBattleDialog());

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

        view.findViewById(R.id.btn_debug_setup_test_character).setOnClickListener(v ->
                DebugCharacterGrants.setupTestCharacter(requireContext()));

        // TODO 后续在这里写设置项：音量、音效开关、存档重置
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
                        sharedPrefs.edit().putInt("eventRate", savedRate).apply();
                        FragmentManager fm = getParentFragmentManager();
                        for (Fragment f : fm.getFragments()) {
                            if (f instanceof MapFragment && f.isAdded()) {
                                ((MapFragment) f).resetEventsAndTimers();
                                break;
                            }
                        }
                    })
                    .setNegativeButton("取消", (d, which) -> selectRadioSilently(savedRate))
                    .setOnCancelListener(d -> selectRadioSilently(savedRate))
                    .show();
        });
    }

    private void showDebugBattleDialog() {
        MonsterManager mm = MonsterManager.getInstance(requireContext());
        List<String> races = mm.getAvailableRacesForBattle();
        String[] raceNames = races.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("选择怪物种族")
                .setItems(raceNames, (d, which) -> showRarityDialog(raceNames[which]))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRarityDialog(String raceId) {
        MonsterManager mm = MonsterManager.getInstance(requireContext());
        int maxR = mm.getMaxRarityForRace(raceId);
        String[] rarityNames = new String[maxR + 1];
        for (int i = 0; i <= maxR; i++) {
            Rarity r = Rarity.fromId(i);
            rarityNames[i] = r != null ? r.getDisplayName() : "Lv." + i;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("选择最高稀有度（" + raceId + "）")
                .setItems(rarityNames, (d, which) -> showCountDialog(raceId, which))
                .setNegativeButton("返回", null)
                .show();
    }

    private void showCountDialog(String raceId, int maxRarity) {
        String[] countNames = {"1 只", "2 只", "3 只", "4 只", "5 只"};
        new AlertDialog.Builder(requireContext())
                .setTitle("选择怪物数量")
                .setItems(countNames, (d, which) -> openBattleWithDebug(raceId, maxRarity, which + 1))
                .setNegativeButton("返回", null)
                .show();
    }

    private void openBattleWithDebug(String raceId, int maxRarity, int count) {
        Intent intent = new Intent(getActivity(), BattleActivity.class);
        intent.putExtra("debugRaceId", raceId);
        intent.putExtra("debugMaxRarity", maxRarity);
        intent.putExtra("debugCount", count);
        startActivity(intent);
    }
}
