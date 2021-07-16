package hs.aalen.arora;

public interface GlobalSettings {
    double SMALL = 0.5;
    double MEDIUM = 0.75;
    double LARGE = 1;

    int getAmountSamples();
    boolean getNightMode();
    double getFocusBoxRatio();

    void setHelpShowing(boolean show);
    boolean getHelpShowing();
}
