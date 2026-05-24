package com.example.treasure_and_battle.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.GameManager;
import com.example.treasure_and_battle.manager.SaveManager;
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

        Character character = PlayerCharacterHolder.getOrCreate(this);
        Character restored = SaveManager.getInstance(this).loadGame("auto");
        if (restored != null) {
            PlayerCharacterHolder.restoreFrom(restored);
            character = restored;
        }

        GameManager.getInstance(this).startGame(character);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        mapFragment = new MapFragment();
        bagFragment = new BagFragment();
        skillFragment = new SkillFragment();
        settingsFragment = new SettingsFragment();

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.fragment_container, settingsFragment, "4").hide(settingsFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, skillFragment, "3").hide(skillFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, bagFragment, "2").hide(bagFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, mapFragment, "1").commit();
        
        activeFragment = mapFragment;

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

    private void switchFragment(Fragment targetFragment) {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }
        if (activeFragment != targetFragment) {
            fm.beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commit();
            activeFragment = targetFragment;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GameManager.getInstance(this).onGameResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        GameManager.getInstance(this).onGamePause();
    }
}
