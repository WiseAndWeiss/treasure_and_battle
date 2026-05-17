package com.example.treasure_and_battle.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.treasure_and_battle.R;

public class EntryActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        Button btnStartGame = findViewById(R.id.btn_start_game);
        Button btnSelectSave = findViewById(R.id.btn_select_save);
        Button btnEncyclopedia = findViewById(R.id.btn_encyclopedia);
        Button btnExitGame = findViewById(R.id.btn_exit_game);

        btnStartGame.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            } else {
                enterGame();
            }
        });

        btnSelectSave.setOnClickListener(v ->
                FloatMsgOverlay.showFloatMsg(this, "功能开发中"));

        btnEncyclopedia.setOnClickListener(v ->
                startActivity(new Intent(this, EncyclopediaActivity.class)));

        btnExitGame.setOnClickListener(v -> finishAffinity());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            enterGame();
        }
    }

    private void enterGame() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
