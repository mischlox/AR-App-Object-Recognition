package hs.aalen.arora;

public interface GlobalSettings {
    static final int SMALL = 256;
    static final int MEDIUM = 512;
    static final int LARGE = 1028;

    int getAmountSamples();
    boolean nightModeOn();
    int getFocusBoxSize();

    void setHelpShowing(boolean show);
    boolean getHelpShowing();
}
