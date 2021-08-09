package hs.aalen.arora.dialogues;

/**
 * The Dialog factory is responsible for creating the dialogs of this application
 * using a static factory-method
 *
 * @author Michael Schlosser
 */
public class DialogFactory {
    /**
     * By using the DialogType enum a specific dialog will be created
     *
     * @param dialogType type of dialog that will be created
     *
     * @return the created dialog
     */
    public static Dialog getDialog(DialogType dialogType) {
        if (dialogType == null) return null;
        if (dialogType == DialogType.ADD_OBJ) return new AddObjectDialog();
        if (dialogType == DialogType.ADD_MODEL) return new AddModelDialog();
        if (dialogType == DialogType.RESET) return new ResetAppDialog();
        if (dialogType == DialogType.INFO) return new InfoDialog();
        if (dialogType == DialogType.REPLAY) return new ReplayDialog();
        if (dialogType == DialogType.HELP_ADD_OBJ) return new HelpAddObjDialog();
        if (dialogType == DialogType.HELP_ADD_MORE_SAMPLES) return new HelpAddFurtherSamplesDialog();
        if (dialogType == DialogType.HELP_OBJ_OVERVIEW) return new HelpObjectOverview();
        if (dialogType == DialogType.HELP_MODEL_OVERVIEW) return new HelpModelOverview();
        if (dialogType == DialogType.HELP_SETTINGS) return new HelpSettingsDialog();
        return null;
    }
}
