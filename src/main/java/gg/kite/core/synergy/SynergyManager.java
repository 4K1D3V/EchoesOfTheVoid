package gg.kite.core.synergy;

import gg.kite.core.EchoesPlugin;
import gg.kite.core.companions.CompanionManager;
import gg.kite.core.companions.VoidCompanion;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class SynergyManager {
    private final EchoesPlugin plugin;
    private final CompanionManager companionManager;
    private final Map<Player, Map<String, Integer>> activeSynergies = new WeakHashMap<>();

    @Inject
    public SynergyManager(EchoesPlugin plugin, CompanionManager companionManager) {
        this.plugin = plugin;
        this.companionManager = companionManager;
    }

    public void tickSynergies() {
        companionManager.getActiveCompanions().forEach((player, companionRef) -> {
            VoidCompanion companion = companionRef.get();
            if (companion == null || companion.getData().corrupted()) return;

            Map<String, Integer> synergies = activeSynergies.computeIfAbsent(player, k -> new HashMap<>());
            ConfigurationSection synergyConfig = plugin.getConfig().getConfigurationSection("synergies");

            synergyConfig.getKeys(false).forEach(synergy -> {
                int duration = synergies.getOrDefault(synergy, 0);
                if (duration > 0) {
                    synergies.put(synergy, duration - 1);
                    return;
                }

                if (synergy.equals("shadow_crimson")) {
                    companionManager.getActiveCompanions().values().stream()
                        .map(WeakReference::get)
                        .filter(Objects::nonNull)
                        .filter(c -> c.getData().type().equals("crimson_golem") &&
                            c.getEntity().getLocation().distance(companion.getEntity().getLocation()) <= synergyConfig.getDouble(synergy + ".range"))
                        .findFirst()
                        .ifPresent(c -> {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 1));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 1));
                            synergies.put(synergy, synergyConfig.getInt(synergy + ".duration"));
                        });
                } else if (synergy.equals("triple_shadow")) {
                    long count = companionManager.getActiveCompanions().values().stream()
                        .map(WeakReference::get)
                        .filter(Objects::nonNull)
                        .filter(c -> c.getData().type().equals("shadow_wisp") &&
                            c.getEntity().getLocation().distance(companion.getEntity().getLocation()) <= synergyConfig.getDouble(synergy + ".range"))
                        .count();
                    if (count >= synergyConfig.getInt(synergy + ".count")) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
                        synergies.put(synergy, synergyConfig.getInt(synergy + ".duration"));
                    }
                }
            });
        });
    }
}