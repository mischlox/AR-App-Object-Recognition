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
        editor.putBoolean("showHelp", show);
        editor.apply();
    }

    @Override
    public boolean getHelpShowing() {
        return prefs.getBoolean("showHelp", true);
    }

    @Override
    public void switchAddSamplesTrigger() {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = "addSamplesState";
        editor.putBoolean(prefKey, !prefs.getBoolean(prefKey, false));
        editor.apply();
    }

    @Override
    public boolean getAddSamplesTrigger() {
        return prefs.getBoolean("addSamplesState", false);
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    @Override
    public void setCurrentClassName(String newName) {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = "currentClass";
        editor.putString(prefKey, newName);
        editor.apply();
    }

    @Override
    public String getCurrentClassName() {
        return prefs.getString("currentClass", "somethings wrong here");
    }

    @Override
    public void setCurrentModel(String id) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("currentModel", id);
        editor.apply();
    }

    @Override
    public String getCurrentModel() {
        return prefs.getString("currentModel", null);
    }

    @Override
    public void setMaxObjects(int max) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("maxObjects", max);
        editor.apply();
    }

    @Override
    public int getMaxObjects() {
        return prefs.getInt("maxObjects", 4);
    }
}
