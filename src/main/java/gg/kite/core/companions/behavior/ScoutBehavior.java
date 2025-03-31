package gg.kite.core.companions.behaviors;

import gg.kite.core.companions.VoidCompanion;
import org.bukkit.Location;

import java.util.concurrent.ThreadLocalRandom;

public final class ScoutBehavior implements Behavior {
    private final VoidCompanion companion;
    private volatile State state = State.FOLLOW;

    public ScoutBehavior(VoidCompanion companion) {
        this.companion = companion;
    }

    @Override
    public void execute() {
        switch (state) {
            case FOLLOW -> {
                Location target = companion.getOwner().getLocation().add(0, 1, 0);
                double speed = companion.getData().corrupted() ? 0.2 : companion.getData().type().equals("aether_sprite") ? 0.6 : 0.5;
                companion.getEntity().teleport(companion.getEntity().getLocation().add(target.subtract(companion.getEntity().getLocation()).toVector().normalize().multiply(speed)));
            }
            case STAY -> {}
            case ASSIST -> {
                Location random = companion.getOwner().getLocation().add(
                    ThreadLocalRandom.current().nextDouble(-3, 3),
                    1,
                    ThreadLocalRandom.current().nextDouble(-3, 3)
                );
                companion.getEntity().teleport(random);
            }
        }
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }
}