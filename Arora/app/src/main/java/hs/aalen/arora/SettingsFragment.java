package hs.aalen.arora;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import hs.aalen.arora.dialogues.DialogFactory;
import hs.aalen.arora.dialogues.DialogType;

/**
 * Global settings for the application
 *
 * @author Michael Schlosser
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final HashMap<Integer, String> resolutionMap = new HashMap<>();
    SharedPreferences prefs;
    private SeekBarPreference amountSamplesPreference;
    private SeekBarPreference countDownPreference;
    private SwitchPreference nightModePreference;
    private SeekBarPreference resolutionPreference;
    private SeekBarPreference confidencePreference;
    private Preference resetAppPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager.setDefaultValues(requireContext(), R.xml.prefs, true);
        addPreferencesFromResource(R.xml.prefs);

        amountSamplesPreference = findPreference(getString(R.string.key_seekbar));
        countDownPreference = findPreference("key_countdown");
        nightModePreference = findPreference(getString(R.string.key_nightmode));
        resolutionPreference = findPreference(getString(R.string.key_resolution));
        resetAppPreference = findPreference(getString(R.string.key_reset));
        confidencePreference = findPreference("key_confidence");

        amountSamplesPreference.setOnPreferenceChangeListener(this);
        nightModePreference.setOnPreferenceChangeListener(this);
        resolutionPreference.setOnPreferenceChangeListener(this);
        countDownPreference.setOnPreferenceChangeListener(this);
        confidencePreference.setOnPreferenceChangeListener(this);

        assert resetAppPreference != null;
        resetAppPreference.setOnPreferenceChangeListener(this);

        prefs = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);

        resolutionMap.put(0, getString(R.string.settings_resolution_small));
        resolutionMap.put(1, getString(R.string.settings_resolution_medium));
        resolutionMap.put(2, getString(R.string.settings_resolution_large));
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        assert resetAppPreference != null;
        resetAppPreference.setOnPreferenceClickListener(preference -> {
            DialogFactory.getDialog(DialogType.RESET).createDialog(getContext());
            return true;
        });
        amountSamplesPreference.setUpdatesContinuously(true);
        countDownPreference.setUpdatesContinuously(true);
        resolutionPreference.setUpdatesContinuously(true);
        confidencePreference.setUpdatesContinuously(true);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SharedPreferences.Editor editor = prefs.edit();
        if (amountSamplesPreference.getKey().equals(preference.getKey())) {
            editor.putInt(getString(R.string.key_seekbar), (int) newValue);
            editor.apply();
            return true;
        } else if (confidencePreference.getKey().equals(preference.getKey())) {
            editor.putInt("key_confidence", (int) newValue);
            editor.apply();
            return true;
        } else if (nightModePreference.getKey().equals(preference.getKey())) {
            editor.putBoolean(getString(R.string.key_nightmode), (boolean) newValue);
            editor.apply();
            return true;
        } else if (resolutionPreference.getKey().equals(preference.getKey())) {
            editor.putInt(getString(R.string.key_resolution), (int) newValue);
            editor.apply();
            resolutionPreference.setSummary(getString(R.string.settings_resolution_hint) + "\n" +
                    getString(R.string.settings_resolution_current) + " " +
                    resolutionMap.get((int) newValue));
            return true;
        } else if (countDownPreference.getKey().equals(preference.getKey())) {
            editor.putInt("key_countdown", (int) newValue);
            editor.apply();
            return true;
        }
        return false;
    }
}
