package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Player;
import com.ultimateban.util.MessageUtil;
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
 * Command for checking alt accounts
 */
public class AltsCommand implements CommandExecutor, TabCompleter {

    private final UltimateBan plugin;

    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public AltsCommand(UltimateBan plugin) {
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
        if (!sender.hasPermission("ultimateban.alts")) {
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

            // Get player's IP addresses
            List<String> ips = plugin.getDatabaseManager().getPlayerIps(targetUuid);
            if (ips.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&cNo IP history found for " + targetName));
                return;
            }

            // Get all accounts linked to these IPs
            List<Player> allAccounts = new ArrayList<>();
            for (String ip : ips) {
                List<UUID> accountsOnIp = plugin.getDatabaseManager().getPlayersWithIp(ip);
                for (UUID accountUuid : accountsOnIp) {
                    // Skip the target player
                    if (accountUuid.equals(targetUuid)) {
                        continue;
                    }

                    // Get player info
                    String accountName = plugin.getDatabaseManager().getPlayerName(accountUuid);
                    if (accountName != null) {
                        Player altPlayer = new Player(accountUuid, accountName);
                        
                        // Check if banned
                        if (plugin.getDatabaseManager().getActiveBan(accountUuid) != null) {
                            altPlayer.setBanned(true);
                        }
                        
                        // Check if muted
                        if (plugin.getDatabaseManager().getActiveMute(accountUuid) != null) {
                            altPlayer.setMuted(true);
                        }
                        
                        // Only add if not already in the list
                        if (!allAccounts.contains(altPlayer)) {
                            allAccounts.add(altPlayer);
                        }
                    }
                }
            }

            // Display results
            if (allAccounts.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&aNo alt accounts found for &f" + targetName));
                return;
            }

            sender.sendMessage(MessageUtil.color("&6Alt accounts for &f" + targetName + " &6(" + allAccounts.size() + "):"));
            
            // Group accounts by banned status for better visibility
            List<Player> bannedAlts = allAccounts.stream()
                    .filter(Player::isBanned)
                    .collect(Collectors.toList());
            
            List<Player> unbannedAlts = allAccounts.stream()
                    .filter(p -> !p.isBanned())
                    .collect(Collectors.toList());
            
            // Show banned alts first
            if (!bannedAlts.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&c&lBanned accounts:"));
                for (Player alt : bannedAlts) {
                    sender.sendMessage(MessageUtil.color(" &8- &c" + alt.getName() + 
                            (alt.isMuted() ? " &7(muted)" : "")));
                }
            }
            
            // Then show unbanned alts
            if (!unbannedAlts.isEmpty()) {
                sender.sendMessage(MessageUtil.color("&a&lActive accounts:"));
                for (Player alt : unbannedAlts) {
                    sender.sendMessage(MessageUtil.color(" &8- &f" + alt.getName() + 
                            (alt.isMuted() ? " &7(muted)" : "")));
                }
            }
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