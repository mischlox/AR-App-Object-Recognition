package com.example.arora;

import android.hardware.Camera;
import android.icu.text.SimpleDateFormat;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Video Recorder Singleton class
 */
public class VideoRecorder extends MediaRecorder {
    private static final String TAG = "VideoRecorder";
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private static final int FORMAT_WIDTH = 1920;
    private static final int FORMAT_HEIGHT = 1080;

    private static VideoRecorder instance;

    public static VideoRecorder getInstance() {
        if(instance == null) {
            instance = new VideoRecorder();
        }
        return instance;
    }

    /**
     * Release videoRecorder and lock camera  for later use
     * @param camera camera instance
     */
    public void releaseVideoRecorder(Camera camera){
        if (instance != null) {
            instance.reset();   // clear recorder configuration
            instance.release(); // release the recorder object
            instance = null;
            camera.lock();      // lock camera for later use
        }
    }

    /**
     * Function to prepare the Video Recorder
     * Sets all needed parameters
     * @return Success or not
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean prepareVideoRecorder(Camera camera, CameraPreview preview) {https://trendoceans.com/wp-content/uploads/2021/02/image-12.png
    camera = VideoStream.getCameraInstance();
        camera.setDisplayOrientation(90);

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        instance.setCamera(camera);

        // Step 2: Set sources
        instance.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Set a CamcorderProfile (requires API Level 8 or higher)
        instance.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        instance.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        instance.setVideoSize(FORMAT_WIDTH,FORMAT_HEIGHT);
        // Set output file
        instance.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        // Set the preview output
        instance.setPreviewDisplay(preview.getHolder().getSurface());
        // Rotate view
        instance.setOrientationHint(90);
        // Prepare configured MediaRecorder
        try {
            instance.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseVideoRecorder(camera);
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseVideoRecorder(camera);
            return false;
        }
        return true;
    }

    /** Create a File for saving an image or video */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        Log.d(TAG, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().toString());
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
