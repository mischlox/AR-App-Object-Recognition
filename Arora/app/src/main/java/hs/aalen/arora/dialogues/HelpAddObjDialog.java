package hs.aalen.arora.dialogues;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import hs.aalen.arora.R;
import hs.aalen.arora.persistence.GlobalConfig;
import hs.aalen.arora.persistence.SharedPrefsHelper;

/**
 * Help dialog that shows an explanation of adding objects
 *
 * @author Michael Schlosser
 */
public class HelpAddObjDialog extends HelpDialog {
    private GlobalConfig globalConfig;

    @Override
    public void createDialog(Context context) {
        super.createDialog(context);

        this.globalConfig = new SharedPrefsHelper(context);
        Button trainingButton = helpDialogView.findViewById(R.id.help_training_button);
        trainingButton.setText(R.string.dialog_start_training);

        CheckBox notShowAgainCheckBox = helpDialogView.findViewById(R.id.help_checkbox);
        notShowAgainCheckBox.setVisibility(View.VISIBLE);
        notShowAgainCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> globalConfig.setHelpShowing(!isChecked));

        // Go to Add object dialog
        trainingButton.setOnClickListener(v -> {
            helpDialog.dismiss();
            new AddObjectDialog().createDialog(context);
        });

        initCards(context.getString(R.string.usage),
                context.getString(R.string.help_text_add_info),
                context.getString(R.string.help_text_tilt_camera),
                context.getString(R.string.help_text_fill_focusbox),
                context.getString(R.string.help_text_counter_training),
                context.getString(R.string.help_text_havefun));
    }
}
