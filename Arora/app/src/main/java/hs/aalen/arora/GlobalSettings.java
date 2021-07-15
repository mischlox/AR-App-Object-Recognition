package hs.aalen.arora;

public interface GlobalSettings {
    static final double SMALL = 0.5;
    static final double MEDIUM = 0.75;
    static final double LARGE = 1;

    int getAmountSamples();
    boolean nightModeOn();
    double getFocusBoxRatio();

    void setHelpShowing(boolean show);
    boolean getHelpShowing();
}
