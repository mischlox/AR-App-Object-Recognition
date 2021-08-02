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

    int getAmountSamples();

    boolean getNightMode();

    double getFocusBoxRatio();

    boolean getHelpShowing();

    void setHelpShowing(boolean show);

    void switchAddSamplesTrigger();

    boolean getAddSamplesTrigger();

    void switchIllegalStateTrigger();

    boolean getIllegalStateTrigger();

    void switchUpdateReplayTrigger();

    boolean getUpdateReplayTrigger();

    String getCurrentModelPos();

    void setCurrentModelPos(String newName);

    String getCurrentModelID();

    void setCurrentModelID(String id);

    String getCurrentObjectName();

    void setCurrentObjectName(String name);

    int getMaxObjects();

    int getCountDown();

    int getConfidenceThreshold();

    void clearConfiguration();
}
