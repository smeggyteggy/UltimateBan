package com.ultimateban.commands;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.models.PunishmentTemplate;
import com.ultimateban.models.PunishmentType;
import com.ultimateban.services.PunishmentEscalationService;
import com.ultimateban.services.PunishmentTemplateService;
import com.ultimateban.util.MessageUtil;
import com.ultimateban.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command for banning players
 */
public class BanCommand implements CommandExecutor, TabCompleter {

    private final UltimateBan plugin;
    private final boolean isTemporary;

    /**
     * Constructor for permanent ban command
     *
     * @param plugin The UltimateBan plugin instance
     */
    public BanCommand(UltimateBan plugin) {
        this.plugin = plugin;
        this.isTemporary = false;
    }

    /**
     * Constructor for ban command
     *
     * @param plugin The UltimateBan plugin instance
     * @param isTemporary Whether this is a temporary ban command
     */
    public BanCommand(UltimateBan plugin, boolean isTemporary) {
        this.plugin = plugin;
        this.isTemporary = isTemporary;
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
            sender.sendMessage(MessageUtil.color("&cUsage: /" + label + " <player> [reason] [-s]"));
            return true;
        }
        
        // Check permission
        if (!sender.hasPermission("ultimateban.ban")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }
        
        // Get target player
        String targetName = args[0];
        
        // Check if silent flag is present
        boolean silent = false;
        String[] newArgs = args;
        if (args.length > 1 && args[args.length - 1].equalsIgnoreCase("-s")) {
            silent = true;
            newArgs = Arrays.copyOf(args, args.length - 1);
        }
        
        // Extract template name if provided with #
        String templateName = null;
        if (newArgs.length > 1 && newArgs[1].startsWith("#")) {
            templateName = newArgs[1].substring(1); // Remove the # symbol
            
            // Rebuild args without the template
            String[] tempArgs = new String[newArgs.length - 1];
            tempArgs[0] = newArgs[0];
            if (newArgs.length > 2) {
                System.arraycopy(newArgs, 2, tempArgs, 1, newArgs.length - 2);
            }
            newArgs = tempArgs;
        }
        
        // Build ban reason
        String reason = "No reason specified";
        if (newArgs.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < newArgs.length; i++) {
                if (i > 1) {
                    reasonBuilder.append(" ");
                }
                reasonBuilder.append(newArgs[i]);
            }
            reason = reasonBuilder.toString();
        }
        
        // Get punisher name
        String punisherName = sender instanceof Player ? sender.getName() : "Console";
        UUID punisherUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        
        // Get target UUID (async)
        CompletableFuture<UUID> targetUuidFuture = plugin.getDatabaseManager().getPlayerUuid(targetName);
        
        targetUuidFuture.thenAccept(targetUuid -> {
            if (targetUuid == null) {
                // Player not found in database, try to resolve from Bukkit
                Player targetPlayer = Bukkit.getPlayerExact(targetName);
                if (targetPlayer != null) {
                    targetUuid = targetPlayer.getUniqueId();
                    targetName = targetPlayer.getName();
                } else {
                    // Player not found
                    sender.sendMessage(MessageUtil.color("&cPlayer not found: " + targetName));
                    return;
                }
            }
            
            // Check if player has permission to ban this target
            if (sender instanceof Player) {
                Player targetPlayer = Bukkit.getPlayer(targetUuid);
                if (targetPlayer != null && !canBan((Player) sender, targetPlayer)) {
                    sender.sendMessage(MessageUtil.color("&cYou don't have permission to ban this player."));
                    return;
                }
            }
            
            // Handle ban based on template or escalation logic
            handleBan(sender, targetUuid, targetName, reason, punisherName, punisherUuid, silent, templateName);
        });
        
        return true;
    }
    
    /**
     * Handle the ban process based on templates or escalation
     * 
     * @param sender The command sender
     * @param targetUuid The target player UUID
     * @param targetName The target player name
     * @param reason The ban reason
     * @param punisherName The punisher name
     * @param punisherUuid The punisher UUID
     * @param silent Whether the ban should be silent
     * @param templateName The optional template name
     */
    private void handleBan(CommandSender sender, UUID targetUuid, String targetName, String reason, 
            String punisherName, UUID punisherUuid, boolean silent, String templateName) {
        
        // Check for active ban
        Punishment activeBan = plugin.getDatabaseManager().getActiveBan(targetUuid);
        if (activeBan != null) {
            sender.sendMessage(MessageUtil.color("&cPlayer " + targetName + " is already banned."));
            return;
        }
        
        // Default punishment is permanent ban
        long duration = -1; // -1 for permanent
        boolean isPermanent = true;
        String originalReason = reason;
        
        try {
            // If template is specified, use it
            if (templateName != null) {
                PunishmentTemplate template = plugin.getPunishmentTemplateService().getTemplate(templateName);
                if (template == null) {
                    sender.sendMessage(MessageUtil.color("&cTemplate not found: #" + templateName));
                    return;
                }
                
                // Use template settings
                duration = template.getDuration();
                isPermanent = duration < 0;
                
                // If template has a reason, use it unless a specific reason was provided
                if (template.getReason() != null && !template.getReason().isEmpty() && reason.equals("No reason specified")) {
                    reason = template.getReason();
                }
                
                // Apply any metadata from template
                Map<String, String> metadata = new LinkedHashMap<>();
                if (template.getMetadata() != null) {
                    metadata.putAll(template.getMetadata());
                }
                
                // Create punishment
                createAndApplyBan(sender, targetUuid, targetName, reason, originalReason, punisherName, 
                        punisherUuid, duration, isPermanent, silent, metadata);
                return;
            }
            
            // If not using template, check for punishment escalation
            if (plugin.getConfig().getBoolean("punishment-escalation.enabled", true)) {
                // Get escalation service
                PunishmentEscalationService escalationService = plugin.getPunishmentEscalationService();
                
                // Get previous punishments for the same category
                List<Punishment> previousPunishments = plugin.getDatabaseManager().getPreviousPunishments(
                        targetUuid, PunishmentType.BAN, PunishmentType.TEMP_BAN);
                
                // Determine escalated punishment
                PunishmentEscalationService.EscalatedPunishment escalated = 
                        escalationService.getEscalatedPunishment(previousPunishments, "ban");
                
                // Apply escalated duration if available
                if (escalated != null) {
                    duration = escalated.getDuration();
                    isPermanent = duration < 0;
                    
                    // Create metadata with escalation info
                    Map<String, String> metadata = new LinkedHashMap<>();
                    metadata.put("escalation_level", String.valueOf(escalated.getLevel()));
                    metadata.put("escalation_category", "ban");
                    
                    if (escalated.getLevel() > 1) {
                        sender.sendMessage(MessageUtil.color("&6Applying escalated punishment (level " + 
                                escalated.getLevel() + "): " + (isPermanent ? "Permanent ban" : 
                                TimeUtil.formatDuration(duration) + " ban")));
                    }
                    
                    // Create punishment
                    createAndApplyBan(sender, targetUuid, targetName, reason, originalReason, punisherName, 
                            punisherUuid, duration, isPermanent, silent, metadata);
                    return;
                }
            }
            
            // If no template or escalation, apply default permanent ban
            createAndApplyBan(sender, targetUuid, targetName, reason, originalReason, punisherName, 
                    punisherUuid, duration, isPermanent, silent, null);
            
        } catch (Exception e) {
            sender.sendMessage(MessageUtil.color("&cAn error occurred while processing the ban."));
            plugin.getLogger().severe("Error in BanCommand: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create and apply a ban
     * 
     * @param sender The command sender
     * @param targetUuid The target player UUID
     * @param targetName The target player name
     * @param reason The ban reason
     * @param originalReason The original reason provided by the command
     * @param punisherName The punisher name
     * @param punisherUuid The punisher UUID
     * @param duration The ban duration
     * @param isPermanent Whether the ban is permanent
     * @param silent Whether the ban should be silent
     * @param metadata Additional metadata for the ban
     */
    private void createAndApplyBan(CommandSender sender, UUID targetUuid, String targetName, String reason, 
            String originalReason, String punisherName, UUID punisherUuid, long duration, boolean isPermanent, 
            boolean silent, Map<String, String> metadata) {
        
        // Create punishment
        Punishment punishment = new Punishment();
        punishment.setPlayerId(targetUuid);
        punishment.setPlayerName(targetName);
        punishment.setPunisherId(punisherUuid);
        punishment.setPunisherName(punisherName);
        punishment.setReason(reason);
        punishment.setType(isPermanent ? PunishmentType.BAN : PunishmentType.TEMP_BAN);
        punishment.setStartTime(System.currentTimeMillis());
        punishment.setEndTime(isPermanent ? -1 : System.currentTimeMillis() + duration);
        punishment.setActive(true);
        
        // Add metadata if available
        if (metadata != null && !metadata.isEmpty()) {
            punishment.setMetadata(metadata);
        }
        
        // Add to database
        long punishmentId = plugin.getDatabaseManager().addPunishment(punishment);
        punishment.setId(punishmentId);
        
        // Kick player if online
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.isOnline()) {
            // Build kick message
            String kickMessage = plugin.getConfig().getString(
                isPermanent ? "messages.ban.player_message" : "messages.tempban.player_message",
                "&c&l⚠ &" + (isPermanent ? "4" : "6") + "&lYOU HAVE BEEN " + 
                (isPermanent ? "" : "TEMPORARILY ") + "BANNED &c&l⚠\n\n" +
                "&r&7Reason: &c%reason%\n" +
                "&7Banned by: &c%staff%\n" +
                (isPermanent ? "" : "&7Duration: &c%duration%\n") +
                (isPermanent ? "" : "&7Expires: &c%expires%\n") +
                "&7Date: &c%date%\n\n" +
                "&7Appeal at: &b&nminecraft.example.com/appeal"
            );
            
            String expires = isPermanent ? "Never" : TimeUtil.formatTimestamp(punishment.getEndTime());
            String durationStr = isPermanent ? "Permanent" : TimeUtil.formatDuration(duration);
            
            kickMessage = kickMessage
                .replace("%reason%", reason)
                .replace("%staff%", punisherName)
                .replace("%expires%", expires)
                .replace("%duration%", durationStr)
                .replace("%date%", TimeUtil.formatTimestamp(punishment.getStartTime()));
            
            // Kick the player
            target.kickPlayer(MessageUtil.color(kickMessage));
        }
        
        // Send messages
        String targetOffline = target == null ? " &7(offline)" : "";
        
        // Notification message to the sender
        String banType = isPermanent ? "&4permanently banned" : "&6temporarily banned";
        String durationText = isPermanent ? "" : " &7for &f" + TimeUtil.formatDuration(duration);
        
        sender.sendMessage(MessageUtil.color(
            "&aYou've " + banType + " &f" + targetName + targetOffline + durationText + 
            "&7.\n&7Reason: &f" + reason
        ));
        
        // Broadcast message if not silent
        if (!silent) {
            String broadcastMessage = plugin.getConfig().getString(
                isPermanent ? "messages.ban.broadcast" : "messages.tempban.broadcast",
                "&c&l⚠ &f%player% &" + (isPermanent ? "4" : "6") + "has been " + 
                (isPermanent ? "" : "temporarily ") + "banned by &f%staff%" + 
                (isPermanent ? "" : " &7for &f%duration%") + 
                "&7.\n&7Reason: &f%reason%"
            );
            
            broadcastMessage = broadcastMessage
                .replace("%player%", targetName)
                .replace("%staff%", punisherName)
                .replace("%reason%", reason)
                .replace("%duration%", isPermanent ? "Permanent" : TimeUtil.formatDuration(duration));
            
            // Send to all players with permission
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ultimateban.see.ban")) {
                    player.sendMessage(MessageUtil.color(broadcastMessage));
                }
            }
            
            // Also send to console
            Bukkit.getConsoleSender().sendMessage(MessageUtil.color(broadcastMessage));
        } else {
            // Only send to staff with silent permission
            String silentMessage = "&8[Silent] " + MessageUtil.color(
                "&c&l⚠ &f" + targetName + " &" + (isPermanent ? "4" : "6") + "has been " + 
                (isPermanent ? "" : "temporarily ") + "banned by &f" + punisherName + 
                (isPermanent ? "" : " &7for &f" + TimeUtil.formatDuration(duration)) + 
                "&7.\n&7Reason: &f" + reason
            );
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ultimateban.see.silent")) {
                    player.sendMessage(silentMessage);
                }
            }
            
            // Also send to console
            Bukkit.getConsoleSender().sendMessage(silentMessage);
        }
    }

    /**
     * Check if a player can ban another player
     * 
     * @param sender The sender
     * @param target The target
     * @return true if the player can ban the target, false otherwise
     */
    private boolean canBan(Player sender, Player target) {
        // Can't ban yourself
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            return false;
        }
        
        // Check if target has higher permissions
        if (target.hasPermission("ultimateban.exempt")) {
            return sender.hasPermission("ultimateban.exempt.override");
        }
        
        // Staff can't ban staff unless they have override permission
        if (target.hasPermission("ultimateban.staff")) {
            return sender.hasPermission("ultimateban.staff.override");
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(player.getName());
                }
            }
        } else if (isTemporary && args.length == 2) {
            // Suggest durations for temporary bans
            List<String> durations = plugin.getConfigManager().getStringList("gui.durations.ban");
            for (String duration : durations) {
                if (duration.toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(duration);
                }
            }
        } else if ((isTemporary && args.length == 3) || (!isTemporary && args.length == 2)) {
            // Suggest common ban reasons
            List<String> reasons = plugin.getConfigManager().getStringList("ban_reasons");
            String currentArg = args[isTemporary ? 2 : 1].toLowerCase();
            
            for (String reason : reasons) {
                if (reason.toLowerCase().startsWith(currentArg)) {
                    suggestions.add(reason);
                }
            }
        }
        
        return suggestions;
    }
} 