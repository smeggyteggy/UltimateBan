package com.ultimateban.managers;

import com.ultimateban.UltimateBan;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages plugin configuration files
 */
public class ConfigManager {

    private final UltimateBan plugin;
    private FileConfiguration config;
    private File configFile;
    private String prefix;

    /**
     * Constructor for ConfigManager
     *
     * @param plugin The UltimateBan plugin instance
     */
    public ConfigManager(UltimateBan plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load the main configuration file
     */
    public void loadConfig() {
        // Create config file if it doesn't exist
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        matchDefaultConfig();

        // Load prefix
        this.prefix = getString("settings.prefix");
    }

    /**
     * Match the configuration with the default one, adding missing values
     */
    private void matchDefaultConfig() {
        try (InputStream defaultConfigStream = plugin.getResource("config.yml")) {
            if (defaultConfigStream == null) {
                return;
            }

            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            
            boolean needsSave = false;
            
            // Add missing keys
            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                    needsSave = true;
                }
            }
            
            if (needsSave) {
                saveConfig();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error matching default config values: " + e.getMessage(), e);
        }
    }

    /**
     * Save the configuration file
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving config file: " + e.getMessage(), e);
        }
    }

    /**
     * Reload the configuration file
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Get the plugin prefix
     *
     * @return The plugin prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Get a string from the configuration
     *
     * @param path The path to the value
     * @return The string value, or an empty string if not found
     */
    public String getString(String path) {
        return config.getString(path, "");
    }

    /**
     * Get a string from the configuration with a default value
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if not found
     * @return The string value, or the default value if not found
     */
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    /**
     * Get an integer from the configuration
     *
     * @param path The path to the value
     * @return The integer value, or 0 if not found
     */
    public int getInt(String path) {
        return config.getInt(path, 0);
    }

    /**
     * Get an integer from the configuration with a default value
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if not found
     * @return The integer value, or the default value if not found
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    /**
     * Get a boolean from the configuration
     *
     * @param path The path to the value
     * @return The boolean value, or false if not found
     */
    public boolean getBoolean(String path) {
        return config.getBoolean(path, false);
    }

    /**
     * Get a boolean from the configuration with a default value
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if not found
     * @return The boolean value, or the default value if not found
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    /**
     * Get a double from the configuration
     *
     * @param path The path to the value
     * @return The double value, or 0 if not found
     */
    public double getDouble(String path) {
        return config.getDouble(path, 0.0);
    }

    /**
     * Get a double from the configuration with a default value
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if not found
     * @return The double value, or the default value if not found
     */
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    /**
     * Get a list of strings from the configuration
     *
     * @param path The path to the value
     * @return The list of strings, or an empty list if not found
     */
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    /**
     * Get a message from the configuration
     *
     * @param path The path to the message
     * @return The message, or a default error message if not found
     */
    public String getMessage(String path) {
        String message = getString("messages." + path);
        return message.isEmpty() ? "&cMissing message: " + path : message;
    }

    /**
     * Check if Discord webhook integration is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isDiscordEnabled() {
        return getBoolean("discord.enabled", false);
    }

    /**
     * Check if hack detection is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isHackDetectionEnabled() {
        return getBoolean("hack_detection.enabled", false);
    }

    // Database methods
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "ultimateban");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "password");
    }

    public boolean getMySQLUseSSL() {
        return config.getBoolean("database.mysql.ssl", false);
    }

    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "database.db");
    }

    // Discord webhook methods
    public String getDiscordWebhookURL() {
        return config.getString("discord.webhook_url", "");
    }

    public boolean isSendPunishmentsEnabled() {
        return config.getBoolean("discord.send_punishments", true);
    }

    public boolean isSendAppealsEnabled() {
        return config.getBoolean("discord.send_appeals", true);
    }

    public String getDiscordEmbedColor(String punishmentType) {
        return config.getString("discord.embed_colors." + punishmentType, "#FF0000");
    }

    // Message methods
    public String getMessage(String path, String defaultValue) {
        return config.getString("messages." + path, defaultValue);
    }

    // Ban animation methods
    public boolean isBanAnimationEnabled() {
        return config.getBoolean("ban_animation.enabled", true);
    }

    public int getBanAnimationDuration() {
        return config.getInt("ban_animation.duration", 40);
    }

    public String getBanAnimationSound() {
        return config.getString("ban_animation.sound", "ENTITY_WITHER_DEATH");
    }

    public boolean areBanAnimationParticlesEnabled() {
        return config.getBoolean("ban_animation.particles", true);
    }

    // Staff cooldown methods
    public boolean areStaffCooldownsEnabled() {
        return config.getBoolean("staff_cooldowns.enabled", true);
    }

    public int getStaffCooldown(String punishmentType) {
        return config.getInt("staff_cooldowns." + punishmentType, 0);
    }

    // Hack detection methods
    public String getHackDetectionAction() {
        return config.getString("hack_detection.action", "tempban");
    }

    public String getHackDetectionDuration() {
        return config.getString("hack_detection.duration", "1d");
    }

    public String getHackDetectionReason() {
        return config.getString("hack_detection.reason", "Suspicious activity detected");
    }

    public int getHackDetectionCPSThreshold() {
        return config.getInt("hack_detection.thresholds.cps", 20);
    }

    public double getHackDetectionReachThreshold() {
        return config.getDouble("hack_detection.thresholds.reach", 4.5);
    }

    public double getHackDetectionSpeedThreshold() {
        return config.getDouble("hack_detection.thresholds.speed", 1.5);
    }

    // GUI methods
    public String getGUITitle(String guiType) {
        return config.getString("gui." + guiType + "_title", "&c&lPunishment GUI");
    }

    public String getGUIIconName(String iconType) {
        return config.getString("gui.icons." + iconType + ".name", "&cIcon Name");
    }

    public List<String> getGUIIconLore(String iconType) {
        return config.getStringList("gui.icons." + iconType + ".lore");
    }

    public String getGUIIconMaterial(String iconType) {
        return config.getString("gui.icons." + iconType + ".material", "STONE");
    }

    // Preset reasons
    public List<String> getPresetReasons(String punishmentType) {
        return config.getStringList("preset_reasons." + punishmentType);
    }

    /**
     * Get the FileConfiguration instance
     *
     * @return The FileConfiguration instance
     */
    public FileConfiguration getConfig() {
        return config;
    }
} 