package hs.aalen.arora.persistence;

/**
 * Interface for all needed methods for global configuration of the application
 *
 * @author Michael Schlosser
 */
public interface GlobalConfig {
    double SMALL = 0.5;
    double MEDIUM = 0.75;
    double LARGE = 1;

    /**
     * Get the amount of samples, that will be trained in training session per object
     *
     * @return the amount of samples stored in global configuration,
     */
    int getAmountSamples();

    /**
     * Returns if dark mode is activated
     *
     * @return true if dark mode is activated, false otherwise
     */
    boolean getNightMode();

    /**
     * Gets the ratio of the focus box. Should be SMALL, MEDIUM or LARGE
     *
     * @return the ratio that is stored in the global configuration
     */
    double getFocusBoxRatio();

    /**
     * Returns if the Help dialog should be shown before adding an object
     *
     * @return true if it should be shown, false otherwise
     */
    boolean getHelpShowing();

    /**
     * Saves the state of showing help
     * @param show value to store
     */
    void setHelpShowing(boolean show);

    /**
     * Activates the process of adding samples
     * Only purpose is to trigger an Observer
     */
    void switchAddSamplesTrigger();

    /**
     * Gets the value of trigger
     *
     * @return stored value
     */
    boolean getAddSamplesTrigger();

    /**
     * Activates a database rollback after adding an object
     * Only purpose is to trigger an Observer
     */
    void switchIllegalStateTrigger();

    /**
     * Gets the value of trigger
     *
     * @return stored value
     */
    boolean getIllegalStateTrigger();

    /**
     * Activates the updating of the Latent-Replay-Buffer
     * Only purpose is to trigger an Observer
     */
    void switchUpdateReplayTrigger();

    /**
     * Gets the value of trigger
     *
     * @return stored value
     */
    boolean getUpdateReplayTrigger();

    /**
     * Gets the model position that is passed through the fragments during adding samples
     *
     * @return stored model position
     */
    String getCurrentModelPos();

    /**
     * Sets the model position that is passed through the fragments during adding samples
     * @param newName new Model position
     */
    void setCurrentModelPos(String newName);

    /**
     * Get the ID of the currently selected model
     *
     * @return stored model id
     */
    String getCurrentModelID();

    /**
     * Set the ID of the currently selected model
     * @param id of model that will be currently selected
     */
    void setCurrentModelID(String id);

    /**
     * Sets the object name that is passed through the fragments during adding samples
     *
     * @return stored object name
     */
    String getCurrentObjectName();

    /**
     * Gets the object name that is passed through the fragments during adding samples
     * @param name new name of current object
     */
    void setCurrentObjectName(String name);

    /**
     * Get the maximum amount of objects that can be stored in the model.
     * Purpose of this should be to provide a possibility to set the parameter with an external
     * configuration file but because lack of time it was not implemented yet
     *
     * @return maximum amount of objects that can be stored in a model
     */
    int getMaxObjects();

    /**
     * Gets the countdown duration
     *
     * @return stored value
     */
    int getCountDown();

    /**
     * Gets the confidence threshold value
     *
     * @return stored value
     */
    int getConfidenceThreshold();

    /**
     * Clear all shared preferences for a total reset
     */
    void clearConfiguration();
}
