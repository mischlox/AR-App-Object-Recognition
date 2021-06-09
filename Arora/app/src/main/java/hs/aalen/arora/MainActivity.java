package hs.aalen.arora;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Main Activity handles camera permission request,
 * welcomes the user and is the entrypoint for the Camera Activity
 *
 * @author Michael Schlosser
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyPermissions();
        Button startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
            }
        });

        Button experimentalButton = findViewById(R.id.experimentalButton);
        experimentalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
                String table = dbHelper.tableToString();
                Log.d(TAG, "TABLE INFO: " +  table);
            }
        });
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
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
    }



}