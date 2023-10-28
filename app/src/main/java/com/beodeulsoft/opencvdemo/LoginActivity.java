package com.beodeulsoft.opencvdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText et_id2, et_pass2; // 사용자 ID와 비밀번호를 위한 EditText
    private Button btn_login2, btn_register2; // 로그인 및 등록 버튼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // XML에서 뷰 초기화
        et_id2 = findViewById(R.id.et_id2);
        et_pass2 = findViewById(R.id.et_pass2);
        btn_login2 = findViewById(R.id.btn_login2);
        btn_register2 = findViewById(R.id.btn_register2);

        //회원가입 버튼 클릭시 수행
        btn_register2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 회원가입 버튼 클릭 시 RegisterActivity로 이동
                Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });

        /* 로그인 버튼 클릭 시 수행:
           1. 입력된 ID와 비밀번호를 가져온다.
           4. 응답(Response)을 받으면 JSONObject로 변환하고,
           5. "success" 키의 값을 확인하여 로그인 성공 여부를 판단한다.
           6. 성공한 경우 Toast 메시지를 표시하고 ChoiceActivity로 이동한다.
           7. 실패한 경우 Toast 메시지를 표시한다.
         */

        // 로그인 버튼 클릭 리스너
        btn_login2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // EditText에 입력된 현재 입력되어 있는 값을 가져온다.
                String userID = et_id2.getText().toString();
                String userPass = et_pass2.getText().toString();

                // 로그인에 대한 서버 응답 처리 리스너
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            if (success) { //로그인에 성공한 경우
                                String userID = jsonObject.getString("userID");
                                String userPass = jsonObject.getString("userPassword");

                                // 성공 메시지 표시 및 ChoiceActivity로 이동
                                Toast.makeText (getApplicationContext(), "로그인에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this,ChoiceActivity.class);
                                intent.putExtra("userID",userID);
                                intent.putExtra("userPass",userPass);
                                startActivity(intent);
                            } else { //로그인에 실패한 경우
                                Toast.makeText (getApplicationContext(), "로그인에 실패하셨습니다.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                // Volley를 사용하여 서버에 로그인 요청 전송
                LoginRequest loginRequest = new LoginRequest(userID, userPass, responseListener);
                RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
                queue.add(loginRequest);
            }
        });

    }
}