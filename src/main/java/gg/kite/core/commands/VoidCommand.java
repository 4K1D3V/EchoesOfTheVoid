package gg.kite.core.commands;

import gg.kite.core.companions.CompanionManager;
import gg.kite.core.companions.VoidCompanion;
import gg.kite.core.companions.behaviors.Behavior;
import gg.kite.core.market.MarketManager;
import gg.kite.core.quests.QuestManager;
import gg.kite.core.rifts.RiftManager;
import gg.kite.core.tools.DebugManager;
import gg.kite.core.voidbeacon.VoidBeacon;
import org.bukkit.DyeColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public final class VoidCommand implements CommandExecutor, TabCompleter {
    private final CompanionManager companionManager;
    private final RiftManager riftManager;
    private final MessageManager messageManager;
    private final EchoesPlugin plugin;
    private final QuestManager questManager;
    private final MarketManager marketManager;
    private final VoidBeacon voidBeacon;
    private final DebugManager debugManager;

    @Inject
    public VoidCommand(CompanionManager companionManager, RiftManager riftManager, MessageManager messageManager,
                       EchoesPlugin plugin, QuestManager questManager, MarketManager marketManager,
                       VoidBeacon voidBeacon, DebugManager debugManager) {
        this.companionManager = companionManager;
        this.riftManager = riftManager;
        this.messageManager = messageManager;
        this.plugin = plugin;
        this.questManager = questManager;
        this.marketManager = marketManager;
        this.voidBeacon = voidBeacon;
        this.debugManager = debugManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (args.length == 0) {
            player.sendMessage(messageManager.getMessage("en", "usage"));
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (!plugin.getConfig().getBoolean("commands.void.enabled_subcommands." + subcommand, true)) {
            player.sendMessage(messageManager.getMessage("en", "unknown_command"));
            return true;
        }

        switch (subcommand) {
            case "summon":
                if (!player.hasPermission("echoes.summon")) {
                    player.sendMessage(messageManager.getMessage("en", "no_permission"));
                    return true;
                }
                String type = args.length > 1 ? args[1] : "shadow_wisp";
                companionManager.spawnCompanion(player, type);
                break;
            case "command":
                if (args.length < 2) {
                    player.sendMessage(messageManager.getMessage("en", "command_usage"));
                    return true;
                }
                getCompanion(player).ifPresent(companion -> {
                  companion.getBehavior().setState(Behavior.State.valueOf(args[1].toUpperCase()));
                    player.sendMessage(messageManager.getMessage("en", "command_set", "{state}", args[1]));
                });
                break;
            case "bond":
                getCompanion(player).ifPresent(companion -> {
                    int newBond = companion.getData().bondLevel() + 1;
                    companion.getData().withBond(newBond);
                    player.sendMessage(messageManager.getMessage("en", "bond_increase", "{level}", String.valueOf(newBond)));
                });
                break;
            case "customize":
                if (args.length < 2) {
                    player.sendMessage(messageManager.getMessage("en", "customize_usage"));
                    return true;
                }
                getCompanion(player).ifPresent(companion -> {
                    if (args[1].equals("name") && plugin.getConfig().getBoolean("commands.void.customize_options.name", true)) {
                        String newName = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Unnamed";
                        companion.getData().withName(newName);
                        companion.getEntity().setCustomName(newName);
                        player.sendMessage(messageManager.getMessage("en", "customize_name", "{name}", newName));
                    } else if (args[1].equals("color") && plugin.getConfig().getBoolean("commands.void.customize_options.color", true)) {
                        DyeColor color = args.length > 2 ? DyeColor.valueOf(args[2].toUpperCase()) : DyeColor.WHITE;
                        companion.getData().withColor(color);
                        companion.getEntity().setGlowColor(color);
                        player.sendMessage(messageManager.getMessage("en", "customize_color", "{color}", color.name()));
                    }
                });
                break;
            case "rift":
                if (!player.hasPermission("echoes.admin")) return true;
                riftManager.spawnRift(player.getLocation());
                break;
            case "trade":
                if (args.length < 2) {
                    player.sendMessage(messageManager.getMessage("en", "trade_usage"));
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target != null && target.isOnline()) {
                    Optional<VoidCompanion> sourceOpt = getCompanion(player);
                    Optional<VoidCompanion> targetOpt = getCompanion(target);
                    if (sourceOpt.isPresent() && targetOpt.isPresent()) {
                        companionManager.spawnCompanion(target, sourceOpt.get().getData().type());
                        companionManager.spawnCompanion(player, targetOpt.get().getData().type());
                        player.sendMessage(messageManager.getMessage("en", "trade_success", "{player}", target.getName()));
                        target.sendMessage(messageManager.getMessage("en", "trade_success", "{player}", player.getName()));
                    }
                }
                break;
            case "stats":
                if (!player.hasPermission("echoes.admin")) return true;
                player.sendMessage("§eEchoes Stats:");
                player.sendMessage("§7- Active Companions: " + companionManager.getActiveCompanions().size());
                player.sendMessage("§7- Active Rifts: " + riftManager.getActiveRiftCount());
                player.sendMessage("§7- TPS: " + String.format("%.2f", plugin.getServer().getTPS()[0]));
                break;
            case "prune":
                if (!player.hasPermission("echoes.admin")) return true;
                companionManager.despawnAll();
                riftManager.closeAllRifts();
                player.sendMessage(messageManager.getMessage("en", "prune"));
                break;
            case "skill":
                if (args.length < 2) {
                    player.sendMessage("Usage: /void skill <path>");
                    return true;
                }
                getCompanion(player).ifPresent(companion -> {
                    companion.getData().withSkillPath(args[1]);
                    player.sendMessage(messageManager.getMessage("en", "skill_set", "{path}", args[1]));
                });
                break;
            case "quest":
                if (args.length < 2) {
                    player.sendMessage("Usage: /void quest <quest>");
                    return true;
                }
                questManager.startQuest(player, args[1]);
                break;
            case "market":
                marketManager.openMarket(player);
                break;
            case "beacon":
                if (args.length < 2) {
                    player.sendMessage("Usage: /void beacon teleport <id>");
                    return true;
                }
                if (args[1].equals("teleport")) {
                    Map<String, Location> network = voidBeacon.getBeaconNetwork();
                    String id = args[2];
                    if (network.containsKey(id)) {
                        int cost = plugin.getConfig().getInt("void_beacon.teleport_cost", 2);
                        if (VaultHook.hasEnough(player, cost)) {
                            VaultHook.withdraw(player, cost);
                            player.teleport(network.get(id));
                            player.sendMessage(messageManager.getMessage("en", "beacon_teleport", "{id}", id));
                        } else {
                            player.sendMessage(messageManager.getMessage("en", "beacon_cost", "{cost}", String.valueOf(cost)));
                        }
                    }
                }
                break;
            case "debug":
                if (!player.hasPermission("echoes.admin")) return true;
                debugManager.openDebugGui(player);
                break;
            case "fuse":
                if (args.length < 2) {
                    player.sendMessage("Usage: /void fuse <player>");
                    return true;
                }
                Player fuseTarget = plugin.getServer().getPlayer(args[1]);
                if (fuseTarget != null && fuseTarget.isOnline()) {
                    companionManager.fuseCompanions(player, fuseTarget);
                } else {
                    player.sendMessage(messageManager.getMessage("en", "player_offline"));
                }
                break;
            default:
                player.sendMessage(messageManager.getMessage("en", "unknown_command"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return plugin.getConfig().getConfigurationSection("commands.void.enabled_subcommands").getKeys(false).stream()
                .filter(cmd -> plugin.getConfig().getBoolean("commands.void.enabled_subcommands." + cmd))
                .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                .filter(cmd -> hasPermission(player, cmd))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "summon" -> player.hasPermission("echoes.summon") ?
                    plugin.getConfig().getStringList("commands.void.summon_types").stream()
                        .filter(t -> t.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList()) :
                    Collections.emptyList();
                case "command" -> companionManager.getCompanion(player).isPresent() ?
                    plugin.getConfig().getStringList("commands.void.behavior_states").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList()) :
                    Collections.emptyList();
                case "customize" -> companionManager.getCompanion(player).isPresent() ?
                    plugin.getConfig().getConfigurationSection("commands.void.customize_options").getKeys(false).stream()
                        .filter(o -> plugin.getConfig().getBoolean("commands.void.customize_options." + o))
                        .filter(o -> o.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList()) :
                    Collections.emptyList();
                case "trade", "fuse" -> plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
                case "skill" -> companionManager.getCompanion(player).map(c ->
                    plugin.getConfig().getConfigurationSection("companions." + c.getData().type() + ".skills").getKeys(false).stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList())).orElse(Collections.emptyList());
                case "quest" -> plugin.getConfig().getConfigurationSection("quests").getKeys(false).stream()
                    .filter(q -> q.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
                case "beacon" -> Collections.singletonList("teleport");
                default -> Collections.emptyList();
            };
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("customize") && args[1].equalsIgnoreCase("color") &&
                companionManager.getCompanion(player).isPresent()) {
                return Arrays.stream(DyeColor.values())
                    .map(color -> color.name().toLowerCase())
                    .filter(c -> c.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("beacon") && args[1].equalsIgnoreCase("teleport")) {
                return voidBeacon.getBeaconNetwork().keySet().stream()
                    .filter(id -> id.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private Optional<VoidCompanion> getCompanion(Player player) {
        Optional<VoidCompanion> companion = companionManager.getCompanion(player);
        if (companion.isEmpty()) player.sendMessage(messageManager.getMessage("en", "no_companion"));
        return companion;
    }

    private boolean hasPermission(Player player, String subcommand) {
        return switch (subcommand) {
            case "summon" -> player.hasPermission("echoes.summon");
            case "rift", "stats", "prune", "debug" -> player.hasPermission("echoes.admin");
            default -> true;
        };
    }
}