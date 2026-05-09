package com.example.treasure_and_battle.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.treasure_and_battle.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private MapFragment mapFragment;
    private BagFragment bagFragment;
    private SkillFragment skillFragment;
    private SettingsFragment settingsFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // 初始化所有 Fragment
        mapFragment = new MapFragment();
        bagFragment = new BagFragment();
        skillFragment = new SkillFragment();
        settingsFragment = new SettingsFragment();

        // 默认显示第一个（地图）
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.fragment_container, settingsFragment, "4").hide(settingsFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, skillFragment, "3").hide(skillFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, bagFragment, "2").hide(bagFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, mapFragment, "1").commit();
        
        activeFragment = mapFragment;

        // 设置底栏导航切换事件
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_map) {
                switchFragment(mapFragment);
                return true;
            } else if (itemId == R.id.nav_bag) {
                switchFragment(bagFragment);
                return true;
            } else if (itemId == R.id.nav_skill) {
                switchFragment(skillFragment);
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });

    }

    // 优雅切换 Fragment 避免每次重复销毁/重建实例和布局
    private void switchFragment(Fragment targetFragment) {
        if (activeFragment != targetFragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commit();
            activeFragment = targetFragment;
        }
    }
}