package hs.aalen.arora;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

public class CameraActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    // Fragments as single instance in order to not recreate them
    private CameraFragment cameraFragment = new CameraFragment();
    private ObjectOverviewFragment objectOverviewFragment = new ObjectOverviewFragment();
    private SettingsFragment settingsFragment = new SettingsFragment();
    private StatisticsFragment statisticsFragment = new StatisticsFragment();
    private HelpFragment helpFragment = new HelpFragment();
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
            bottomNavigationView.setSelectedItemId(R.id.placeholder);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        Fragment selectedFragment = null;
        switch (item.getItemId()) {
            case R.id.nav_camera:
                bottomNavigationView.setSelectedItemId(R.id.placeholder);
                selectedFragment = cameraFragment;
                break;
            case R.id.nav_object_overview:
            case R.id.object_overview_button:
                syncNavBars(R.id.nav_object_overview, R.id.object_overview_button);
                selectedFragment = objectOverviewFragment;
                break;
            case R.id.nav_settings:
            case R.id.settings_button:
                syncNavBars(R.id.nav_settings, R.id.settings_button);
                selectedFragment = settingsFragment;
                break;
            case R.id.nav_statistics:
            case R.id.statistics_button:
                syncNavBars(R.id.nav_statistics, R.id.statistics_button);
                selectedFragment = statisticsFragment;
                break;
            case R.id.nav_help:
            case R.id.help_button:
                syncNavBars(R.id.nav_help, R.id.help_button);
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
        if(selectedFragment != null) {
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
     * @param sideNavID ID of side navigation icon
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