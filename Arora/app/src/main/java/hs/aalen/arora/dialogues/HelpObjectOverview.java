package hs.aalen.arora.dialogues;

import android.content.Context;

import hs.aalen.arora.R;

/**
 * Help dialog that shows an explanation for functionality of object overview
 *
 * @author Michael Schlosser
 */
public class HelpObjectOverview extends HelpDialog {
    @Override
    public void createDialog(Context context) {
        super.createDialog(context);
        initCards(context.getString(R.string.help_object_overview),
                context.getString(R.string.help_text_obj_view),
                context.getString(R.string.help_text_obj_view_2),
                context.getString(R.string.help_text_obj_edit_del),
                context.getString(R.string.help_text_obj_blue_border),
                context.getString(R.string.help_text_havefun));
    }
}
