package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.IpBan;
import com.ultimateban.util.IPUtil;
import com.ultimateban.util.MessageUtil;
import com.ultimateban.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command for banning IP addresses
 */
public class IpBanCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;
    private final boolean isTemporary;

    /**
     * Constructor for permanent IP ban command
     *
     * @param plugin The UltimateBan plugin instance
     */
    public IpBanCommand(UltimateBan plugin) {
        this(plugin, false);
    }

    /**
     * Constructor
     *
     * @param plugin      The UltimateBan plugin instance
     * @param isTemporary Whether this is a temporary IP ban command
     */
    public IpBanCommand(UltimateBan plugin, boolean isTemporary) {
        this.plugin = plugin;
        this.isTemporary = isTemporary;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = isTemporary ? "ultimateban.tempipban" : "ultimateban.ipban";
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < (isTemporary ? 3 : 2)) {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cUsage: /" + command.getName() + " <ip|player> " +
                    (isTemporary ? "<duration> " : "") + "<reason>"));
            return true;
        }

        String targetIpOrPlayer = args[0];
        String ipAddress;
        
        // Check if input is an IP address or a player name
        if (IPUtil.isValidIp(targetIpOrPlayer)) {
            ipAddress = targetIpOrPlayer;
        } else {
            // Try to find player online
            Player target = Bukkit.getPlayer(targetIpOrPlayer);
            
            if (target != null) {
                ipAddress = target.getAddress().getAddress().getHostAddress();
            } else {
                // Try to get last IP from database
                List<String> ips = plugin.getDatabaseManager().getPlayerIps(plugin.getDatabaseManager().getPlayerUUID(targetIpOrPlayer));
                if (ips.isEmpty()) {
                    sender.sendMessage(MessageUtil.color("&c&l⚠ &cCouldn't find IP address for player " + targetIpOrPlayer));
                    return true;
                }
                
                ipAddress = ips.get(0); // Get the most recent IP
            }
        }
        
        // Calculate ban duration
        long endTime;
        int reasonStartIndex;
        
        if (isTemporary) {
            try {
                long duration = TimeUtil.parseDuration(args[1]);
                if (duration <= 0) {
                    sender.sendMessage(MessageUtil.color("&c&l⚠ &cInvalid duration format! Use: 1h, 1d, 1w, 1m"));
                    return true;
                }
                
                endTime = System.currentTimeMillis() + duration;
                reasonStartIndex = 2;
            } catch (IllegalArgumentException e) {
                sender.sendMessage(MessageUtil.color("&c&l⚠ &cInvalid duration format! Use: 1h, 1d, 1w, 1m"));
                return true;
            }
        } else {
            endTime = Long.MAX_VALUE; // Permanent ban
            reasonStartIndex = 1;
        }

        // Build the reason string
        StringBuilder reason = new StringBuilder();
        for (int i = reasonStartIndex; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        // Check if there's an active ban for this IP already
        if (plugin.getDatabaseManager().getActiveIpBan(ipAddress) != null) {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cIP address " + ipAddress + " is already banned!"));
            return true;
        }

        // Create the IP ban
        boolean isSubnet = false; // Default, allow for subnet bans with a different command
        
        IpBan ipBan = new IpBan(
                ipAddress,
                sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID(),
                sender.getName(),
                reason.toString().trim(),
                System.currentTimeMillis(),
                endTime,
                isSubnet
        );

        // Save the ban
        if (plugin.getDatabaseManager().saveIpBan(ipBan)) {
            // Success message to sender
            String successMsg = plugin.getConfigManager().getConfig().getString(
                    "messages.ipban.success",
                    "&a&l✓ &aYou have banned IP &f" + ipAddress + (isTemporary ? " &afor &f%duration%" : " &apermanently")
            );
            
            successMsg = successMsg
                    .replace("%ip%", ipAddress)
                    .replace("%duration%", isTemporary ? TimeUtil.formatDuration(endTime - System.currentTimeMillis()) : "permanently");
            
            sender.sendMessage(MessageUtil.color(successMsg));
            
            // Broadcast if enabled
            boolean shouldBroadcast = plugin.getConfigManager().getConfig().getBoolean("broadcast.ipban", true);
            if (shouldBroadcast) {
                String broadcastMsg = plugin.getConfigManager().getConfig().getString(
                        "messages.ipban.broadcast",
                        "&c&l⚠ &fIP &c" + ipAddress + " &7has been &" + (isTemporary ? "6" : "c") + "&l" +
                                (isTemporary ? "TEMPORARILY " : "") + "BANNED &7by &c" + sender.getName() +
                                (isTemporary ? " &7for &f%duration%" : "") + "&7:\n&f" + reason
                );
                
                broadcastMsg = broadcastMsg
                        .replace("%ip%", ipAddress)
                        .replace("%staff%", sender.getName())
                        .replace("%reason%", reason.toString().trim())
                        .replace("%duration%", isTemporary ? TimeUtil.formatDuration(endTime - System.currentTimeMillis()) : "");
                
                Bukkit.broadcastMessage(MessageUtil.color(broadcastMsg));
            }
            
            // Kick all players with this IP if they're online
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getAddress().getAddress().getHostAddress().equals(ipAddress)) {
                    // Skip players with bypass permission
                    if (player.hasPermission("ultimateban.bypass.ipban")) {
                        continue;
                    }
                    
                    // Format kick message
                    String kickMsg = plugin.getConfigManager().getConfig().getString(
                            "messages.ipban.player-message",
                            "&c&l⚠ &4&lYOUR IP IS BANNED &c&l⚠\n\n" +
                                    "&r&7Reason: &c" + reason.toString().trim() + "\n" +
                                    "&7Banned by: &c" + sender.getName() + "\n" +
                                    (isTemporary ? "&7Expires: &c" + TimeUtil.formatTimestamp(endTime) + "\n" : "") +
                                    "&7Date: &c" + TimeUtil.formatTimestamp(System.currentTimeMillis()) + "\n\n" +
                                    "&7Appeal at: &b&nminecraft.example.com/appeal"
                    );
                    
                    kickMsg = kickMsg
                            .replace("%reason%", reason.toString().trim())
                            .replace("%staff%", sender.getName())
                            .replace("%expires%", isTemporary ? TimeUtil.formatTimestamp(endTime) : "Never")
                            .replace("%date%", TimeUtil.formatTimestamp(System.currentTimeMillis()));
                    
                    player.kickPlayer(MessageUtil.color(kickMsg));
                }
            }
        } else {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cFailed to ban IP " + ipAddress));
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission(isTemporary ? "ultimateban.tempipban" : "ultimateban.ipban")) {
            return completions;
        }
        
        if (args.length == 1) {
            // Suggest online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            
            // Also suggest example IP format
            completions.add("127.0.0.1");
        } else if (args.length == 2 && isTemporary) {
            // Suggest durations for temporary bans
            completions.add("1h");
            completions.add("1d");
            completions.add("1w");
            completions.add("1m");
            completions.add("1y");
        } else if ((args.length == 2 && !isTemporary) || (args.length == 3 && isTemporary)) {
            // Suggest common reasons
            completions.add("Proxy/VPN");
            completions.add("Ban evasion");
            completions.add("Multiple accounts");
        }
        
        return completions;
    }
} 