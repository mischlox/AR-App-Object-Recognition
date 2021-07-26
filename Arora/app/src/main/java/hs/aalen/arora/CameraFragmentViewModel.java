package hs.aalen.arora;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
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
    }

    private final MutableLiveData<Boolean> captureMode = new MutableLiveData<>(false);
    private final MutableLiveData<Map<String, Float>> confidence = new MutableLiveData<>(new TreeMap<>());
    private final MutableLiveData<Map<String, Integer>> numSamplesCurrent = new MutableLiveData<>(new TreeMap<>());
    private final MutableLiveData<Integer> numSamplesMax = new MutableLiveData<>(0);
    private final MutableLiveData<Map<String, Integer>> numSamples = new MutableLiveData<>(new TreeMap<>());
    private final MutableLiveData<TrainingState> trainingState = new MutableLiveData<>(TrainingState.NOT_STARTED);
    private final MutableLiveData<Float> lastLoss = new MutableLiveData<>();
    private final MutableLiveData<Boolean> inferenceSnackbarWasDisplayed = new MutableLiveData<>(false);

    private final ArrayList<String> positions = new ArrayList<>();

    private LiveData<String> firstChoice;

    public void setCaptureMode(boolean newValue) {
        captureMode.postValue(newValue);
    }

    public ArrayList<String> getPositions() {
        return positions;
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
        int currentNumber = 0;
        assert map != null;
        if (map.containsKey(className)) {
            Integer boxedCurrentNumber = map.get(className);
            if(boxedCurrentNumber != null)
            currentNumber = boxedCurrentNumber;
        }
        map.put(className, currentNumber + 1);
        increaseNumSamplesCurrent(className);
        numSamples.postValue(map);
    }

    private void increaseNumSamplesCurrent(String className) {
        Map<String, Integer> map = numSamplesCurrent.getValue();
        int currentNumber = 0;
        assert map != null;
        if (map.containsKey(className)) {
            Integer boxedCurrentNumber = map.get(className);
            if(boxedCurrentNumber != null)
                currentNumber = boxedCurrentNumber;
            Integer boxedNumSamplesMax = numSamplesMax.getValue();
            if(boxedNumSamplesMax != null) {
                if (currentNumber > boxedNumSamplesMax) currentNumber = 0;
            }
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

    /** Whether model training is not yet started, already started, or temporarily paused. */
    public LiveData<TrainingState> getTrainingState() {
        return trainingState;
    }

    public void setTrainingState(TrainingState newValue)
    {
        trainingState.postValue(newValue);
        Log.d(TAG, "addSamples: Set Training State to " + newValue.toString());
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

    /**
     * Name of the class with the highest confidence score.
     */
    public LiveData<String> getFirstChoice() {
        if (firstChoice == null) {
            firstChoice = Transformations.map(confidence, map -> {
                if (map.isEmpty()) {
                    return null;
                }
            try {
                return mapEntriesDecreasingValue(map).get(0).getKey();
            } catch (ConcurrentModificationException | NullPointerException e) {
                Log.e(TAG, "mapEntriesDecreasingValue: " + e.toString());
                return "";
            }
            });
        }
        return firstChoice;
    }

    private static List<Map.Entry<String, Float>> mapEntriesDecreasingValue(Map<String, Float> map) {
        List<Map.Entry<String, Float>> entryList = null;
        try {
            entryList = new ArrayList<>(map.entrySet());
            entryList.sort((e1, e2) -> -Float.compare(e1.getValue(), e2.getValue()));
        }catch (NullPointerException | ConcurrentModificationException e){e.printStackTrace();}
        return entryList;
    }
}
