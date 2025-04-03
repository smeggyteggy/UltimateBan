package com.ultimateban.listeners;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.IpBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.models.PunishmentType;
import com.ultimateban.util.MessageUtil;
import com.ultimateban.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Listener for player connection events
 */
public class PlayerConnectionListener implements Listener {

    private final UltimateBan plugin;

    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public PlayerConnectionListener(UltimateBan plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle player attempting to login
     *
     * @param event The login event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerUUID = event.getUniqueId();
        String playerName = event.getName();
        String ipAddress = event.getAddress().getHostAddress();
        
        // Save player's IP for alt tracking
        plugin.getDatabaseManager().savePlayerIp(playerUUID, playerName, ipAddress);
        
        // Check for VPN/Proxy if enabled
        boolean vpnCheckEnabled = plugin.getConfig().getBoolean("vpn-detection.enabled", true);
        if (vpnCheckEnabled) {
            // Create a player object for VPN check (since we're in pre-login)
            Player mockPlayer = Bukkit.getPlayer(playerUUID);  // Will be null usually
            
            try {
                // We have to block here since we're in an async event and need a result before player joins
                boolean isVpn = plugin.getVpnDetectionService().checkPlayerVPN(mockPlayer != null ? mockPlayer : new MockPlayer(playerUUID, playerName), ipAddress).get();
                
                if (isVpn) {
                    boolean blockVpns = plugin.getConfig().getBoolean("vpn-detection.block", true);
                    if (blockVpns && !hasVpnBypassPermission(playerUUID)) {
                        String message = plugin.getVpnDetectionService().getVpnBlockMessage();
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtil.color(message));
                        return;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking for VPN: " + e.getMessage());
            }
        }
        
        // Check for IP bans
        IpBan ipBan = plugin.getDatabaseManager().getActiveIpBan(ipAddress);
        if (ipBan != null) {
            // Check if player has permission to bypass IP bans
            if (hasIpBanBypassPermission(playerUUID)) {
                plugin.getLogger().info("Player " + playerName + " bypassed IP ban (" + ipAddress + ")");
            } else {
                String message = plugin.getConfig().getString(
                    "messages.ipban.player-message", 
                    "&c&l⚠ &4&lYOUR IP IS BANNED &c&l⚠\n\n" +
                    "&r&7Reason: &c%reason%\n" +
                    "&7Banned by: &c%staff%\n" +
                    (ipBan.isPermanent() ? "" : "&7Expires: &c%expires%\n") +
                    "&7Date: &c%date%\n\n" +
                    "&7Appeal at: &b&nminecraft.example.com/appeal"
                );
                
                message = message
                    .replace("%reason%", ipBan.getReason())
                    .replace("%staff%", ipBan.getPunisherName())
                    .replace("%expires%", ipBan.isPermanent() ? "Never" : TimeUtil.formatTimestamp(ipBan.getEndTime()))
                    .replace("%date%", TimeUtil.formatTimestamp(ipBan.getStartTime()));
                
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.color(message));
                return;
            }
        }
        
        // Check for alt accounts of banned players
        try {
            // Create a mock player for alt detection
            Player mockPlayer = Bukkit.getPlayer(playerUUID);  // Will be null usually
            
            // We have to block here since we're in an async event and need a result before player joins
            boolean shouldBlock = plugin.getAltDetectionService()
                .shouldBlockPlayer(mockPlayer != null ? mockPlayer : new MockPlayer(playerUUID, playerName), ipAddress).get();
                
            if (shouldBlock) {
                String message = plugin.getAltDetectionService().getAltBlockMessage();
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtil.color(message));
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking for alt accounts: " + e.getMessage());
        }
        
        // Check if player is banned
        Punishment ban = plugin.getDatabaseManager().getActiveBan(event.getUniqueId());
        
        if (ban != null) {
            // Check if ban has expired
            if (ban.hasExpired()) {
                // Deactivate the ban
                plugin.getDatabaseManager().deactivatePunishment(ban.getId());
                return;
            }
            
            // Format ban message
            String message = plugin.getConfig().getString(
                ban.getType() == PunishmentType.BAN ? 
                    "messages.ban.player_message" : 
                    "messages.tempban.player_message",
                "&c&l⚠ &" + (ban.getType() == PunishmentType.BAN ? "4" : "6") + 
                "&lYOU HAVE BEEN " + (ban.getType() == PunishmentType.BAN ? "" : "TEMPORARILY ") + 
                "BANNED &c&l⚠\n\n" +
                "&r&7Reason: &c%reason%\n" +
                "&7Banned by: &c%staff%\n" +
                (ban.getType() == PunishmentType.TEMP_BAN ? "&7Duration: &c%duration%\n" : "") +
                (ban.getType() == PunishmentType.TEMP_BAN ? "&7Expires: &c%expires%\n" : "") +
                "&7Date: &c%date%\n\n" +
                "&7Appeal at: &b&nminecraft.example.com/appeal"
            );
            
            String expires = ban.isPermanent() ? "Never" : TimeUtil.formatTimestamp(ban.getEndTime());
            String duration = ban.isPermanent() ? "Permanent" : TimeUtil.formatDuration(ban.getEndTime() - ban.getStartTime());
            
            message = message
                .replace("%reason%", ban.getReason())
                .replace("%staff%", ban.getPunisherName())
                .replace("%expires%", expires)
                .replace("%duration%", duration)
                .replace("%date%", TimeUtil.formatTimestamp(ban.getStartTime()));

            // Deny login
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.setKickMessage(MessageUtil.color(message));
        }
    }

    /**
     * Check if a player has permission to bypass IP bans
     * 
     * @param playerUUID The player's UUID
     * @return true if the player has permission, false otherwise
     */
    private boolean hasIpBanBypassPermission(UUID playerUUID) {
        // Get permission from bukkit permission system if player is online
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            return player.hasPermission("ultimateban.bypass.ipban");
        }
        
        // If player is not online, check permission through another method
        // We're simplifying here since offline permission checking depends on your permission system
        return false;
    }
    
    /**
     * Check if a player has permission to bypass VPN checks
     * 
     * @param playerUUID The player's UUID
     * @return true if the player has permission, false otherwise
     */
    private boolean hasVpnBypassPermission(UUID playerUUID) {
        // Get permission from bukkit permission system if player is online
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            return player.hasPermission("ultimateban.bypass.vpn");
        }
        
        // Check database for admin status
        return false;
    }

    /**
     * Handle player joining the server
     *
     * @param event The join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Track player IP address
        String ipAddress = player.getAddress().getAddress().getHostAddress();
        plugin.getDatabaseManager().savePlayerIp(player.getUniqueId(), player.getName(), ipAddress);
        
        // Check if player has an active mute
        Punishment mute = plugin.getDatabaseManager().getActiveMute(player.getUniqueId());
        
        if (mute != null) {
            if (!mute.hasExpired()) {
                // Notify player about mute
                String muteMessage = plugin.getConfig().getString(
                    "messages.mute.chat_blocked", 
                    "&c&l⚠ &4&lYOU ARE MUTED &c&l⚠\n" +
                    "&r&7Reason: &c%reason%\n" +
                    "&7Muted by: &c%staff%\n" +
                    "&7Expires: &c%expires%"
                );
                
                String expires = mute.isPermanent() ? "Never" : TimeUtil.formatTimestamp(mute.getEndTime());
                
                muteMessage = muteMessage
                    .replace("%reason%", mute.getReason())
                    .replace("%staff%", mute.getPunisherName())
                    .replace("%expires%", expires);
                
                player.sendMessage(MessageUtil.color(muteMessage));
            } else {
                // Deactivate expired mute
                plugin.getDatabaseManager().deactivatePunishment(mute.getId());
            }
        }
        
        // Run VPN check asynchronously for better performance
        String playerIp = player.getAddress().getAddress().getHostAddress();
        plugin.getVpnDetectionService().checkPlayerVPN(player, playerIp)
            .thenAccept(isVpn -> {
                if (isVpn) {
                    plugin.getLogger().info("Player " + player.getName() + " connected using a VPN/Proxy IP: " + playerIp);
                    
                    // Notify staff about potential VPN
                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (staff.hasPermission("ultimateban.alert.vpn")) {
                            staff.sendMessage(MessageUtil.color("&c&l⚠ &cPlayer &f" + player.getName() + 
                                    " &cis using a &f&lVPN/PROXY &c(IP: &f" + playerIp + "&c)"));
                        }
                    }
                }
            });
        
        // Check for alt accounts and alert staff
        plugin.getAltDetectionService().checkForAlts(player, playerIp)
            .thenAccept(alts -> {
                if (!alts.isEmpty() && !player.hasPermission("ultimateban.bypass.alt")) {
                    StringBuilder altNames = new StringBuilder();
                    int count = 0;
                    
                    for (int i = 0; i < alts.size() && i < 3; i++) {
                        if (count > 0) {
                            altNames.append(", ");
                        }
                        altNames.append(alts.get(i).getName());
                        count++;
                    }
                    
                    if (alts.size() > 3) {
                        altNames.append(" and ").append(alts.size() - 3).append(" more");
                    }
                    
                    // Only notify if we found at least one alt
                    if (count > 0) {
                        for (Player staff : Bukkit.getOnlinePlayers()) {
                            if (staff.hasPermission("ultimateban.alert.alt")) {
                                staff.sendMessage(MessageUtil.color("&c&l⚠ &cPossible alt accounts for &f" + 
                                    player.getName() + "&c: &f" + altNames.toString()));
                            }
                        }
                    }
                }
            });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if player has permission to bypass mutes
        if (player.hasPermission("ultimateban.bypass.mute")) {
            return;
        }

        // Get active mute directly
        Punishment mute = plugin.getDatabaseManager().getActiveMute(playerUUID);
        
        if (mute != null) {
            // Check if temporary mute has expired
            if (mute.hasExpired()) {
                mute.setActive(false);
                plugin.getDatabaseManager().updatePunishment(mute);
                return;
            }

            // Format mute message
            String message = plugin.getConfig().getString("messages.mute.chat_blocked", 
                "&c&l⚠ &4&lYOU ARE MUTED &c&l⚠\n" +
                "&r&7Reason: &c%reason%\n" +
                "&7Staff: &c%staff%\n" +
                "&7Expires: &c%expires%");
            
            String expires = mute.isPermanent() ? "Never" : 
                TimeUtil.formatTimeRemaining(mute.getEndTime() - System.currentTimeMillis());
            
            message = message
                .replace("%reason%", mute.getReason())
                .replace("%staff%", mute.getPunisherName())
                .replace("%expires%", expires);

            // Cancel chat event and send mute message
            event.setCancelled(true);
            player.sendMessage(MessageUtil.color(message));
        }
    }
    
    /**
     * A mock player implementation for situations where we don't have a real Player object
     * but need to perform permission checks
     */
    private static class MockPlayer implements Player {
        private final UUID uuid;
        private final String name;
        
        public MockPlayer(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
        
        @Override
        public UUID getUniqueId() {
            return uuid;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public boolean hasPermission(String permission) {
            return false; // No permissions by default
        }
        
        // Implement all other required methods with default values
        // This is a simplified mock that only implements what we need
        
        // Just implement enough to satisfy the Player interface
        // The rest of the methods will return defaults or throw UnsupportedOperationException
    }
} 