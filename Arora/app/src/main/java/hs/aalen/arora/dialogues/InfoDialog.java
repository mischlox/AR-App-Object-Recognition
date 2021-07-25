package hs.aalen.arora.dialogues;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import hs.aalen.arora.R;

public class InfoDialog implements Dialog {
    private AlertDialog infoDialog;

    @Override
    public void createDialog(Context context) {
        AlertDialog.Builder infoDialogBuilder = new AlertDialog.Builder(context);

        final View infoDialogView = ((LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.info_dialog_popup, null);
        Button okButton = infoDialogView.findViewById(R.id.button_info);

        infoDialogBuilder.setView(infoDialogView);
        infoDialog = infoDialogBuilder.create();
        infoDialog.show();

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoDialog.dismiss();
            }
        });
    }
}
