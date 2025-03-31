package gg.kite.core.companions;

import gg.kite.core.companions.behaviors.Behavior;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface VoidCompanion {
    Entity getEntity();
    Player getOwner();
    CompanionData getData();
    Behavior getBehavior();
    void spawn(Location location);
    void despawn();
    void tick();
}