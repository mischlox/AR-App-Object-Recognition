package hs.aalen.arora.dialogues;

import android.content.Context;

import hs.aalen.arora.R;
/**
 * Help dialog that shows an explanation for functionality of model overview
 *
 * @author Michael Schlosser
 */
public class HelpModelOverview extends HelpDialog {
    @Override
    public void createDialog(Context context) {
        super.createDialog(context);
        initCards(context.getString(R.string.help_model_overview),
                context.getString(R.string.help_text_model_intro),
                context.getString(R.string.help_text_model_frozen),
                context.getString(R.string.help_text_radiobutton),
                context.getString(R.string.help_text_see_all_objects),
                context.getString(R.string.help_text_add_new_model),
                context.getString(R.string.help_text_havefun));
    }
}
