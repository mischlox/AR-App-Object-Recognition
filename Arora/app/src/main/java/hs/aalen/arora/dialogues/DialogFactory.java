package hs.aalen.arora.dialogues;

public class DialogFactory {
    public static Dialog getDialog(DialogType dialogType) {
        if(dialogType == null) return null;
        if(dialogType == DialogType.HELP) return new HelpDialog();
        if(dialogType == DialogType.ADD_OBJ) return new AddObjectDialog();
        if(dialogType == DialogType.ADD_MODEL) return new AddModelDialog();
        return null;
    }
}
