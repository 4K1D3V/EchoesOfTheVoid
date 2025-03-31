package gg.kite.core;

import com.google.inject.AbstractModule;

public final class EchoesModule extends AbstractModule {
    private final EchoesPlugin plugin;

    public EchoesModule(EchoesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(EchoesPlugin.class).toInstance(plugin);
        bind(ConfigManager.class).asEagerSingleton();
        bind(CompanionManager.class).asEagerSingleton();
        bind(VoidBeacon.class).asEagerSingleton();
        bind(RiftManager.class).asEagerSingleton();
        bind(SynergyManager.class).asEagerSingleton();
        bind(VoidCommand.class).asEagerSingleton();
        bind(CompanionListener.class).asEagerSingleton();
        bind(MessageManager.class).asEagerSingleton();
        bind(QuestManager.class).asEagerSingleton();
        bind(MarketManager.class).asEagerSingleton();
        bind(SeasonalManager.class).asEagerSingleton();
        bind(DebugManager.class).asEagerSingleton();
        bind(InstabilityManager.class).asEagerSingleton();
        bind(DungeonManager.class).asEagerSingleton();
    }
}