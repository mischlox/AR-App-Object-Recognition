package com.example.arora;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

/**
 * Represents a Video Stream
 */
public interface VideoStream {

    boolean detectCamera(Context context);

    static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
            if (c != null) Log.d("success", "camera opened successfully");
        }
        catch (Exception e) {
            Log.d("exception", e.getMessage());
        }

        return c;
    }
}
