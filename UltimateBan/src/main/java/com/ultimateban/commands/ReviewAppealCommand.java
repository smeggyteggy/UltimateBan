package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Appeal;
import com.ultimateban.models.Punishment;
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

public class ReviewAppealCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public ReviewAppealCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.reviewappeal")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color("&cUsage: /reviewappeal <accept/reject> <id> [response]"));
            return true;
        }

        String action = args[0].toLowerCase();
        if (!action.equals("accept") && !action.equals("reject")) {
            sender.sendMessage(MessageUtil.color("&cInvalid action! Use 'accept' or 'reject'"));
            return true;
        }

        int appealId;
        try {
            appealId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.color("&cInvalid appeal ID!"));
            return true;
        }

        Appeal appeal = plugin.getDatabaseManager().getAppeal(appealId);
        if (appeal == null) {
            sender.sendMessage(MessageUtil.color("&cAppeal not found!"));
            return true;
        }

        if (!appeal.isPending()) {
            sender.sendMessage(MessageUtil.color("&cThis appeal has already been reviewed!"));
            return true;
        }

        StringBuilder response = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            response.append(args[i]).append(" ");
        }

        if (action.equals("accept")) {
            appeal.accept(
                sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID(),
                sender.getName(),
                response.toString().trim()
            );
            // Deactivate the punishment
            Punishment punishment = plugin.getDatabaseManager().getPunishment(appeal.getPunishmentId());
            if (punishment != null) {
                plugin.getDatabaseManager().deactivatePunishment(punishment.getId());
            }
        } else {
            appeal.reject(
                sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID(),
                sender.getName(),
                response.toString().trim()
            );
        }

        if (plugin.getDatabaseManager().updateAppeal(appeal)) {
            sender.sendMessage(MessageUtil.color("&aSuccessfully " + action + "ed appeal #" + appealId));
            // Notify the player if they're online
            Player target = Bukkit.getPlayer(appeal.getPlayerUUID());
            if (target != null) {
                target.sendMessage(MessageUtil.color("&eYour appeal #" + appealId + " has been " + action + "ed"));
                if (!response.toString().trim().isEmpty()) {
                    target.sendMessage(MessageUtil.color("&7Response: " + response.toString().trim()));
                }
            }
        } else {
            sender.sendMessage(MessageUtil.color("&cFailed to update appeal!"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("accept");
            completions.add("reject");
        } else if (args.length == 2) {
            // Add pending appeal IDs
            plugin.getDatabaseManager().getPendingAppeals().forEach(appeal -> 
                completions.add(String.valueOf(appeal.getId())));
        }
        return completions;
    }
} 