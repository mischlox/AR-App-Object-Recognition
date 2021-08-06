package hs.aalen.arora;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.ListFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import hs.aalen.arora.dialogues.DialogFactory;
import hs.aalen.arora.dialogues.DialogType;
import hs.aalen.arora.persistence.DatabaseHelper;
import hs.aalen.arora.persistence.GlobalConfig;
import hs.aalen.arora.persistence.SQLiteHelper;
import hs.aalen.arora.persistence.SharedPrefsHelper;

/**
 * Fragment for changing the model
 *
 * @author Michael Schlosser
 */
public class ModelOverviewFragment extends ListFragment {
    private static final String TAG = ModelOverviewFragment.class.getSimpleName();
    private final ArrayList<String> modelNames = new ArrayList<>();
    private DatabaseHelper modelDatabaseHelper;
    private GlobalConfig globalConfig;
    private ListView modelListView;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_model_overview, container, false);
        modelDatabaseHelper = new SQLiteHelper(getActivity());
        globalConfig = new SharedPrefsHelper(requireContext());
        FloatingActionButton addModelButton = view.findViewById(R.id.list_model_add_model_button);

        addModelButton.setOnClickListener(v -> DialogFactory.getDialog(DialogType.ADD_MODEL).createDialog(getContext()));

        modelListView = view.findViewById(android.R.id.list);
        TextView noModelsText = view.findViewById(R.id.no_models_info_text);
        if (populateView())
            noModelsText.setVisibility(View.INVISIBLE);
        else
            noModelsText.setVisibility(View.VISIBLE);
        return view;
    }

    /**
     * Populate the Object Overview list with the models
     *
     * @return true if there are any models that can be populated, false otherwise
     */
    public boolean populateView() {
        modelNames.clear();
        Cursor data = modelDatabaseHelper.getAllModels();

        while (data.moveToNext()) {
            modelNames.add(data.getString(1));
        }
        ListAdapter adapter = new ModelOverviewAdapter(getActivity(), modelNames);
        modelListView.setAdapter(adapter);
        return !adapter.isEmpty();
    }

    /**
     * Custom Array Adapter in order to make a more fitting list for the model overview
     *
     * @author Michael Schlosser
     */
    class ModelOverviewAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> modelNames;
        int selectedPosition;

        private AlertDialog showObjectDialog;
        private ListView objectsListView;
        private ArrayList<String> listItems;

        private ProgressBar numObjectsInModelProgressBar;
        private TextView numObjectsInModelTextView;

        ModelOverviewAdapter(Context c, ArrayList<String> names) {
            super(c, R.layout.model_item, R.id.list_model_name, names);
            this.context = c;
            this.modelNames = names;

            if(globalConfig.getCurrentModelID() != null) {
                this.selectedPosition = Integer.parseInt(globalConfig.getCurrentModelID()) - 1;
            }
            else {
                this.selectedPosition = -1;
            }
        }

        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) requireActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View item = layoutInflater.inflate(R.layout.model_item, parent, false);

            numObjectsInModelProgressBar = item.findViewById(R.id.list_model_progressbar);
            numObjectsInModelTextView = item.findViewById(R.id.list_model_progress_text);
            updateProgressBar(position);
            TextView modelNameTextView = item.findViewById(R.id.list_model_name);

            ImageView infoButton = item.findViewById(R.id.list_model_is_frozen);
            infoButton.setOnClickListener(v -> Toast.makeText(context, R.string.model_was_frozen_info, Toast.LENGTH_LONG).show());

            modelNameTextView.setText(this.modelNames.get(position));

            RadioButton modelSelectorButton = item.findViewById(R.id.list_model_radiobutton);
            modelSelectorButton.setChecked(position == selectedPosition);
            modelSelectorButton.setTag(position);
            modelSelectorButton.setOnClickListener(v -> {
                selectedPosition = (Integer) v.getTag();
                int newModelID = selectedPosition + 1;
                globalConfig.setCurrentModelID(newModelID + "");
                Log.d(TAG, "onClick: test model selection: current model: "
                        + globalConfig.getCurrentModelID());
                Log.d(TAG, "onClick: test model selection: current model: "
                        + globalConfig.getCurrentModelID() + " selected position: " +  selectedPosition + " new Model ID: " + newModelID + " position: " + position);
                notifyDataSetChanged();
            });
            if(position == selectedPosition && modelDatabaseHelper.modelIsFrozen(globalConfig.getCurrentModelID())) {
                infoButton.setVisibility(View.VISIBLE);
            } else {
                infoButton.setVisibility(View.INVISIBLE);

            }

            ImageButton objectsOfModelButton = item.findViewById(R.id.list_model_objects_button);
            objectsOfModelButton.setOnClickListener(v -> showObjectsOfModelDialog(position + 1));

            return item;
        }

        public void updateProgressBar(int position) {
            int maxObjects = globalConfig.getMaxObjects();
            int numCurrentObjects = modelDatabaseHelper.getAllObjectsByModelID((position + 1) + "").getCount();
            numObjectsInModelProgressBar.setMax(maxObjects);
            numObjectsInModelProgressBar.setProgress(numCurrentObjects);
            String numObjectsInModelText = numCurrentObjects + " / " + maxObjects;
            numObjectsInModelTextView.setText(numObjectsInModelText);
            setColor(numCurrentObjects, maxObjects);
        }

        /**
         * Dialog that contains a list with all objects that are saved to the selected model
         *
         * @param position selected model
         */
        private void showObjectsOfModelDialog(int position) {
            // Show all objects dialog items
            AlertDialog.Builder showObjectsDialogBuilder = new AlertDialog.Builder(getActivity());
            final View showObjectsDialogView = getLayoutInflater()
                    .inflate(R.layout.show_objects_dialog_popup, null);

            listItems = new ArrayList<>();
            objectsListView = showObjectsDialogView.findViewById(R.id.list_model_objects_list);
            Button cancelButton = showObjectsDialogView.findViewById(R.id.list_model_cancel_button);
            cancelButton.setOnClickListener(v -> showObjectDialog.dismiss());
            showObjectsDialogBuilder.setView(showObjectsDialogView);
            showObjectDialog = showObjectsDialogBuilder.create();

            TextView noObjectsText = showObjectsDialogView.findViewById(R.id.list_model_no_objects);
            if (populateShowObjectsDialogView(position)) {
                noObjectsText.setVisibility(View.INVISIBLE);
            } else {
                noObjectsText.setVisibility(View.VISIBLE);
            }
            showObjectDialog.show();
        }

        /**
         * Changes color of progressbar text if it is full / almost full
         *
         * @param numCurrentObjects number of current objects
         * @param maxObjects        maximum of objects that can be saved
         */
        private void setColor(int numCurrentObjects, int maxObjects) {
            float THRES_RED = 1.0f;
            float THRES_YELLOW = 0.75f;

            float fullRatio = ((float) numCurrentObjects / (float) maxObjects);
            if (fullRatio >= THRES_RED) {
                numObjectsInModelTextView.setTextColor(ContextCompat.getColor(context, R.color.red));
            } else if (fullRatio < THRES_RED && fullRatio >= THRES_YELLOW) {
                numObjectsInModelTextView.setTextColor(ContextCompat.getColor(context, R.color.yellow));
            } else {
                numObjectsInModelTextView.setTextColor(ContextCompat.getColor(context, R.color.green));
            }
        }

        /**
         * Helper method for populating the object list above
         *
         * @param position position of selected model in object overview list
         * @return true if there are any objects to be populated, false otherwise
         */
        private boolean populateShowObjectsDialogView(int position) {
            listItems.clear();
            Cursor data = modelDatabaseHelper.getObjectNamesByModelID(position + "");
            while (data.moveToNext()) {
                listItems.add(data.getString(0));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_list_item_1,
                    listItems);
            objectsListView.setAdapter(adapter);
            return !adapter.isEmpty();
        }
    }
}
