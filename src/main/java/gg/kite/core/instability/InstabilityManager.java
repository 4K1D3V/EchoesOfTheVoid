package gg.kite.core.instability;

import gg.kite.core.EchoesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

public final class InstabilityManager {
private final EchoesPlugin plugin;
private final AtomicInteger instability = new AtomicInteger(0);

@Inject
public InstabilityManager(EchoesPlugin plugin) {
this.plugin = plugin;
}

public void addInstability(int points) {
int newValue = instability.addAndGet(points);
ConfigurationSection thresholds = plugin.getConfig().getConfigurationSection("instability.thresholds");
thresholds.getKeys(false).stream()
.map(Integer::parseInt)
.filter(t -> newValue >= t && newValue - points < t) .forEach(this::triggerEvent); } public void tick() { instability.set(Math.max(0, instability.get() - plugin.getConfig().getInt("instability.decay_rate", 1))); } private void triggerEvent(int threshold) { String event=plugin.getConfig().getString("instability.thresholds." + threshold + ".event" ); switch (event) { case "storm" -> plugin.getServer().getWorlds().forEach(w -> w.setStorm(true));
  case "fog" -> plugin.getServer().getOnlinePlayers().forEach(p ->
  p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1)));
  case "xp_boost" -> plugin.getServer().getOnlinePlayers().forEach(p ->
  p.sendMessage("Instability grants double XP for 5 minutes!"));
  }
  plugin.getServer().broadcastMessage("Instability has reached " + threshold + ": " + event + " unleashed!");
  }
  
  public int getInstability() {
  return instability.get();
  }
  }