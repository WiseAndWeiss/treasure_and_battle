package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
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

        return view;
    }
}
