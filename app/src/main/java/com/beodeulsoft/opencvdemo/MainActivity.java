package com.beodeulsoft.opencvdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    TessBaseAPI tessBaseAPI;

    private Button btnTakePicture, find;
    private ImageView imageView;
    private ImageView imageResult;
    private TextView textView, vt;
    private TextureView textureView;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    //protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    Bitmap imgBase;
    Bitmap roi;
    private static final String TAG = "MAINACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        imageResult = findViewById(R.id.imageResult);
        textView = findViewById(R.id.textView);
        textureView = findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        btnTakePicture = findViewById(R.id.btnTakePicture);

        //버튼 및 결과값 나와야하는 테스트뷰
        find = findViewById(R.id.find);
        vt=findViewById(R.id.tv);


        // 전화번호 찾기
        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            if (success) {
                                String phoneNum = jsonObject.getString("PhoneNumber");
                                vt.setText(phoneNum);
                            } else {
                                Toast.makeText (getApplicationContext(), "회원이 아닙니다.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                };


                String strDetectText = textView.getText().toString();
                if(!TextUtils.isEmpty(strDetectText)) {
                    //서버로 volley를 이용해서 요청
                    // strDetectText = "00누2222"; // 텍스트인식 없이 테스트 해보고싶으면 주석해제
                    SelectPhoneNumRequest selectPhoneNum = new SelectPhoneNumRequest(strDetectText,responseListener);
                    RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                    queue.add(selectPhoneNum);
                }
            }
        });


        assert btnTakePicture != null;
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        tessBaseAPI = new TessBaseAPI();
        String dir = getFilesDir() + "/tesseract";
        if(checkLanguageFile(dir+"/tessdata"))
            tessBaseAPI.init(dir, "kor");
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(imageReader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Log.d(TAG, "takePicture");

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 8;

                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        bitmap = GetRotatedBitmap(bitmap, 90);

                        Bitmap imgRoi;
                        OpenCVLoader.initDebug(); // 초기화

                        Mat matBase=new Mat();
                        Utils.bitmapToMat(bitmap ,matBase);
                        Mat matGray = new Mat();
                        Mat matTopHat = new Mat();
                        Mat matBlackHat = new Mat();
                        Mat matThresh = new Mat();
                        Mat matDilate = new Mat();

                        Imgproc.cvtColor(matBase, matGray, Imgproc.COLOR_BGR2GRAY); // GrayScale
                        Mat matKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3,3), new Point(0,0));
                        Imgproc.morphologyEx(matGray, matTopHat, Imgproc.MORPH_TOPHAT, matKernel, new Point(0,0)); //원본에서 열기연산 제외
                        Imgproc.morphologyEx(matGray, matBlackHat, Imgproc.MORPH_BLACKHAT, matKernel, new Point(0,0)); //닫기연산에서 원본 제외

                        Mat matAdd = new Mat();
                        Core.add(matGray, matTopHat, matAdd);
                        Mat matSub = new Mat();
                        Core.subtract(matAdd, matBlackHat, matSub);
                        Mat matBlur = new Mat();
                        Imgproc.GaussianBlur(matSub, matBlur, new org.opencv.core.Size(5,5), 0); //노이즈 제거
                        Imgproc.adaptiveThreshold(matBlur, matThresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 19, 9); //영상이진화
                        Imgproc.dilate(matThresh, matDilate, matKernel); //엣지 테두리 더 굵게 처리

                        List<MatOfPoint> contours = new ArrayList<>();
                        Mat hierarchy = new Mat();
                        Mat matContour = new Mat();
                        Imgproc.cvtColor(matDilate, matContour, Imgproc.COLOR_GRAY2BGR);
                        //관심영역 추출
                        Imgproc.findContours(matDilate, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                        //Imgproc.drawContours(matContour, contours, -1, new Scalar(255, 0, 0), 5);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable(){
                                    @Override
                                    public void run() {
                                        imageResult.setImageResource(0);
                                        textView.setText("");
                                    }
                                });
                            }
                        }).start();
                        Imgproc.adaptiveThreshold(matGray, matGray, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 20); //글자 선명화 처리
                        imgRoi= Bitmap.createBitmap(matGray.cols(), matGray.rows(), Bitmap.Config.ARGB_8888); // 비트맵 생성
                        Utils.matToBitmap(matGray, imgRoi);
                        int nContourCount;
                        float ratio;
                        for(int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                            MatOfPoint matOfPoint = contours.get(idx);

                            if(Imgproc.contourArea(matOfPoint) < 1000.0)
                                continue;

                            Rect rect = Imgproc.boundingRect(matOfPoint);

                            ratio = (float)rect.width / (float)rect.height;
                            if (rect.width < 30 || rect.height < 30 || rect.width <= rect.height || ratio < 1.7 || ratio > 5.0)
                                continue; // 사각형 크기와 비율에 따라 출력 여부 결정
                            Mat matRoi = matThresh.submat(rect);
                            //모든 번호판 유형 표시
                            Imgproc.rectangle(matContour, rect.tl(), rect.br(), new Scalar(0, 255,0), 2);
                            nContourCount = getContourCount(matContour, matRoi, rect);
                            if(nContourCount < 6 || nContourCount > 9) //인식된 글자갯수 체크
                                continue;
                            //최종선택 번호판 유형 표시
                            Imgproc.rectangle(matContour, rect.tl(), rect.br(), new Scalar(255, 0,0), 2);
                            roi = Bitmap.createBitmap( imgRoi, (int)rect.tl().x, (int)rect.tl().y, rect.width, rect.height);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable(){
                                        @Override
                                        public void run() {
                                            imageResult.setImageBitmap(roi);
                                            new AsyncTess().execute(roi);
                                            btnTakePicture.setEnabled(false);
                                            btnTakePicture.setText("텍스트 인식중...");
                                        }
                                    });
                                }
                            }).start();
                            break;
                        }
                        imgBase= Bitmap.createBitmap(matBase.cols(), matBase.rows(), Bitmap.Config.ARGB_8888); // 비트맵 생성
                        Utils.matToBitmap(matContour, imgBase); // Mat을 비트맵으로 변환
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable(){
                                    @Override
                                    public void run() {
                                        imageView.setImageBitmap(imgBase);
                                    }
                                });
                            }
                        }).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

            };
            imageReader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        //if (null != cameraDevice) {
        //    cameraDevice.close();
        //    cameraDevice = null;
        //}
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "죄송하지만, 사진촬영 권한이 승인되지 않으면 이앱을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    boolean checkLanguageFile(String dir)
    {
        File file = new File(dir);
        if(!file.exists() && file.mkdirs())
            createFiles(dir);
        else if(file.exists()){
            String filePath = dir + "/kor.traineddata";
            File langDataFile = new File(filePath);
            if(!langDataFile.exists())
                createFiles(dir);
        }
        return true;
    }

    private void createFiles(String dir)
    {
        AssetManager assetMgr = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = assetMgr.open("kor.traineddata");

            String destFile = dir + "/kor.traineddata";

            outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static Bitmap GetRotatedBitmap(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2) {
                    bitmap = b2;
                }
            } catch (OutOfMemoryError ex) {
                ex.printStackTrace();
            }
        }
        return bitmap;
    }

    private class AsyncTess extends AsyncTask<Bitmap, Integer, String> {
        @Override
        protected String doInBackground(Bitmap... mRelativeParams) {
            tessBaseAPI.setImage(mRelativeParams[0]);
            return tessBaseAPI.getUTF8Text();
        }

        protected void onPostExecute(String result) {
            //특수문자 제거
            String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
            result = result.replaceAll(match, " ");
            result = result.replaceAll(" ", "");
            if(result.length() >= 7 && result.length() <= 8) {
                textView.setText(result);
                Toast.makeText(MainActivity.this, "" + result, Toast.LENGTH_SHORT).show();
            }
            else {
                textView.setText("");
                Toast.makeText(MainActivity.this, "번호판 문자인식에 실패했습니다", Toast.LENGTH_LONG).show();
            }

            btnTakePicture.setEnabled(true);
            btnTakePicture.setText("텍스트 인식");
        }
    }

    protected  int getContourCount(Mat matContour, Mat matSubContour, Rect rcComp) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(matSubContour, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        int nCount = 0;
        float fHeight, fWidth;
        float fCompHeight = rcComp.height;
        for(int idx = 0; idx < contours.size(); idx++) {
            MatOfPoint matOfPoint = contours.get(idx);
            Rect rect = Imgproc.boundingRect(matOfPoint);
            //내부에서 찾으므로 아래조건 불필요
            //if(rcComp.x > rect.x || rcComp.y > rect.y || rcComp.x + rcComp.width < rect.x + rect.width || rcComp.y + rcComp.height < rect.y + rect.height)
            //    continue; // 번호판 내부에 있는지 체크
            fHeight = rect.height;
            fWidth = rect.width;
            if (rect.width > rect.height || fHeight / fWidth < 1.2 || fHeight / fWidth > 3.0 || fCompHeight / fHeight > 2.1 || fCompHeight / fHeight < 1.2)
                continue; // 글자유형 체크
            nCount++;
            //번호판 내부 글자유형에 표시
            Imgproc.rectangle(matContour, new Point(rcComp.x + rect.x, rcComp.y + rect.y), new Point(rcComp.x + rect.x + rect.width, rcComp.y + rect.y + rect.height), new Scalar(0, 0, 255), 2);
        }
        return nCount;
    }


}