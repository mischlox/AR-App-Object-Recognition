package hs.aalen.arora.dialogues;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import hs.aalen.arora.persistence.DatabaseHelper;
import hs.aalen.arora.persistence.SQLiteHelper;
import hs.aalen.arora.persistence.GlobalConfig;
import hs.aalen.arora.R;
import hs.aalen.arora.persistence.SharedPrefsHelper;

/**
 * This dialog will inform the user about the consequences of resetting the application
 * The choice if everything should be actually deleted will be made through this dialog
 *
 * @author Michael Schlosser
 */
public class ResetAppDialog implements Dialog {
    private AlertDialog resetAppDialog;
    private DatabaseHelper databaseHelper;
    private GlobalConfig globalConfig;

    @Override
    public void createDialog(Context context) {
        databaseHelper = new SQLiteHelper(context);
        globalConfig = new SharedPrefsHelper(context);
        AlertDialog.Builder resetAppDialogBuilder = new AlertDialog.Builder(context);
        final View resetAppDialogView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.reset_app_dialog_popup, null);

        Button resetButton = resetAppDialogView.findViewById(R.id.reset_app_delete);
        resetButton.setOnClickListener(v -> {
            if (deleteAllData()) {
                Toast.makeText(context, R.string.success_delete_all, Toast.LENGTH_SHORT).show();
                resetAppDialog.dismiss();
            } else {
                Toast.makeText(context, R.string.error_occured, Toast.LENGTH_SHORT).show();
            }

        });
        Button cancelButton = resetAppDialogView.findViewById(R.id.reset_app_cancel);
        cancelButton.setOnClickListener(v -> resetAppDialog.dismiss());

        resetAppDialogBuilder.setView(resetAppDialogView);
        resetAppDialog = resetAppDialogBuilder.create();
        resetAppDialog.show();

    }

    /**
     * Wipe database and delete all parameter paths
     *
     * @return true if successful, false otherwise
     */
    private boolean deleteAllData() {
        globalConfig.clearConfiguration();
        Cursor cursor = databaseHelper.getAllModels();
        long success = 1;
        while (cursor.moveToNext()) {
            try {
                File file = new File(cursor.getString(2));
                if (file.exists()) success = file.delete() ? 1 : -1;
            } catch (Exception e) {
                e.printStackTrace();
                success = -1;
            }
            databaseHelper.deleteAll();
        }
        return success != -1;
    }
}
