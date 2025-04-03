package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UltimateBanCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;

    public UltimateBanCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultimateban.admin")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (plugin.reload()) {
                    sender.sendMessage(MessageUtil.color("&aPlugin reloaded successfully!"));
                } else {
                    sender.sendMessage(MessageUtil.color("&cFailed to reload plugin!"));
                }
                break;
            case "version":
                sender.sendMessage(MessageUtil.color("&6UltimateBan &fversion &a" + plugin.getDescription().getVersion()));
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.color("&6=== UltimateBan Help ==="));
        sender.sendMessage(MessageUtil.color("&f/ultimateban reload &7- Reload the plugin configuration"));
        sender.sendMessage(MessageUtil.color("&f/ultimateban version &7- Show plugin version"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ultimateban.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "version");
        }
        return new ArrayList<>();
    }
} 