package gg.kite.core.market;

import gg.kite.core.EchoesPlugin;
import gg.kite.core.MessageManager;
import gg.kite.core.companions.CompanionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Inject;
import java.util.Arrays;

public final class MarketManager implements Listener {
    private final EchoesPlugin plugin;
    private final CompanionManager companionManager;
    private final MessageManager messageManager;

    @Inject
    public MarketManager(EchoesPlugin plugin, CompanionManager companionManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.companionManager = companionManager;
        this.messageManager = messageManager;
    }

    public void openMarket(Player player) {
        Inventory market = Bukkit.createInventory(null, 9, "Void Market");
        plugin.getConfig().getStringList("commands.void.summon_types").forEach(type -> {
            ItemStack item = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§5" + type.replace("_", " "));
            meta.setLore(Arrays.asList("§7Cost: " + plugin.getConfig().getInt("market.shard_rate") + " Void Shards"));
            item.setItemMeta(meta);
            market.addItem(item);
        });
        player.openInventory(market);
    }

    @EventHandler
    public void onMarketClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Void Market")) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String type = item.getItemMeta().getDisplayName().replace("§5", "").replace(" ", "_").toLowerCase();
        int cost = plugin.getConfig().getInt("market.shard_rate");
        if (VaultHook.hasEnough(player, cost)) {
            VaultHook.withdraw(player, cost);
            companionManager.spawnCompanion(player, type);
            player.sendMessage(messageManager.getMessage("en", "market_purchase", "{type}", type.replace("_", " ")));
            player.closeInventory();
        } else {
            player.sendMessage(messageManager.getMessage("en", "market_cost", "{cost}", String.valueOf(cost)));
        }
    }
}