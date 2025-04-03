package com.ultimateban.services;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.models.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for detecting alternative accounts
 */
public class AltDetectionService {
    private final UltimateBan plugin;
    private final boolean enabled;
    private final boolean blockAlts;
    private final boolean notifyStaff;
    private final String staffPermission;
    
    // Detection methods
    private final boolean useIpMatch;
    private final boolean useUuidPattern;
    private final boolean useNameSimilarity;
    private final boolean useJoinPattern;
    
    public AltDetectionService(UltimateBan plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("alt-detection.enabled", false);
        this.blockAlts = plugin.getConfig().getBoolean("alt-detection.block", false);
        this.notifyStaff = plugin.getConfig().getBoolean("alt-detection.notify-staff", false);
        this.staffPermission = plugin.getConfig().getString("alt-detection.staff-permission", "ultimateban.alert.alt");
        
        // Load detection methods
        this.useIpMatch = plugin.getConfig().getBoolean("alt-detection.detection-methods.ip-match", true);
        this.useUuidPattern = plugin.getConfig().getBoolean("alt-detection.detection-methods.uuid-pattern", true);
        this.useNameSimilarity = plugin.getConfig().getBoolean("alt-detection.detection-methods.name-similarity", true);
        this.useJoinPattern = plugin.getConfig().getBoolean("alt-detection.detection-methods.join-pattern", true);
    }
    
    /**
     * Check if a player has alt accounts
     * @param player The player to check
     * @param ip The player's IP address
     * @return A CompletableFuture with a list of potential alt accounts
     */
    public CompletableFuture<List<PotentialAlt>> checkForAlts(Player player, String ip) {
        if (!enabled) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        // If player has bypass permission, don't check for alts
        if (player.hasPermission("ultimateban.bypass.alt")) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            List<PotentialAlt> potentialAlts = new ArrayList<>();
            
            // Method 1: IP Address matching
            if (useIpMatch) {
                try {
                    // Get players with the same IP
                    List<UUID> playersWithSameIp = plugin.getDatabaseManager().getPlayersByIp(ip);
                    for (UUID uuid : playersWithSameIp) {
                        if (!uuid.equals(player.getUniqueId())) {
                            String altName = plugin.getDatabaseManager().getPlayerName(uuid);
                            if (altName != null) {
                                PotentialAlt alt = new PotentialAlt(uuid, altName, "IP Match", 90);
                                potentialAlts.add(alt);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error checking for IP-based alts: " + e.getMessage());
                }
            }
            
            // Method 2: UUID pattern matching
            if (useUuidPattern) {
                // This would be a more advanced check that looks for patterns in UUID generation
                // For example, if UUIDs are sequential or have similar patterns
                // This is a simplified placeholder implementation
                try {
                    // Get recent players and check if their UUIDs are close to the current player's
                    List<UUID> recentPlayers = plugin.getDatabaseManager().getRecentPlayers(30);
                    for (UUID uuid : recentPlayers) {
                        if (!uuid.equals(player.getUniqueId())) {
                            // Check if UUIDs have similar characteristics
                            // This is a very simplistic check, a real implementation would be more sophisticated
                            if (uuidSimilarity(player.getUniqueId(), uuid) > 0.7) {
                                String altName = plugin.getDatabaseManager().getPlayerName(uuid);
                                if (altName != null) {
                                    PotentialAlt alt = new PotentialAlt(uuid, altName, "UUID Pattern", 60);
                                    potentialAlts.add(alt);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error checking for UUID-based alts: " + e.getMessage());
                }
            }
            
            // Method 3: Name similarity
            if (useNameSimilarity) {
                try {
                    // Get players with similar names
                    List<String> playerNames = plugin.getDatabaseManager().getAllPlayerNames();
                    for (String otherName : playerNames) {
                        if (!otherName.equalsIgnoreCase(player.getName())) {
                            double similarity = calculateNameSimilarity(player.getName(), otherName);
                            if (similarity > 0.7) {
                                UUID uuid = plugin.getDatabaseManager().getPlayerUUID(otherName);
                                if (uuid != null) {
                                    PotentialAlt alt = new PotentialAlt(uuid, otherName, "Name Similarity", 
                                            (int) (similarity * 100));
                                    potentialAlts.add(alt);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error checking for name-based alts: " + e.getMessage());
                }
            }
            
            // Method 4: Join pattern
            if (useJoinPattern) {
                try {
                    // Get players who joined within a short time after another player left
                    // This is a simplistic implementation, a real one would check actual login/logout times
                    List<Map<String, Object>> suspiciousPatterns = plugin.getDatabaseManager().getJoinPatterns();
                    for (Map<String, Object> pattern : suspiciousPatterns) {
                        UUID uuid1 = (UUID) pattern.get("player1");
                        UUID uuid2 = (UUID) pattern.get("player2");
                        
                        if (uuid1.equals(player.getUniqueId()) || uuid2.equals(player.getUniqueId())) {
                            UUID altUuid = uuid1.equals(player.getUniqueId()) ? uuid2 : uuid1;
                            String altName = plugin.getDatabaseManager().getPlayerName(altUuid);
                            if (altName != null) {
                                PotentialAlt alt = new PotentialAlt(altUuid, altName, "Join Pattern", 70);
                                potentialAlts.add(alt);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error checking for join pattern-based alts: " + e.getMessage());
                }
            }
            
            // Remove duplicates and sort by confidence
            return potentialAlts.stream()
                    .distinct()
                    .sorted(Comparator.comparingInt(PotentialAlt::getConfidence).reversed())
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * Check if a player should be blocked based on alt detection
     * @param player The player to check
     * @param ip The player's IP address
     * @return A CompletableFuture that will resolve to true if the player should be blocked
     */
    public CompletableFuture<Boolean> shouldBlockPlayer(Player player, String ip) {
        if (!enabled || !blockAlts) {
            return CompletableFuture.completedFuture(false);
        }
        
        if (player.hasPermission("ultimateban.bypass.alt")) {
            return CompletableFuture.completedFuture(false);
        }
        
        return checkForAlts(player, ip).thenApply(alts -> {
            if (alts.isEmpty()) {
                return false;
            }
            
            // Check if any of the alts have active bans
            for (PotentialAlt alt : alts) {
                try {
                    List<Punishment> punishments = plugin.getDatabaseManager().getPlayerPunishments(alt.getUuid());
                    for (Punishment punishment : punishments) {
                        if (punishment.isActive() && 
                                (punishment.getType() == PunishmentType.BAN || 
                                 punishment.getType() == PunishmentType.TEMP_BAN)) {
                            // Only block if the confidence is high enough (IP match or 90%+ confidence)
                            if (alt.getDetectionMethod().equals("IP Match") || alt.getConfidence() >= 90) {
                                plugin.getLogger().info("Blocking alt account " + player.getName() + 
                                        " associated with banned player " + alt.getName());
                                
                                // Notify staff
                                notifyStaffAboutAlt(player, alt, true);
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error checking punishments for alt " + alt.getName() + ": " + e.getMessage());
                }
            }
            
            // Notify staff about potential alts even if not blocked
            if (!alts.isEmpty()) {
                notifyStaffAboutAlt(player, alts.get(0), false);
            }
            
            return false;
        });
    }
    
    /**
     * Get the kick message for alt accounts
     * @return The configured kick message
     */
    public String getAltBlockMessage() {
        return plugin.getConfig().getString("punishments.alt-blocked-message", 
                "&c&l⚠ &4&lALT ACCOUNT DETECTED &c&l⚠\n\n&r&7Your connection matches a banned player.\n&7If you believe this is a mistake, please contact staff.");
    }
    
    /**
     * Notify staff about a potential alt account
     * @param player The player who might be an alt
     * @param alt The potential alt account information
     * @param blocked Whether the player was blocked
     */
    private void notifyStaffAboutAlt(Player player, PotentialAlt alt, boolean blocked) {
        if (!notifyStaff) {
            return;
        }
        
        String message = ChatColor.RED + "⚠ " + 
                ChatColor.YELLOW + player.getName() + 
                ChatColor.GRAY + " might be an alt of " + 
                ChatColor.YELLOW + alt.getName() + 
                ChatColor.GRAY + " (" + alt.getDetectionMethod() + ", " + alt.getConfidence() + "% confidence)";
        
        if (blocked) {
            message += ChatColor.RED + " - BLOCKED";
        }
        
        // Send message to staff
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(staffPermission)) {
                staff.sendMessage(message);
            }
        }
        
        // Log to console as well
        plugin.getLogger().info(ChatColor.stripColor(message));
    }
    
    /**
     * Calculate the similarity between two UUIDs
     * This is a simplified implementation
     * @param uuid1 First UUID
     * @param uuid2 Second UUID
     * @return Similarity score between 0 and 1
     */
    private double uuidSimilarity(UUID uuid1, UUID uuid2) {
        // This is a placeholder implementation
        // A real implementation would look for patterns in UUID generation
        String str1 = uuid1.toString();
        String str2 = uuid2.toString();
        
        // Count how many segments are the same
        String[] parts1 = str1.split("-");
        String[] parts2 = str2.split("-");
        
        int sameSegments = 0;
        for (int i = 0; i < parts1.length; i++) {
            if (parts1[i].equals(parts2[i])) {
                sameSegments++;
            }
        }
        
        return sameSegments / (double) parts1.length;
    }
    
    /**
     * Calculate the similarity between two player names
     * @param name1 First name
     * @param name2 Second name
     * @return Similarity score between 0 and 1
     */
    private double calculateNameSimilarity(String name1, String name2) {
        // Use normalized Levenshtein distance
        int distance = levenshteinDistance(name1.toLowerCase(), name2.toLowerCase());
        int maxLength = Math.max(name1.length(), name2.length());
        
        // Convert to similarity (0 distance = 1.0 similarity)
        return 1.0 - (distance / (double) maxLength);
    }
    
    /**
     * Calculate the Levenshtein distance between two strings
     * @param s1 First string
     * @param s2 Second string
     * @return The edit distance between the strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Class to represent a potential alt account
     */
    public static class PotentialAlt {
        private final UUID uuid;
        private final String name;
        private final String detectionMethod;
        private final int confidence;
        
        public PotentialAlt(UUID uuid, String name, String detectionMethod, int confidence) {
            this.uuid = uuid;
            this.name = name;
            this.detectionMethod = detectionMethod;
            this.confidence = confidence;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDetectionMethod() {
            return detectionMethod;
        }
        
        public int getConfidence() {
            return confidence;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PotentialAlt that = (PotentialAlt) o;
            return uuid.equals(that.uuid);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }
    }
} 