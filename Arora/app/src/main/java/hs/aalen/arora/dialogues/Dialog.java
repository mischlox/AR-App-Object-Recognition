package hs.aalen.arora.dialogues;

import android.content.Context;

/**
 * An interface that has to be implemented by a dialog in this application
 * to make it creatable with the factory
 *
 * @author Michael Schlosser
 */
public interface Dialog {
    /**
     * Method that will create the dialog in the context it is referring
     *
     * @param context the dialog will refer to
     */
    void createDialog(Context context);
}
