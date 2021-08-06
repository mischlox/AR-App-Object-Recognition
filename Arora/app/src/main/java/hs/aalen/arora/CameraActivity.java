package hs.aalen.arora;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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
import hs.aalen.arora.persistence.GlobalConfig;
import hs.aalen.arora.persistence.SharedPrefsHelper;

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

    private GlobalConfig globalConfig;

    // Checks if SharedPreferences were changed and delegates calls to its fragments
    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            try {
                if (key.equals(getString(R.string.key_resolution))) {
                    cameraFragment.setFocusBoxRatio(globalConfig.getFocusBoxRatio());
                } else if (key.equals(getString(R.string.key_nightmode))) {
                    if (globalConfig.getNightMode()) {
                        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    recreate();
                } else if (key.equals(getString(R.string.key_seekbar))) {
                    amountSamples = globalConfig.getAmountSamples();
                } else if (key.equals("addSamplesState")) {
                    if (globalConfig.getAddSamplesTrigger()) {
                        // This can only happen if adding further samples from Object Overview
                        if(!cameraFragment.isAdded()) {
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.navbar_container, cameraFragment)
                                    .commit();
                        }
                        Log.d(TAG, "onSharedPreferenceChanged: backup Start training");
                        if(className == null) className = globalConfig.getCurrentModelPos();
                        cameraFragment.addSamples(className, amountSamples);
                        cameraFragment.setNewObjectAdded(true);
                        globalConfig.switchAddSamplesTrigger();
                    }
                } else if (key.equals("illegalStateTrigger")) {
                    if (globalConfig.getIllegalStateTrigger()) {
                        cameraFragment.removeObjectFromModel(globalConfig.getCurrentObjectName(),
                                globalConfig.getCurrentModelID(),
                                true);
                        globalConfig.switchIllegalStateTrigger();
                        Toast.makeText(CameraActivity.this, R.string.please_do_not_change_tabs, Toast.LENGTH_SHORT).show();
                    }
                } else if(key.equals("updateReplayTrigger")) {
                    if(globalConfig.getUpdateReplayTrigger()) {
                        cameraFragment.updateReplayBuffer();
                        globalConfig.switchUpdateReplayTrigger();
                    }
                } else if (key.equals("currentClass")) {
                    className = globalConfig.getCurrentModelPos();
                } else if (key.equals("currentModel")) {
                    cameraFragment.setModelID(globalConfig.getCurrentModelID());
                    if (cameraFragment.isAdded()) {
                        cameraFragment.loadNewModel();
                    } else if (modelOverviewFragment.isAdded()) {
                        modelOverviewFragment.populateView();
                    }
                } else if (key.equals("maxObjects")) {
                    cameraFragment.setPositionsList(globalConfig.getMaxObjects());
                } else if (key.equals("key_countdown")) {
                    cameraFragment.setCountDown(globalConfig.getCountDown());
                } else if (key.equals("key_confidence")) {
                    cameraFragment.setConfidenceThres(globalConfig.getConfidenceThreshold());
                }

            } catch (NullPointerException e) {
                e.printStackTrace();
                recreate();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: entrypoint");
        super.onCreate(savedInstanceState);
        globalConfig = new SharedPrefsHelper(this);
        SharedPrefsHelper prefsHelper = (SharedPrefsHelper) globalConfig;
        prefsHelper.getPrefs().registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        setContentView(R.layout.activity_camera);

        // Configure cameraFragment on startup
        amountSamples = globalConfig.getAmountSamples();
        cameraFragment.setFocusBoxRatio(globalConfig.getFocusBoxRatio());
        cameraFragment.setModelID(globalConfig.getCurrentModelID());
        cameraFragment.setPositionsList(globalConfig.getMaxObjects());
        cameraFragment.setCountDown(globalConfig.getCountDown());
        cameraFragment.setConfidenceThres(globalConfig.getConfidenceThreshold());
        cameraFragment.setPositionsList(globalConfig.getMaxObjects());

        // Bind navigation views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_nav_view);

        bottomNavigationView.setBackground(null);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        buttonCamera = findViewById(R.id.nav_bottom_camera_button);
        buttonCamera.setImageResource(R.drawable.ic_eye);
        buttonCamera.setOnClickListener(v -> {
            // If not in Camera Fragment, go into it
            if (cameraFragment.isAdded()) {
                // Start inference/training dialog from here
                createDialog();
            } else {
                buttonCamera.setImageResource(R.drawable.ic_add);
                selectedFragment = cameraFragment;
                setTitle(navigationView.getMenu().findItem(R.id.nav_camera).getTitle());
                syncNavBars(R.id.nav_camera, R.id.nav_bottom_placeholder);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.navbar_container, selectedFragment)
                        .commit();
            }
        });
        drawer = findViewById(R.id.drawer_layout);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.navbar_container, cameraFragment)
                    .commit();
            navigationView.setCheckedItem(R.id.nav_camera);
            bottomNavigationView.setSelectedItemId(R.id.nav_bottom_placeholder);
        }
    }

    /**
     * Checks if Show Help is enabled in the Shared Preferences
     * Skips the showing of the Help Dialog if disabled and starts the Add Object Dialog directly
     */
    public void createDialog() {
        boolean showHelp = globalConfig.getHelpShowing();

        if (showHelp) {
            DialogFactory.getDialog(DialogType.HELP_ADD_OBJ).createDialog(this);
        } else {
            DialogFactory.getDialog(DialogType.ADD_OBJ).createDialog(this);
        }
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

    /**
     * Overridden method for bottom and side navigation in one
     * Changes fragment on a click and syncs both navigation bars
     *
     * @param item item of the menu
     * @return true if successful
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        if(cameraFragment.isAdded() && item.getItemId() != R.id.nav_camera && cameraFragment.hasNewObjectAdded()) {
            DialogFactory.getDialog(DialogType.REPLAY).createDialog(this);
            cameraFragment.setNewObjectAdded(false);
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.nav_camera) {
            buttonCamera.setImageResource(R.drawable.ic_add);
            syncNavBars(R.id.nav_camera, R.id.nav_bottom_placeholder);
            selectedFragment = cameraFragment;
        } else if (itemId == R.id.nav_object_overview || itemId == R.id.nav_bottom_object_overview) {
            buttonCamera.setImageResource(R.drawable.ic_eye);
            syncNavBars(R.id.nav_object_overview, R.id.nav_bottom_object_overview);
            selectedFragment = objectOverviewFragment;
        } else if (itemId == R.id.nav_settings || itemId == R.id.nav_bottom_settings) {
            buttonCamera.setImageResource(R.drawable.ic_eye);
            syncNavBars(R.id.nav_settings, R.id.nav_bottom_settings);
            selectedFragment = settingsFragment;
        } else if (itemId == R.id.nav_model_overview || itemId == R.id.nav_bottom_model_overview) {
            buttonCamera.setImageResource(R.drawable.ic_eye);
            syncNavBars(R.id.nav_model_overview, R.id.nav_bottom_model_overview);
            selectedFragment = modelOverviewFragment;
        } else if (itemId == R.id.nav_help || itemId == R.id.nav_bottom_help) {
            buttonCamera.setImageResource(R.drawable.ic_eye);
            syncNavBars(R.id.nav_help, R.id.nav_bottom_help);
            selectedFragment = helpFragment;
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

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}