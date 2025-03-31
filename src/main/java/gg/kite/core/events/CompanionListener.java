package gg.kite.core.events;

import gg.kite.core.companions.CompanionManager;
import gg.kite.core.companions.VoidCompanion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import javax.inject.Inject;
import java.util.Optional;

public final class CompanionListener implements Listener {
    private final CompanionManager companionManager;

    @Inject
    public CompanionListener(CompanionManager companionManager) {
        this.companionManager = companionManager;
    }

    @EventHandler
    public void onCompanionDamage(EntityDamageByEntityEvent event) {
        companionManager.getActiveCompanions().values().stream()
            .map(WeakReference::get)
            .filter(Objects::nonNull)
            .filter(c -> c.getEntity().equals(event.getEntity()))
            .findFirst()
            .ifPresent(c -> {
                event.setCancelled(true);
                c.getOwner().sendMessage("Your companion cannot be damaged!");
            });
    }

    @EventHandler
    public void onCompanionInteract(PlayerInteractEntityEvent event) {
        Optional<VoidCompanion> companionOpt = companionManager.getActiveCompanions().values().stream()
            .map(WeakReference::get)
            .filter(Objects::nonNull)
            .filter(c -> c.getEntity().equals(event.getRightClicked()))
            .findFirst();
        companionOpt.ifPresent(companion -> {
            if (event.getPlayer().equals(companion.getOwner())) {
                event.getPlayer().openInventory(companion.getData().inventory());
                event.setCancelled(true);
            }
        });
    }
}