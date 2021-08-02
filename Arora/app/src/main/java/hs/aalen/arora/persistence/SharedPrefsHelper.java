package hs.aalen.arora.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import hs.aalen.arora.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * Helper class with methods for storing and retrieving settings from Shared Preferences
 *
 * @author Michael Schlosser
 */
public class SharedPrefsHelper implements GlobalConfig {
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
    public boolean getHelpShowing() {
        return prefs.getBoolean("showHelp", true);
    }

    @Override
    public void setHelpShowing(boolean show) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("showHelp", show);
        editor.apply();
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

    @Override
    public void switchIllegalStateTrigger() {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = "illegalStateTrigger";
        editor.putBoolean(prefKey, !prefs.getBoolean(prefKey, false));
        editor.apply();
    }

    @Override
    public boolean getIllegalStateTrigger() {
        return prefs.getBoolean("illegalStateTrigger", false);
    }

    @Override
    public void switchUpdateReplayTrigger() {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = "updateReplayTrigger";
        editor.putBoolean(prefKey, !prefs.getBoolean(prefKey, false));
        editor.apply();
    }

    @Override
    public boolean getUpdateReplayTrigger() {
        return prefs.getBoolean("updateReplayTrigger", false);
    }

    @Override
    public String getCurrentModelPos() {
        return prefs.getString("currentClass", "somethings wrong here");
    }

    @Override
    public void setCurrentModelPos(String newName) {
        SharedPreferences.Editor editor = prefs.edit();
        String prefKey = "currentClass";
        editor.putString(prefKey, newName);
        editor.apply();
    }

    @Override
    public String getCurrentModelID() {
        return prefs.getString("currentModel", null);
    }

    @Override
    public void setCurrentModelID(String id) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("currentModel", id);
        editor.apply();
    }

    @Override
    public String getCurrentObjectName() {
        return prefs.getString("currentObject", "");
    }

    @Override
    public void setCurrentObjectName(String name) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("currentObject", name);
        editor.apply();
    }

    @Override
    public int getMaxObjects() {
        return prefs.getInt("maxObjects", 10);
    }

    @Override
    public int getCountDown() {
        return prefs.getInt("key_countdown", 5);
    }

    @Override
    public int getConfidenceThreshold() {
        return prefs.getInt("key_confidence", 50);
    }

    @Override
    public void clearConfiguration() {
        prefs.edit().clear().apply();
    }

}
