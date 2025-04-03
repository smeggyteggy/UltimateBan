package com.ultimateban.services;

import com.ultimateban.UltimateBan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for detecting VPN/Proxy connections
 */
public class VPNDetectionService {
    private final UltimateBan plugin;
    private final String apiKey;
    private final boolean enabled;
    private final boolean blockVpns;
    private final long cacheTime;
    
    // Cache results to reduce API calls
    private final Map<String, CachedResult> resultCache = new ConcurrentHashMap<>();
    
    public VPNDetectionService(UltimateBan plugin) {
        this.plugin = plugin;
        this.apiKey = plugin.getConfig().getString("vpn-detection.api-key", "");
        this.enabled = plugin.getConfig().getBoolean("vpn-detection.enabled", false);
        this.blockVpns = plugin.getConfig().getBoolean("vpn-detection.block", false);
        this.cacheTime = plugin.getConfig().getLong("vpn-detection.cache-time", 1440);
        
        // Clean up old cache entries every 30 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupCache, 36000L, 36000L);
    }
    
    /**
     * Check if a player is using a VPN
     * @param player The player to check
     * @param ip The IP address to check
     * @return A CompletableFuture that will resolve to true if the IP is a VPN, false otherwise
     */
    public CompletableFuture<Boolean> checkPlayerVPN(Player player, String ip) {
        if (!enabled || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("ultimateban.bypass.vpn")) {
            plugin.getLogger().info("Player " + player.getName() + " has VPN bypass permission");
            return CompletableFuture.completedFuture(false);
        }
        
        // Check cache first
        if (resultCache.containsKey(ip)) {
            CachedResult cachedResult = resultCache.get(ip);
            if (!cachedResult.isExpired()) {
                plugin.getLogger().info("Using cached VPN result for " + ip);
                return CompletableFuture.completedFuture(cachedResult.isVpn());
            } else {
                resultCache.remove(ip);
            }
        }
        
        // Make API request asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Checking if " + ip + " is a VPN...");
                String apiUrl = "https://ipqualityscore.com/api/json/ip/" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString()) 
                        + "/" + URLEncoder.encode(ip, StandardCharsets.UTF_8.toString());
                
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    
                    // Simple JSON parsing without dependencies
                    String jsonResponse = response.toString();
                    boolean isVpn = jsonResponse.contains("\"proxy\":true") || 
                                    jsonResponse.contains("\"vpn\":true") || 
                                    jsonResponse.contains("\"tor\":true");
                    
                    // Cache the result
                    resultCache.put(ip, new CachedResult(isVpn, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cacheTime)));
                    
                    plugin.getLogger().info("IP " + ip + " VPN check result: " + isVpn);
                    return isVpn;
                } else {
                    plugin.getLogger().warning("Failed to check VPN: HTTP error code " + responseCode);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error checking VPN: " + e.getMessage());
                e.printStackTrace();
            }
            
            return false;
        });
    }
    
    /**
     * Check if a player should be blocked based on VPN detection
     * @param player The player to check
     * @param ip The IP address to check
     * @return A CompletableFuture that will resolve to true if the player should be blocked
     */
    public CompletableFuture<Boolean> shouldBlockPlayer(Player player, String ip) {
        if (!enabled || !blockVpns) {
            return CompletableFuture.completedFuture(false);
        }
        
        return checkPlayerVPN(player, ip)
            .thenApply(isVpn -> isVpn && blockVpns);
    }
    
    /**
     * Get the kick message for VPN connections
     * @return The configured kick message
     */
    public String getVpnBlockMessage() {
        return plugin.getConfig().getString("punishments.vpn-blocked-message", 
                "&c&l⚠ &4&lCONNECTION BLOCKED &c&l⚠\n\n&r&7VPN or proxy connections are not allowed on this server.");
    }
    
    /**
     * Remove expired cache entries
     */
    private void cleanupCache() {
        long now = System.currentTimeMillis();
        resultCache.entrySet().removeIf(entry -> entry.getValue().getExpiry() < now);
    }
    
    /**
     * Class to store cached VPN check results
     */
    private static class CachedResult {
        private final boolean vpn;
        private final long expiry;
        
        public CachedResult(boolean vpn, long expiry) {
            this.vpn = vpn;
            this.expiry = expiry;
        }
        
        public boolean isVpn() {
            return vpn;
        }
        
        public long getExpiry() {
            return expiry;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }
} 