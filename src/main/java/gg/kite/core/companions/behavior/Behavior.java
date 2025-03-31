package gg.kite.core.companions.behaviors;

import gg.kite.core.companions.VoidCompanion;

public interface Behavior {
    void execute();
    void setState(State state);

    enum State {
        FOLLOW, STAY, ASSIST
    }
}