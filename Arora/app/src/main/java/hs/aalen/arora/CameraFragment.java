package hs.aalen.arora;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hs.aalen.arora.dialogues.DialogFactory;
import hs.aalen.arora.dialogues.DialogType;
import hs.aalen.arora.persistence.DatabaseHelper;
import hs.aalen.arora.persistence.SQLiteHelper;
import hs.aalen.arora.utils.DateUtils;
import hs.aalen.arora.utils.FocusBoxImage;
import hs.aalen.arora.utils.ImageUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Class that handles the Camera Usage and shows Object Information in Real Time
 * <p>
 * Parts of this code are taken from:
 * https://github.com/tensorflow/examples/blob/master/lite/examples/model_personalization/
 *
 * @author Michael Schlosser
 */
public class CameraFragment extends Fragment {
    private static final String TAG = CameraFragment.class.getSimpleName();
    // Front or back camera
    private static final CameraX.LensFacing LENS_FACING = CameraX.LensFacing.BACK;
    /**
     * Class that is trained will be saved to this queue
     */
    private final ConcurrentLinkedQueue<String> addSampleRequests = new ConcurrentLinkedQueue<>();
    // Put all items to this list for simpler use
    private final ArrayList<TextView> expandableViewList = new ArrayList<>();
    int numSamplesPerClass;
    private Context context;
    // Expandable Object Data CardView
    private TextView objectName;
    private TextView objectConfidence;
    private TextView typeObjectInfoValues;
    private TextView additionalObjectInfoValues;
    private TextView timestampObjectInfoValues;
    private ImageView objectPreviewImage;
    private FloatingActionButton expandCollapseButton;
    private CardView objectDataCardView;
    private ProgressBar trainingProgressBar;
    private TextView trainingProgressBarLabel;
    private TextView trainingProgressBarTextView;
    private TextView countDownTextView;
    private TextureView viewFinder;
    private Integer viewFinderRotation = null;
    private Size bufferDimens = new Size(0, 0);
    private DatabaseHelper databaseHelper;
    private Size viewFinderDimens = new Size(0, 0);
    private CameraFragmentViewModel viewModel;

    private ContinualLearningModelWrapper continualLearningModel;
    private Bitmap preview = null;
    private String currentObjectName; // for mapping preview image correctly

    private String modelID;
    private double focusBoxRatio;
    private ArrayList<String> positionsList;
    private int countdown;
    private int confidenceThres;

    private boolean newObjectAdded;

    /**
     * Analyzer is responsible for processing camera input
     * (including inference and send training samples to transfer learning model)
     * into an RGB Float matrix
     */
    private final ImageAnalysis.Analyzer inferenceAnalyzer =
            (imageProxy, rotationDegrees) -> {
                // Preprocess camera images all the time, because it is needed by inference and by training
                Bitmap rgbBitmap;
                try {
                    rgbBitmap = ImageUtils.yuvCameraImageToBitmap(imageProxy);
                } catch (NullPointerException | IllegalStateException e) {
                    return;
                }

                float[] rgbImage = ImageUtils.prepareCameraImage(rgbBitmap, rotationDegrees, identifyFocusBoxCorners(rgbBitmap.getWidth(), rgbBitmap.getHeight(), focusBoxRatio));
                // Get the head of queue
                String sampleClass = addSampleRequests.poll();

                // Training Mode
                if (sampleClass != null) {
                    if (preview == null) {
                        preview = ImageUtils.scaleAndRotateBitmap(rgbBitmap,
                                rotationDegrees,
                                ContinualLearningModelWrapper.IMAGE_SIZE,
                                identifyFocusBoxCorners(rgbBitmap.getWidth(),
                                        rgbBitmap.getHeight(),
                                        focusBoxRatio)
                        );
                        // Update the preview image for the specific object
                        databaseHelper.updateImageBlob(currentObjectName, preview);
                    }
                    try {
                        // Add Sample to model and save to DB for Latent Replay
                        continualLearningModel.addSample(rgbImage, sampleClass).get();
                        continualLearningModel.storeTrainingSample();
                    } catch (ExecutionException | NullPointerException | IllegalStateException e) {
                        // Rollback DB if something went wrong
                        removeObjectFromModel(currentObjectName, modelID, true);
                        Toast.makeText(context, R.string.please_do_not_change_tabs, Toast.LENGTH_LONG).show();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Necessary in order to avoid exception because TFL API learns in training batches of 20 Samples
                    viewModel.increaseNumSamples(sampleClass);
                    if (numSamplesPerClass > 20) {
                        viewModel.setTrainingState(CameraFragmentViewModel.TrainingState.STARTED);
                        numSamplesPerClass = 0;
                    }
                }
                // Inference Mode
                else {
                    // reset preview because training mode is not active
                    preview = null;
                    // We don't perform inference when adding samples, since we should be in capture mode
                    // at the time, so the inference results are not actually displayed.
                    TransferLearningModel.Prediction[] predictions = null;
                    try {
                        predictions = continualLearningModel.predict(rgbImage);
                    } catch (NullPointerException | IllegalStateException e) {
                        // do nothing
                    }
                    if (predictions == null) {
                        return;
                    }
                    for (TransferLearningModel.Prediction prediction : predictions) {
                        viewModel.setConfidence(prediction.getClassName(), prediction.getConfidence());
                    }
                    Log.d(TAG, "addSamples: inference: object is: " + viewModel.getFirstChoice().getValue());
                }
            };

    /**
     * Add specific amount of Training Data to queue
     * Also maps an open model position to the object
     *
     * @param objectName position of object to train
     * @param amount     amount of input samples for training
     */
    public void addSamples(String objectName, int amount) {
        currentObjectName = objectName;
        Log.d(TAG, "addSamples: current class: " + objectName);
        String openPos = databaseHelper.getObjectModelPosByName(objectName);
        if(openPos == null) {
            openPos = getOpenModelPosition();
        }
        if (openPos.equals("")) {
            Toast.makeText(context, R.string.model_is_full, Toast.LENGTH_SHORT).show();
            removeObjectFromModel(currentObjectName, modelID, false);
            return;
        }
        // Add the model position finally to the object in the DB
        if (databaseHelper.updateModelPos(objectName, openPos)) {
            databaseHelper.addSamplesToObject(objectName, amount);
            AddSamplesThread addSamplesThread = new AddSamplesThread(openPos, amount);
            addSamplesThread.start();
        } else {
            Toast.makeText(context, R.string.error_occured, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Chooses the first open position of the model
     *
     * @return An open Position that the object can set
     */
    private String getOpenModelPosition() {
        String openPosition = "";
        if (viewModel.getPositions().isEmpty()) {
            Log.w(TAG, "getOpenModelPosition: No Model Position open!");
        } else {
            // Set the position of the model to the first open position and remove it afterwards
            openPosition = viewModel.getPositions().get(0);
            viewModel.getPositions().remove(0);
        }
        return openPosition;
    }

    /**
     * Remove the object from the Database and adds an open position back to the model
     * (Used for rollback)
     *
     * @param objectName  name of the object in the model
     * @param modelID     ID of the currently selected model
     * @param hasPosition flag to check if a model position has to be freed up
     */
    public void removeObjectFromModel(String objectName, String modelID, boolean hasPosition) {
        databaseHelper.deleteObjectByNameAndModelID(objectName, modelID);
        // Add the model position back to the Position-ArrayList
        if (hasPosition) {
            String posOfObject = databaseHelper.getObjectModelPosByNameAndModelID(objectName, modelID);
            if (!(viewModel.getPositions().contains(posOfObject))) {
                viewModel.getPositions().add(posOfObject);
            }
        }
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newObjectAdded = false;
        viewModel = new ViewModelProvider(this).get(CameraFragmentViewModel.class);
        databaseHelper = new SQLiteHelper(getActivity());
        loadNewModel();
    }

    /**
     * Initialize model with open positions and map them to objects in DB
     */
    public void loadNewModel() {
        this.viewModel.getPositions().addAll(positionsList);
        if (modelID == null) {
            // Create new model in Dialog
            DialogFactory.getDialog(DialogType.ADD_MODEL).createDialog(context);
        } else {
            mapObjectsFromDB();
            continualLearningModel = new ContinualLearningModelWrapper(getActivity(), positionsList, modelID);
        }
    }

    /**
     * Compares saved model positions of saved objects with open positions in current model
     */
    private void mapObjectsFromDB() {
        if (databaseHelper.objectsExist()) {
            removeCorruptObjects();
            Cursor data = this.databaseHelper.getAllObjectsByModelID(modelID);
            while (data.moveToNext()) {
                try {
                    // Map the object to its belonging position in the model
                    viewModel.getPositions().remove(data.getString(6));
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                // Return if there are no open positions in the model anymore
                if (viewModel.getPositions().isEmpty()) {
                    return;
                }
            }
        }
    }

    /**
     * Deletes an object if there went something wrong during adding (e.g. missing model pos)
     * because they cannot be mapped otherwise
     */
    public void removeCorruptObjects() {
        Cursor data = this.databaseHelper.getAllObjects();
        while (data.moveToNext()) {
            String modelPos = data.getString(6);
            String modelID = data.getString(7);
            if (modelPos == null || modelID == null) {
                databaseHelper.deleteObjectById(data.getString(0));
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: entrypoint");
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @org.jetbrains.annotations.NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize CardView and including view when the view is created
        countDownTextView = requireActivity().findViewById(R.id.countdown);
        initCardView();
        // define focusbox
        FocusBoxImage focusBox = requireActivity().findViewById(R.id.focus_box);
        focusBox.setFocusBoxLocation(identifyFocusBoxCorners(this.getResources().getDisplayMetrics().widthPixels,
                this.getResources().getDisplayMetrics().heightPixels, focusBoxRatio));

        // Enable/Disable training
        viewModel
                .getTrainingState()
                .observe(
                        getViewLifecycleOwner(),
                        trainingState -> {
                            switch (trainingState) {
                                case STARTED:
                                    continualLearningModel.enableTraining((epoch, loss) -> viewModel.setLastLoss(loss));
                                    Log.d(TAG, "addSamples:  training enabled");
                                    break;
                                case PAUSED:
                                    continualLearningModel.disableTraining();
                                    Log.d(TAG, "addSamples: training disabled (pause)");
                                    setProgressCircle(0);
                                    break;
                                case NOT_STARTED:
                                    Log.d(TAG, "addSamples: training disabled (not started)");
                                    setProgressCircle(0);
                                    break;
                            }
                        });
        // For determinate progress bar
        viewModel
                .getNumSamples()
                .observe(
                        getViewLifecycleOwner(),
                        numSamples -> {
                            Integer trainingProgressMax = viewModel.getNumSamplesMax().getValue();
                            if (trainingProgressMax != null)
                                trainingProgressBar.setMax(trainingProgressMax);
                            int progress = 0;
                            try {
                                Integer wrappedNumSamples = numSamples.get(addSampleRequests.peek());
                                if (wrappedNumSamples != null)
                                    this.numSamplesPerClass = wrappedNumSamples;
                                Map<String, Integer> mapNumSamplesWrapped = viewModel.getNumSamplesCurrent().getValue();
                                if (mapNumSamplesWrapped != null) {
                                    Integer wrappedNumSamplesCurrent = mapNumSamplesWrapped.get(addSampleRequests.peek());
                                    if (wrappedNumSamplesCurrent != null)
                                        progress = wrappedNumSamplesCurrent;
                                }
                            } catch (NullPointerException e) { // Do nothing
                            }
                            setProgressCircle(progress);
                        }
                );
        // For showing object information
        final Observer<String> firstChoiceObserver = s -> {
            try {
                Map<String, Float> wrappedConfidenceMap = viewModel.getConfidence().getValue();
                float confidence = 0;
                if (wrappedConfidenceMap != null) {
                    Float wrappedConfidence = wrappedConfidenceMap.get(s);
                    if (wrappedConfidence != null) {
                        confidence = wrappedConfidence;
                    }
                }
                float confidencePercent = confidence * 100;
                DecimalFormat df = new DecimalFormat("##.#");
                if (confidencePercent > confidenceThres) {
                    if (databaseHelper.objectsExist())
                        objectConfidence.setText(String.format("%s %%", df.format(confidencePercent)));
                    if (!s.isEmpty()) populateAllViewItems(s);
                }
                else {
                    objectConfidence.setText("");
                    objectName.setText("");
                }
            } catch (NullPointerException e) {
                // Do nothing
            }
        };
        viewModel.getFirstChoice().observe(getViewLifecycleOwner(), firstChoiceObserver);
        viewFinder = requireActivity().findViewById(R.id.view_finder);
        viewFinder.post(this::startCamera);
    }

    /**
     * Helper method to initialize all view elements in this fragment
     */
    private void initCardView() {
        objectName = requireActivity().findViewById(R.id.object_name);
        objectConfidence = requireActivity().findViewById(R.id.object_confidence);
        objectDataCardView = requireActivity().findViewById(R.id.object_metadata_cardview);
        TextView typeObjectInfoColumns = requireActivity().findViewById(R.id.type_object_info_columns);
        typeObjectInfoValues = requireActivity().findViewById(R.id.type_object_info_values);
        TextView additionalObjectInfoColumns = requireActivity().findViewById(R.id.additional_object_info_columns);
        additionalObjectInfoValues = requireActivity().findViewById(R.id.additional_object_info_values);
        TextView timestampObjectInfoColumns = requireActivity().findViewById(R.id.timestamp_object_info_columns);
        timestampObjectInfoValues = requireActivity().findViewById(R.id.timestamp_object_info_values);
        objectPreviewImage = requireActivity().findViewById(R.id.object_info_preview_image);
        expandCollapseButton = requireActivity().findViewById(R.id.expand_collapse_button);

        trainingProgressBar = requireActivity().findViewById(R.id.progressbar_training);
        trainingProgressBarLabel = requireActivity().findViewById(R.id.progressbar_training_text);
        trainingProgressBarTextView = requireActivity().findViewById(R.id.progressbar_textview);

        List<TextView> itemsCardView = Arrays.asList(typeObjectInfoColumns, typeObjectInfoValues,
                additionalObjectInfoColumns, additionalObjectInfoValues,
                timestampObjectInfoColumns, timestampObjectInfoValues);
        expandableViewList.addAll(itemsCardView);

        expandCollapseButton.setOnClickListener(v -> showMore());

    }

    /**
     * Function that identifies the corners of a square in the center of the display
     *
     * @param width  of Bitmap
     * @param height of Bitmap
     * @param ratio  crop-factor
     * @return the four positions
     */
    public int[] identifyFocusBoxCorners(int width, int height, double ratio) {
        int[] locations = new int[4];
        // You can only square the smaller side. Otherwise there would occur an OutOfBoundsException
        int size = (int) ((Math.min(width, height)) * ratio);
        Point center = new Point(width / 2, height / 2);
        locations[0] = center.x - (size / 2); // left
        locations[1] = center.y - (size / 2); // top
        locations[2] = center.x + (size / 2); // right
        locations[3] = center.y + (size / 2); // bottom

        return locations;
    }

    /**
     * Helper function to set percentual amount of progress in progress bar
     */
    private void setProgressCircle(int progress) {
        Log.d(TAG, "addSamples setProgressCircle Max: " + trainingProgressBar.getMax() + " progress: " + progress);
        if (progress == 0) {
            objectName.setVisibility(VISIBLE);
            objectConfidence.setVisibility(VISIBLE);
            trainingProgressBarLabel.setText(R.string.detecting_progress);
            trainingProgressBarTextView.setVisibility(GONE);
            trainingProgressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.circle));
            trainingProgressBarLabel.setTextColor(ContextCompat.getColor(context, R.color.blue_arora));
            trainingProgressBar.setIndeterminate(true);
        } else {
            objectName.setVisibility(View.INVISIBLE);
            objectConfidence.setVisibility(View.INVISIBLE);
            trainingProgressBarLabel.setText(R.string.training_progress);
            trainingProgressBarLabel.setTextColor(ContextCompat.getColor(context, R.color.green));
            trainingProgressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.circle_green));
            trainingProgressBarTextView.setVisibility(VISIBLE);
            trainingProgressBar.setIndeterminate(false);
        }

        trainingProgressBar.setProgress(progress);
        int relativeProgress = (int) (((float) progress / (float) trainingProgressBar.getMax()) * 100);
        String relativeProgressStr = relativeProgress + "%";
        trainingProgressBarTextView.setText(relativeProgressStr);
    }

    /**
     * Populates all item from expandable card view with data from DB
     *
     * @param pos primary key of data
     */
    private void populateAllViewItems(String pos) {
        Cursor data = this.databaseHelper.getObjectByModelPosAndModelID(pos, modelID);
        if (data.moveToFirst()) {
            // Object name
            objectName.setText(data.getString(1));
            // Preview image
            if (data.getBlob(5) != null) {
                byte[] image = data.getBlob(5);
                objectPreviewImage.setImageBitmap(
                        BitmapFactory.decodeByteArray(image, 0, image.length));
            }

            // expandable view
            for (TextView item : expandableViewList) {
                if (item == typeObjectInfoValues) {
                    item.setText(data.getString(2));
                } else if (item == additionalObjectInfoValues) {
                    item.setText(data.getString(3));
                } else if (item == timestampObjectInfoValues) {
                    String dateString = data.getString(4);
                    item.setText(DateUtils.parseDateTime(dateString));
                }
            }
        }
    }

    /**
     * Setup the Camera Preview and Analysis Method using CameraX
     */
    public void startCamera() {
        // Check if display is in portrait or landscape mode
        viewFinderRotation = ImageUtils.getDisplaySurfaceRotation(viewFinder.getDisplay());
        if (viewFinderRotation == null) {
            viewFinderRotation = 0;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(metrics);

        // Workaround: display metrics will return width and height of display WITH action bar at the top
        // Therefore the height of pixels had to be subtracted
        Rational screenAspectRatio = new Rational(metrics.widthPixels, metrics.heightPixels - 385);

        PreviewConfig config = new PreviewConfig.Builder()
                .setLensFacing(LENS_FACING)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .setTargetResolution(new Size(metrics.widthPixels, metrics.heightPixels))
                .build();
        Preview preview = new Preview(config);

        preview.setOnPreviewOutputUpdateListener(previewOutput -> {
            ViewGroup parent = (ViewGroup) viewFinder.getParent();
            parent.removeView(viewFinder);
            parent.addView(viewFinder, 0);

            viewFinder.setSurfaceTexture(previewOutput.getSurfaceTexture());

            Integer rotation = ImageUtils.getDisplaySurfaceRotation(viewFinder.getDisplay());
            updateTransform(rotation, previewOutput.getTextureSize(), viewFinderDimens);
        });
        viewFinder.addOnLayoutChangeListener((
                view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            Size newViewFinderDimens = new Size(right - left, bottom - top);
            Integer rotation = ImageUtils.getDisplaySurfaceRotation(viewFinder.getDisplay());
            updateTransform(rotation, bufferDimens, newViewFinderDimens);
        });
        HandlerThread inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                .setLensFacing(LENS_FACING)
                .setCallbackHandler(new Handler(inferenceThread.getLooper()))
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(analysisConfig);
        imageAnalysis.setAnalyzer(inferenceAnalyzer);
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    /**
     * expand the card view and show all object info at the top of the fragment
     * when clicking the expand/collapse button
     */
    private void showMore() {
        if (viewIsVisible()) {
            TransitionManager.beginDelayedTransition(objectDataCardView, new AutoTransition());
            setVisibilityAll(GONE);
            objectPreviewImage.setVisibility(GONE);
            expandCollapseButton.setImageResource(R.drawable.ic_expand);
        } else {
            TransitionManager.beginDelayedTransition(objectDataCardView, new AutoTransition());
            setVisibilityAll(VISIBLE);
            objectPreviewImage.setVisibility(VISIBLE);
            expandCollapseButton.setImageResource(R.drawable.ic_collapse);
        }
    }

    /**
     * Fit the camera preview into the ViewFinder
     *
     * @param rotation            view finder rotation.
     * @param newBufferDimens     camera preview dimensions.
     * @param newViewFinderDimens view finder dimensions.
     */
    private void updateTransform(Integer rotation, Size newBufferDimens, Size newViewFinderDimens) {
        if (Objects.equals(rotation, viewFinderRotation)
                && Objects.equals(newBufferDimens, bufferDimens)
                && Objects.equals(newViewFinderDimens, viewFinderDimens)) {
            return;
        }

        if (rotation == null) {
            return;
        } else {
            viewFinderRotation = rotation;
        }

        if (newBufferDimens.getWidth() == 0 || newBufferDimens.getHeight() == 0) {
            return;
        } else {
            bufferDimens = newBufferDimens;
        }

        if (newViewFinderDimens.getWidth() == 0 || newViewFinderDimens.getHeight() == 0) {
            return;
        } else {
            viewFinderDimens = newViewFinderDimens;
        }

        Log.d(TAG, String.format("Applying output transformation.\n"
                + "View finder size: %s.\n"
                + "Preview output size: %s\n"
                + "View finder rotation: %s\n", viewFinderDimens, bufferDimens, viewFinderRotation));
        Matrix matrix = new Matrix();

        float centerX = viewFinderDimens.getWidth() / 2f;
        float centerY = viewFinderDimens.getHeight() / 2f;

        matrix.postRotate(-viewFinderRotation.floatValue(), centerX, centerY);

        float bufferRatio = bufferDimens.getHeight() / (float) bufferDimens.getWidth();

        int scaledWidth;
        int scaledHeight;
        if (viewFinderDimens.getWidth() > viewFinderDimens.getHeight()) {
            scaledHeight = viewFinderDimens.getWidth();
            scaledWidth = Math.round(viewFinderDimens.getWidth() * bufferRatio);
        } else {
            scaledHeight = viewFinderDimens.getHeight();
            scaledWidth = Math.round(viewFinderDimens.getHeight() * bufferRatio);
        }

        float xScale = scaledWidth / (float) viewFinderDimens.getWidth();
        float yScale = scaledHeight / (float) viewFinderDimens.getHeight();

        matrix.preScale(xScale, yScale, centerX, centerY);

        viewFinder.setTransform(matrix);
    }

    /**
     * Helper function for showMore that checks if items are visible or not
     *
     * @return true if view is visible, false otherwise
     */
    private boolean viewIsVisible() {
        for (TextView item : expandableViewList) {
            if (item.getVisibility() == VISIBLE) {
                Log.d(TAG, "showMore: viewIsVisible: " + item.getVisibility());
                return true;
            }
        }
        return false;
    }

    /**
     * Helper function for showMore
     * that makes textViews un-/visible
     *
     * @param visibility either GONE oder VISIBLE
     */
    private void setVisibilityAll(int visibility) {
        for (TextView item : expandableViewList) {
            item.setVisibility(visibility);
        }
    }

    @Override
    public void onResume() {
        mapObjectsFromDB();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.setTrainingState(CameraFragmentViewModel.TrainingState.PAUSED);

        if (addSampleRequests.size() != 0) {
            addSampleRequests.clear();
            Toast.makeText(context, R.string.please_do_not_change_tabs, Toast.LENGTH_SHORT).show();
        }
        continualLearningModel.close();
        continualLearningModel = null;
    }

    public void setCountDown(int countDown) {
        this.countdown = countDown;
    }

    public void setConfidenceThres(int confidenceThres) {
        this.confidenceThres = confidenceThres;
    }

    public void setFocusBoxRatio(double focusBoxRatio) {
        this.focusBoxRatio = focusBoxRatio;
    }

    public void setModelID(String id) {
        this.modelID = id;
    }

    public void setPositionsList(int maxObjects) {
        ArrayList<String> positionsList = new ArrayList<>();
        for (int i = 1; i < maxObjects + 1; i++) {
            String pos = i + "";
            positionsList.add(pos);
        }
        this.positionsList = positionsList;
    }

    /**
     * Background Thread to add Sample Requests
     */
    class AddSamplesThread extends Thread {
        int numSamples;
        String modelPosition;
        CountDownTimer countDownThread;

        AddSamplesThread(String modelPosition, int numSamples) {
            this.modelPosition = modelPosition;
            this.numSamples = numSamples;

            countDownThread = new CountDownTimer((countdown * 1000), 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    String count = (millisUntilFinished / 1000) + "";
                    if(count.equals("0")) count = "Start!";
                    countDownTextView.setText(count);
                }

                @Override
                public void onFinish() {
                    countDownTextView.setText("");
                    addSamples();
                }
            };
        }

        public void addSamples() {
            viewModel.setCaptureMode(true);
            Log.d(TAG, "addSamples: Capture Mode is enabled!");
            for (int i = 0; i < numSamples; i++) {
                addSampleRequests.add(modelPosition);
                Log.d(TAG, "addSamples: Add sample #" + i + " with name: " + modelPosition + "(queue size: " + addSampleRequests.size() + ")");
            }
            viewModel.setNumSamplesMax(addSampleRequests.size());
            viewModel.setCaptureMode(false);
            Log.d(TAG, "addSamples: Capture Mode is disabled!");
        }

        @Override
        public void run() {
            countDownThread.start();
        }
    }

    public boolean hasNewObjectAdded() {
        return newObjectAdded;
    }

    public void setNewObjectAdded(boolean newObjectAdded) {
        this.newObjectAdded = newObjectAdded;
    }

    /**
     * Shows a loading spinner while the heavy update algorithm is executed
     */
    public void updateReplayBuffer() {
        ProgressBar replaySpinner = requireActivity().findViewById(R.id.wait_for_replay_spinner);
        TextView replayText = requireActivity().findViewById(R.id.wait_for_replay_spinner_text);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(()-> {
            replaySpinner.setVisibility(VISIBLE);
            replayText.setVisibility(VISIBLE);
        });
        executor.execute(() -> {
            continualLearningModel.updateReplayBufferSmart();
            handler.post(() -> {
                replaySpinner.setVisibility(View.INVISIBLE);
                replayText.setVisibility(View.INVISIBLE);
                Toast.makeText(context, R.string.succesfully_configured_replay, Toast.LENGTH_SHORT).show();
            });
        });
    }
}