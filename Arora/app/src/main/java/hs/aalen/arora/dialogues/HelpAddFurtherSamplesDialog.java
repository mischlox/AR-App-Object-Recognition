package hs.aalen.arora.dialogues;

import android.content.Context;

import hs.aalen.arora.R;

/**
 * Help dialog that shows an explanation of adding further samples to
 * an existing object
 *
 * @author Michael Schlosser
 */
public class HelpAddFurtherSamplesDialog extends HelpDialog {
    @Override
    public void createDialog(Context context) {
        super.createDialog(context);
        initCards(context.getString(R.string.help_add_further_samples),
                context.getString(R.string.help_text_train_further_samples),
                context.getString(R.string.help_text_train_further_samples_adv),
                context.getString(R.string.help_text_train_further_samples_third),
                context.getString(R.string.help_text_train_further_samples_fourth),
                context.getString(R.string.help_text_havefun));
    }
}
