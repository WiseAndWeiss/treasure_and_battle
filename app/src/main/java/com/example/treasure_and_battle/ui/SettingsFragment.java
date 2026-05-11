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

// 设置界面：音效、音量、存档、账号、关于
public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 获取 SharedPreference 实例
        sharedPrefs = requireContext().getSharedPreferences("GameSettings", Context.MODE_PRIVATE);

        // 初始化控件
        SwitchCompat switchMapPoi = view.findViewById(R.id.switch_show_map_poi);

        // 读取当前保存的状态（默认隐藏，这样地图最开始就是纯粹的）
        boolean isShowPoi = sharedPrefs.getBoolean("showMapPoi", false);
        switchMapPoi.setChecked(isShowPoi);

        // 监听开关的变化并保存
        switchMapPoi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean("showMapPoi", isChecked);
            editor.apply();
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
            // 先同步弹出 battle 返回栈，避免异步 pop 未完成时再次 add 导致无法进入或状态错乱
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

        // TODO 后续在这里写设置项：音量、音效开关、存档重置

        return view;
    }
}