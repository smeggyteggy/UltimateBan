package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.gui.PunishmentGUI;
import com.ultimateban.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PunishCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public PunishCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.punish")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.color("&cThis command can only be used by players!"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color("&cUsage: /punish <player>"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            sender.sendMessage(MessageUtil.color("&cPlayer not found or not online!"));
            return true;
        }

        // Check if player has bypass permission
        if (target.hasPermission("ultimateban.bypass.override")) {
            sender.sendMessage(MessageUtil.color("&cThis player cannot be punished!"));
            return true;
        }

        // Open the punishment GUI
        new PunishmentGUI(plugin, (Player) sender, target).open();
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