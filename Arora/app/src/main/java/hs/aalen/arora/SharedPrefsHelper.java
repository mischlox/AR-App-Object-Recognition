package hs.aalen.arora;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Helper class with methods for storing and retrieving settings from Shared Preferences
 *
 * @author Michael Schlosser
 */
public class SharedPrefsHelper implements GlobalSettings {
    Context context;
    SharedPreferences prefs;
    public SharedPrefsHelper(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("prefs", MODE_PRIVATE);
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    @Override
    public int getAmountSamples() {
        return prefs.getInt(context.getString(R.string.key_seekbar), 50);
    }

    @Override
    public boolean getNightMode() {
        return prefs.getBoolean(context.getString(R.string.key_nightmode), false);
    }

    @Override
    public double getFocusBoxRatio() {
        switch (prefs.getInt(context.getString(R.string.key_resolution), 1)) {
            case 2:
                return LARGE;
            case 1:
                return MEDIUM;
            case 0:
                return SMALL;
        }
        return MEDIUM;
    }

    @Override
    public void setHelpShowing(boolean show) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(context.getString(R.string.key_show_help), show);
        editor.apply();
    }

    @Override
    public boolean getHelpShowing() {
        return prefs.getBoolean("showHelp", true);
    }

    @Override
    public void switchAddSamplesTrigger() {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = context.getString(R.string.key_add_samples_state);
        editor.putBoolean(prefKey, !prefs.getBoolean(prefKey, false));
        editor.apply();
    }

    @Override
    public boolean getAddSamplesTrigger() {
        return prefs.getBoolean(context.getString(R.string.key_add_samples_state), false);
    }

    @Override
    public void switchIllegalStateTrigger() {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = context.getString(R.string.key_switch_illegal_state_trigger);
        editor.putBoolean(prefKey, !prefs.getBoolean(prefKey, false));
        editor.apply();
    }

    @Override
    public boolean getIllegalStateTrigger() {
        return prefs.getBoolean(context.getString(R.string.key_switch_illegal_state_trigger), false);
    }

    @Override
    public void setCurrentModelPos(String newName) {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = context.getString(R.string.key_current_model_pos);
        editor.putString(prefKey, newName);
        editor.apply();
    }

    @Override
    public String getCurrentModelPos() {
        return prefs.getString(context.getString(R.string.key_current_model_pos), "somethings wrong here");
    }

    @Override
    public void setCurrentModel(String id) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.key_current_model), id);
        editor.apply();
    }

    @Override
    public String getCurrentModel() {
        return prefs.getString(context.getString(R.string.key_current_model), null);
    }

    @Override
    public void setCurrentObject(String name) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.key_current_object), name);
        editor.apply();
    }

    @Override
    public String getCurrentObject() {
        return prefs.getString(context.getString(R.string.key_current_object), "");
    }

    @Override
    public void setMaxObjects(int max) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(context.getString(R.string.key_max_objects), max);
        editor.apply();
    }

    @Override
    public int getMaxObjects() {
        return prefs.getInt(context.getString(R.string.key_max_objects), 4);
    }

    @Override
    public int getCountDown() {
        return prefs.getInt(context.getString(R.string.key_countdown), 5);
    }
}
