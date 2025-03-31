package gg.kite.core.companions.behaviors;

import gg.kite.core.companions.VoidCompanion;
import org.bukkit.Location;

public final class TankBehavior implements Behavior {
    private final VoidCompanion companion;
    private volatile State state = State.FOLLOW;

    public TankBehavior(VoidCompanion companion) {
        this.companion = companion;
    }

    @Override
    public void execute() {
        switch (state) {
            case FOLLOW -> {
                Location target = companion.getOwner().getLocation();
                companion.getEntity().teleport(companion.getEntity().getLocation().add(target.subtract(companion.getEntity().getLocation()).toVector().normalize().multiply(0.3)));
            }
            case STAY -> {}
            case ASSIST -> {
                companion.getEntity().getNearbyEntities(5, 5, 5).stream()
                    .filter(e -> e instanceof LivingEntity && !e.equals(companion.getOwner()))
                    .findFirst()
                    .ifPresent(e -> companion.getEntity().teleport(e.getLocation().add(0, 1, 0)));
            }
        }
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }
}