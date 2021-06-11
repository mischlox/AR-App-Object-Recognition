/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package hs.aalen.arora;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.ConditionVariable;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import org.tensorflow.lite.examples.transfer.api.AssetModelLoader;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static android.content.ContentValues.TAG;

/**
 * App-layer wrapper for TransferLearningModel.
 *
 * <p>This wrapper allows to run training continuously, using start/stop API, in contrast to
 * run-once API of TransferLearningModel.
 *
 * This code is taken from:
 * https://github.com/tensorflow/examples/blob/master/lite/examples/model_personalization/
 */
public class TransferLearningModelWrapper {
    public static final String TAG = TransferLearningModel.class.getSimpleName();
    public static final int IMAGE_SIZE = 224;

    private final TransferLearningModel model;

    private final ConditionVariable shouldTrain = new ConditionVariable();
    private volatile TransferLearningModel.LossConsumer lossConsumer;
    private DatabaseHelper databaseHelper;

    TransferLearningModelWrapper(Context context, Collection<String> classes) {
        databaseHelper = new DatabaseHelper(context);
        model = new TransferLearningModel(
                new AssetModelLoader(context, "model"),
                classes);

        if(databaseHelper.modelExists()) {
            loadParametersFromDB();
        }

        new Thread(() -> {
            while (!Thread.interrupted()) {
                shouldTrain.block();
                try {
                    model.train(1, lossConsumer).get();
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception occurred during model training", e.getCause());
                } catch (InterruptedException e) {
                    // no-op
                }
            }
        }).start();
    }

    private boolean writeParametersToDB() {
        Log.d(TAG, "writeParametersToDB: backup: write parameters from db");
        boolean success = databaseHelper.saveModel("", model.getModelParameters());
        if(!success) {
            Log.d(TAG, "backup: model could not be saved sucessfully ");
        }
        return success;
    }

    public void loadParametersFromDB() {
        Log.d(TAG, "loadParametersFromDB: backup: load Parameters from db");
        ByteBuffer[] parameters = databaseHelper.getParameters("");
//        model.setModelParameters(parameters);
    }

    // This method is thread-safe.
    public Future<Void> addSample(float[] image, String className) {
        return model.addSample(image, className);
    }

    // This method is thread-safe, but blocking.
    public TransferLearningModel.Prediction[] predict(float[] image) {
        return model.predict(image);
    }

    public int getTrainBatchSize() {
        return model.getTrainBatchSize();
    }

    /**
     * Start training the model continuously until {@link #disableTraining() disableTraining} is
     * called.
     *
     * @param lossConsumer callback that the loss values will be passed to.
     */
    public void enableTraining(TransferLearningModel.LossConsumer lossConsumer) {
        this.lossConsumer = lossConsumer;
        shouldTrain.open();
    }

    /**
     * Stops training the model.
     */
    public void disableTraining() {
        shouldTrain.close();
    }

    /** Stores model parameters,
     *  frees all model resources and shuts down all background threads. */
    public void close() {
        boolean success = writeParametersToDB();
        if(success) {
            Log.d(TAG, "close: backup: successfully saved parameters!");
        }
        else {
            Log.d(TAG, "close: backup: could not save parameters!");
        }
        
        Log.d(TAG, "backup: ");
        model.close();
    }
}
