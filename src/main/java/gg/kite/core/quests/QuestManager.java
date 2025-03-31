package gg.kite.core.quests;

import gg.kite.core.EchoesPlugin;
import gg.kite.core.MessageManager;
import gg.kite.core.companions.CompanionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestManager implements Listener {
    private final EchoesPlugin plugin;
    private final MessageManager messageManager;
    private final CompanionManager companionManager;
    private final Map<UUID, String> activeQuests = new HashMap<>();
    private final Map<UUID, Integer> questProgress = new HashMap<>();

    @Inject
    public QuestManager(EchoesPlugin plugin, MessageManager messageManager, CompanionManager companionManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.companionManager = companionManager;
    }

    public void startQuest(Player player, String quest) {
        if (!plugin.getConfig().getConfigurationSection("quests").getKeys(false).contains(quest)) return;
        activeQuests.put(player.getUniqueId(), quest);
        questProgress.put(player.getUniqueId(), 0);
        player.sendMessage(messageManager.getMessage("en", "quest_start", "{quest}", quest));
    }

    public void tickQuests() {
        // No periodic tasks needed yet
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null || !activeQuests.containsKey(player.getUniqueId()) || !activeQuests.get(player.getUniqueId()).equals("kill_mobs")) return;

        int progress = questProgress.getOrDefault(player.getUniqueId(), 0) + 1;
        int goal = plugin.getConfig().getInt("quests.kill_mobs.count");
        questProgress.put(player.getUniqueId(), progress);

        if (progress >= goal) {
            completeQuest(player, "kill_mobs");
        }
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!activeQuests.containsKey(player.getUniqueId()) || !activeQuests.get(player.getUniqueId()).equals("collect_items")) return;
        if (event.getItem().getItemStack().getType() != Material.valueOf(plugin.getConfig().getString("quests.collect_items.item"))) return;

        int progress = questProgress.getOrDefault(player.getUniqueId(), 0) + event.getItem().getItemStack().getAmount();
        int goal = plugin.getConfig().getInt("quests.collect_items.count");
        questProgress.put(player.getUniqueId(), progress);

        if (progress >= goal) {
            completeQuest(player, "collect_items");
        }
    }

    private void completeQuest(Player player, String quest) {
        String reward = plugin.getConfig().getString("quests." + quest + ".reward");
        int xp = plugin.getConfig().getInt("quests." + quest + ".reward_xp");
        companionManager.getCompanion(player).ifPresent(companion -> {
            switch (reward) {
                case "purify" -> companion.getData().withCorrupted(false);
                case "tier_up" -> companion.getData().withTier(companion.getData().tier() + 1);
            }
            companion.getData().withXp(companion.getData().xp() + xp);
        });
        activeQuests.remove(player.getUniqueId());
        questProgress.remove(player.getUniqueId());
        player.sendMessage(messageManager.getMessage("en", "quest_complete", "{quest}", quest));
    }
}