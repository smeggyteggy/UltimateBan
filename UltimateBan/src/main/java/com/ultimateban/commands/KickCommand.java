package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.models.PunishmentType;
import com.ultimateban.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KickCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public KickCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.kick")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color("&cUsage: /kick <player> <reason>"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sender.sendMessage(MessageUtil.color("&cPlayer not found or not online!"));
            return true;
        }

        // Check if player has bypass permission
        if (target.hasPermission("ultimateban.bypass.kick")) {
            sender.sendMessage(MessageUtil.color("&cThis player cannot be kicked!"));
            return true;
        }

        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        Punishment punishment = new Punishment(
            target.getUniqueId(),
            target.getName(),
            sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID(),
            sender.getName(),
            PunishmentType.KICK,
            reason.toString().trim(),
            System.currentTimeMillis(),
            System.currentTimeMillis() // Kicks don't expire
        );

        if (plugin.getDatabaseManager().savePunishment(punishment)) {
            sender.sendMessage(MessageUtil.color("&aSuccessfully kicked " + targetName));
            target.kickPlayer(MessageUtil.color("&cYou have been kicked by " + sender.getName() + "\n&7Reason: " + reason.toString().trim()));
            // Broadcast to all players if enabled in config
            if (plugin.getConfigManager().getConfig().getBoolean("broadcast.kick", true)) {
                Bukkit.broadcastMessage(MessageUtil.color("&c" + targetName + " has been kicked by " + sender.getName()));
            }
        } else {
            sender.sendMessage(MessageUtil.color("&cFailed to kick " + targetName));
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