package com.beodeulsoft.opencvdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChoiceActivity extends AppCompatActivity {

    private Button ch_bt0,ch_bt1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        ch_bt0 = findViewById(R.id.ch_bt0);
        ch_bt1 = findViewById(R.id.ch_bt1);

        ch_bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChoiceActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        ch_bt0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChoiceActivity.this,ReportActivity.class);
                startActivity(intent);
            }
        });
    }
}