package hs.aalen.arora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import hs.aalen.arora.dialogues.DialogFactory;
import hs.aalen.arora.dialogues.DialogType;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

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

        // set night mode
        if (getSharedPreferences("prefs", MODE_PRIVATE).getBoolean(getString(R.string.key_nightmode), false)) {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        verifyPermissions();

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CameraActivity.class)));

        Button experimentalButton = findViewById(R.id.experimentalButton);
        experimentalButton.setOnClickListener(v -> {
//                DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
//                String model_table = dbHelper.tableToString("model_table");
//                String object_table = dbHelper.tableToString("object_table");
//                Log.d(TAG, "TABLE INFO:\n" + model_table + object_table);
            DialogFactory.getDialog(DialogType.INFO).createDialog(MainActivity.this);
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
        if (!areGranted) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
    }
}