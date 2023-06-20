package com.gladic.simplevideorecorder;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.camera2.CameraManager;
import android.icu.text.SimpleDateFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.RecordingStats;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.impl.VideoCaptureConfig;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/*
 * https://www.programcreek.com/java-api-examples/?code=uestccokey%2FEZFilter%2FEZFilter-master%2Fapp%2Fsrc%2Fmain%2Fjava%2Fcn%2Fezandroid%2Fezfilter%2Fdemo%2FCamera2FilterActivity.java#
 * https://github.com/Jiankai-Sun/Android-Camera2-API-Example/blob/master/app/src/main/java/com/jack/mainactivity/MainActivity.java
 * https://github.com/7eau/CameraXDemo_Java/blob/master/app/src/main/java/com/awo/newcameraxtest/MainActivity.java
 * */
public class MainActivity extends AppCompatActivity {

    boolean defCamera = true;
    private CameraSelector cameraSelector;

    private ProcessCameraProvider cameraProvider;

    private PreviewView previewView;
    ImageView imageHolder;
    private Button buttonswitchcamera;
    private Button buttonstartstream;

    private Preview preview;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;

    private CameraManager mCameraManager;
    private Recorder recorder;
    private Recording recording;
    private  RecordingStats recordingStats;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.videocamerapreview);
        assert previewView != null;

        buttonstartstream = findViewById(R.id.buttonstartstream);
        buttonswitchcamera = findViewById(R.id.buttonswitchcamera);

        buttonstartstream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCameraX();
            }
        });

        buttonswitchcamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    private void startCameraX() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));


    }

    @SuppressLint("MissingPermission")
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();

        if (defCamera) {
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

        } else {
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();
        }
        ;

        //Preview Use case
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        //Video Capture
        QualitySelector qualitySelector = QualitySelector.fromOrderedList(
                Arrays.asList(Quality.FHD, Quality.HD, Quality.HIGHEST)
        );

        /*this is described on class usage with VideoCapture */
        recorder = new Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build();

        /*
        * Those are content values , where is the option to enable audio here ?
        *  Examples on java should be more elaborated to we avoid ducking around
        * */
        ContentValues contentValues;

        contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "SIMPLE_VIDEO_NO_SOUND");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

        ContentResolver contentResolver = getContentResolver();

        //This should be used as option to save data on sdcard or other place without much effort, but with lack of good examples is a bit hard figure out
        /*
        FileOutputOptions foptions = new  FileOutputOptions.Builder(new File("data/test.mp4")).setDurationLimitMillis(1111111).setFileSizeLimit(90000000)
                .build();
        */


        /*
        *  Where to enable audio stuff ?
        *  There is a way to set stuff to write on samba system files or other related network shared files stuff ?
        * */
        MediaStoreOutputOptions options =
                new MediaStoreOutputOptions.Builder(
                        contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        .setContentValues(contentValues)
                        .build();

        /*
         recording = recorder.prepareRecording(this, options)
                .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                        // Handle the start of a new active recording

                    } else if (videoRecordEvent instanceof VideoRecordEvent.Pause) {
                        // Handle the case where the active recording is paused

                    } else if (videoRecordEvent instanceof VideoRecordEvent.Resume) {
                        // Handles the case where the active recording is resumed

                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent =
                                (VideoRecordEvent.Finalize) videoRecordEvent;
                        // Handles a finalize event for the active recording, checking Finalize.getError()
                        int error = finalizeEvent.getError();
                        if (error != VideoRecordEvent.Finalize.ERROR_NONE) {

                        }
                    }

                    // All events, including VideoRecordEvent.Status, contain RecordingStats.
                    // This can be used to update the UI or track the recording duration.
                    recordingStats = videoRecordEvent.getRecordingStats();
                });
        */
        videoCapture = VideoCapture.withOutput(recorder);

        //ImageCapture use case
       /* imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        */

        //cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
    }

    private void switchCamera() {

        defCamera = !defCamera;
       // cameraProviderFuture.cancel(true);
        cameraProviderFuture.cancel(true);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try
                {
                    cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

}