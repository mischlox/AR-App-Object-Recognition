package hs.aalen.arora.dialogues;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import hs.aalen.arora.R;

public class InfoDialog implements Dialog {
    private AlertDialog infoDialog;

    @Override
    public void createDialog(Context context) {
        AlertDialog.Builder infoDialogBuilder = new AlertDialog.Builder(context);
        final View infoDialogView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.info_dialog_popup, null);

        TextView infoText = infoDialogView.findViewById(R.id.info_text);
        infoText.setText(Html.fromHtml(context.getString(R.string.info_text), Html.FROM_HTML_MODE_LEGACY));
        infoText.setMovementMethod(LinkMovementMethod.getInstance());
        Button okButton = infoDialogView.findViewById(R.id.button_info);

        infoDialogBuilder.setView(infoDialogView);
        infoDialog = infoDialogBuilder.create();
        infoDialog.show();

        okButton.setOnClickListener(v -> infoDialog.dismiss());
    }
}
