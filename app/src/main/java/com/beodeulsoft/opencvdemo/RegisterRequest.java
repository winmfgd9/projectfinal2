package com.beodeulsoft.opencvdemo;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class RegisterRequest extends StringRequest {

    // 서버 URL 설정 (PHP 파일 연동)
    final static private String URL = "http://winmfgd9.cafe24.com/Register.php";
    private Map<String, String> map; // 서버에 전송할 파라미터를 담을 Map

    // 생성자
    public RegisterRequest(String userID, String userPassword, String userName, int userAge, String NickName,String Email,String PhoneNumber,String CarName,String CarLicense, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null); // POST 방식으로 데이터 전송

        map = new HashMap<>(); // Map 초기화
        // 각 파라미터를 Map에 설정
        map.put("userID",userID);
        map.put("userPassword",userPassword);
        map.put("userName",userName);
        map.put("userAge",userAge + ""); // int형을 String형으로 변환하여 저장
        map.put("NickName",NickName);
        map.put("Email",Email);
        map.put("PhoneNumber",PhoneNumber);
        map.put("CarName",CarName);
        map.put("CarLicense",CarLicense);
    }

    // @Nullable 주석이 원래 있었음을 표시하는 주석
    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return map; // map 반환
    }
}