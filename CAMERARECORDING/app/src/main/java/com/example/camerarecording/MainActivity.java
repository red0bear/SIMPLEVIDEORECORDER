package com.example.camerarecording;


import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;


/*
* https://www.tutorialspoint.com/android/android_textureview.htm
* */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity  {
    boolean recording=false;
    private Button startstream;
    private Button takepic;
    private Button switchcamera;

    private TextureView cameraView;

    private MediaRecorder recorder;

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        return(result);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = this;
        if (null == activity) {
            return;
        }

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        File file = new File(path, "teste.mp4");

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        recorder.setOutputFile(file);
        recorder.setVideoEncodingBitRate(10000000);
        recorder.setVideoFrameRate(30);
        //  recorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        //recorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));

        recorder.prepare();
    }

    private TextureView myTexture;
    private Camera mCamera;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTexture = new TextureView(this);

        startstream = findViewById(R.id.buttonstartstream);
        takepic = findViewById(R.id.buttontakepic);
        switchcamera = findViewById(R.id.buttonswitchcamera);
        cameraView  = findViewById(R.id.surfaceView);


        cameraView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                                  int width, int height) {
                mCamera = Camera.open();
                Camera.Parameters parameters=mCamera.getParameters();
                //Camera.Size previewSize = mCamera.getParameters().getSupportedPreviewSizes().get(0).width.getPreviewSize();
                parameters.setPreviewSize(mCamera.getParameters().getSupportedPreviewSizes().get(0).width, mCamera.getParameters().getSupportedPreviewSizes().get(0).height);
                mCamera.setParameters(parameters);

                myTexture.setSurfaceTexture(surfaceTexture);

                Size viewSize = new Size(width, height);
                Size videoSize = new Size(mCamera.getParameters().getSupportedPreviewSizes().get(0).width, mCamera.getParameters().getSupportedPreviewSizes().get(0).height);
                ScaleManager scaleManager = new ScaleManager(viewSize, videoSize);
                Matrix matrix = scaleManager.getScaleMatrix(ScalableType.NONE);
                if (matrix != null) {
                    myTexture.setTransform(matrix);
                }

                myTexture.setLayoutParams(new FrameLayout.LayoutParams(
                        mCamera.getParameters().getSupportedPreviewSizes().get(0).width, mCamera.getParameters().getSupportedPreviewSizes().get(0).height, Gravity.CENTER));

                try {
                    mCamera.setPreviewTexture(surfaceTexture);
                } catch (IOException t) {
                }

                mCamera.startPreview();

               myTexture.setAlpha(1.0f);
               myTexture.setRotation(90.0f);

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                    int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                mCamera.stopPreview();
                mCamera.release();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }

        });

        recorder = new MediaRecorder();
        try {
            setUpMediaRecorder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        startstream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        takepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();

        mCamera=Camera.open();
        //startPreview();
    }

    @Override
    public void onPause() {
        //if (inPreview) {
            mCamera.stopPreview();
        //}

        mCamera.release();
        mCamera=null;
        //inPreview=false;

        super.onPause();
    }
}