package hs.aalen.arora.dialogues;

public class DialogFactory {
    public static Dialog getDialog(DialogType dialogType) {
        if (dialogType == null) return null;
        if (dialogType == DialogType.ADD_OBJ) return new AddObjectDialog();
        if (dialogType == DialogType.ADD_MODEL) return new AddModelDialog();
        if (dialogType == DialogType.RESET) return new ResetAppDialog();
        if (dialogType == DialogType.INFO) return new InfoDialog();
        if (dialogType == DialogType.REPLAY) return new ReplayDialog();
        if (dialogType == DialogType.HELP_ADD_OBJ) return new HelpAddObjDialog();
        if (dialogType == DialogType.HELP_ADD_MORE_SAMPLES) return new HelpAddFurtherSamplesDialog();
        if (dialogType == DialogType.HELP_OBJ_OVERVIEW) return new HelpAddObjDialog();
        if (dialogType == DialogType.HELP_MODEL_OVERVIEW) return new HelpAddObjDialog();
        if (dialogType == DialogType.HELP_SETTINGS) return new HelpSettingsDialog();
        return null;
    }
}
