package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnmuteCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public UnmuteCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.unmute")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color("&cUsage: /unmute <player>"));
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = plugin.getDatabaseManager().getPlayerUUID(targetName);
        
        if (targetUUID == null) {
            sender.sendMessage(MessageUtil.color("&cPlayer not found!"));
            return true;
        }

        if (plugin.getDatabaseManager().deactivatePunishment(targetUUID, "MUTE")) {
            sender.sendMessage(MessageUtil.color("&aSuccessfully unmuted " + targetName));
            // Broadcast to all players if enabled in config
            if (plugin.getConfigManager().getConfig().getBoolean("broadcast.unmute", true)) {
                Bukkit.broadcastMessage(MessageUtil.color("&a" + targetName + " has been unmuted by " + sender.getName()));
            }
        } else {
            sender.sendMessage(MessageUtil.color("&cFailed to unmute " + targetName + ". Player might not be muted."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
        }
        return completions;
    }
} 