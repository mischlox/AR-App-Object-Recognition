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
public class VideoStreamImp implements VideoStream {
    private static final String TAG = "VideoStream";

    @Override
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
}

