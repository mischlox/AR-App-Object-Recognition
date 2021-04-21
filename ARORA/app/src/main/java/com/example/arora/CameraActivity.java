package com.example.arora;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.VideoView;

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        VideoView camView = (VideoView)findViewById(R.id.camView);

        camView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kittentest));
        camView.start();
    }
}