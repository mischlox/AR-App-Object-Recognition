package hs.aalen.arora.dialogues;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import hs.aalen.arora.persistence.DatabaseHelper;
import hs.aalen.arora.persistence.SQLiteHelper;
import hs.aalen.arora.persistence.GlobalSettings;
import hs.aalen.arora.R;
import hs.aalen.arora.persistence.SharedPrefsHelper;

public class ReplayDialog implements Dialog {
    private AlertDialog replayDialog;
    private DatabaseHelper databaseHelper;
    private GlobalSettings settings;

    @Override
    public void createDialog(Context context) {
        databaseHelper = new SQLiteHelper(context);
        settings = new SharedPrefsHelper(context);

        AlertDialog.Builder replayDialogBuilder = new AlertDialog.Builder(context);
        final View replayDialogView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.replay_dialog_popup, null);

        Button freezeButton = replayDialogView.findViewById(R.id.replay_freeze_model);
        freezeButton.setOnClickListener(v -> {
            databaseHelper.updateModelIsFrozen(settings.getCurrentModelID(), true);
            Toast.makeText(context, R.string.model_was_frozen_successfully, Toast.LENGTH_SHORT).show();
            replayDialog.dismiss();
        });

        Button continueLaterButton = replayDialogView.findViewById(R.id.replay_continue_later);

        continueLaterButton.setOnClickListener(v -> {
            replayDialog.dismiss();
            settings.switchUpdateReplayTrigger();
        });

        replayDialogBuilder.setView(replayDialogView);
        replayDialog = replayDialogBuilder.create();
        replayDialog.show();
    }
}
