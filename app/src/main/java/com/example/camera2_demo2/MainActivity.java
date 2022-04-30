package com.example.camera2_demo2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
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
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private final String[] Permission_need =
            {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION};

    //后置摄像头信息
    private String backCameraId;
    private CameraCharacteristics backCharacteristics;
    //前置摄像头信息
    private String frontCameraId;
    private CameraCharacteristics frontCharacteristics;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession mCaptureSession;
    private com.example.camera2_demo2.textureView camera_preview;

    private List<Surface> surfaceList = new ArrayList<>();
    private CaptureRequest.Builder requestBuilder;
    private CameraCharacteristics cameraCharacteristics;

    private Surface previewSurface;//用于预览的surface

    private SurfaceTexture mSurfaceTexture;

    private Button btn_take;
    private TextView take;
    private TextView recode;
    private TextView bph;
    private TextView dj;
    private TextView kmsj;
    private TextView iso;

    private TextView seek_dj_tx;
    private TextView seek_kmsj_tx;
    private TextView seek_iso_tx;

    private SeekBar seek_dj;
    private SeekBar seek_kmsj;
    private SeekBar seek_iso;

    private LinearLayout dd;
    private LinearLayout zidong;
    private LinearLayout bcd;
    private LinearLayout yy;
    private LinearLayout qt;
    private LinearLayout yt;
    private LinearLayout ly_setAll;

    private boolean flag = false;
    private boolean btn_flag = false;
    private Size size;

    private ImageReader imageReader;
    private Surface jpegPreviewSurface;//拍照时用于获取数据的surface
    private CaptureRequest.Builder captureImageRequestBuilder;//用于拍照的 CaptureRequest.Builder对象

    private String[] km_time = {"1/500","1/250","1/125","1/60","1/30","1/15","1/8","1/4","1/2","1","2","4","8","16"};
    private long[] km_time_in = {2000000 ,4000000 ,8000000 ,10000000 ,20000000 ,40000000 ,125000000 ,250000000 ,500000000 ,1000000000 ,2000000000 ,4000000000L ,8000000000L ,16000000000L };


    private CaptureRequest.Builder setBuilder;

    View ly_bph;
    View ly_dj;
    View ly_kmsj;
    View ly_iso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkRequiredPermissions();
        initCamera2();
        openCamera();
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();// 去掉标题栏
        initView();
        initEvent();

        camera_preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                mSurfaceTexture = surfaceTexture;
                startPreview();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        });

    }

    private void initView(){
        take = findViewById(R.id.tx_take);
        recode = findViewById(R.id.tx_recode);
        camera_preview = findViewById(R.id.camera_preview);
        btn_take = findViewById(R.id.btn_take);
        bph = findViewById(R.id.bph);
        dj = findViewById(R.id.dj);
        kmsj = findViewById(R.id.kmsj);
        iso = findViewById(R.id.iso);
        dd = findViewById(R.id.dd);

        ly_bph = View.inflate(this,R.layout.layout_bph,null);
        ly_dj = View.inflate(this,R.layout.layout_dj,null);
        ly_kmsj = View.inflate(this,R.layout.layout_kmsj,null);
        ly_iso = View.inflate(this,R.layout.layout_iso,null);


        seek_dj_tx = ly_dj.findViewById(R.id.seek_dj_tx);
        seek_kmsj_tx = ly_kmsj.findViewById(R.id.seek_kmsj_tx);
        seek_iso_tx = ly_iso.findViewById(R.id.seek_iso_tx);

        ly_setAll = findViewById(R.id.cc);

    }

    private void initEvent(){
        take.setOnClickListener(this);
        recode.setOnClickListener(this);
        btn_take.setOnClickListener(this);
        findViewById(R.id.ln_bph).setOnClickListener(this);
        findViewById(R.id.ln_dj).setOnClickListener(this);
        findViewById(R.id.ln_kmsj).setOnClickListener(this);
        findViewById(R.id.ln_iso).setOnClickListener(this);
        findViewById(R.id.btn_reset).setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tx_take:
                take.setTextColor(Color.parseColor("#1E90FF"));
                recode.setTextColor(Color.parseColor("#ffffff"));
                ly_setAll.setVisibility(View.VISIBLE);
                btn_flag = false;
                break;
            case R.id.tx_recode:
                take.setTextColor(Color.parseColor("#ffffff"));
                recode.setTextColor(Color.parseColor("#1E90FF"));
                ly_setAll.setVisibility(View.INVISIBLE);
                btn_flag = true;
                break;
            case R.id.btn_take:
                if (btn_flag){
                    if (flag){
                        stopRecode();
                        btn_take.setBackgroundResource(R.drawable.round_button);
                        flag = false;
                    }else {
                        startRecode();
                        btn_take.setBackgroundResource(R.drawable.round_recoder_button);
                        flag = true;
                    }
                }else {
                    System.out.println("awdawdawdawdadw开拍！");
                    takePic();
                }
                break;
            case R.id.ln_bph:
                dd.removeAllViews();
                set_mode();
                zidong = ly_bph.findViewById(R.id.zidong);
                bcd = ly_bph.findViewById(R.id.bcd);
                yy = ly_bph.findViewById(R.id.yy);
                qt = ly_bph.findViewById(R.id.qt);
                yt = ly_bph.findViewById(R.id.yt);
                setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE);
                try {
                    mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                zidong.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                bcd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,2);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                yy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,8);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                qt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,5);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                yt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_AWB_MODE,6);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                dd.addView(ly_bph);
                break;
            case R.id.ln_dj:
                dd.removeAllViews();
                set_mode();
                seek_dj = ly_dj.findViewById(R.id.seek_dj);
                seek_dj.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        seek_dj_tx.setText(""+i);
                        dj.setText(""+i);
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE);
                            setBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE,(float)i);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                dd.addView(ly_dj);
                break;
            case R.id.ln_kmsj:
                dd.removeAllViews();
                set_mode();
                seek_kmsj = ly_kmsj.findViewById(R.id.seek_kmsj);
                seek_kmsj.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        seek_kmsj_tx.setText(km_time[i]);
                        kmsj.setText(km_time[i]);
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF);
                            setBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,km_time_in[i]);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                dd.addView(ly_kmsj);
                break;
            case R.id.ln_iso:
                dd.removeAllViews();
                set_mode();
                seek_iso = ly_iso.findViewById(R.id.seek_iso);
                seek_iso.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        seek_iso_tx.setText(""+i*100);
                        iso.setText(""+i*100);
                        if (setBuilder != null){
                            setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF);
                            setBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,i*100);
                            try {
                                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                dd.addView(ly_iso);
                System.out.println("adadwadadad");
                break;
            case R.id.btn_reset:
                bph.setText("自动");
                dj.setText("自动");
                kmsj.setText("自动");
                iso.setText("自动");
                reset();
                dd.removeAllViews();
                break;
        }
    }

    private void reset(){
        seek_dj_tx.setText("自动");
        seek_kmsj_tx.setText("自动");
        seek_iso_tx.setText("自动");
        if (setBuilder != null){
            try {
                setBuilder = null;
                setBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        setBuilder.addTarget(previewSurface);
        setBuilder.addTarget(previewDataSurface);
        try {
            mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void set_mode(){
        if (setBuilder != null){
            setBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_OFF_KEEP_STATE);
            setBuilder.addTarget(previewSurface);
            setBuilder.addTarget(previewDataSurface);
            try {
                mCaptureSession.setRepeatingRequest(setBuilder.build(), null,null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Surface previewDataSurface;

    //创建一个用于接收预览数据的surface，防止用于预览的surface在拍照后卡顿
    private void getPreviewSurface() {
        ImageReader previewDataImageReader = ImageReader.newInstance(camera_preview.getWidth(), camera_preview.getHeight(), ImageFormat.YUV_420_888, 5);
        previewDataImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireNextImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    Image.Plane yPlane = planes[0];
                    Image.Plane uPlane = planes[1];
                    Image.Plane vPlane = planes[2];
                    ByteBuffer yBuffer = yPlane.getBuffer(); // Data from Y channel
                    ByteBuffer uBuffer = uPlane.getBuffer(); // Data from U channel
                    ByteBuffer vBuffer = vPlane.getBuffer(); // Data from V channel
                }
                if (image != null) {
                    image.close();
                }
            }
        }, null);
        previewDataSurface = previewDataImageReader.getSurface();
    }

    private Surface mediaSurface;

    private void startPreview(){
        closeSession();
        getPreviewSurface();
        getImgReader();
        try {
            setUpMediaRecorder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaSurface = mMediaRecorder.getSurface();
        //设置预览页面1,获取相机支持的size并和surfaceTexture比较选出适合的
        size = getSupportSize();
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());//设置surface的大小，控件texture大小要与surface匹配
        previewSurface = new Surface(mSurfaceTexture);
        surfaceList.add(previewSurface);
        surfaceList.add(previewDataSurface);
        surfaceList.add(jpegPreviewSurface);
        surfaceList.add(mediaSurface);
        try {
            setBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
            requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //CaptureRequest必须指定一个或多个surface,可以多次调用方法添加
            requestBuilder.addTarget(previewSurface);
            requestBuilder.addTarget(previewDataSurface);
            System.out.println("可用光圈值为:"+Arrays.toString(cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)));
            System.out.println("可用自动白平衡模式为:"+Arrays.toString(cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)));
            System.out.println("可用曝光值范围为:"+cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE));
            System.out.println("可调节曝光值步长为:"+cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP));
            System.out.println("可用自动对焦模式为:"+ Arrays.toString(cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)));
            System.out.println("最小焦距为:"+ cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE));
            System.out.println("超焦距为:"+ cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE));
            System.out.println("可用曝光时间:"+ cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE));
            System.out.println("可用ISO:"+ cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                System.out.println("可用放缩比例范围为:"+cameraCharacteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE));
            }
            System.out.println("可用边缘增强模式为:"+ Arrays.toString(cameraCharacteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES)));
            //创建CaptureSession
            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    try {
                        mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    //在handler中开启相机
    Handler cameraHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        checkRequiredPermissions();
                        Toast.makeText(MainActivity.this, "开个相机权限行不行？", Toast.LENGTH_SHORT).show();
                    } else {
                        cameraManager.openCamera(backCameraId, mStateCallback, null);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            if (msg.what == 1) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        checkRequiredPermissions();
                        Toast.makeText(MainActivity.this, "开个相机权限行不行？", Toast.LENGTH_SHORT).show();
                    } else {
                        cameraManager.openCamera(frontCameraId, mStateCallback, null);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //申请相机权限
    private boolean checkRequiredPermissions() {
        if (!isPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(Permission_need, 1);//如果权限未授权，则申请授权
            shouldShowRequestPermissionRationale("该权限将用于手机拍照录像和存储功能，若拒绝则运行像地狱");//显示权限信息
        }
        return isPermission();
    }

    //检查相机权限
    public boolean isPermission() {
        for (String permission : Permission_need) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 初始化相机
     * 1.获取实例
     * 2.获取设备列表
     * 3.获取各个摄像头可控等级
     * 4.筛选前后摄像头
     */
    private void initCamera2() {
        //创建CameraManager实例
        cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        //获取相机设备ID列表
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) { //检查设备可控等级和筛选前后摄像头
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (isHardwareLevelSupported(characteristics)) {
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = cameraId;
                        backCharacteristics = characteristics;
                        break;
                    } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        frontCameraId = cameraId;
                        frontCharacteristics = characteristics;
                        break;
                    }
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    //检查该cameraID的可控等级是否达到INFO_SUPPORTED_HARDWARE_LEVEL_FULL或以上
    private boolean isHardwareLevelSupported(CameraCharacteristics characteristics) {
        int requiredLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        int[] levels = {CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3};
        for (int i = 0; i < 5; i++) {
            if (requiredLevel == levels[i]) {
                if (i > 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private void openCamera() {
        if (backCameraId != null) {
            Message message_back = Message.obtain();
            message_back.what = 0;
            cameraHandler.sendMessage(message_back);
        } else if (frontCameraId != null) {
            Message message_front = Message.obtain();
            message_front.what = 1;
            cameraHandler.sendMessage(message_front);
        } else {
            Toast.makeText(this, "宁的摄像头有、问题", Toast.LENGTH_SHORT).show();
        }
    }

    //创建相机回调
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //打开成功，可以获取CameraDevice对象
            cameraDevice = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //断开连接
        }

        @Override
        public void onError(@NonNull CameraDevice camera, final int error) {
            //发生异常
        }
    };

    //获取支持的分辨率大小
    private Size getSupportSize() {
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(backCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        int aspectRatio = 4/3;//寻找size为3：4且不超过texture宽高的size
        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap != null) {
            Size[] outputSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
            if (outputSizes != null) {
                for (Size size : outputSizes) {
                    if (size.getWidth() / size.getHeight() == aspectRatio && size.getHeight() <= camera_preview.getHeight() && size.getWidth() <= camera_preview.getWidth()) {
                        return size;
                    }
                }
            }
        }
        return null;
    }

    //使用ImageReader创建一个用于保存照片的surface
    private void getImgReader() {
        //创建ImageReader，用于创建保存预览的surface
        imageReader = ImageReader.newInstance(camera_preview.getWidth(), camera_preview.getHeight(), ImageFormat.JPEG, 5);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                new Thread(new ImageSaver(image)).start();
            }
        }, null);
        jpegPreviewSurface = imageReader.getSurface();
    }

    //设置屏幕方向
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private void closeSession(){
        if (mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void takePic() {
        try {
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            setBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            setBuilder.addTarget(previewDataSurface);
            setBuilder.addTarget(jpegPreviewSurface);
            mCaptureSession.capture(setBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private MediaRecorder mMediaRecorder = new MediaRecorder();
    private CaptureRequest.Builder mPreviewBuilder;


    private void setUpMediaRecorder() throws IOException {
        size = getSupportSize();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //设置用于录制的音源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);//开始捕捉和编码数据到setOutputFile（指定的文件）
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //设置在录制过程中产生的输出文件的格式

        mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getCanonicalFile() + "/myRadio1.mp4");//设置输出文件的路径
        mMediaRecorder.setVideoEncodingBitRate(10000000);//设置录制的视频编码比特率
        mMediaRecorder.setVideoFrameRate(30);//设置要捕获的视频帧速率

        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());//设置要捕获的视频的宽度和高度
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//设置视频编码器，用于录制
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//设置audio的编码格式
        int rotation = MainActivity.this.getWindowManager().getDefaultDisplay().getRotation();
        mMediaRecorder.setOrientationHint(ORIENTATION.get(rotation));
        mMediaRecorder.prepare();
    }

    private void startRecode(){
        try {
            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(previewSurface);
        mPreviewBuilder.addTarget(mediaSurface);
        try {
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //开启录像
                mMediaRecorder.start();
            }
        });
    }

    private void stopRecode(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        try {
            mCaptureSession.setRepeatingRequest(requestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //保存照片
    private static class ImageSaver implements Runnable {
        private Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            System.out.println("wdawdawdad" + mImage);
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File mImageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/myPicture.jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImageFile = null;
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}