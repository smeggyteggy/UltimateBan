package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HistoryCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public HistoryCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.history")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color("&cUsage: /history <player>"));
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = plugin.getDatabaseManager().getPlayerUUID(targetName);
        
        if (targetUUID == null) {
            sender.sendMessage(MessageUtil.color("&cPlayer not found!"));
            return true;
        }

        List<Punishment> punishments = plugin.getDatabaseManager().getPlayerPunishments(targetUUID);
        if (punishments.isEmpty()) {
            sender.sendMessage(MessageUtil.color("&cNo punishment history found for " + targetName));
            return true;
        }

        sender.sendMessage(MessageUtil.color("&6=== Punishment History for " + targetName + " ==="));
        for (Punishment punishment : punishments) {
            String status = punishment.isActive() ? "&cActive" : "&aExpired";
            String duration = punishment.isPermanent() ? "Permanent" : 
                formatDuration(punishment.getDuration());
            sender.sendMessage(MessageUtil.color(String.format(
                "&7Type: &f%s &7| %s &7| Duration: &f%s &7| Reason: &f%s",
                punishment.getType().name(),
                status,
                duration,
                punishment.getReason()
            )));
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

    private String formatDuration(long duration) {
        if (duration < 0) return "Permanent";
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;

        if (months > 0) return months + "m";
        if (weeks > 0) return weeks + "w";
        if (days > 0) return days + "d";
        if (hours > 0) return hours + "h";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }
} 