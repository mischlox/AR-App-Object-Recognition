package hs.aalen.arora;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
    private final CameraFragment cameraFragment = new CameraFragment();
    private final ObjectOverviewFragment objectOverviewFragment = new ObjectOverviewFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();
    private final StatisticsFragment statisticsFragment = new StatisticsFragment();
    private final HelpFragment helpFragment = new HelpFragment();
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Fragment selectedFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_nav_view);

        bottomNavigationView.setBackground(null);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        FloatingActionButton buttonCamera = findViewById(R.id.nav_bottom_camera_button);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If not in Camera Fragment, go into it
                if (selectedFragment != cameraFragment) {
                    selectedFragment = cameraFragment;
                    getSupportFragmentManager().beginTransaction().replace(R.id.navbar_container, selectedFragment).commit();
                    setTitle(navigationView.getMenu().findItem(R.id.nav_camera).getTitle());
                    syncNavBars(R.id.nav_camera, R.id.nav_bottom_placeholder);
                } else {
                    // TODO implement dialog to add object
                    System.out.println("Magic");
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
            getSupportFragmentManager().beginTransaction().replace(R.id.navbar_container,
                    new CameraFragment()).commit();
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
                syncNavBars(R.id.nav_camera, R.id.nav_bottom_placeholder);
                selectedFragment = cameraFragment;
                break;
            case R.id.nav_object_overview:
            case R.id.nav_bottom_object_overview:
                syncNavBars(R.id.nav_object_overview, R.id.nav_bottom_object_overview);
                selectedFragment = objectOverviewFragment;
                break;
            case R.id.nav_settings:
            case R.id.nav_bottom_settings:
                syncNavBars(R.id.nav_settings, R.id.nav_bottom_settings);
                selectedFragment = settingsFragment;
                break;
            case R.id.nav_statistics:
            case R.id.nav_bottom_statistics:
                syncNavBars(R.id.nav_statistics, R.id.nav_bottom_statistics);
                selectedFragment = statisticsFragment;
                break;
            case R.id.nav_help:
            case R.id.nav_bottom_help:
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
}