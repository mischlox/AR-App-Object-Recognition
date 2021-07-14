package hs.aalen.arora;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import org.jetbrains.annotations.NotNull;

import java.util.prefs.PreferencesFactory;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

/**
 * Global settings for the application
 *
 * @author Michael Schlosser
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener{
    private static final String TAG = SettingsFragment.class.getSimpleName();

    SharedPreferences prefs;
    private SeekBarPreference seekBarPreference;
    private SwitchPreference nightModeToggle;
    private ListPreference resolutionDropDown;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager.setDefaultValues(getContext(), R.xml.prefs, true);
        addPreferencesFromResource(R.xml.prefs);
        findPreference(getString(R.string.key_seekbar)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.key_nightmode)).setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.key_resolution)).setOnPreferenceChangeListener(this);
        prefs = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: settings:");
        switch (item.getItemId()){
            
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        seekBarPreference = findPreference(getString(R.string.key_seekbar));
        nightModeToggle = findPreference(getString(R.string.key_nightmode));
        resolutionDropDown = findPreference(getString(R.string.key_resolution));

        seekBarPreference.setUpdatesContinuously(true);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange: settings ");
        SharedPreferences.Editor editor = prefs.edit();
        if(seekBarPreference.getKey().equals(preference.getKey())) {
            editor.putInt(getString(R.string.key_seekbar), (int)newValue);
            Log.d(TAG, "onPreferenceChange: settings "+ (int)newValue);
            editor.apply();
            return true;
        }
        else if(nightModeToggle.getKey().equals(preference.getKey())) {
            editor.putBoolean(getString(R.string.key_nightmode), (boolean)newValue);
            Log.d(TAG, "onPreferenceChange: settings " + (boolean)newValue);
            editor.apply();
            Toast.makeText(this.getActivity(), getString(R.string.toast_restart_app), Toast.LENGTH_SHORT).show();
            return true;
        }
        else if(resolutionDropDown.getKey().equals(preference.getKey())) {
            editor.putString(getString(R.string.key_resolution), newValue.toString());
            editor.apply();
        }
        return false;
    }
}
