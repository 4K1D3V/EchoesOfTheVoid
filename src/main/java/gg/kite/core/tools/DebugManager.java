package gg.kite.core.tools;

import gg.kite.core.EchoesPlugin;
import gg.kite.core.companions.CompanionManager;
import gg.kite.core.rifts.RiftManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Inject;
import java.util.Arrays;

public final class DebugManager {
    private final EchoesPlugin plugin;
    private final CompanionManager companionManager;
    private final RiftManager riftManager;

    @Inject
    public DebugManager(EchoesPlugin plugin, CompanionManager companionManager, RiftManager riftManager) {
        this.plugin = plugin;
        this.companionManager = companionManager;
        this.riftManager = riftManager;
    }

    public void openDebugGui(Player player) {
        Inventory debug = Bukkit.createInventory(null, 9, "Debug Menu");
        ItemStack stats = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = stats.getItemMeta();
        meta.setDisplayName("§eServer Stats");
        meta.setLore(Arrays.asList(
            "§7Companions: " + companionManager.getActiveCompanions().size(),
            "§7Rifts: " + riftManager.getActiveRiftCount(),
            "§7TPS: " + String.format("%.2f", plugin.getServer().getTPS()[0])
        ));
        stats.setItemMeta(meta);
        debug.setItem(0, stats);
        player.openInventory(debug);
    }
}