package com.example.camerarecording;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.time.Instant;


/*
* https://www.tutorialspoint.com/android/android_textureview.htm
* https://github.com/commonsguy/cw-advandroid/blob/master/Camera/Preview/src/com/commonsware/android/camera/PreviewDemo.java
* https://stackoverflow.com/questions/9238383/using-surfaceview-to-capture-a-video
* https://stackoverflow.com/questions/16852774/getdefaultdisplay-getrotation-returns-always-same-value
* */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity  {

    private boolean cameraswitch_number = false;
    private boolean disableaudio = true;

    private Button startstream;
    private Button takepic;
    private Button switchcamera;

    private Button buttonmute;

    private SurfaceView cameraView;
    private  SurfaceHolder previewHolder;

    private MediaRecorder recorder;

    private Context ctx;


    private boolean recordingv = false;

    private Camera mCamera ;

    private SurfaceHolder.Callback callback;

    private void switchcameranow(int camera)
    {

        if(mCamera!=null)
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        recorder = new MediaRecorder();
        mCamera=Camera.open(camera);

        try {
            mCamera.setPreviewDisplay(previewHolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        int rotate = (info.orientation - degrees + 360) % 360;
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(rotate);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();

        if(callback == null){

        }
        else
            previewHolder.removeCallback(callback);

        callback = new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                // no-op -- wait until surfaceChanged()
                try {
                    mCamera.setPreviewDisplay(previewHolder);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public void surfaceChanged(SurfaceHolder holder,
                                       int format, int width,
                                       int height) {
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                // no-op
            }
        };

        previewHolder.addCallback(callback);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = this;
        if (null == activity) {
            return;
        }

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        path.mkdirs();
        File file = new File(path, Instant.now().toEpochMilli() + ".mp4");

        if(disableaudio)
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        recorder.setOutputFile(file);
        //recorder.setVideoEncodingBitRate(10000000);
        recorder.setVideoFrameRate(60);
        recorder.setPreviewDisplay(cameraView.getHolder().getSurface());
        //recorder.setVideoSize(mCamera.getParameters().getSupportedPreviewSizes().get(0).width,mCamera.getParameters().getSupportedPreviewSizes().get(0).height );
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

        if(disableaudio)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD);
        // int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        //recorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
    }

    private TextureView myTexture;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this.getApplicationContext();

        startstream = findViewById(R.id.buttonstartstream);
        takepic = findViewById(R.id.buttontakepic);
        switchcamera = findViewById(R.id.buttonswitchcamera);

        buttonmute= findViewById(R.id.buttonmute);

        cameraView  = findViewById(R.id.surfaceView);
        previewHolder = cameraView.getHolder();

        recorder = new MediaRecorder();
        switchcameranow(Camera.CameraInfo.CAMERA_FACING_BACK);

        startstream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!recordingv) {
                    try {
                        recorder = new MediaRecorder();
                        setUpMediaRecorder();
                        recorder.prepare();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    recorder.start();
                    recordingv = true;

                    startstream.setText("STOP");
                }else
                {

                    recordingv = false;
                    recorder.stop();
                    recorder.release();
                    recorder = null;

                    if(cameraswitch_number) {

                        switchcameranow(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    }
                    else {
                        switchcameranow(Camera.CameraInfo.CAMERA_FACING_BACK);
                    }

                    startstream.setText("START STREAM");
                }
            }
        });

        buttonmute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableaudio =! disableaudio;

                if(disableaudio)
                    buttonmute.setText("MUTE MIC");
                else
                    buttonmute.setText("MUTED");

            }
        });

        takepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        switchcamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                cameraswitch_number=!cameraswitch_number;

                if(cameraswitch_number) {

                    switchcameranow(Camera.CameraInfo.CAMERA_FACING_FRONT);
                }
                else {
                    switchcameranow(Camera.CameraInfo.CAMERA_FACING_BACK);
                }

            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        mCamera.startPreview();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }
}