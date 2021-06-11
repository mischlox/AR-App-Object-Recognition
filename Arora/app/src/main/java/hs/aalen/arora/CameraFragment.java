package hs.aalen.arora;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
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
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

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
    static final Object sync = new Object();
    private static final String TAG = CameraFragment.class.getSimpleName();
    // Front or back camera
    private static final CameraX.LensFacing LENS_FACING = CameraX.LensFacing.BACK;
    private static final int LOWER_BYTE_MASK = 0xFF;
    // Amount of classes
    private static final int NUM_TFL_CLASSES = 4;
    /**
     * Class that is trained will be saved to this queue
     */
    private final ConcurrentLinkedQueue<String> addSampleRequests = new ConcurrentLinkedQueue<>();
    int numSamplesPerClass;
    // Expandable Object Data CardView
    private TextView objectName;
    private TextView objectConfidence;
    private TextView typeObjectInfoColumns;
    private TextView typeObjectInfoValues;
    private TextView additionalObjectInfoColumns;
    private TextView additionalObjectInfoValues;
    private TextView timestampObjectInfoColumns;
    private TextView timestampObjectInfoValues;
    private ImageView objectPreviewImage;
    // Put all items to this list for simpler use
    private final ArrayList<TextView> expandableViewList = new ArrayList<>();
    private FloatingActionButton expandCollapseButton;
    private CardView objectDataCardView;
    private ProgressBar trainingProgressBar;
    private TextView trainingProgressBarLabel;
    private TextView trainingProgressBarTextView;
    private TextureView viewFinder;
    private Integer viewFinderRotation = null;
    private Size bufferDimens = new Size(0, 0);
    private DatabaseHelper databaseHelper;
    private Size viewFinderDimens = new Size(0, 0);
    private CameraFragmentViewModel viewModel;
    private TransferLearningModelWrapper transferLearningModel;
    private Bitmap preview = null;
    private String currentClass; // for mapping preview image correctly

    SharedPreferences sharedPreferences;


    /**
     * Analyzer is responsible for processing camera input
     * (including inference and send training samples to transfer learning model)
     * into an RGB Float matrix
     */
    private final ImageAnalysis.Analyzer inferenceAnalyzer =
            (imageProxy, rotationDegrees) -> {
                final String imageId = UUID.randomUUID().toString();
                // Preprocess camera images all the time, because it is needed by inference and by training
                Bitmap rgbBitmap;
                try {
                    rgbBitmap = yuvCameraImageToBitmap(imageProxy);
                } catch (NullPointerException e) {return;}
                float[] rgbImage = prepareCameraImage(rgbBitmap, rotationDegrees);
                // Get the head of queue
                String sampleClass = addSampleRequests.poll();

                // Training Mode
                if (sampleClass != null) {
                    if(preview == null) {
                        Log.d(TAG, "addSamples preview: put Image to object " + sampleClass);
                        preview = scaleAndRotateBitmap(rgbBitmap, rotationDegrees, TransferLearningModelWrapper.IMAGE_SIZE);
                        databaseHelper.updateImageBlob(currentClass, preview);
                    }
                    try {
                        transferLearningModel.addSample(rgbImage, sampleClass).get();
                    } catch (ExecutionException e) {
                        throw new RuntimeException("Failed to add sample to model", e.getCause());
                    } catch (InterruptedException | NullPointerException e) {
                    }
                    viewModel.increaseNumSamples(sampleClass);
                    if (numSamplesPerClass > 20) {
                        viewModel.setTrainingState(CameraFragmentViewModel.TrainingState.STARTED);
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
                        predictions = transferLearningModel.predict(rgbImage);
                    } catch (NullPointerException e) {

                    }
                    if (predictions == null) {
                        return;
                    }
                    for (TransferLearningModel.Prediction prediction : predictions) {
                        viewModel.setConfidence(prediction.getClassName(), prediction.getConfidence());
                        Log.d(TAG, "addSamples: inference: new Prediction: " + prediction.getClassName() + " conf: " + prediction.getConfidence());
                    }
                    Log.d(TAG, "addSamples: inference: object is: " + viewModel.getFirstChoice().getValue());
                }
            };

    /**
     * Takes an ImageProxy object in YUV format
     * and converts it into a RGB Bitmap
     *
     * @param imageProxy the image that will be converted
     * @return the converted image as a Bitmap
     */
    private static Bitmap yuvCameraImageToBitmap(ImageProxy imageProxy) throws NullPointerException {
        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException(
                    "Expected a YUV420 image, but got " + imageProxy.getFormat());
        }

        ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = imageProxy.getPlanes()[1];

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();

        byte[][] yuvBytes = new byte[3][];
        int[] argbArray = new int[width * height];
        for (int i = 0; i < imageProxy.getPlanes().length; i++) {
            final ByteBuffer buffer = imageProxy.getPlanes()[i].getBuffer();
            yuvBytes[i] = new byte[buffer.capacity()];
            buffer.get(yuvBytes[i]);
        }

        ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                width,
                height,
                yPlane.getRowStride(),
                uPlane.getRowStride(),
                uPlane.getPixelStride(),
                argbArray);

        return Bitmap.createBitmap(argbArray, width, height, Bitmap.Config.ARGB_8888);
    }

    /**
     * Normalizes a camera image to [0 ; 1] and crops it
     * to the size that is expected by the model
     *
     * @param bitmap          The converted image that will be normalized and cropped
     * @param rotationDegrees Handles landscape/portrait mode with post rotation if necessary
     * @return the cropped and normalized image
     */
    private static float[] prepareCameraImage(Bitmap bitmap, int rotationDegrees) {
        int modelImageSize = TransferLearningModelWrapper.IMAGE_SIZE;
        Bitmap rotatedBitmap = scaleAndRotateBitmap(bitmap, rotationDegrees, modelImageSize);

        float[] normalizedRgb = new float[modelImageSize * modelImageSize * 3];
        int nextIdx = 0;
        for (int y = 0; y < modelImageSize; y++) {
            for (int x = 0; x < modelImageSize; x++) {
                int rgb = rotatedBitmap.getPixel(x, y);

                float r = ((rgb >> 16) & LOWER_BYTE_MASK) * (1 / 255.f);
                float g = ((rgb >> 8) & LOWER_BYTE_MASK) * (1 / 255.f);
                float b = (rgb & LOWER_BYTE_MASK) * (1 / 255.f);

                normalizedRgb[nextIdx++] = r;
                normalizedRgb[nextIdx++] = g;
                normalizedRgb[nextIdx++] = b;
            }
        }

        return normalizedRgb;
    }

    private static Bitmap scaleAndRotateBitmap(Bitmap bitmap, int rotationDegrees, int modelImageSize) {

        Bitmap paddedBitmap = padToSquare(bitmap);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                paddedBitmap, modelImageSize, modelImageSize, true);

        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(
                scaledBitmap, 0, 0, modelImageSize, modelImageSize, rotationMatrix, false);
    }

    /**
     * Crop image into square format
     *
     * @param source image to be cropped
     * @return cropped image
     */
    private static Bitmap padToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        int paddingX = width < height ? (height - width) / 2 : 0;
        int paddingY = height < width ? (width - height) / 2 : 0;
        Bitmap paddedBitmap = Bitmap.createBitmap(
                width + 2 * paddingX, height + 2 * paddingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF);
        canvas.drawBitmap(source, paddingX, paddingY, null);
        return paddedBitmap;
    }

    /**
     * Get the display rotation
     *
     * @param display android display
     * @return rotation value
     */
    private static Integer getDisplaySurfaceRotation(Display display) {
        if (display == null) {
            return null;
        }

        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return null;
        }
    }

    /**
     * expand the cardview and show all object info at the top of the fragment
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
            Log.d(TAG, "showMore: setVisibilityAll: for " + item.getText());

            item.setVisibility(visibility);
        }
    }

    /**
     * Populates all item from expandable cardview with data from DB
     *
     * @param pos primary key of data
     */
    private void populateAllViewItems(String pos) {
        Log.d(TAG, "updateView setTextAll: " + pos);
        Cursor data = this.databaseHelper.getByModelPos(pos);
        printObjectFromDB(data);
        if (data.moveToFirst()) {
            Log.d(TAG, "updateView updating ...: ");
            // Object name
            objectName.setText(data.getString(1));
            // Preview image
            if(data.getBlob(5) != null) {
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
                    item.setText(data.getString(4));
                }
            }
        }
        else {
            Log.e(TAG, "updateView updateAllViewItems: No object in View!");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        viewModel = new ViewModelProvider(this).get(CameraFragmentViewModel.class);
        databaseHelper = new DatabaseHelper(getActivity());
        mapObjectsFromDB();
        List<String> classList = Arrays.asList("1", "2", "3", "4");
        transferLearningModel = new TransferLearningModelWrapper(getActivity(), classList);
        this.viewModel.getClasses().addAll(classList);
    }

    private void mapObjectsFromDB() {
        if(databaseHelper.objectExists()) {
            Cursor data = this.databaseHelper.getAllObjects();
            while (data.moveToNext()) {
                try {
                    viewModel.getClasses().remove(data.getString(6));
                } catch (IllegalStateException e) {
                    return;
                }
                Log.d(TAG, "mapObjectsFromDB: pop open pos " + data.getString(6));
                if (viewModel.getClasses().isEmpty()) {
                    Log.w(TAG, "mapObjectsFromDB: Model is full! Cannot add further data");
                    return;
                }
            }
        }
    }

    /**
     * Helper function to set percentual amount of progress in progress bar
     */
    private void setProgressCircle(int progress) {
        Log.d(TAG, "addSamples setProgressCircle Max: " + trainingProgressBar.getMax() + " progress: " + progress);
        if (progress == 0) {
            trainingProgressBarLabel.setVisibility(GONE);
            trainingProgressBarTextView.setVisibility(GONE);
        } else {
            trainingProgressBarLabel.setVisibility(VISIBLE);
            trainingProgressBarTextView.setVisibility(VISIBLE);
        }
        trainingProgressBar.setProgress(progress);
        int relativeProgress = (int) (((float) progress / (float) trainingProgressBar.getMax()) * 100);
        trainingProgressBarTextView.setText(relativeProgress + "%");

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: entrypoint");
        View inflatedView = inflater.inflate(R.layout.fragment_camera, container, false);

        return inflatedView;
    }

    @Override
    public void onViewCreated(@NonNull @org.jetbrains.annotations.NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated: entrypoint");
        super.onViewCreated(view, savedInstanceState);

        // Initialize CardView and including view when the view is created
        objectName = getActivity().findViewById(R.id.object_name);
        objectConfidence = getActivity().findViewById(R.id.object_confidence);
        objectDataCardView = getActivity().findViewById(R.id.object_metadata_cardview);
        typeObjectInfoColumns = getActivity().findViewById(R.id.type_object_info_columns);
        typeObjectInfoValues = getActivity().findViewById(R.id.type_object_info_values);
        additionalObjectInfoColumns = getActivity().findViewById(R.id.additional_object_info_columns);
        additionalObjectInfoValues = getActivity().findViewById(R.id.additional_object_info_values);
        timestampObjectInfoColumns = getActivity().findViewById(R.id.timestamp_object_info_columns);
        timestampObjectInfoValues = getActivity().findViewById(R.id.timestamp_object_info_values);
        objectPreviewImage = getActivity().findViewById(R.id.object_info_preview_image);
        expandCollapseButton = getActivity().findViewById(R.id.expand_collapse_button);

        List<TextView> itemsCardView = Arrays.asList(typeObjectInfoColumns, typeObjectInfoValues,
                additionalObjectInfoColumns, additionalObjectInfoValues,
                timestampObjectInfoColumns, timestampObjectInfoValues);
        expandableViewList.addAll(itemsCardView);

        expandCollapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMore();
            }
        });

        trainingProgressBar = getActivity().findViewById(R.id.progressbar_training);
        trainingProgressBarLabel = getActivity().findViewById(R.id.progressbar_training_text);
        trainingProgressBarTextView = getActivity().findViewById(R.id.progressbar_textview);
        // Enable/Disable training
        viewModel
                .getTrainingState()
                .observe(
                        getViewLifecycleOwner(),
                        trainingState -> {
                            switch (trainingState) {
                                case STARTED:
                                    transferLearningModel.enableTraining((epoch, loss) -> viewModel.setLastLoss(loss));
                                    Log.d(TAG, "addSamples:  training enabled");
                                    break;
                                case PAUSED:
                                    transferLearningModel.disableTraining();
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
                            trainingProgressBar.setMax(viewModel.getNumSamplesMax().getValue());
                            int progress;
                            try {
                                this.numSamplesPerClass = numSamples.get(addSampleRequests.peek());
                                progress = viewModel.getNumSamplesCurrent().getValue().get(addSampleRequests.peek());
                            } catch (NullPointerException e) {
                                progress = 0;
                            }
                            setProgressCircle(progress);
                        }
                );
        // For showing object information
        final Observer<String> firstChoiceObserver = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                try {
                    float confidence = viewModel.getConfidence().getValue().get(s);
                    float confidencePercent = confidence * 100;
                    DecimalFormat df = new DecimalFormat("##.#");
                    objectConfidence.setText(df.format(confidencePercent) + " %");
                    if(!s.isEmpty()) populateAllViewItems(s);
                } catch (NullPointerException e) {
                }
            }
        };
        viewModel.getFirstChoice().observe(getViewLifecycleOwner(), firstChoiceObserver);
        transferLearningModel.loadParametersFromDB();
        viewFinder = getActivity().findViewById(R.id.view_finder);
        viewFinder.post(this::startCamera);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        transferLearningModel.close();
        transferLearningModel = null;
    }

    /**
     * Setup the Camera Preview and Analysis Method using CameraX
     */
    private void startCamera() {
        // Check if display is in portrait or landscape mode
        viewFinderRotation = getDisplaySurfaceRotation(viewFinder.getDisplay());
        if (viewFinderRotation == null) {
            viewFinderRotation = 0;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(metrics);

        // Workaround: display metrics will return width and height of display WITH action bar at the top
        // Therefore I had to subtract the height of Pixels
        // TODO find a way to do this without hardcoding it
        Rational screenAspectRatio = new Rational(metrics.widthPixels, metrics.heightPixels - 385);

        PreviewConfig config = new PreviewConfig.Builder()
                .setLensFacing(LENS_FACING)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .build();
        Preview preview = new Preview(config);

        preview.setOnPreviewOutputUpdateListener(previewOutput -> {
            ViewGroup parent = (ViewGroup) viewFinder.getParent();
            parent.removeView(viewFinder);
            parent.addView(viewFinder, 0);

            viewFinder.setSurfaceTexture(previewOutput.getSurfaceTexture());

            Integer rotation = getDisplaySurfaceRotation(viewFinder.getDisplay());
            updateTransform(rotation, previewOutput.getTextureSize(), viewFinderDimens);
        });
        viewFinder.addOnLayoutChangeListener((
                view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            Size newViewFinderDimens = new Size(right - left, bottom - top);
            Integer rotation = getDisplaySurfaceRotation(viewFinder.getDisplay());
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

    @Override
    public void onResume() {
        mapObjectsFromDB();
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: this");
        super.onDestroyView();
    }

    public ConcurrentLinkedQueue<String> getAddSampleRequests() {
        return addSampleRequests;
    }

    /**
     * Add specific amount of Training Data to queue
     * Also maps a open model position to the object
     *
     * @param classname name of object to train
     * @param amount    amount of input samples for training
     */
    public void addSamples(String classname, int amount) {
        currentClass = classname;
        Log.d(TAG, "addSamples: current class: " + classname);
        String openPos = getOpenModelPosition();
        if(openPos.equals("")) {
            Toast.makeText(getContext(), "Model is full!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(databaseHelper.updateModelPos(classname, openPos)) {
            Thread addSamplesThread = new AddSamplesThread(openPos, amount);
            addSamplesThread.start();
        }
        else {
            Toast.makeText(getContext(), "An error occured!", Toast.LENGTH_SHORT).show();
        }
    }

    private String getOpenModelPosition() {
        String openPosition = "";
        if(viewModel.getClasses().isEmpty()) {
            Log.w(TAG, "getOpenModelPosition: No Model Position open!");
        }
        else {
            openPosition = viewModel.getClasses().get(0);
            viewModel.getClasses().remove(0);
        }
        return openPosition;
    }

    /**
     * Background Thread to add Sample Requests
     */
    class AddSamplesThread extends Thread {
        int amount;
        String className;

        AddSamplesThread(String className, int amount) {
            this.className = className;
            this.amount = amount;
        }

        @Override
        public void run() {
            viewModel.setCaptureMode(true);
            Log.d(TAG, "addSamples: Capture Mode is enabled!");
            for (int i = 0; i < amount; i++) {
                addSampleRequests.add(className);
                Log.d(TAG, "addSamples: Add sample #" + i + " with name: " + className + "(queue size: " + addSampleRequests.size() + ")");
            }
            viewModel.setNumSamplesMax(addSampleRequests.size());
            viewModel.setCaptureMode(false);
            Log.d(TAG, "addSamples: Capture Mode is disabled!");
        }
    }

    public CameraFragmentViewModel getViewModel() {
        return viewModel;
    }

    /**
     * For debug purposes print the open positions and model positions of objects
     */
    private void printModelMapping() {
        Log.d(TAG, "(model mapping) printModelMapping: open pos:" + viewModel.getClasses().toString());
        Cursor data = this.databaseHelper.getAllObjects();
        while(data.moveToNext()){
            String name = data.getString(1);
            String modelPos = data.getString(6);
            Log.d(TAG, "(model mapping) printModelMapping: name: " + name + " pos: " + modelPos);
        }
    }

    /**
     * For debug purposes print object information
     */
    private void printObjectFromDB(Cursor data) {
        Log.d(TAG, "updateView printObjectFromDB: <<< OBJECT INFO BEGIN >>>");
        while(data.moveToNext()) {
            Log.d(TAG, "updateView printObjectFromDB: id: " + data.getString(0));
            Log.d(TAG, "updateView printObjectFromDB: name: " + data.getString(1));
            Log.d(TAG, "updateView printObjectFromDB: type: " + data.getString(2));
            Log.d(TAG, "updateView printObjectFromDB: additional: " + data.getString(3));
            Log.d(TAG, "updateView printObjectFromDB: timestamp: " + data.getString(4));
            Log.d(TAG, "updateView printObjectFromDB: blob: " + (data.getBlob(5) != null));
            Log.d(TAG, "updateView printObjectFromDB: modelpos: " + data.getString(6));
        }
        Log.d(TAG, "updateView printObjectFromDB: <<< OBJECT INFO END >>>");
    }
}