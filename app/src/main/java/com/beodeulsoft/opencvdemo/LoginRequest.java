package com.beodeulsoft.opencvdemo;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class LoginRequest extends StringRequest {

    // 서버 URL 설정 (PHP 파일 연동)
    final static private String URL = "http://winmfgd9.cafe24.com/Login.php";
    private Map<String, String> map; // 서버로 전송할 파라미터를 저장하는 Map 객체

    // 생성자
    public LoginRequest(String userID, String userPassword, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null); // POST 방식으로 데이터 전송

        map = new HashMap<>();
        map.put("userID",userID); // 사용자 ID를 맵에 추가
        map.put("userPassword",userPassword); // 사용자 비밀번호를 맵에 추가
    }

    // Volley를 사용하여 요청할 때 파라미터들을 전송
    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return map; // 위에서 저장한 맵 객체 반환
    }
}
