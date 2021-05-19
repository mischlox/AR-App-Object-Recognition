package com.example.arora;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.arora.ui.main.CameraFragmentViewModelFragment;

public class CameraFragmentViewModel extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_fragment_view_model_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraFragmentViewModelFragment.newInstance())
                    .commitNow();
        }
    }
}