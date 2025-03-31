package gg.kite.core.rifts;

import gg.kite.core.EchoesPlugin;
import gg.kite.core.MessageManager;
import gg.kite.core.companions.CompanionManager;
import gg.kite.core.dungeons.DungeonManager;
import gg.kite.core.instability.InstabilityManager;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class RiftManager {
    private final EchoesPlugin plugin;
    private final CompanionManager companionManager;
    private final MessageManager messageManager;
    private final InstabilityManager instabilityManager;
    private final DungeonManager dungeonManager;
    private final ConcurrentHashMap<Location, Rift> activeRifts = new ConcurrentHashMap<>();
    private final boolean mythicEnabled;

    @Inject
    public RiftManager(EchoesPlugin plugin, CompanionManager companionManager, MessageManager messageManager,
                       InstabilityManager instabilityManager, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.companionManager = companionManager;
        this.messageManager = messageManager;
        this.instabilityManager = instabilityManager;
        this.dungeonManager = dungeonManager;
        this.mythicEnabled = plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs") &&
            plugin.getConfig().getBoolean("rifts.mythic_enabled", false);
    }

    public void tickRifts() {
        if (activeRifts.size() < plugin.getConfig().getInt("rifts.max_active", 10) &&
            ThreadLocalRandom.current().nextDouble() < plugin.getConfig().getDouble("rifts.spawn_chance")) {
            plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> plugin.getConfig().getBoolean("worlds." + p.getWorld().getName() + ".enabled", true))
                .findAny()
                .ifPresent(player -> spawnRift(player.getLocation()));
        }
        activeRifts.values().removeIf(Rift::tick);
    }

    public void spawnRift(Location location) {
        if (activeRifts.size() >= plugin.getConfig().getInt("rifts.max_active", 10)) return;
        boolean isDungeon = ThreadLocalRandom.current().nextDouble() < plugin.getConfig().getDouble("rifts.dungeon_chance", 0.02);
        Rift rift = new Rift(location, plugin.getConfig().getInt("rifts.duration"),
            plugin.getConfig().getInt("rifts.stages.1.mob_count"),
            ThreadLocalRandom.current().nextDouble() < plugin.getConfig().getDouble("rifts.boss_chance", 0.05),
            isDungeon);
        activeRifts.put(location.clone(), rift);
        plugin.getServer().broadcastMessage(messageManager.getMessage("en", isDungeon ? "dungeon_spawn" : "rift_spawn"));
        instabilityManager.addInstability(10);
    }

    public void closeAllRifts() {
        activeRifts.values().forEach(Rift::close);
        activeRifts.clear();
    }

    public int getActiveRiftCount() {
        return activeRifts.size();
    }

    private final class Rift {
        private final Location center;
        private int ticksLeft;
        private int mobCount;
        private int spawnCounter;
        private final boolean hasBoss;
        private final boolean isDungeon;
        private int stage;
        private int radius;

        Rift(Location center, int duration, int initialMobCount, boolean hasBoss, boolean isDungeon) {
            this.center = center.clone();
            this.ticksLeft = duration;
            this.mobCount = initialMobCount;
            this.spawnCounter = 0;
            this.hasBoss = hasBoss;
            this.isDungeon = isDungeon;
            this.stage = 1;
            this.radius = plugin.getConfig().getInt("rifts.stages.1.radius", 1);
            center.getBlock().setType(Material.valueOf(plugin.getConfig().getString("rifts.block_type", "END_PORTAL")));
            if (isDungeon) dungeonManager.createDungeon(center);
            else spawnMobs();
        }

        boolean tick() {
            ticksLeft--;
            if (!isDungeon && ticksLeft % 20 == 0 && spawnCounter < mobCount) spawnMobs();
            ConfigurationSection stages = plugin.getConfig().getConfigurationSection("rifts.stages");
            stages.getKeys(false).stream()
                .map(Integer::parseInt)
                .filter(s -> s > stage && ticksLeft <= stages.getInt(s + ".ticks"))
                .findFirst()
                .ifPresent(nextStage -> {
                    stage = nextStage;
                    mobCount = stages.getInt(stage + ".mob_count");
                    radius = stages.getInt(stage + ".radius");
                });
            if (ticksLeft <= 0) {
                close();
                return true;
            }
            return false;
        }

        void close() {
            center.getBlock().setType(Material.AIR);
            if (!isDungeon && hasBoss) dropLoot();
            if (isDungeon) dungeonManager.closeDungeon(center);
        }

        void spawnMobs() {
            if (spawnCounter++ < mobCount) {
                Location spawnLoc = center.clone().add(
                    ThreadLocalRandom.current().nextDouble(-radius, radius),
                    0,
                    ThreadLocalRandom.current().nextDouble(-radius, radius)
                );
                if (hasBoss && spawnCounter == 1 && mythicEnabled) {
                    MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob(plugin.getConfig().getString("rifts.mythic_boss", "VoidWarden")).orElse(null);
                    if (mob != null) {
                        MythicBukkit.inst().getAPIHelper().spawnMythicMob(mob, spawnLoc, 1);
                        return;
                    }
                }
                Entity entity = center.getWorld().spawnEntity(spawnLoc, EntityType.valueOf(plugin.getConfig().getString("rifts.mob_type", "ZOMBIE")));
                entity.setCustomName(plugin.getConfig().getString("rifts.mob_name", "Echo Stalker"));
                if (hasBoss && spawnCounter == 1) entity.setMaxHealth(entity.getMaxHealth() * 2);
            }
        }

        void dropLoot() {
            ConfigurationSection loot = plugin.getConfig().getConfigurationSection("rifts.loot_table");
            loot.getKeys(false).forEach(key -> {
                if (ThreadLocalRandom.current().nextDouble() < loot.getDouble(key + ".chance")) {
                    int amount = ThreadLocalRandom.current().nextInt(loot.getInt(key + ".min"), loot.getInt(key + ".max") + 1);
                    center.getWorld().dropItemNaturally(center, new ItemStack(Material.valueOf(loot.getString(key + ".item")), amount));
                }
            });
        }
    }
}