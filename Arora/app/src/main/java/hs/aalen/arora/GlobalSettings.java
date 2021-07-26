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

    void setHelpShowing(boolean show);
    boolean getHelpShowing();

    void switchAddSamplesTrigger();
    boolean getAddSamplesTrigger();

    void switchIllegalStateTrigger();
    boolean getIllegalStateTrigger();

    void setCurrentModelPos(String newName);
    String getCurrentModelPos();

    void setCurrentModel(String id);
    String getCurrentModel();

    void setCurrentObject(String name);
    String getCurrentObject();

    int getMaxObjects();

    int getCountDown();

    int getConfidenceThres();

    void clearConfiguration();
}
