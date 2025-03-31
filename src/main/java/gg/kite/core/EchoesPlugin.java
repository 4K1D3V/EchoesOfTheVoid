package gg.kite.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import gg.kite.core.commands.VoidCommand;
import gg.kite.core.companions.CompanionManager;
import gg.kite.core.config.ConfigManager;
import gg.kite.core.dungeons.DungeonManager;
import gg.kite.core.events.CompanionListener;
import gg.kite.core.instability.InstabilityManager;
import gg.kite.core.market.MarketManager;
import gg.kite.core.quests.QuestManager;
import gg.kite.core.rifts.RiftManager;
import gg.kite.core.seasonal.SeasonalManager;
import gg.kite.core.synergy.SynergyManager;
import gg.kite.core.tools.DebugManager;
import gg.kite.core.voidbeacon.VoidBeacon;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class EchoesPlugin extends JavaPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoesPlugin.class);
    private static final int BSTATS_PLUGIN_ID = 12345;
    private Injector injector;
    private ScheduledExecutorService executorService;
    private ScheduledExecutorService aiExecutor;

    @Override
    public void onEnable() {
        saveResource("messages.yml", false);
        saveDefaultConfig();
        String resourcePackUrl = getConfig().getString("general.resource_pack_url", "");
        if (!resourcePackUrl.isEmpty()) getServer().setResourcePack(resourcePackUrl);
        injector = Guice.createInjector(new EchoesModule(this));

        CompanionManager companionManager = injector.getInstance(CompanionManager.class);
        VoidBeacon voidBeacon = injector.getInstance(VoidBeacon.class);
        RiftManager riftManager = injector.getInstance(RiftManager.class);
        SynergyManager synergyManager = injector.getInstance(SynergyManager.class);
        VoidCommand voidCommand = injector.getInstance(VoidCommand.class);
        QuestManager questManager = injector.getInstance(QuestManager.class);
        MarketManager marketManager = injector.getInstance(MarketManager.class);
        SeasonalManager seasonalManager = injector.getInstance(SeasonalManager.class);
        DebugManager debugManager = injector.getInstance(DebugManager.class);
        InstabilityManager instabilityManager = injector.getInstance(InstabilityManager.class);
        DungeonManager dungeonManager = injector.getInstance(DungeonManager.class);

        getServer().getPluginManager().registerEvents(injector.getInstance(CompanionListener.class), this);
        getServer().getPluginManager().registerEvents(voidBeacon, this);
        getServer().getPluginManager().registerEvents(marketManager, this);
        getServer().getPluginManager().registerEvents(questManager, this);
        getCommand("void").setExecutor(voidCommand);
        getCommand("void").setTabCompleter(voidCommand);
        voidBeacon.registerRecipe();

        int threadPoolSize = getConfig().getInt("performance.thread_pool_size", 4);
        int aiPriority = getConfig().getInt("performance.ai_thread_priority", 5);
        executorService = Executors.newScheduledThreadPool(threadPoolSize);
        aiExecutor = Executors.newScheduledThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r);
            t.setPriority(aiPriority);
            return t;
        });
        int tickIntervalBase = getConfig().getInt("performance.tick_interval_base", 2);
        executorService.scheduleAtFixedRate(() -> {
            double tps = getServer().getTPS()[0];
            int adjustedInterval = (int) (tickIntervalBase * (20.0 / Math.max(5.0, tps)));
            if (getConfig().getBoolean("rifts.enabled", true)) riftManager.tickRifts();
            if (getConfig().getBoolean("synergies.enabled", true)) synergyManager.tickSynergies();
            if (getConfig().getBoolean("quests.enabled", true)) questManager.tickQuests();
            seasonalManager.tickSeasonalEvents();
            voidBeacon.tickAuras();
            instabilityManager.tick();
            dungeonManager.tickDungeons();
        }, 0, tickIntervalBase, TimeUnit.MILLISECONDS);
        aiExecutor.scheduleAtFixedRate(companionManager::tickCompanions, 0, tickIntervalBase, TimeUnit.MILLISECONDS);

        VaultHook.setupEconomy(this);
        companionManager.startPersistenceTask(executorService);
        setupMetrics(companionManager, riftManager, voidBeacon, instabilityManager);

        LOGGER.info("Echoes of the Void v1.0.0 by KiteGG initialized with {} threads, AI priority {}.", threadPoolSize, aiPriority);
    }

    @Override
    public void onDisable() {
        injector.getInstance(CompanionManager.class).despawnAll();
        injector.getInstance(RiftManager.class).closeAllRifts();
        injector.getInstance(DungeonManager.class).closeAllDungeons();
        executorService.shutdown();
        aiExecutor.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) executorService.shutdownNow();
            if (!aiExecutor.awaitTermination(5, TimeUnit.SECONDS)) aiExecutor.shutdownNow();
        } catch (InterruptedException e) {
            LOGGER.error("Failed to shut down executors cleanly", e);
        }
        LOGGER.info("Echoes of the Void shut down cleanly.");
    }

    public Injector getInjector() {
        return injector;
    }

    private void setupMetrics(CompanionManager companionManager, RiftManager riftManager, VoidBeacon voidBeacon, InstabilityManager instabilityManager) {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new AdvancedPie("companion_types", () -> {
            Map<String, Integer> counts = new HashMap<>();
            companionManager.getActiveCompanions().values().stream()
                .map(WeakReference::get)
                .filter(Objects::nonNull)
                .forEach(c -> counts.merge(c.getData().type(), 1, Integer::sum));
            return counts;
        }));
        metrics.addCustomChart(new SingleLineChart("active_companions", () ->
            (int) companionManager.getActiveCompanions().values().stream().filter(Objects::nonNull).count()));
        metrics.addCustomChart(new SingleLineChart("active_rifts", riftManager::getActiveRiftCount));
        metrics.addCustomChart(new SingleLineChart("beacon_count", () ->
            (int) (voidBeacon.getBeaconCount(1) + voidBeacon.getBeaconCount(2) + voidBeacon.getBeaconCount(3))));
        metrics.addCustomChart(new SingleLineChart("instability_level", instabilityManager::getInstability));
        metrics.addCustomChart(new SingleLineChart("tps_impact", () ->
            (int) (20 - Math.min(20, getServer().getTPS()[0]))));
        LOGGER.info("bStats metrics enabled with big server tracking.");
    }
}