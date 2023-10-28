package com.beodeulsoft.opencvdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChoiceActivity extends AppCompatActivity {

    private Button ch_bt0,ch_bt1,ch_bt2; // 버튼 객체 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice); // activity_choice 레이아웃과 연결

        ch_bt0 = findViewById(R.id.ch_bt0); // 레이아웃에서 id가 ch_bt0인 버튼과 연결
        ch_bt1 = findViewById(R.id.ch_bt1); // 레이아웃에서 id가 ch_bt1인 버튼과 연결
        ch_bt2 = findViewById(R.id.ch_bt2); // 레이아웃에서 id가 ch_bt2인 버튼과 연결

        // 첫 번째 버튼에 클릭 리스너 설정. 클릭 시 MainActivity로 이동.
        ch_bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChoiceActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        // 두 번째 버튼에 클릭 리스너 설정. 클릭 시 ReportActivity로 이동.
        ch_bt0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChoiceActivity.this,ReportActivity.class);
                startActivity(intent);
            }
        });

        // 두 번째 버튼에 클릭 리스너 설정. 클릭 시 parking로 이동.
        ch_bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChoiceActivity.this,parking.class);
                startActivity(intent);
            }
        });
    }
}