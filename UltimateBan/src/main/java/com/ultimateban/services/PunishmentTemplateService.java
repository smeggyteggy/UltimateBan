package com.ultimateban.services;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.PunishmentType;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing punishment templates
 */
public class PunishmentTemplateService {
    private final UltimateBan plugin;
    private final Map<String, PunishmentTemplate> templates = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The UltimateBan plugin instance
     */
    public PunishmentTemplateService(UltimateBan plugin) {
        this.plugin = plugin;
        loadTemplates();
    }
    
    /**
     * Load punishment templates from config
     */
    public void loadTemplates() {
        templates.clear();
        
        ConfigurationSection templatesSection = plugin.getConfig().getConfigurationSection("punishment-templates");
        if (templatesSection == null) {
            plugin.getLogger().warning("No punishment templates found in config.");
            return;
        }
        
        for (String key : templatesSection.getKeys(false)) {
            ConfigurationSection templateSection = templatesSection.getConfigurationSection(key);
            if (templateSection != null) {
                try {
                    String type = templateSection.getString("type", "WARN");
                    PunishmentType punishmentType = PunishmentType.valueOf(type);
                    String reason = templateSection.getString("reason", "No reason provided");
                    String duration = templateSection.getString("duration", "0");
                    String displayName = templateSection.getString("display-name", key);
                    String description = templateSection.getString("description", "");
                    
                    PunishmentTemplate template = new PunishmentTemplate(
                            key,
                            punishmentType,
                            reason,
                            duration,
                            ChatColor.translateAlternateColorCodes('&', displayName),
                            description
                    );
                    
                    templates.put(key, template);
                    plugin.getLogger().info("Loaded punishment template: " + key);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid punishment type in template: " + key);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + templates.size() + " punishment templates.");
    }
    
    /**
     * Get all available punishment templates
     * @return List of templates
     */
    public List<PunishmentTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }
    
    /**
     * Get a template by its ID
     * @param id The template ID
     * @return The template or null if not found
     */
    public PunishmentTemplate getTemplate(String id) {
        return templates.get(id);
    }
    
    /**
     * Get templates by punishment type
     * @param type The punishment type
     * @return List of templates for that type
     */
    public List<PunishmentTemplate> getTemplatesByType(PunishmentType type) {
        List<PunishmentTemplate> result = new ArrayList<>();
        
        for (PunishmentTemplate template : templates.values()) {
            if (template.getType() == type) {
                result.add(template);
            }
        }
        
        return result;
    }
    
    /**
     * Class representing a punishment template
     */
    public static class PunishmentTemplate {
        private final String id;
        private final PunishmentType type;
        private final String reason;
        private final String duration;
        private final String displayName;
        private final String description;
        
        /**
         * Constructor
         * @param id Template ID
         * @param type Punishment type
         * @param reason Default reason
         * @param duration Default duration
         * @param displayName Display name for UI
         * @param description Description for UI
         */
        public PunishmentTemplate(String id, PunishmentType type, String reason, String duration, String displayName, String description) {
            this.id = id;
            this.type = type;
            this.reason = reason;
            this.duration = duration;
            this.displayName = displayName;
            this.description = description;
        }
        
        /**
         * Get the template ID
         * @return Template ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Get the punishment type
         * @return Punishment type
         */
        public PunishmentType getType() {
            return type;
        }
        
        /**
         * Get the default reason
         * @return Default reason
         */
        public String getReason() {
            return reason;
        }
        
        /**
         * Get the default duration
         * @return Default duration
         */
        public String getDuration() {
            return duration;
        }
        
        /**
         * Get the display name
         * @return Display name
         */
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * Get the description
         * @return Description
         */
        public String getDescription() {
            return description;
        }
    }
} 