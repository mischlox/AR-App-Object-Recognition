package com.example.arora;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;


/**
 * Activity with Embedded Camera View and all Informations needed
 * */
public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";


    private Camera mCamera;
    private CameraPreview mPreview;
    private VideoRecorder videoRecorder = VideoRecorder.getInstance();
    private Boolean isRecording = false;

    private Button trainingButton;

    @Override
    protected void onPause() {
        super.onPause();
        videoRecorder.releaseVideoRecorder(mCamera);       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        verifyPermissions();
        mCamera = VideoStream.getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camView);
        preview.addView(mPreview);

        trainingButton = (Button) findViewById(R.id.trainingButton);
        trainingButton.setOnClickListener(
                new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onClick(View v) {
                        if (isRecording) {
                            stopRecording();
                        }
                        else {
                            startRecording();
                        }
                    }
                }
        );
    }

    /**
     * Asks for permission if it is not granted for Read/Write or Camera
     */
    private void verifyPermissions() {
        Log.d(TAG, "verifyPermissions: asking user for permissions");
        String[] permissions = {Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE};
        boolean areGranted = true;
        for (String permission : permissions) {
            int isGranted = ContextCompat.checkSelfPermission(this.getApplicationContext(), permission);
            if (isGranted != PackageManager.PERMISSION_GRANTED) areGranted = false;
        }
        if(!areGranted){
            ActivityCompat.requestPermissions(CameraActivity.this, permissions, 1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startRecording() {
        // initialize video camera
        if (videoRecorder.prepareVideoRecorder(mCamera, mPreview)) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            videoRecorder.start();

            // inform the user that recording has started
            trainingButton.setText("Stop");
            isRecording = true;
        } else {
            // prepare didn't work, release the camera
            videoRecorder.releaseVideoRecorder(mCamera);
            // inform user
            Log.d(TAG, "Start Recording failed");
        }
    }

    /**
     * Stop recording and release camera
     */
    private void stopRecording() {
        videoRecorder.stop();
        videoRecorder.releaseVideoRecorder(mCamera);
        mCamera.lock();
        Log.d(TAG, "Recording has stopped");
        trainingButton.setText("Train");
        isRecording = false;
    }

}