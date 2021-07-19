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
import android.os.ConditionVariable;
import android.util.Log;

import org.tensorflow.lite.examples.transfer.api.AssetModelLoader;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * App-layer wrapper for TransferLearningModel.
 *
 * <p>This wrapper allows to run training continuously, using start/stop API, in contrast to
 * run-once API of TransferLearningModel.
 *
 * This code is based on:
 * https://github.com/tensorflow/examples/blob/master/lite/examples/model_personalization/
 */
public class TransferLearningModelWrapper {
    public static final String TAG = TransferLearningModel.class.getSimpleName();
    public static final int IMAGE_SIZE = 224;

    private final TransferLearningModel model;

    private final ConditionVariable shouldTrain = new ConditionVariable();
    private volatile TransferLearningModel.LossConsumer lossConsumer;
    private final DatabaseHelper databaseHelper;
    private Context context;
    Path parametersFilePath;

    TransferLearningModelWrapper(Context context, Collection<String> classes) {
        this.context = context;
        databaseHelper = new DatabaseHelper(context);

        model = new TransferLearningModel(
                new AssetModelLoader(context, "model"),
                classes);
        Log.d(TAG, "TransferLearningModelWrapper: backup: create model");

        if(databaseHelper.modelsExists()) {
            Log.d(TAG, "TransferLearningModelWrapper: Load existing Model");
            loadModel();
        } else Log.d(TAG, "TransferLearningModelWrapper: Models do not exist!");

        new Thread(() -> {
            while (!Thread.interrupted()) {
                shouldTrain.block();
                try {
                    model.train(1, lossConsumer).get();
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception occurred during model training", e.getCause());
                } catch (InterruptedException e) {
                    Log.e(TAG, "TransferLearningModelWrapper: Thread was interruped! trying to restart", e.getCause());
                }
            }
        }).start();
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

    /**
     * Calls a method for loading a model
     * @return
     */
    public boolean loadModel() {
        try{
            readParametersFromFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Calls a method for saving the model
     * @return
     */
    private boolean saveModel()  {
        try{
            writeParametersToFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void writeParametersToFile() throws IOException{
        String filename = generateFileName("model-parameters",".bin");
        parametersFilePath = Paths.get(context.getFilesDir().toString() + File.separator + filename);
        Log.d(TAG, "writeParametersToFile: backup1: filepath is " + parametersFilePath.toString());
        parametersFilePath = Files.createFile(parametersFilePath);
        // save model to DB. If model with specific name already exists: Update the existing one
        if (databaseHelper.insertModel("test", parametersFilePath))
            model.saveParameters(FileChannel.open(parametersFilePath, StandardOpenOption.WRITE));
    }

    /**
     * Generates a random file name
     *
     * @return generated random file name
     */
    private String generateFileName(String prefix, String suffix) {
        return prefix+"-" + UUID.randomUUID() + suffix;
    }

    private void readParametersFromFile() throws IOException {
        Log.d(TAG, "readParametersFromFile: backup: file");
        Path path = databaseHelper.getLatestModelPath();
        Log.d(TAG, "readParametersFromFile: backup1: latest path is: " + path.toString());
        model.loadParameters(FileChannel.open(databaseHelper.getLatestModelPath(), StandardOpenOption.READ));
    }

    /** Stores model parameters,
     *  frees all model resources and shuts down all background threads. */
    public void close() {
        boolean success = saveModel();

        if(success) {
            Log.d(TAG, "close: backup: successfully saved parameters!");
        }
        else {
            Log.d(TAG, "close: backup: could not save parameters!");
        }
        model.close();
    }
}
