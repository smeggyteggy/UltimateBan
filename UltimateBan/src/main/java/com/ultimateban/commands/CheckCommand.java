package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.util.MessageUtil;
import com.ultimateban.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Command for checking a player's punishment status
 */
public class CheckCommand implements CommandExecutor, TabCompleter {

    private final UltimateBan plugin;

    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public CheckCommand(UltimateBan plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute the command
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.color("&cUsage: /" + label + " <player>"));
            return true;
        }

        // Check permission
        if (!sender.hasPermission("ultimateban.check")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }

        // Get target player
        String targetName = args[0];

        // Get target UUID (async)
        CompletableFuture<UUID> targetUuidFuture = plugin.getDatabaseManager().getPlayerUuid(targetName);

        targetUuidFuture.thenAccept(targetUuid -> {
            if (targetUuid == null) {
                // Player not found in database
                sender.sendMessage(MessageUtil.color("&cPlayer not found: " + targetName));
                return;
            }

            // Get player name from database (in case of capitalization differences)
            String storedName = plugin.getDatabaseManager().getPlayerName(targetUuid);
            if (storedName != null) {
                targetName = storedName;
            }

            // Check for active punishments
            Punishment activeBan = plugin.getDatabaseManager().getActiveBan(targetUuid);
            Punishment activeMute = plugin.getDatabaseManager().getActiveMute(targetUuid);

            // Get total counts
            int totalBans = plugin.getDatabaseManager().getPunishmentCount(targetUuid, "ban");
            int totalMutes = plugin.getDatabaseManager().getPunishmentCount(targetUuid, "mute");
            int totalKicks = plugin.getDatabaseManager().getPunishmentCount(targetUuid, "kick");
            int totalWarns = plugin.getDatabaseManager().getPunishmentCount(targetUuid, "warn");

            // Build the response
            StringBuilder response = new StringBuilder();
            response.append(MessageUtil.color("&6Punishment check for &f" + targetName + "&6:"));
            
            // Show ban status
            if (activeBan != null) {
                String banType = activeBan.isPermanent() ? "&4BANNED" : "&6TEMP-BANNED";
                String expiry = activeBan.isPermanent() ? "Never" : 
                        TimeUtil.formatTimeRemaining(activeBan.getEndTime() - System.currentTimeMillis());
                
                response.append(MessageUtil.color("\n&6Ban status: " + banType));
                response.append(MessageUtil.color("\n &7Reason: &f" + activeBan.getReason()));
                response.append(MessageUtil.color("\n &7Staff: &f" + activeBan.getPunisherName()));
                response.append(MessageUtil.color("\n &7Date: &f" + TimeUtil.formatTimestamp(activeBan.getStartTime())));
                
                if (!activeBan.isPermanent()) {
                    response.append(MessageUtil.color("\n &7Expires: &f" + expiry));
                }
            } else {
                response.append(MessageUtil.color("\n&6Ban status: &aNOT BANNED"));
            }
            
            // Show mute status
            if (activeMute != null) {
                String muteType = activeMute.isPermanent() ? "&4MUTED" : "&6TEMP-MUTED";
                String expiry = activeMute.isPermanent() ? "Never" : 
                        TimeUtil.formatTimeRemaining(activeMute.getEndTime() - System.currentTimeMillis());
                
                response.append(MessageUtil.color("\n&6Mute status: " + muteType));
                response.append(MessageUtil.color("\n &7Reason: &f" + activeMute.getReason()));
                response.append(MessageUtil.color("\n &7Staff: &f" + activeMute.getPunisherName()));
                response.append(MessageUtil.color("\n &7Date: &f" + TimeUtil.formatTimestamp(activeMute.getStartTime())));
                
                if (!activeMute.isPermanent()) {
                    response.append(MessageUtil.color("\n &7Expires: &f" + expiry));
                }
            } else {
                response.append(MessageUtil.color("\n&6Mute status: &aNOT MUTED"));
            }
            
            // Show punishment counts
            response.append(MessageUtil.color("\n&6Punishment history:"));
            response.append(MessageUtil.color("\n &7Bans: &f" + totalBans));
            response.append(MessageUtil.color("\n &7Mutes: &f" + totalMutes));
            response.append(MessageUtil.color("\n &7Kicks: &f" + totalKicks));
            response.append(MessageUtil.color("\n &7Warns: &f" + totalWarns));
            
            // Add a message about how to view full history
            response.append(MessageUtil.color("\n&6Use &f/history " + targetName + " &6to view full punishment history."));
            
            // Send the message
            sender.sendMessage(response.toString());
        });

        return true;
    }

    /**
     * Tab complete the command
     *
     * @param sender The command sender
     * @param command The command
     * @param alias The command alias
     * @param args The command arguments
     * @return A list of tab completions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(HumanEntity::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
} 