package hs.aalen.arora;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import java.util.LinkedList;
import java.util.Set;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static hs.aalen.arora.GlobalSettings.MEDIUM;

/**
 * Main Activity of Application handles Navigation between all fragments
 *
 * @author Michael Schlosser
 */
public class CameraActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener, GlobalSettings {
    private static final String TAG = "CameraActivity";

    // Fragments as single instance in order to not recreate them
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private final CameraFragment cameraFragment = new CameraFragment();
    private final ObjectOverviewFragment objectOverviewFragment = new ObjectOverviewFragment();
    private final ModelOverviewFragment modelOverviewFragment = new ModelOverviewFragment();
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

    // Help Dialog items
    private AlertDialog.Builder helpDialogBuilder;
    private AlertDialog helpDialog;
    private FloatingActionButton backwardButton;
    private FloatingActionButton forwardButton;
    private Button trainingButton;
    private CheckBox notShowAgainCheckBox;
    private TextView helpTextView;
    private ProgressBar helpProgress;
    private TextView helpProgressText;

    // For Database Access
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);

    // Further configuration
    private int amountSamples = 50;

    private GlobalSettings settings;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: entrypoint");
        super.onCreate(savedInstanceState);
        settings = this;
        prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        setContentView(R.layout.activity_camera);

        Toolbar toolbar = findViewById(R.id.toolbar);

        cameraFragment.setFocusBoxSize(getFocusBoxSize());

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
                    applyPreferences();
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

        applyPreferences();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (selectedFragment != null) {
            transaction.replace(R.id.navbar_container, selectedFragment);
            transaction.commit();
            setTitle(item.getTitle());
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void applyPreferences() {
        // set amount of samples
        amountSamples = settings.getAmountSamples();
        Log.d(TAG, "sharedPrefs: settings amountSamples = " + amountSamples);
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

    public void createDialog() {
        boolean showHelp = settings.getHelpShowing();

        if(showHelp) {
            createHelpDialog();
        }
        else {
            createAddObjectDialog();
        }
    }

    public void createHelpDialog() {
        helpDialogBuilder = new AlertDialog.Builder(this);

        final View helpDialogView = getLayoutInflater().inflate(R.layout.help_popup, null);
        backwardButton =helpDialogView.findViewById(R.id.help_backward_button);
        forwardButton = helpDialogView.findViewById(R.id.help_forward_button);
        trainingButton = helpDialogView.findViewById(R.id.help_training_button);
        notShowAgainCheckBox = helpDialogView.findViewById(R.id.help_checkbox);
        helpTextView = helpDialogView.findViewById(R.id.help_dialog_text);
        helpProgress = helpDialogView.findViewById(R.id.help_progress);
        helpProgressText = helpDialogView.findViewById(R.id.help_progress_text);

        helpDialogBuilder.setView(helpDialogView);
        helpDialog = helpDialogBuilder.create();
        helpDialog.show();

        // Linked list to browse through text views
        LinkedList<Pair<Integer, String>> textList = new LinkedList<>();
        textList.add(Pair.create(2,getString(R.string.help_text_tilt_camera)));
        textList.add(Pair.create(3,getString(R.string.help_text_pause_training)));
        textList.add(Pair.create(4,getString(R.string.help_text_havefun)));

        notShowAgainCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setHelpShowing(!isChecked);
            }
        });

        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTextAnimation(helpTextView, R.anim.anim_fade_out);

                String currentText = helpTextView.getText().toString();
                int currentProgress = helpProgress.getProgress();
                Pair<Integer, String> nextItem = textList.removeFirst();

                helpProgress.setProgress(nextItem.first);
                startTextAnimation(helpTextView, R.anim.anim_fade_in);
                helpTextView.setText(nextItem.second, TextView.BufferType.SPANNABLE);
                helpProgressText.setText(nextItem.first.toString() + " / " + helpProgress.getMax());

                textList.addLast(Pair.create(currentProgress, currentText));
            }
        });

        backwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTextAnimation(helpTextView, R.anim.anim_fade_out);

                String currentText = helpTextView.getText().toString();
                int currentProgress = helpProgress.getProgress();
                Pair<Integer, String> nextItem = textList.removeLast();

                helpProgress.setProgress(nextItem.first);
                startTextAnimation(helpTextView, R.anim.anim_fade_in);
                helpTextView.setText(nextItem.second, TextView.BufferType.SPANNABLE);
                helpProgressText.setText(nextItem.first.toString() + " / " + helpProgress.getMax());

                textList.addFirst(Pair.create(currentProgress, currentText));
            }
        });

        trainingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAddObjectDialog();
                helpDialog.dismiss();
            }
        });

    }

    private void startTextAnimation(TextView textView, int animationResource) {
        Animation animation = AnimationUtils.loadAnimation(this, animationResource);
        textView.startAnimation(animation);
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

                    boolean success = addObject(objectName, objectType, objectAdditionalData);

                    if(success) {
                        Toast.makeText(CameraActivity.this, "Sucessfully inserted object!", Toast.LENGTH_SHORT).show();
                        // Reset text
                        String className = dialogObjectName.getText().toString();
                        dialogObjectName.setText("");
                        dialogObjectType.setText("");
                        dialogObjectAdditionalData.setText("");
                        addObjectDialog.dismiss();
                        cameraFragment.setFocusBoxSize(getFocusBoxSize());
                        cameraFragment.addSamples(className, amountSamples);
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
    private boolean addObject(String objectName, String objectType, String objectAdditionalData) {
        return databaseHelper.insertObject(objectName, objectType, objectAdditionalData);
    }

    @Override
    public int getAmountSamples() {
        return prefs.getInt(getString(R.string.key_seekbar), 50);
    }

    @Override
    public boolean nightModeOn() {
        return prefs.getBoolean(getString(R.string.key_nightmode), false);
    }

    @Override
    public int getFocusBoxSize() {
        switch (prefs.getString(getString(R.string.key_resolution), "MEDIUM")) {
            case "LARGE":
                return LARGE;
            case "MEDIUM":
                return MEDIUM;
            case "SMALL":
                return SMALL;
        }
        return MEDIUM;
    }

    @Override
    public void setHelpShowing(boolean show) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("showHelp", show);
        editor.apply();
    }

    @Override
    public boolean getHelpShowing() {
        return prefs.getBoolean("showHelp", true);
    }



}