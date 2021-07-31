package hs.aalen.arora.dialogues;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import hs.aalen.arora.persistence.DatabaseHelper;
import hs.aalen.arora.persistence.SQLiteHelper;
import hs.aalen.arora.persistence.GlobalSettings;
import hs.aalen.arora.R;
import hs.aalen.arora.persistence.SharedPrefsHelper;

/**
 * Dialog for Adding an object that will give the opportunity to set values
 * and starts training afterwards
 *
 * @author Michael Schlosser
 */
public class AddObjectDialog implements Dialog {
    private AlertDialog addObjectDialog;
    private EditText dialogObjectName;
    private EditText dialogObjectType;
    private EditText dialogObjectAdditionalData;
    private DatabaseHelper databaseHelper;
    private GlobalSettings settings;

    @Override
    public void createDialog(Context context) {
        databaseHelper = new SQLiteHelper(context);
        settings = new SharedPrefsHelper(context);
        // Add Object Dialog items
        AlertDialog.Builder addObjectDialogBuilder = new AlertDialog.Builder(context);
        final View addObjectDialogView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.add_object_dialog_popup, null);

        dialogObjectName = addObjectDialogView.findViewById(R.id.add_dialog_object_name);
        dialogObjectType = addObjectDialogView.findViewById(R.id.add_dialog_object_type);
        dialogObjectAdditionalData = addObjectDialogView.findViewById(R.id.add_dialog_object_additional_data);
        Button startTrainingButton = addObjectDialogView.findViewById(R.id.add_dialog_start_training);
        Button cancelDialogButton = addObjectDialogView.findViewById(R.id.add_dialog_cancel);

        addObjectDialogBuilder.setView(addObjectDialogView);
        addObjectDialog = addObjectDialogBuilder.create();
        addObjectDialog.show();

        startTrainingButton.setOnClickListener(v -> {
            // Save typed in values by user to DB
            String objectName = dialogObjectName.getText().toString();
            String objectType = dialogObjectType.getText().toString();
            String objectAdditionalData = dialogObjectAdditionalData.getText().toString();

            if (objectName.length() != 0) {
                long success = addObject(objectName, objectType, objectAdditionalData, settings.getCurrentModelID());
                if (success != -1) {
                    // Save the name of the object to Global Configuration
                    settings.setCurrentModelPos(dialogObjectName.getText().toString());
                    settings.setCurrentObjectName(objectName);
                    // Reset text
                    dialogObjectName.setText("");
                    dialogObjectType.setText("");
                    dialogObjectAdditionalData.setText("");
                    // Trigger adding Samples in Camera Fragment and therefore start training
                    settings.switchAddSamplesTrigger();
                    addObjectDialog.dismiss();
                } else {
                    Toast.makeText(context, R.string.object_exists, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, R.string.dialog_please_set_name, Toast.LENGTH_LONG).show();
            }
        });

        cancelDialogButton.setOnClickListener(v -> addObjectDialog.dismiss());
    }

    /**
     * Add a new object to Database
     *
     * @param objectName           Name of object
     * @param objectType           Type of object
     * @param objectAdditionalData Additional data of object
     * @return id of inserted object
     */
    private long addObject(String objectName, String objectType, String objectAdditionalData, String modelID) {
        settings.setCurrentObjectName(objectName);
        return databaseHelper.insertObject(objectName, objectType, objectAdditionalData, modelID);
    }
}
