package com.example.treasure_and_battle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;

public class EntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        Button btnStartGame = findViewById(R.id.btn_start_game);
        Button btnSelectSave = findViewById(R.id.btn_select_save);
        Button btnEncyclopedia = findViewById(R.id.btn_encyclopedia);
        Button btnExitGame = findViewById(R.id.btn_exit_game);

        btnStartGame.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfessionSelectActivity.class)
                    .putExtra("mode", "new_game"));
        });

        btnSelectSave.setOnClickListener(v ->
                SaveSelectDialog.show(this, SaveSelectDialog.MODE_LOAD, new SaveSelectDialog.OnSaveActionListener() {
                    @Override
                    public void onLoadSave(Character character) {
                        PlayerCharacterHolder.restoreFrom(character);
                        startActivity(new Intent(EntryActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onSaveComplete() {}
                }));

        btnEncyclopedia.setOnClickListener(v ->
                startActivity(new Intent(this, EncyclopediaActivity.class)));

        btnExitGame.setOnClickListener(v -> finishAffinity());
    }
}
