package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Appeal;
import com.ultimateban.models.Punishment;
import com.ultimateban.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppealCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public AppealCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.color("&cThis command can only be used by players!"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("ultimateban.appeal")) {
            player.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.color("&cUsage: /appeal <reason>"));
            return true;
        }

        // Check if player has any active punishments
        List<Punishment> activePunishments = plugin.getDatabaseManager().getActivePunishments(player.getUniqueId());
        if (activePunishments.isEmpty()) {
            player.sendMessage(MessageUtil.color("&cYou don't have any active punishments to appeal!"));
            return true;
        }

        // Check if player already has pending appeals
        if (!plugin.getDatabaseManager().getPlayerAppeals(player.getUniqueId()).isEmpty()) {
            player.sendMessage(MessageUtil.color("&cYou already have a pending appeal!"));
            return true;
        }

        StringBuilder reason = new StringBuilder();
        for (String arg : args) {
            reason.append(arg).append(" ");
        }

        // Create appeal for the most recent active punishment
        Punishment punishment = activePunishments.get(0);
        Appeal appeal = new Appeal(
            punishment.getId(),
            player.getUniqueId(),
            player.getName(),
            reason.toString().trim()
        );

        if (plugin.getDatabaseManager().saveAppeal(appeal) > 0) {
            player.sendMessage(MessageUtil.color("&aYour appeal has been submitted successfully!"));
            // Notify staff if enabled in config
            if (plugin.getConfigManager().getConfig().getBoolean("notify.staff.appeals", true)) {
                plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("ultimateban.reviewappeal"))
                    .forEach(p -> p.sendMessage(MessageUtil.color("&eNew appeal submitted by " + player.getName())));
            }
        } else {
            player.sendMessage(MessageUtil.color("&cFailed to submit your appeal!"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
} 