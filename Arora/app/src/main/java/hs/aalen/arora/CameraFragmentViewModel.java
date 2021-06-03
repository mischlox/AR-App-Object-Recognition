package hs.aalen.arora;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * View Model of {@link CameraFragment}
 * To keep Data out of Controller and to persist state when configuration changes happen
 *
 * Parts of this code are taken from:
 * https://github.com/tensorflow/examples/blob/master/lite/examples/model_personalization/
 *
 * @author Michael Schlosser
 */
public class CameraFragmentViewModel extends ViewModel {
    private static final String TAG = "CameraFragmentViewModel";
    /**
     * Current state of training.
     */
    public enum TrainingState {
        NOT_STARTED,
        STARTED,
        PAUSED
    };

    private MutableLiveData<Boolean> captureMode = new MutableLiveData<>(false);
    private MutableLiveData<Map<String, Float>> confidence = new MutableLiveData<>(new TreeMap<>());
    private MutableLiveData<Integer> trainBatchSize = new MutableLiveData<>(0);
    private MutableLiveData<Map<String, Integer>> numSamplesCurrent = new MutableLiveData<>(new TreeMap<>());
    private MutableLiveData<Integer> numSamplesMax = new MutableLiveData<>(0);
    private MutableLiveData<Map<String, Integer>> numSamples = new MutableLiveData<>(new TreeMap<>());
    private MutableLiveData<TrainingState> trainingState = new MutableLiveData<>(TrainingState.NOT_STARTED);
    private MutableLiveData<Float> lastLoss = new MutableLiveData<>();
    private MutableLiveData<Boolean> inferenceSnackbarWasDisplayed = new MutableLiveData<>(false);

    private LiveData<String> firstChoice;
    private LiveData<String> secondChoice;
    private LiveData<Integer> totalSamples;
    private LiveData<Integer> neededSamples;

    /**
     * Whether capture mode is enabled.
     */
    public MutableLiveData<Boolean> getCaptureMode() {
        return captureMode;
    }

    public void setCaptureMode(boolean newValue) {
        captureMode.postValue(newValue);
    }


    /**
     * Number of added samples for each class.
     */
    public LiveData<Map<String, Integer>> getNumSamples() {
        return numSamples;
    }

    public MutableLiveData<Integer> getNumSamplesMax() {
        return numSamplesMax;
    }

    public void setNumSamplesMax(int newValue) {
        numSamplesMax.postValue(newValue);
    }

    public MutableLiveData<Map<String, Integer>> getNumSamplesCurrent() {
        return numSamplesCurrent;
    }

    public void increaseNumSamples(String className) {
        Map<String, Integer> map = numSamples.getValue();
        int currentNumber;
        if (map.containsKey(className)) {
            currentNumber = map.get(className);
        } else {
            currentNumber = 0;
        }
        map.put(className, currentNumber + 1);
        increaseNumSamplesCurrent(className);
        numSamples.postValue(map);
    }

    private void increaseNumSamplesCurrent(String className) {
        Map<String, Integer> map = numSamplesCurrent.getValue();
        int currentNumber;
        if (map.containsKey(className)) {
            currentNumber = map.get(className);
            if (currentNumber > numSamplesMax.getValue()) currentNumber = 0;
        } else {
            currentNumber = 0;
        }
        map.put(className, currentNumber + 1);
        numSamplesCurrent.postValue(map);
    }



    /**
     * Confidence values for each class from inference.
     */
    public LiveData<Map<String, Float>> getConfidence() {
        return confidence;
    }

    public void setConfidence(String className, float confidenceScore) {
        Map<String, Float> map = confidence.getValue();
        assert map != null;
        map.put(className, confidenceScore);
        confidence.postValue(map);
    }

    /** Number of samples in a single training batch. */
    public MutableLiveData<Integer> getTrainBatchSize() {
        return trainBatchSize;
    }

    public void setTrainBatchSize(int newValue) {
        trainBatchSize.postValue(newValue);
    }

    /** Whether model training is not yet started, already started, or temporarily paused. */
    public LiveData<TrainingState> getTrainingState() {
        return trainingState;
    }

    public void setTrainingState(TrainingState newValue)
    {
        trainingState.postValue(newValue);
        Log.d(TAG, "addSamples: Set Training State to " + newValue.toString());
    }

    /**
     * Last training loss value reported by the training routine.
     */
    public LiveData<Float> getLastLoss() {
        return lastLoss;
    }

    public void setLastLoss(float newLoss) {
        lastLoss.postValue(newLoss);
    }

    /**
     * Whether "you can switch to inference mode now" snackbar has been shown before.
     */
    public MutableLiveData<Boolean> getInferenceSnackbarWasDisplayed() {
        return inferenceSnackbarWasDisplayed;
    }

    public void markInferenceSnackbarWasCalled() {
        inferenceSnackbarWasDisplayed.postValue(true);
    }

    /**
     * Name of the class with the highest confidence score.
     */
    public LiveData<String> getFirstChoice() {
        if (firstChoice == null) {
            firstChoice = Transformations.map(confidence, map -> {
                if (map.isEmpty()) {
                    return null;
                }
                String choice;
            try{
                choice = mapEntriesDecreasingValue(map).get(0).getKey();
            } catch (ConcurrentModificationException | NullPointerException e) {
                Log.e(TAG, "mapEntriesDecreasingValue: " + e.toString());
                return "";
            }
                return mapEntriesDecreasingValue(map).get(0).getKey();
            });
        }
        return firstChoice;
    }

    /**
     * Name of the class with the second highest confidence score.
     */
    public LiveData<String> getSecondChoice() {
        if (secondChoice == null) {
            secondChoice = Transformations.map(confidence, map -> {
                if (map.size() < 2) {
                    return null;
                }
                return mapEntriesDecreasingValue(map).get(1).getKey();
            });
        }
        return secondChoice;
    }

    /**
     * A single integer representing the total number of samples added for all classes.
     */
    public LiveData<Integer> getTotalSamples() {
        if (totalSamples == null) {
            totalSamples = Transformations.map(getNumSamples(), map -> {
                int total = 0;
                for (int number : map.values()) {
                    total += number;
                }
                return total;
            });
        }
        return totalSamples;
    }

    /**
     * Number of samples needed to complete a single batch.
     */
    public LiveData<Integer> getNeededSamples() {
        if (neededSamples == null) {
            MediatorLiveData<Integer> result = new MediatorLiveData<>();
            result.addSource(
                    getTotalSamples(),
                    totalSamples -> {
                        result.setValue(Math.max(0, getTrainBatchSize().getValue() - totalSamples));
                    });
            result.addSource(
                    getTrainBatchSize(),
                    trainBatchSize -> {
                        result.setValue(Math.max(0, trainBatchSize - getTotalSamples().getValue()));
                    });
            neededSamples = result;
        }
        return neededSamples;
    }

    private static List<Map.Entry<String, Float>> mapEntriesDecreasingValue(Map<String, Float> map) {
        List<Map.Entry<String, Float>> entryList = null;
        entryList = new ArrayList<>(map.entrySet());
        Collections.sort(entryList, (e1, e2) -> -Float.compare(e1.getValue(), e2.getValue()));

        return entryList;
    }
}
