package hs.aalen.arora.dialogues;

import android.content.Context;
import android.util.Pair;

import hs.aalen.arora.R;

public class HelpAddObjDialog extends HelpDialog {
    @Override
    public void createDialog(Context context) {
        super.createDialog(context);
        initCards(
                context.getString(R.string.help_text_add_info),
                context.getString(R.string.help_text_tilt_camera),
                context.getString(R.string.help_text_counter_training),
                context.getString(R.string.help_text_havefun),
                context.getString(R.string.help_object_overview),
                context.getString(R.string.success_training));
    }
}
