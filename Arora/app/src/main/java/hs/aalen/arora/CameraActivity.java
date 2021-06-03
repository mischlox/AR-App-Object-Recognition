package hs.aalen.arora;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

/**
 * Main Activity of Application handles Navigation between all fragments
 *
 * @author Michael Schlosser
 */
public class CameraActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {
    // Fragments as single instance in order to not recreate them
    private static final String TAG = "CameraActivity";

    private final CameraFragment cameraFragment = new CameraFragment();
    private final ObjectOverviewFragment objectOverviewFragment = new ObjectOverviewFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private final StatisticsFragment statisticsFragment = new StatisticsFragment();
    private final HelpFragment helpFragment = new HelpFragment();
    FloatingActionButton buttonCamera;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Fragment selectedFragment = cameraFragment;

    // Add Object Dialog items
    private AlertDialog.Builder addObjectDialogBuilder;
    private AlertDialog addObjectDialog;
    private EditText dialogObjectName;
    private EditText dialogObjectType;
    private EditText dialogObjectAdditionalData;
    private Button cancelDialogButton, startTrainingButton;

    // For Database Access
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);

    // Further configuration
    private int amountSamples = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: entrypoint");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Bind navigation views
        setSupportActionBar(toolbar);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_nav_view);

        bottomNavigationView.setBackground(null);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        buttonCamera = findViewById(R.id.nav_bottom_camera_button);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If not in Camera Fragment, go into it
                if (selectedFragment != cameraFragment) {
                    buttonCamera.setImageResource(R.drawable.ic_add);
                    selectedFragment = cameraFragment;
                    setTitle(navigationView.getMenu().findItem(R.id.nav_camera).getTitle());
                    syncNavBars(R.id.nav_camera, R.id.nav_bottom_placeholder);
                    getSupportFragmentManager().beginTransaction().replace(R.id.navbar_container, selectedFragment).commit();
                } else {
                    // Start inference/training dialog from here
                    createAddObjectDialog();
                }
            }
        });
        drawer = findViewById(R.id.drawer_layout);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.navbar_container, cameraFragment).commit();
            navigationView.setCheckedItem(R.id.nav_camera);
            bottomNavigationView.setSelectedItemId(R.id.nav_bottom_placeholder);
        }
    }

    /**
     * Overridden method for bottom and side navigation in one
     * Changes fragment on a click and syncs both navigation bars
     *
     * @param   item item of the menu
     * @return  true if successful
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_camera:
                buttonCamera.setImageResource(R.drawable.ic_add);
                syncNavBars(R.id.nav_camera, R.id.nav_bottom_placeholder);
                selectedFragment = cameraFragment;
                break;
            case R.id.nav_object_overview:
            case R.id.nav_bottom_object_overview:
                buttonCamera.setImageResource(R.drawable.ic_eye);
                syncNavBars(R.id.nav_object_overview, R.id.nav_bottom_object_overview);
                selectedFragment = objectOverviewFragment;
                break;
            case R.id.nav_settings:
            case R.id.nav_bottom_settings:
                buttonCamera.setImageResource(R.drawable.ic_eye);

                // Apply preferences
                amountSamples = settingsFragment.getNumSamples();
                Log.d(TAG, "sharedPrefs: amountSamples = " + amountSamples);

                syncNavBars(R.id.nav_settings, R.id.nav_bottom_settings);
                selectedFragment = settingsFragment;
                break;
            case R.id.nav_statistics:
            case R.id.nav_bottom_statistics:
                buttonCamera.setImageResource(R.drawable.ic_eye);
                syncNavBars(R.id.nav_statistics, R.id.nav_bottom_statistics);
                selectedFragment = statisticsFragment;
                break;
            case R.id.nav_help:
            case R.id.nav_bottom_help:
                buttonCamera.setImageResource(R.drawable.ic_eye);
                syncNavBars(R.id.nav_help, R.id.nav_bottom_help);
                selectedFragment = helpFragment;
                break;
            case R.id.nav_dark_mode:
                Toast.makeText(this, "To be implemented: Toggle Dark Mode", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_delete:
                Toast.makeText(this, "To be implemented: Delete all Objects", Toast.LENGTH_SHORT).show();
                break;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (selectedFragment != null) {
            transaction.replace(R.id.navbar_container, selectedFragment);
            transaction.commit();
            setTitle(item.getTitle());
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Helper function that selects bottom navigation icon when side navigation icon is clicked
     * and vice versa
     *
     * @param sideNavID   ID of side navigation icon
     * @param bottomNavID ID of bottom navigation icon
     */
    private void syncNavBars(int sideNavID, int bottomNavID) {
        navigationView.setCheckedItem(sideNavID);
        bottomNavigationView.getMenu().findItem(bottomNavID).setChecked(true);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void createAddObjectDialog(){
        addObjectDialogBuilder = new AlertDialog.Builder(this);
        final View addObjectDialogView = getLayoutInflater().inflate(R.layout.add_object_dialog_popup, null);
        dialogObjectName = addObjectDialogView.findViewById(R.id.add_dialog_object_name);
        dialogObjectType = addObjectDialogView.findViewById(R.id.add_dialog_object_type);
        dialogObjectAdditionalData = addObjectDialogView.findViewById(R.id.add_dialog_object_additional_data);
        startTrainingButton = addObjectDialogView.findViewById(R.id.add_dialog_start_training);
        cancelDialogButton = addObjectDialogView.findViewById(R.id.add_dialog_cancel);

        addObjectDialogBuilder.setView(addObjectDialogView);
        addObjectDialog = addObjectDialogBuilder.create();
        addObjectDialog.show();

        startTrainingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String objectName = dialogObjectName.getText().toString();
                String objectType = dialogObjectType.getText().toString();
                String objectAdditionalData = dialogObjectAdditionalData.getText().toString();
                if(objectName.length() != 0 &&
                        objectType.length() != 0 &&
                        objectAdditionalData.length() != 0) {

                    boolean success = addData(objectName, objectType, objectAdditionalData);

                    if(success) {
                        Toast.makeText(CameraActivity.this, "Sucessfully inserted object!", Toast.LENGTH_SHORT).show();
                        // Reset text
                        String className = dialogObjectName.getText().toString();
                        dialogObjectName.setText("");
                        dialogObjectType.setText("");
                        dialogObjectAdditionalData.setText("");
                        addObjectDialog.dismiss();
                        cameraFragment.addSamples(className, 50);
                    }
                }
            }
        });

        cancelDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addObjectDialog.dismiss();
            }
        });

    }

    /**
     * Add a new object to Database
     *
     * @param objectName Name of object
     * @param objectType Type of object
     * @param objectAdditionalData Additional data of object
     * @return true if successful, false otherwise
     */
    private boolean addData(String objectName, String objectType, String objectAdditionalData) {
        return databaseHelper.addData(objectName, objectType, objectAdditionalData);
    }


}