package gg.kite.core;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {
    private static Economy economy;

    private VaultHook() {}

    public static void setupEconomy(EchoesPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    public static boolean hasEnough(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public static void withdraw(Player player, double amount) {
        if (economy != null) economy.withdrawPlayer(player, amount);
    }

    public static void deposit(Player player, double amount) {
        if (economy != null) economy.depositPlayer(player, amount);
    }
}