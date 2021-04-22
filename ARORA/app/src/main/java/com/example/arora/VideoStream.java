package com.example.arora;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

/**
 * Implementation of the Videostream Class
 */
public class VideoStream {
    private static final String TAG = "VideoStream";

    public boolean detectCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // TODO: Implement this device has camera
            Log.d(TAG, "device detected successfully");
            return true;
        } else {
            // TODO: Implement this device has NO camera
            Log.d(TAG, "No device found!");
            return false;
        }
    }

    static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
            if (c != null) Log.d("success", "camera opened successfully");
        }
        catch (Exception e) {
            Log.d("exception", e.getMessage());
        }
        configureCamera(c);
        return c;
    }

    /**
     * Configure Parameters for camera
     * @param camera Camera that is configured
     */
    private static void configureCamera(Camera camera) {
        // TODO: Check if these parameters have a big impact on performance
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setVideoStabilization(true);
        camera.setParameters(parameters);
    }
}

