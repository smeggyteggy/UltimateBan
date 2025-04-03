package com.ultimateban;

import com.ultimateban.commands.BanCommand;
import com.ultimateban.commands.UnbanCommand;
import com.ultimateban.commands.MuteCommand;
import com.ultimateban.commands.UnmuteCommand;
import com.ultimateban.commands.WarnCommand;
import com.ultimateban.commands.KickCommand;
import com.ultimateban.commands.AppealCommand;
import com.ultimateban.commands.ReviewAppealCommand;
import com.ultimateban.commands.HistoryCommand;
import com.ultimateban.commands.PunishCommand;
import com.ultimateban.commands.UltimateBanCommand;
import com.ultimateban.commands.IpBanCommand;
import com.ultimateban.commands.TempBanCommand;
import com.ultimateban.commands.IpUnbanCommand;
import com.ultimateban.commands.TempIpBanCommand;
import com.ultimateban.commands.TempMuteCommand;
import com.ultimateban.commands.AltsCommand;
import com.ultimateban.commands.CheckCommand;
import com.ultimateban.database.DatabaseManager;
import com.ultimateban.listeners.PlayerConnectionListener;
import com.ultimateban.managers.ConfigManager;
import com.ultimateban.util.MessageUtil;
import com.ultimateban.util.IPUtil;
import com.ultimateban.services.AltDetectionService;
import com.ultimateban.services.PunishmentEscalationService;
import com.ultimateban.services.PunishmentTemplateService;
import com.ultimateban.services.VPNDetectionService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for UltimateBan
 */
public class UltimateBan extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private IPUtil ipUtil;
    private AltDetectionService altDetectionService;
    private VPNDetectionService vpnDetectionService;
    private PunishmentTemplateService templateService;
    private PunishmentEscalationService escalationService;

    @Override
    public void onEnable() {
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Initialize config
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize database
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database connection! Disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize IP utility
        ipUtil = new IPUtil(this);
        
        // Initialize services
        vpnDetectionService = new VPNDetectionService(this);
        altDetectionService = new AltDetectionService(this);
        templateService = new PunishmentTemplateService(this);
        escalationService = new PunishmentEscalationService(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        // Register commands
        registerCommands();

        // Log successful enable
        getLogger().info(MessageUtil.color("&a================================"));
        getLogger().info(MessageUtil.color("&6UltimateBan &ahas been enabled!"));
        getLogger().info(MessageUtil.color("&aVersion: &f" + getDescription().getVersion()));
        getLogger().info(MessageUtil.color("&aAuthor: &fYourName"));
        getLogger().info(MessageUtil.color("&a================================"));
    }

    @Override
    public void onDisable() {
        // Close database connection
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        // Log disable
        getLogger().info(MessageUtil.color("&c================================"));
        getLogger().info(MessageUtil.color("&6UltimateBan &chas been disabled!"));
        getLogger().info(MessageUtil.color("&c================================"));
    }

    /**
     * Register commands
     */
    private void registerCommands() {
        // Register existing commands
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("ultimateban").setExecutor(new UltimateBanCommand(this));
        
        // Register new commands
        getCommand("alts").setExecutor(new AltsCommand(this));
        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("ipunban").setExecutor(new IpUnbanCommand(this));
        getCommand("check").setExecutor(new CheckCommand(this));
    }

    /**
     * Register a command with the plugin
     *
     * @param name The command name
     * @param executor The command executor
     */
    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("Failed to register command: " + name);
        }
    }

    /**
     * Get the configuration manager
     *
     * @return The configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the database manager
     *
     * @return The database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Get the IPUtil instance
     *
     * @return The IPUtil instance
     */
    public IPUtil getIpUtil() {
        return ipUtil;
    }

    /**
     * Get the AltDetectionService instance
     *
     * @return The AltDetectionService instance
     */
    public AltDetectionService getAltDetectionService() {
        return altDetectionService;
    }

    /**
     * Get the VPNDetectionService instance
     *
     * @return The VPNDetectionService instance
     */
    public VPNDetectionService getVpnDetectionService() {
        return vpnDetectionService;
    }

    /**
     * Get the PunishmentTemplateService instance
     *
     * @return The PunishmentTemplateService instance
     */
    public PunishmentTemplateService getTemplateService() {
        return templateService;
    }

    /**
     * Get the PunishmentEscalationService instance
     *
     * @return The PunishmentEscalationService instance
     */
    public PunishmentEscalationService getEscalationService() {
        return escalationService;
    }

    /**
     * Reload the plugin
     *
     * @return True if reload was successful
     */
    public boolean reload() {
        try {
            // Reload config
            configManager.reloadConfig();
            
            // Reinitialize database
            databaseManager.closeConnection();
            if (!databaseManager.initialize()) {
                getLogger().severe("Failed to reconnect to database during reload!");
                return false;
            }
            
            getLogger().info("Plugin reloaded successfully!");
            return true;
        } catch (Exception e) {
            getLogger().severe("Error reloading plugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 