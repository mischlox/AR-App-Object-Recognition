package hs.aalen.arora;

import android.content.SharedPreferences;

import com.google.gson.Gson;

/**
 * Will save and store state of the Transfer Learning Model
 * when app is closed and reopened
 */
public class TlModelParameterBackup {

    // TODO save actual model, not wrapper
//    public void saveModel() {
//        TransferLearningModelWrapper tfModel = transferLearningModel;
//
//        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
//        Gson gson = new Gson();
//        String json = gson.toJson(tfModel);
//        prefsEditor.putString("tfmodel", json);
//        prefsEditor.commit();
//    }
//
//    public TransferLearningModelWrapper restoreModel() {
//        Gson gson = new Gson();
//        String json = sharedPreferences.getString("tfmodel", "");
//        return gson.fromJson(json, TransferLearningModelWrapper.class);
//    }
}
