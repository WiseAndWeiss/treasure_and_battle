package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.treasure_and_battle.R;

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
        btnBattle.setOnClickListener(v -> {
            FragmentManager fm = requireActivity().getSupportFragmentManager();
            fm.popBackStackImmediate("battle", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Fragment orphan = fm.findFragmentByTag(BattleFragment.TAG);
            FragmentTransaction ft = fm.beginTransaction().setReorderingAllowed(true);
            if (orphan != null) {
                ft.remove(orphan);
            }
            ft.add(R.id.fragment_container, new BattleFragment(), BattleFragment.TAG)
                    .hide(this)
                    .addToBackStack("battle")
                    .commit();
        });

        View btnBackToMain = view.findViewById(R.id.btn_back_to_main);
        btnBackToMain.setOnClickListener(v -> {
            startActivity(new android.content.Intent(requireActivity(), EntryActivity.class));
            requireActivity().finish();
        });

        view.findViewById(R.id.btn_debug_gain_exp).setOnClickListener(v ->
                DebugCharacterGrants.grantExp(requireContext(), 500_000));
        view.findViewById(R.id.btn_debug_gain_talent).setOnClickListener(v ->
                DebugCharacterGrants.grantTalentPoints(requireContext(), 20));
        view.findViewById(R.id.btn_debug_gain_skill).setOnClickListener(v ->
                DebugCharacterGrants.grantSkillPoints(requireContext(), 20));

        // TODO 后续在这里写设置项：音量、音效开关、存档重置

        return view;
    }
}
