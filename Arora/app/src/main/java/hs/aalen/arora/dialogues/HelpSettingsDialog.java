package hs.aalen.arora.dialogues;

import android.content.Context;
import hs.aalen.arora.R;
/**
 * Help dialog that shows an explanation for configuration options
 *
 * @author Michael Schlosser
 */
public class HelpSettingsDialog extends HelpDialog {
    @Override
    public void createDialog(Context context) {
        super.createDialog(context);
        initCards(context.getString(R.string.help_configure_app),
                context.getString(R.string.help_config_intro),
                context.getString(R.string.help_config_samples),
                context.getString(R.string.help_config_thres),
                context.getString(R.string.help_config_countdown),
                context.getString(R.string.help_config_resolution),
                context.getString(R.string.help_config_darkmode),
                context.getString(R.string.help_config_reset),
                context.getString(R.string.help_text_havefun));
    }
}
