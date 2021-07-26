package hs.aalen.arora;

/**
 * Interface for all needed methods for global configuration of the application
 *
 * @author Michael Schlosser
 */
public interface GlobalSettings {
    double SMALL = 0.5;
    double MEDIUM = 0.75;
    double LARGE = 1;

    int getAmountSamples();

    boolean getNightMode();

    double getFocusBoxRatio();

    boolean getHelpShowing();

    void setHelpShowing(boolean show);

    void switchAddSamplesTrigger();

    boolean getAddSamplesTrigger();

    void switchIllegalStateTrigger();

    boolean getIllegalStateTrigger();

    String getCurrentModelPos();

    void setCurrentModelPos(String newName);

    String getCurrentModel();

    void setCurrentModel(String id);

    String getCurrentObject();

    void setCurrentObject(String name);

    int getMaxObjects();

    int getCountDown();

    int getConfidenceThres();

    void clearConfiguration();
}
