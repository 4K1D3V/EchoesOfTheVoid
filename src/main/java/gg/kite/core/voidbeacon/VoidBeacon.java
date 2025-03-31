package gg.kite.core.voidbeacon;

import gg.kite.core.EchoesPlugin;
import gg.kite.core.MessageManager;
import gg.kite.core.companions.CompanionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VoidBeacon implements Listener {
    private final EchoesPlugin plugin;
    private final CompanionManager companionManager;
    private final MessageManager messageManager;
    private final Map<Location, Integer> beacons = new HashMap<>();
    private final Map<String, Location> beaconNetwork = new HashMap<>();

    @Inject
    public VoidBeacon(EchoesPlugin plugin, CompanionManager companionManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.companionManager = companionManager;
        this.messageManager = messageManager;
    }

    public void registerRecipe() {
        if (!plugin.getConfig().getBoolean("void_beacon.recipe.enabled", true)) return;
        ItemStack beacon = new ItemStack(Material.BEACON);
        ItemMeta meta = beacon.getItemMeta();
        meta.setDisplayName("§5Void Beacon");
        beacon.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "void_beacon"), beacon);
        recipe.shape(plugin.getConfig().getStringList("void_beacon.recipe.shape").toArray(new String[0]));
        ConfigurationSection ingredients = plugin.getConfig().getConfigurationSection("void_beacon.recipe.ingredients");
        ingredients.getKeys(false).forEach(key ->
            recipe.setIngredient(key.charAt(0), Material.valueOf(ingredients.getString(key))));
        plugin.getServer().addRecipe(recipe);
    }

    @EventHandler
    public void onBeaconPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.BEACON || !event.getItemInHand().hasItemMeta() ||
            !event.getItemInHand().getItemMeta().getDisplayName().equals("§5Void Beacon")) return;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        beacons.put(location, 1);
        String id = UUID.randomUUID().toString().substring(0, 8);
        beaconNetwork.put(id, location);
        player.sendMessage(messageManager.getMessage("en", "beacon_place"));
    }

    public void tickAuras() {
        beacons.forEach((location, tier) -> {
            ConfigurationSection tierConfig = plugin.getConfig().getConfigurationSection("void_beacon.tiers." + tier);
            String auraType = tierConfig.getString("aura.type");
            if (!auraType.equals("none")) {
                int radius = tierConfig.getInt("aura.radius");
                int strength = tierConfig.getInt("aura.strength");
                location.getWorld().getNearbyEntities(location, radius, radius, radius).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> p.addPotionEffect(new PotionEffect(
                        PotionEffectType.getByName(auraType), 40, strength, false, false)));
            }
        });
    }

    public void upgradeBeacon(Player player, Block block) {
        Location location = block.getLocation();
        int currentTier = beacons.getOrDefault(location, 0);
        int nextTier = currentTier + 1;
        ConfigurationSection tierConfig = plugin.getConfig().getConfigurationSection("void_beacon.tiers." + nextTier);
        if (tierConfig == null) return;

        int cost = tierConfig.getInt("cost");
        if (VaultHook.hasEnough(player, cost)) {
            VaultHook.withdraw(player, cost);
            beacons.put(location, nextTier);
            player.sendMessage(messageManager.getMessage("en", "beacon_upgrade", "{tier}", String.valueOf(nextTier)));
        } else {
            player.sendMessage(messageManager.getMessage("en", "beacon_cost", "{cost}", String.valueOf(cost)));
        }
    }

    public void purifyCompanion(Player player, Block block) {
        companionManager.getCompanion(player).ifPresent(companion -> {
            companion.getData().withCorrupted(false);
            player.sendMessage(messageManager.getMessage("en", "beacon_purify"));
        });
    }

    public void recallCompanion(Player player, Block block) {
        companionManager.getCompanion(player).ifPresent(companion -> {
            companion.getEntity().teleport(block.getLocation().add(0, 1, 0));
            player.sendMessage(messageManager.getMessage("en", "beacon_recall"));
        });
    }

    public long getBeaconCount(int tier) {
        return beacons.values().stream().filter(t -> t == tier).count();
    }

    public Map<String, Location> getBeaconNetwork() {
        return Map.copyOf(beaconNetwork);
    }
}