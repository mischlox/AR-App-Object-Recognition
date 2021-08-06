package hs.aalen.arora.dialogues;

import android.content.Context;
import android.util.Pair;

import hs.aalen.arora.R;

public class HelpSettingsDialog extends HelpDialog {
    @Override
    public void createDialog(Context context) {
        // Linked list to browse through text views
        textList.add(Pair.create(2, context.getString(R.string.help_text_tilt_camera)));
        textList.add(Pair.create(3, context.getString(R.string.help_text_counter_training)));
        textList.add(Pair.create(4, context.getString(R.string.help_text_havefun)));
        super.createDialog(context);
    }
}
