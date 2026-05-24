package com.example.treasure_and_battle.ui;

import android.content.Context;
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

        rbFast = view.findViewById(R.id.rb_rate_fast);
        rbNormal = view.findViewById(R.id.rb_rate_normal);
        rbSlow = view.findViewById(R.id.rb_rate_slow);
        rgRate = view.findViewById(R.id.rg_event_rate);

        savedRate = sharedPrefs.getInt("eventRate", 0);
        selectRadioSilently(savedRate);

        SwitchCompat switchToast = view.findViewById(R.id.switch_event_toast);
        boolean showToast = sharedPrefs.getBoolean("showEventToast", true);
        switchToast.setChecked(showToast);
        switchToast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPrefs.edit().putBoolean("showEventToast", isChecked).apply();
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
}
