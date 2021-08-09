package hs.aalen.arora.dialogues;

/**
 * If a new dialog is implemented, a new element should be added to this Enum
 * and another else if statement should be added to the DialogFactory
 *
 * @author Michael Schlosser
 */
public enum DialogType {
    ADD_OBJ,
    ADD_MODEL,
    RESET,
    INFO,
    REPLAY,
    HELP_ADD_OBJ,
    HELP_ADD_MORE_SAMPLES,
    HELP_OBJ_OVERVIEW,
    HELP_MODEL_OVERVIEW,
    HELP_SETTINGS
}
