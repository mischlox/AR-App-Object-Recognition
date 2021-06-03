package hs.aalen.arora;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import org.jetbrains.annotations.NotNull;

import java.util.prefs.PreferencesFactory;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener{
    private static final String TAG = SettingsFragment.class.getSimpleName();

    private int numSamples;
    private SeekBarPreference seekBarPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager.setDefaultValues(getContext(), R.xml.prefs, true);
        addPreferencesFromResource(R.xml.prefs);
        findPreference(getString(R.string.key_seekbar)).setOnPreferenceChangeListener(this);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()){

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        seekBarPreference = findPreference(getString(R.string.key_seekbar));
    }

    public int getNumSamples() {
        return numSamples;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(seekBarPreference.getKey().equals(preference.getKey())) {
            numSamples = seekBarPreference.getValue();
        }
        return false;
    }
}
