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
import hs.aalen.arora.persistence.GlobalConfig;
import hs.aalen.arora.R;
import hs.aalen.arora.persistence.SharedPrefsHelper;

/**
 * Class that creates an Alert Dialog for creating a new Model
 *
 * @author Michael Schlosser
 */
public class AddModelDialog implements Dialog {
    private AlertDialog addModelDialog;
    private EditText dialogModelName;
    private DatabaseHelper databaseHelper;
    private GlobalConfig globalConfig;

    @Override
    public void createDialog(Context context) {
        databaseHelper = new SQLiteHelper(context);
        globalConfig = new SharedPrefsHelper(context);
        // Add Model dialog items
        AlertDialog.Builder addModelDialogBuilder = new AlertDialog.Builder(context);
        final View addModelDialogView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.add_model_dialog_popup, null);
        dialogModelName = addModelDialogView.findViewById(R.id.add_dialog_model_name);
        Button saveButton = addModelDialogView.findViewById(R.id.add_model_dialog_save);
        Button cancelButton = addModelDialogView.findViewById(R.id.reset_app_cancel);

        addModelDialogBuilder.setView(addModelDialogView);
        addModelDialog = addModelDialogBuilder.create();
        // Disable cancelButton because at least one model has to exist
        if (!(databaseHelper.modelsExists())) {
            addModelDialog.setCanceledOnTouchOutside(false);
            cancelButton.setEnabled(false);
        }
        addModelDialog.show();

        saveButton.setOnClickListener(v -> {
            String modelName = dialogModelName.getText().toString();
            if (modelName.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.please_give_model_name), Toast.LENGTH_SHORT).show();
            } else {
                // Insert a model to DB and set it up for Transfer Learning
                if (databaseHelper.insertModel(modelName)) {
                    globalConfig.setCurrentModelID(databaseHelper.getModelIdByName(modelName));
                    dialogModelName.setText("");
                    addModelDialog.dismiss();
                } else {
                    Toast.makeText(context, R.string.model_exists, Toast.LENGTH_SHORT).show();
                }
            }
        });
        cancelButton.setOnClickListener(v -> addModelDialog.dismiss());
    }
}
