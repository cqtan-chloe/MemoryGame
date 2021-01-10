package com.example.memory_game;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Intent intent = getIntent();
        TextView text = findViewById(R.id.timeUsed);
        text.setText(intent.getStringExtra("time"));

        findViewById(R.id.playAgain).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);
    }
}