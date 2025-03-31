package gg.kite.core.dungeons;

import gg.kite.core.EchoesPlugin;
import gg.kite.core.MessageManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonManager {
    private final EchoesPlugin plugin;
    private final MessageManager messageManager;
    private final ConcurrentHashMap<Location, Dungeon> activeDungeons = new ConcurrentHashMap<>();

    @Inject
    public DungeonManager(EchoesPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    public void createDungeon(Location riftLocation) {
        String worldName = "dungeon_" + UUID.randomUUID().toString();
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        World dungeonWorld = creator.createWorld();
        Location spawnLoc = new Location(dungeonWorld, 0, 64, 0);
        Dungeon dungeon = new Dungeon(riftLocation, spawnLoc, dungeonWorld);
        activeDungeons.put(riftLocation, dungeon);
        dungeon.start();
    }

    public void closeDungeon(Location riftLocation) {
        Dungeon dungeon = activeDungeons.remove(riftLocation);
        if (dungeon != null) dungeon.close();
    }

    public void tickDungeons() {
        activeDungeons.values().removeIf(Dungeon::tick);
    }

    public void closeAllDungeons() {
        activeDungeons.values().forEach(Dungeon::close);
        activeDungeons.clear();
    }

    private final class Dungeon {
        private final Location riftLocation;
        private final Location spawnLocation;
        private final World world;
        private int stage;
        private int mobsLeft;

        Dungeon(Location riftLocation, Location spawnLocation, World world) {
            this.riftLocation = riftLocation;
            this.spawnLocation = spawnLocation;
            this.world = world;
            this.stage = 1;
            this.mobsLeft = plugin.getConfig().getInt("rifts.dungeons.stages.1.mob_count");
        }

        void start() {
            spawnStageMobs();
            world.getBlockAt(spawnLocation).setType(Material.END_PORTAL);
        }

        boolean tick() {
            if (world.getEntities().stream().noneMatch(e -> e.getType().isAlive() && !e.getType().equals(EntityType.PLAYER))) {
                if (stage < 3) {
                    stage++;
                    mobsLeft = plugin.getConfig().getInt("rifts.dungeons.stages." + stage + ".mob_count");
                    spawnStageMobs();
                } else {
                    dropLoot();
                    teleportPlayersBack();
                    plugin.getServer().unloadWorld(world, false);
                    return true;
                }
            }
            return false;
        }

        void close() {
            teleportPlayersBack();
            plugin.getServer().unloadWorld(world, false);
        }

        void spawnStageMobs() {
            ConfigurationSection stageConfig = plugin.getConfig().getConfigurationSection("rifts.dungeons.stages." + stage);
            String mobType = stageConfig.getString("mob_type");
            List<String> traps = stageConfig.getStringList("traps");
            for (int i = 0; i < mobsLeft; i++) {
                Location spawn = spawnLocation.clone().add(Math.random() * 10 - 5, 0, Math.random() * 10 - 5);
                world.spawnEntity(spawn, EntityType.valueOf(mobType));
            }
            traps.forEach(trap -> world.getBlockAt(spawnLocation.clone().add(Math.random() * 5 - 2, 0, Math.random() * 5 - 2))
                .setType(Material.valueOf(trap)));
        }

        void dropLoot() {
            ConfigurationSection loot = plugin.getConfig().getConfigurationSection("rifts.dungeons.loot");
            loot.getKeys(false).forEach(key -> {
                if (ThreadLocalRandom.current().nextDouble() < loot.getDouble(key + ".chance")) {
                    int amount = ThreadLocalRandom.current().nextInt(loot.getInt(key + ".min"), loot.getInt(key + ".max") + 1);
                    world.dropItemNaturally(spawnLocation, new ItemStack(Material.valueOf(loot.getString(key + ".item")), amount));
                }
            });
        }

        void teleportPlayersBack() {
            world.getPlayers().forEach(p -> p.teleport(riftLocation));
        }
    }
}