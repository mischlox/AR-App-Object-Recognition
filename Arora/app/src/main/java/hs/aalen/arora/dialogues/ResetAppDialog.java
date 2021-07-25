package hs.aalen.arora.dialogues;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import hs.aalen.arora.DatabaseHelper;
import hs.aalen.arora.R;

public class ResetAppDialog implements Dialog {
    private AlertDialog resetAppDialog;
    private DatabaseHelper databaseHelper;

    @Override
    public void createDialog(Context context) {
        databaseHelper = new DatabaseHelper(context);
        AlertDialog.Builder resetAppDialogBuilder = new AlertDialog.Builder(context);
        final View resetAppDialogView = ((LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.reset_app_dialog_popup, null);

        Button resetButton = resetAppDialogView.findViewById(R.id.reset_app_delete);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO wipe whole DB, (delete all paths), recreate application
                databaseHelper.deleteAll();

            }
        });
        Button cancelButton = resetAppDialogView.findViewById(R.id.reset_app_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAppDialog.dismiss();
            }
        });

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
        return false;
    }
}
