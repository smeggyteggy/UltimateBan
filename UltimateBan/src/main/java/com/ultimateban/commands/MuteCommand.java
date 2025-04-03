package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.models.PunishmentType;
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

public class MuteCommand implements CommandExecutor, TabCompleter {
    private final UltimateBan plugin;
    private final boolean isTemporary;

    public MuteCommand(UltimateBan plugin) {
        this(plugin, false);
    }

    public MuteCommand(UltimateBan plugin, boolean isTemporary) {
        this.plugin = plugin;
        this.isTemporary = isTemporary;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = isTemporary ? "ultimateban.tempmute" : "ultimateban.mute";
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color("&cUsage: /" + command.getName() + " <player> " + (isTemporary ? "<duration> " : "") + "<reason>"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID = target != null ? target.getUniqueId() : plugin.getDatabaseManager().getPlayerUUID(targetName);
        
        if (targetUUID == null) {
            sender.sendMessage(MessageUtil.color("&cPlayer not found!"));
            return true;
        }

        // Check if player has bypass permission
        if (target != null && target.hasPermission("ultimateban.bypass.mute")) {
            sender.sendMessage(MessageUtil.color("&cThis player cannot be muted!"));
            return true;
        }

        long endTime = isTemporary ? System.currentTimeMillis() + parseDuration(args[1]) : Long.MAX_VALUE;
        if (isTemporary && endTime == -1) {
            sender.sendMessage(MessageUtil.color("&cInvalid duration format! Use: 1h, 1d, 1w, 1m"));
            return true;
        }

        StringBuilder reason = new StringBuilder();
        for (int i = isTemporary ? 2 : 1; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        Punishment punishment = new Punishment(
            targetUUID,
            targetName,
            sender instanceof Player ? ((Player) sender).getUniqueId() : UUID.randomUUID(),
            sender.getName(),
            isTemporary ? PunishmentType.TEMP_MUTE : PunishmentType.MUTE,
            reason.toString().trim(),
            System.currentTimeMillis(),
            endTime
        );

        if (plugin.getDatabaseManager().savePunishment(punishment)) {
            // Success message to sender
            String successMsg = plugin.getConfigManager().getConfig().getString(
                "messages." + (isTemporary ? "tempmute" : "mute") + ".success", 
                "&a&l✓ &aYou have muted &f%player% " + (isTemporary ? "for &f%duration%" : "permanently")
            );
            
            successMsg = successMsg
                .replace("%player%", targetName)
                .replace("%duration%", isTemporary ? TimeUtil.formatDuration(endTime - System.currentTimeMillis()) : "permanently");
            
            sender.sendMessage(MessageUtil.color(successMsg));
            
            // Message to the muted player
            if (target != null) {
                String playerMsg = plugin.getConfigManager().getConfig().getString(
                    "messages." + (isTemporary ? "tempmute" : "mute") + ".player_message",
                    "&c&l⚠ &" + (isTemporary ? "6" : "4") + "&lYOU HAVE BEEN " + (isTemporary ? "TEMPORARILY " : "") + "MUTED &c&l⚠\n" +
                    "&r&7Reason: &c%reason%\n" +
                    "&7Muted by: &c%staff%" + 
                    (isTemporary ? "\n&7Duration: &c%duration%\n&7Expires: &c%expires%" : "")
                );
                
                playerMsg = playerMsg
                    .replace("%reason%", reason.toString().trim())
                    .replace("%staff%", sender.getName())
                    .replace("%duration%", isTemporary ? TimeUtil.formatDuration(endTime - System.currentTimeMillis()) : "")
                    .replace("%expires%", isTemporary ? TimeUtil.formatTimestamp(endTime) : "")
                    .replace("%date%", TimeUtil.formatTimestamp(System.currentTimeMillis()));
                
                target.sendMessage(MessageUtil.color(playerMsg));
            }
            
            // Broadcast to all players if enabled in config
            boolean shouldBroadcast = plugin.getConfigManager().getConfig().getBoolean("broadcast." + (isTemporary ? "tempmute" : "mute"), true);
            if (shouldBroadcast) {
                String broadcastMsg = plugin.getConfigManager().getConfig().getString(
                    "messages." + (isTemporary ? "tempmute" : "mute") + ".broadcast",
                    "&c&l⚠ &f%player% &7has been &" + (isTemporary ? "6" : "c") + "&l" + (isTemporary ? "TEMPORARILY " : "") + "MUTED &7by &c%staff%" + 
                    (isTemporary ? " &7for &f%duration%" : "") + "&7:\n&f%reason%"
                );
                
                broadcastMsg = broadcastMsg
                    .replace("%player%", targetName)
                    .replace("%staff%", sender.getName())
                    .replace("%reason%", reason.toString().trim())
                    .replace("%duration%", isTemporary ? TimeUtil.formatDuration(endTime - System.currentTimeMillis()) : "");
                
                Bukkit.broadcastMessage(MessageUtil.color(broadcastMsg));
            }
        } else {
            sender.sendMessage(MessageUtil.color("&c&l⚠ &cFailed to mute " + targetName));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
        } else if (args.length == 2 && isTemporary) {
            completions.add("1h");
            completions.add("1d");
            completions.add("1w");
            completions.add("1m");
        }
        return completions;
    }

    private long parseDuration(String duration) {
        try {
            long amount = Long.parseLong(duration.substring(0, duration.length() - 1));
            char unit = duration.charAt(duration.length() - 1);
            switch (unit) {
                case 'h': return amount * 3600000; // hours
                case 'd': return amount * 86400000; // days
                case 'w': return amount * 604800000; // weeks
                case 'm': return amount * 2592000000L; // months
                default: return -1;
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return -1;
        }
    }
} 