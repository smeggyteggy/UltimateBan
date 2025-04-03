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

public class UnbanCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public UnbanCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.unban")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color("&cUsage: /unban <player>"));
            return true;
        }

        String targetName = args[0];
        if (plugin.getDatabaseManager().unbanPlayer(targetName)) {
            sender.sendMessage(MessageUtil.color("&aSuccessfully unbanned " + targetName));
            // Broadcast to all players if enabled in config
            if (plugin.getConfigManager().getConfig().getBoolean("broadcast.unban", true)) {
                Bukkit.broadcastMessage(MessageUtil.color("&a" + targetName + " has been unbanned by " + sender.getName()));
            }
        } else {
            sender.sendMessage(MessageUtil.color("&cFailed to unban " + targetName + ". Player might not be banned."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.getDatabaseManager().getBannedPlayers();
        }
        return new ArrayList<>();
    }
} 