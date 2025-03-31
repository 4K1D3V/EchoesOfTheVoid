package gg.kite.core.companions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gg.kite.core.EchoesPlugin;
import gg.kite.core.MessageManager;
import gg.kite.core.config.ConfigManager;
import gg.kite.core.instability.InstabilityManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class CompanionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompanionManager.class);
    private final Map<UUID, WeakReference<VoidCompanion>> companions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> summonCooldowns = new ConcurrentHashMap<>();
    private final EchoesPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final InstabilityManager instabilityManager;
    private final File persistenceFile;

    @Inject
    public CompanionManager(EchoesPlugin plugin, ConfigManager configManager, MessageManager messageManager, InstabilityManager instabilityManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.instabilityManager = instabilityManager;
        this.persistenceFile = new File(plugin.getDataFolder(), "companions.json");
        loadPersistedCompanions();
    }

    public void spawnCompanion(Player player, String type) {
        long cooldown = summonCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long now = System.currentTimeMillis() / 1000;
        int summonCooldown = plugin.getConfig().getInt("performance.summon_cooldown", 60);
        if (now < cooldown) {
            player.sendMessage(messageManager.getMessage("en", "cooldown", "{seconds}", String.valueOf(cooldown - now)));
            return;
        }

        if (companions.size() >= plugin.getConfig().getInt("general.max_active_companions", 200)) {
            player.sendMessage(messageManager.getMessage("en", "companion_limit"));
            return;
        }

        List<String> enabledTypes = plugin.getConfig().getStringList("companions.enabled_types");
        if (!enabledTypes.contains(type)) {
            player.sendMessage(messageManager.getMessage("en", "unknown_command"));
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        int chunkCompanions = countCompanionsInChunk(chunk);
        if (chunkCompanions >= plugin.getConfig().getInt("performance.max_companions_per_chunk", 5)) {
            player.sendMessage(messageManager.getMessage("en", "chunk_limit"));
            return;
        }

        String worldName = player.getWorld().getName();
        if (!plugin.getConfig().getBoolean("worlds." + worldName + ".enabled", true) ||
            countCompanionsInWorld(worldName) >= plugin.getConfig().getInt("worlds." + worldName + ".max_companions", 100)) {
            player.sendMessage(messageManager.getMessage("en", "world_limit"));
            return;
        }

        despawnCompanion(player);
        VoidCompanion companion = CompanionFactory.create(type, player, configManager, messageManager);
        companion.spawn(player.getLocation());
        companions.put(player.getUniqueId(), new WeakReference<>(companion));
        summonCooldowns.put(player.getUniqueId(), now + summonCooldown);
        LOGGER.debug("Spawned {} for player {} in chunk {}/{}", type, player.getName(), chunk.getX(), chunk.getZ());
    }

    public void fuseCompanions(Player player1, Player player2) {
        Optional<VoidCompanion> comp1 = getCompanion(player1);
        Optional<VoidCompanion> comp2 = getCompanion(player2);
        if (comp1.isEmpty() || comp2.isEmpty()) {
            player1.sendMessage(messageManager.getMessage("en", "no_companion"));
            return;
        }
        String fusionKey = comp1.get().getData().type() + "_" + comp2.get().getData().type();
        String fusionType = plugin.getConfig().getString("companions.fusions." + fusionKey + ".type");
        if (fusionType == null) {
            player1.sendMessage(messageManager.getMessage("en", "fusion_invalid"));
            return;
        }
        despawnCompanion(player1);
        despawnCompanion(player2);
        VoidCompanion fused = CompanionFactory.create(fusionType, player1, configManager, messageManager);
        fused.spawn(player1.getLocation());
        companions.put(player1.getUniqueId(), new WeakReference<>(fused));
        player1.sendMessage(messageManager.getMessage("en", "fusion_success", "{type}", fusionType));
        player2.sendMessage(messageManager.getMessage("en", "fusion_contributed", "{type}", fusionType));
        instabilityManager.addInstability(20);
    }

    public Optional<VoidCompanion> getCompanion(Player player) {
        return Optional.ofNullable(companions.get(player.getUniqueId()))
            .map(WeakReference::get)
            .filter(Objects::nonNull);
    }

    public void despawnCompanion(Player player) {
        getCompanion(player).ifPresent(companion -> {
            companion.despawn();
            companions.remove(player.getUniqueId());
        });
    }

    public void despawnAll() {
        companions.values().stream()
            .map(WeakReference::get)
            .filter(Objects::nonNull)
            .forEach(VoidCompanion::despawn);
        companions.clear();
        saveCompanions();
    }

    public void tickCompanions() {
        companions.values().parallelStream()
            .map(WeakReference::get)
            .filter(Objects::nonNull)
            .forEach(companion -> {
                companion.tick();
                if (companion.getData().corrupted()) instabilityManager.addInstability(5);
            });
        companions.entrySet().removeIf(entry -> {
            VoidCompanion companion = entry.getValue().get();
            return companion == null || companion.getEntity().isDead();
        });
    }

    public void startPersistenceTask(ScheduledExecutorService executor) {
        int interval = plugin.getConfig().getInt("performance.persistence_interval", 600);
        executor.scheduleAtFixedRate(this::saveCompanions, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void saveCompanions() {
        Gson gson = new Gson();
        Map<UUID, CompanionData> data = companions.entrySet().stream()
            .filter(e -> e.getValue().get() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get().getData()));
        try (Writer writer = new FileWriter(persistenceFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save companion data", e);
        }
    }

    private void loadPersistedCompanions() {
        if (!persistenceFile.exists()) return;
        Gson gson = new Gson();
        try (Reader reader = new FileReader(persistenceFile)) {
            Type type = new TypeToken<Map<UUID, CompanionData>>() {}.getType();
            Map<UUID, CompanionData> data = gson.fromJson(reader, type);
            data.forEach((uuid, companionData) -> {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    VoidCompanion companion = CompanionFactory.create(companionData.type(), player, configManager, messageManager);
                    companion.spawn(player.getLocation());
                    companion.getData().inventory().setContents(companionData.inventory().getContents());
                    companions.put(uuid, new WeakReference<>(companion));
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to load companion data", e);
        }
    }

    public Map<Player, WeakReference<VoidCompanion>> getActiveCompanions() {
        return Map.copyOf(companions.entrySet().stream()
            .collect(Collectors.toMap(
                e -> plugin.getServer().getPlayer(e.getKey()),
                Map.Entry::getValue,
                (e1, e2) -> e1,
                ConcurrentHashMap::new
            )));
    }

    private int countCompanionsInChunk(Chunk chunk) {
        return (int) Arrays.stream(chunk.getEntities())
            .filter(e -> companions.values().stream().anyMatch(wr -> wr.get() != null && wr.get().getEntity().equals(e)))
            .count();
    }

    private int countCompanionsInWorld(String worldName) {
        return (int) companions.values().stream()
            .map(WeakReference::get)
            .filter(Objects::nonNull)
            .filter(c -> c.getEntity().getWorld().getName().equals(worldName))
            .count();
    }
}