package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.IpBan;
import com.ultimateban.util.IPUtil;
import com.ultimateban.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for unbanning IP addresses
 */
public class IpUnbanCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public IpUnbanCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.ipunban")) {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cUsage: /ipunban <ip|player>"));
            return true;
        }

        String targetIpOrPlayer = args[0];
        String ipAddress;
        
        // Check if input is an IP address or a player name
        if (IPUtil.isValidIp(targetIpOrPlayer)) {
            ipAddress = targetIpOrPlayer;
        } else {
            // Try to get last IP from database
            List<String> ips = plugin.getDatabaseManager().getPlayerIps(plugin.getDatabaseManager().getPlayerUUID(targetIpOrPlayer));
            if (ips.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&c&l⚠ &cCouldn't find IP address for player " + targetIpOrPlayer));
                return true;
            }
            
            ipAddress = ips.get(0); // Get the most recent IP
        }

        // Check if the IP is actually banned
        IpBan ipBan = plugin.getDatabaseManager().getActiveIpBan(ipAddress);
        if (ipBan == null) {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cIP address " + ipAddress + " is not banned!"));
            return true;
        }

        // Deactivate the ban
        if (plugin.getDatabaseManager().deactivateIpBan(ipBan.getId())) {
            // Success message to sender
            String successMsg = plugin.getConfigManager().getConfig().getString(
                    "messages.ipunban.success",
                    "&a&l✓ &aYou have unbanned IP &f" + ipAddress
            ).replace("%ip%", ipAddress);
            
            sender.sendMessage(MessageUtil.color(successMsg));
            
            // Broadcast if enabled
            boolean shouldBroadcast = plugin.getConfigManager().getConfig().getBoolean("broadcast.ipunban", true);
            if (shouldBroadcast) {
                String broadcastMsg = plugin.getConfigManager().getConfig().getString(
                        "messages.ipunban.broadcast",
                        "&a&l✓ &fIP &c" + ipAddress + " &7has been &a&lUNBANNED &7by &c" + sender.getName()
                ).replace("%ip%", ipAddress)
                 .replace("%staff%", sender.getName());
                
                Bukkit.broadcastMessage(MessageUtil.color(broadcastMsg));
            }
        } else {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cFailed to unban IP " + ipAddress));
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("ultimateban.ipunban")) {
            return completions;
        }
        
        if (args.length == 1) {
            // Future enhancement: Add a method to get banned IPs from the database
            // For now, just suggest example IP format
            completions.add("127.0.0.1");
        }
        
        return completions;
    }
} 