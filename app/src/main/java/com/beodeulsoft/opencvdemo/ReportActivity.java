package com.beodeulsoft.opencvdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    EditText t1,t2,t3,t4; // 사용자 입력을 받기 위한 텍스트 필드
    Button browse,upload; // 버튼 변수
    ImageView img; // 이미지 뷰 변수
    Bitmap bitmap; // 선택된 이미지의 비트맵
    String encodeImageString; // 인코딩된 이미지 문자열
    private static final String url="http://winmfgd9.cafe24.com/fileupload.php";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // UI 컴포넌트와 변수 연결
        img=(ImageView)findViewById(R.id.img);
        upload=(Button)findViewById(R.id.upload);
        browse=(Button)findViewById(R.id.browse);

        // 이미지 선택 버튼 클릭 리스너
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dexter 라이브러리를 사용해 저장소 접근 권한 확인
                Dexter.withActivity(ReportActivity.this)
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response)
                            {   // 권한이 허용되었을 때, 이미지 선택 액티비티 시작
                                Intent intent=new Intent(Intent.ACTION_PICK);
                                intent.setType("image/*");
                                startActivityForResult(Intent.createChooser(intent,"Browse Image"),1);
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                // 권한이 거부되었을 때의 처리 (현재는 아무 처리도 없음)
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                // 권한 요청의 이유를 사용자에게 보여주어야 할 때의 처리 (계속 권한 요청)
                                token.continuePermissionRequest();
                            }
                        }).check(); // 권한 체크 시작
            }
        });

        // 업로드 버튼 클릭 리스너
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploaddatatodb();
            } // 데이터와 이미지 업로드 메서드 호출
        });
    }


    // 이미지를 선택한 후의 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        // requestCode가 1이고 결과가 OK일 경우 (이미지가 성공적으로 선택되었을 경우)
        if(requestCode==1 && resultCode==RESULT_OK)
        {   // 선택한 이미지의 경로를 가져옴
            Uri filepath=data.getData();
            try
            {   // 선택한 이미지를 스트림으로 읽어들임
                InputStream inputStream=getContentResolver().openInputStream(filepath);
                // 스트림을 비트맵으로 변환
                bitmap= BitmapFactory.decodeStream(inputStream);
                // 비트맵을 ImageView에 설정
                img.setImageBitmap(bitmap);
                // 비트맵을 인코딩 (Base64 혹은 다른 방식)
                encodeBitmapImage(bitmap);
            }catch (Exception ex)
            // 이미지 처리 중 예외 발생시 처리 (현재는 아무 처리도 없음)
            {

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 비트맵 이미지를 Base64 문자열로 인코딩
    private void encodeBitmapImage(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        byte[] bytesofimage=byteArrayOutputStream.toByteArray();
        encodeImageString=android.util.Base64.encodeToString(bytesofimage, Base64.DEFAULT);
    }

    // 서버에 데이터와 이미지를 업로드
    private void uploaddatatodb()
    {
        // 각 텍스트 입력란을 찾아서 변수에 할당
        t1=(EditText)findViewById(R.id.t1);
        t2=(EditText)findViewById(R.id.t2);
        t3=(EditText)findViewById(R.id.t3);
        t4=(EditText)findViewById(R.id.t4);

        // 각 입력란의 텍스트 값을 변수에 할당 (여기에는 trim()을 사용해 앞뒤 공백 제거)
        final String Class=t1.getText().toString().trim();
        final String License=t2.getText().toString().trim();
        final String Point=t3.getText().toString().trim();
        final String Day=t4.getText().toString().trim();

        // Volley를 사용하여 서버에 데이터를 POST 방식으로 전송
        StringRequest request=new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response)
            {   // 서버로부터의 응답을 처리 (성공적인 경우)
                t1.setText("");
                t2.setText("");
                // 기본 이미지로 설정
                img.setImageResource(R.drawable.ic_launcher_foreground);
                // 서버 응답을 토스트 메시지로 표시
                Toast.makeText(getApplicationContext(),response.toString(),Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // 에러 발생 시 처리
                Toast.makeText(getApplicationContext(),error.toString(),Toast.LENGTH_LONG).show();
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError
            {
                // 서버로 전송할 데이터 맵 생성
                Map<String,String> map=new HashMap<String, String>();
                map.put("t1",Class);
                map.put("t2",License);
                map.put("t3",Point);
                map.put("t4",Day);
                map.put("upload",encodeImageString); // 이미지 인코딩 문자열 추가
                return map;
            }
        };


        RequestQueue queue= Volley.newRequestQueue(getApplicationContext());
        queue.add(request);
    }
}