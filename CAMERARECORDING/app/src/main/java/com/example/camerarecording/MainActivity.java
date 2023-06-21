package com.example.camerarecording;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;


/*
* https://www.tutorialspoint.com/android/android_textureview.htm
* https://github.com/commonsguy/cw-advandroid/blob/master/Camera/Preview/src/com/commonsware/android/camera/PreviewDemo.java
* https://stackoverflow.com/questions/9238383/using-surfaceview-to-capture-a-video
* https://stackoverflow.com/questions/16852774/getdefaultdisplay-getrotation-returns-always-same-value
* https://stackoverflow.com/questions/24176463/difference-between-setvideoframerate-and-setcapturerate-mediarecorder-start-fa
* https://stackoverflow.com/questions/51332386/mediarecorder-and-videosource-surface-stop-failed-1007-a-serious-android-bug
* */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity  {

    private boolean cameraswitch_number = false;
    private boolean disableaudio = true;

    private Button startstream;
    private Button takepic;
    private Button switchcamera;

    private Button buttontotatecamera;

    private Button buttonmute;

    private SurfaceView cameraView;
    //private SurfaceHolder previewHolder;

    private MediaRecorder recorder;

    private Context ctx;

    int degrees =0;
    int rotation =0;

    int rotate = 0;

    int width  = 0;
    int height = 0;

    private boolean recordingv = false;

    private Camera mCamera ;

    private Camera.Parameters params;

    private SurfaceHolder.Callback callback;

    private void rotate_camera()
    {
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                rotation = rotation + 1;//getWindowManager().getDefaultDisplay().getRotation();
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                rotation = rotation + 1;//getWindowManager().getDefaultDisplay().getRotation();
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                rotation = rotation + 1;//getWindowManager().getDefaultDisplay().getRotation();
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                rotation =0;
                break;// Landscape right
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        int rotate = (info.orientation - degrees + 360) % 360;
        mCamera.setDisplayOrientation(rotate);
    }

    private void switchcameranow(int camera)
    {

        if(mCamera!=null)
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        /*
        *  necessary since there is no clean way to pass to media recorder
        * */
        recorder = new MediaRecorder();

        mCamera = Camera.open(camera);

        Camera.CameraInfo info = new Camera.CameraInfo();

        if(cameraswitch_number)
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, info);
        else
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);

        rotate = (info.orientation - getWindowManager().getDefaultDisplay().getRotation() + 360) % 360;

        mCamera.setDisplayOrientation(rotate);

        width = mCamera.getParameters().getSupportedPreviewSizes().get(0).width;
        height = mCamera.getParameters().getSupportedPreviewSizes().get(0).height;

        if(callback == null){

        }
        else
            cameraView.getHolder().removeCallback(callback);

        callback = new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                // no-op -- wait until surfaceChanged()
                //cameraView.getHolder()
                //cameraView.setPreviewDisplay(holder);
                //mCamera.setDisplayOrientation(rotate);

                try {
                    mCamera.setPreviewDisplay(cameraView.getHolder());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public void surfaceChanged(SurfaceHolder holder,
                                       int format, int width,
                                       int height) {
                try {
                    mCamera.setPreviewDisplay(cameraView.getHolder());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                cameraView.getHolder().setFixedSize(width,height);
                //   mCamera.setPreviewDisplay(holder);
                mCamera.setDisplayOrientation(rotate);
                //
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                // no-op
            }
        };

        /*Bizarre */
        try {
            mCamera.setPreviewDisplay(cameraView.getHolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        cameraView.getHolder().addCallback(callback);

        mCamera.startPreview();

        /*This is called before unlock camera
        * For some reason unlock can not be called after function here
        * */
        recorder.setCamera(mCamera);


    }

    /*
    * https://stackoverflow.com/questions/11485517/android-takepicture-failed
    * */
    private void takeapic()
    {

        Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onPictureTaken(byte[] data, Camera camera) {
                // Save the image JPEG data to the SD card
                FileOutputStream outStream = null;
                try {
                    File path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES);
                    path.mkdirs();
                    File file = new File(path, Instant.now().toEpochMilli() + ".jpeg");

                    outStream = new FileOutputStream(file);
                    outStream.write(data);
                    outStream.close();
                } catch (FileNotFoundException e) {
                    Log.d("CAMERA", e.getMessage());
                } catch (IOException e) {
                    Log.d("CAMERA", e.getMessage());
                }
                camera.startPreview();
            }
        };

        mCamera.takePicture(null,null,photoCallback);

    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = this;
        if (null == activity) {
            return;
        }

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        path.mkdirs();
        File file = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            file = new File(path, Instant.now().toEpochMilli() + ".mp4");
        }

        /*
        *  WOW, nice and broken feature
        * */
        mCamera.unlock();

        if(disableaudio)
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        /*
        *  SURFACE can only work using recorder.setInputSurface(MediaCodec.createPersistentInputSurface())
        *  CAMERA sux seems i have already a setup on previous .. but it replace with new one rotating to original :(
        * */
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        /*
        * This frame record seems broken ; it doesn't work and keep frozen on 24 fps whats sounds very bad.
        * */
       // recorder.setCaptureRate(30);
        recorder.setVideoFrameRate(60);
        recorder.setVideoSize(width,height);

        /*This works only  playback video*/
        recorder.setOrientationHint(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recorder.setOutputFile(file);
        }

        recorder.setVideoEncodingBitRate(15000000);
        /*
        * This is simple copy could be done by Camera it self ? surface changes when you try record video
        *
        *  1. recorder.setInputSurface(MediaCodec.createPersistentInputSurface())
        *  This should create a persistence Surface by given already Surface created ?
        *  Why a already configured camera can not passed to media record  ?
        *  i can set a preview by surfaceview but cant pass it to media record
        *
        * This first generation camera is good , but things have no good examples and missing friendly pass from a class to others config
        * where it could be more messy on version 2
        *
        *
        *  Spy less people and do better code and easy to use.
        *
        * */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             recorder.setPreviewDisplay(cameraView.getHolder().getSurface());
        }

        if(disableaudio)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    }

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
        buttontotatecamera = findViewById(R.id.buttontotatecamera);

        cameraView  = findViewById(R.id.surfaceView);

        switchcameranow(Camera.CameraInfo.CAMERA_FACING_BACK);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        buttontotatecamera.setOnClickListener((new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                rotate_camera();
            }

        }));


        startstream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!recordingv) {
                    try {
                       // recorder = new MediaRecorder();
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
                takeapic();
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