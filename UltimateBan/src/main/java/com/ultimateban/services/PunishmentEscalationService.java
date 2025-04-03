package com.ultimateban.services;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Punishment;
import com.ultimateban.models.PunishmentType;
import com.ultimateban.util.TimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Service for handling punishment escalation
 */
public class PunishmentEscalationService {
    private final UltimateBan plugin;
    private final boolean enabled;
    private final int resetDays;
    private final Map<String, Map<Integer, EscalationStep>> escalationSteps = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The UltimateBan plugin instance
     */
    public PunishmentEscalationService(UltimateBan plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("punishment-escalation.enabled", true);
        this.resetDays = plugin.getConfig().getInt("punishment-escalation.reset-days", 30);
        loadEscalationSteps();
    }
    
    /**
     * Load escalation steps from config
     */
    private void loadEscalationSteps() {
        escalationSteps.clear();
        
        ConfigurationSection escalationSection = plugin.getConfig().getConfigurationSection("punishment-escalation");
        if (escalationSection == null) {
            plugin.getLogger().warning("No punishment escalation config found.");
            return;
        }
        
        // Load each category
        for (String category : escalationSection.getKeys(false)) {
            if (category.equals("enabled") || category.equals("reset-days")) {
                continue;
            }
            
            ConfigurationSection categorySection = escalationSection.getConfigurationSection(category);
            if (categorySection != null) {
                Map<Integer, EscalationStep> steps = new HashMap<>();
                
                for (String level : categorySection.getKeys(false)) {
                    try {
                        int stepLevel = Integer.parseInt(level);
                        ConfigurationSection stepSection = categorySection.getConfigurationSection(level);
                        
                        if (stepSection != null) {
                            String type = stepSection.getString("type", "WARN");
                            String reason = stepSection.getString("reason", "Repeated offense");
                            String duration = stepSection.getString("duration", "0");
                            
                            PunishmentType punishmentType = PunishmentType.valueOf(type);
                            EscalationStep step = new EscalationStep(punishmentType, reason, duration);
                            steps.put(stepLevel, step);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid escalation level: " + level);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid punishment type in escalation config: " + category + "." + level);
                    }
                }
                
                escalationSteps.put(category, steps);
                plugin.getLogger().info("Loaded " + steps.size() + " escalation steps for category: " + category);
            }
        }
    }
    
    /**
     * Get the next punishment for a player based on their history
     * @param playerUUID The player's UUID
     * @param category The category of the offense
     * @return The next punishment step
     */
    public EscalationStep getNextPunishment(UUID playerUUID, String category) {
        if (!enabled || !escalationSteps.containsKey(category)) {
            return null;
        }
        
        Map<Integer, EscalationStep> steps = escalationSteps.get(category);
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        
        // Count how many offenses the player has had in this category
        int offenseCount = countOffenses(playerUUID, category);
        
        // Get the next step (current offense count + 1)
        int nextStep = offenseCount + 1;
        if (steps.containsKey(nextStep)) {
            return steps.get(nextStep);
        }
        
        // If there's no step for this specific level, use the highest available step
        int highestLevel = 0;
        for (int level : steps.keySet()) {
            if (level > highestLevel) {
                highestLevel = level;
            }
        }
        
        return steps.get(highestLevel);
    }
    
    /**
     * Count how many offenses a player has had in a category within the reset period
     * @param playerUUID The player's UUID
     * @param category The category of the offense
     * @return The number of offenses
     */
    private int countOffenses(UUID playerUUID, String category) {
        long resetTime = System.currentTimeMillis() - (resetDays * 24 * 60 * 60 * 1000L);
        
        try {
            // Get all punishments for this player after the reset time
            List<Punishment> punishments = plugin.getDatabaseManager().getPlayerPunishmentsAfter(playerUUID, resetTime);
            
            // Count punishments in this category
            int count = 0;
            for (Punishment punishment : punishments) {
                // Check if the punishment has a category metadata
                String punishmentCategory = punishment.getMetadata("category");
                if (punishmentCategory != null && punishmentCategory.equals(category)) {
                    count++;
                }
            }
            
            return count;
        } catch (Exception e) {
            plugin.getLogger().severe("Error counting player offenses: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Apply punishment escalation
     * @param playerUUID The player's UUID
     * @param playerName The player's name
     * @param staffUUID The staff's UUID
     * @param staffName The staff's name
     * @param category The category of the offense
     * @return The applied punishment or null if escalation is disabled
     */
    public Punishment applyEscalation(UUID playerUUID, String playerName, UUID staffUUID, String staffName, String category) {
        if (!enabled) {
            return null;
        }
        
        EscalationStep step = getNextPunishment(playerUUID, category);
        if (step == null) {
            return null;
        }
        
        // Create the punishment
        long startTime = System.currentTimeMillis();
        long endTime;
        
        if (step.getDuration().equalsIgnoreCase("permanent")) {
            endTime = Long.MAX_VALUE;
        } else {
            try {
                long duration = TimeUtil.parseDuration(step.getDuration());
                endTime = startTime + duration;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid duration in escalation step: " + step.getDuration());
                endTime = startTime + 3600000; // Default to 1 hour
            }
        }
        
        Punishment punishment = new Punishment(
                playerUUID,
                playerName,
                staffUUID,
                staffName,
                step.getType(),
                step.getReason(),
                startTime,
                endTime
        );
        
        // Add category metadata
        punishment.setMetadata("category", category);
        
        // Save the punishment
        if (plugin.getDatabaseManager().savePunishment(punishment)) {
            return punishment;
        }
        
        return null;
    }
    
    /**
     * Class representing an escalation step
     */
    public static class EscalationStep {
        private final PunishmentType type;
        private final String reason;
        private final String duration;
        
        /**
         * Constructor
         * @param type The punishment type
         * @param reason The reason
         * @param duration The duration
         */
        public EscalationStep(PunishmentType type, String reason, String duration) {
            this.type = type;
            this.reason = reason;
            this.duration = duration;
        }
        
        /**
         * Get the punishment type
         * @return The punishment type
         */
        public PunishmentType getType() {
            return type;
        }
        
        /**
         * Get the reason
         * @return The reason
         */
        public String getReason() {
            return reason;
        }
        
        /**
         * Get the duration
         * @return The duration
         */
        public String getDuration() {
            return duration;
        }
    }
} 