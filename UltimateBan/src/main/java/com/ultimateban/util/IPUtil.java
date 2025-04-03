package com.ultimateban.util;

import com.ultimateban.UltimateBan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Utility class for IP-related operations
 */
public class IPUtil {
    private static final Map<String, Boolean> vpnCache = new HashMap<>();
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    
    private final UltimateBan plugin;
    
    public IPUtil(UltimateBan plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if an IP address is a VPN/Proxy
     * Uses ipqualityscore.com API to detect VPNs, proxies, and TOR nodes
     * 
     * @param ip The IP address to check
     * @return CompletableFuture that resolves to true if IP is a VPN/Proxy
     */
    public CompletableFuture<Boolean> isVpnOrProxy(String ip) {
        // Return cached result if available
        if (vpnCache.containsKey(ip)) {
            return CompletableFuture.completedFuture(vpnCache.get(ip));
        }
        
        // Check if VPN checking is enabled
        if (!plugin.getConfigManager().getConfig().getBoolean("vpn-detection.enabled", true)) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Get API key from config
        String apiKey = plugin.getConfigManager().getConfig().getString("vpn-detection.api-key", "");
        if (apiKey.isEmpty()) {
            plugin.getLogger().warning("VPN detection is enabled but no API key is configured. Disabling VPN checks.");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use ipqualityscore.com API to check if IP is a VPN/Proxy
                String urlString = "https://ipqualityscore.com/api/json/ip/" + apiKey + "/" + ip + 
                                    "?strictness=1&allow_public_access_points=true";
                URL url = new URL(urlString);
                
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
                    boolean isProxy = jsonResponse.contains("\"proxy\":true") || 
                                      jsonResponse.contains("\"vpn\":true") || 
                                      jsonResponse.contains("\"tor\":true");
                    
                    // Cache the result
                    vpnCache.put(ip, isProxy);
                    return isProxy;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check if IP is a VPN/Proxy: " + e.getMessage(), e);
            }
            
            return false;
        });
    }
    
    /**
     * Check if a string is a valid IP address
     * 
     * @param ip The string to check
     * @return true if the string is a valid IP address
     */
    public static boolean isValidIp(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }
    
    /**
     * Extract CIDR subnet from an IP address
     * For example, 192.168.1.100 -> 192.168.1.0/24
     * 
     * @param ip The IP address
     * @return The CIDR subnet
     */
    public static String getSubnet(String ip) {
        if (!isValidIp(ip)) {
            return null;
        }
        
        String[] parts = ip.split("\\.");
        return parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
    }
    
    /**
     * Clear the VPN/Proxy cache
     */
    public static void clearCache() {
        vpnCache.clear();
    }
} 