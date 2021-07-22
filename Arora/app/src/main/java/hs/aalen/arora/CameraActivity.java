package hs.aalen.arora;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

import hs.aalen.arora.dialogues.DialogFactory;
import hs.aalen.arora.dialogues.DialogType;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

/**
 * Main Activity of Application handles Navigation between all fragments and Global Settings
 *
 * @author Michael Schlosser
 */
public class CameraActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "CameraActivity";

    // Fragments as single instance in order to not recreate them
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private final CameraFragment cameraFragment = new CameraFragment();
    private final ObjectOverviewFragment objectOverviewFragment = new ObjectOverviewFragment();
    private final ModelOverviewFragment modelOverviewFragment = new ModelOverviewFragment();
    private final HelpFragment helpFragment = new HelpFragment();
    private FloatingActionButton buttonCamera;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Fragment selectedFragment = cameraFragment;

    // Further configuration
    private int amountSamples = 50;
    private String className;

    private GlobalSettings settings;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(getString(R.string.key_resolution))) {
                cameraFragment.setFocusBoxRatio(settings.getFocusBoxRatio());
            }
            else if(key.equals(getString(R.string.key_nightmode))) {
                if(settings.getNightMode()) {
                    AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
                }
                else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
            else if(key.equals(getString(R.string.key_seekbar))) {
                amountSamples = settings.getAmountSamples();
            }
            else if(key.equals("addSamplesState")) {
                if(settings.getAddSamplesTrigger()) {
                    Log.d(TAG, "onSharedPreferenceChanged: backup Start training");
                    cameraFragment.addSamples(className, amountSamples);
                    settings.switchAddSamplesTrigger();
                }
            }
            else if(key.equals("currentClass")) {
                className = settings.getCurrentClassName();
            }
            else if(key.equals("currentModel")) {
                cameraFragment.setModelID(settings.getCurrentModel());
                if(selectedFragment == cameraFragment) {
                    cameraFragment.loadNewModel();
                }
                else if(selectedFragment == modelOverviewFragment) {
                    modelOverviewFragment.populateView();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: entrypoint");
        super.onCreate(savedInstanceState);
        settings = new SharedPrefsHelper(this);
        SharedPrefsHelper prefsHelper = (SharedPrefsHelper) settings;
        prefsHelper.getPrefs().registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        setContentView(R.layout.activity_camera);

        cameraFragment.setFocusBoxRatio(settings.getFocusBoxRatio());
        cameraFragment.setModelID(settings.getCurrentModel());

        // Bind navigation views
        Toolbar toolbar = findViewById(R.id.toolbar);
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
                    createDialog();
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
        // set Training Status to paused when going to different fragment
        if (item.getItemId() != R.id.nav_camera && selectedFragment == cameraFragment) {
            try {
                Log.d(TAG, "onNavigationItemSelected: set Training State to paused");
                cameraFragment.getViewModel().setTrainingState(CameraFragmentViewModel.TrainingState.PAUSED);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
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
                syncNavBars(R.id.nav_settings, R.id.nav_bottom_settings);
                selectedFragment = settingsFragment;
                break;
            case R.id.nav_model_overview:
            case R.id.nav_bottom_model_overview:
                buttonCamera.setImageResource(R.drawable.ic_eye);
                syncNavBars(R.id.nav_model_overview, R.id.nav_bottom_model_overview);
                selectedFragment = modelOverviewFragment;
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

    /**
     * Checks if Show Help is enabled in the Shared Preferences
     * Skips the showing of the Help Dialog if disabled and starts the Add Object Dialog directly
     */
    public void createDialog() {
        boolean showHelp = settings.getHelpShowing();

        if(showHelp) {
            DialogFactory.getDialog(DialogType.HELP).createDialog(this);
        }
        else {
            DialogFactory.getDialog(DialogType.ADD_OBJ).createDialog(this);
        }
    }
}