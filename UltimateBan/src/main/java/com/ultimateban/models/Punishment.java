package com.ultimateban.models;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a punishment in the UltimateBan system
 */
public class Punishment {
    
    private int id;
    private UUID playerUUID;
    private String playerName;
    private UUID punisherUUID;
    private String punisherName;
    private PunishmentType type;
    private String reason;
    private final long startTime;
    private final long endTime;
    private boolean active;
    private Map<String, String> metadata;

    /**
     * Constructor for a new punishment
     * 
     * @param playerUUID The UUID of the punished player
     * @param playerName The name of the punished player
     * @param punisherUUID The UUID of the staff member who issued the punishment
     * @param punisherName The name of the staff member who issued the punishment
     * @param type The type of punishment
     * @param reason The reason for the punishment
     * @param startTime The time when the punishment starts
     * @param endTime The time when the punishment ends (use -1 for permanent)
     */
    public Punishment(UUID playerUUID, String playerName, UUID punisherUUID, String punisherName, 
            PunishmentType type, String reason, long startTime, long endTime) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.punisherUUID = punisherUUID;
        this.punisherName = punisherName;
        this.type = type;
        this.reason = reason;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = true;
        this.metadata = new HashMap<>();
    }

    /**
     * Get the ID of the punishment
     * 
     * @return The punishment ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set the ID of the punishment
     * 
     * @param id The punishment ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the UUID of the punished player
     * 
     * @return The player's UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * Get the name of the punished player
     * 
     * @return The player's name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Get the UUID of the staff member who issued the punishment
     * 
     * @return The punisher's UUID
     */
    public UUID getPunisherUUID() {
        return punisherUUID;
    }

    /**
     * Get the name of the staff member who issued the punishment
     * 
     * @return The punisher's name
     */
    public String getPunisherName() {
        return punisherName;
    }

    /**
     * Get the type of punishment
     * 
     * @return The punishment type
     */
    public PunishmentType getType() {
        return type;
    }

    /**
     * Get the reason for the punishment
     * 
     * @return The punishment reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Get the time when the punishment starts
     * 
     * @return The start time in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the time when the punishment ends
     * 
     * @return The end time in milliseconds, or -1 if permanent
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Check if the punishment is active
     * 
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set whether the punishment is active
     * 
     * @param active true to activate, false to deactivate
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Check if the punishment is permanent
     * 
     * @return true if permanent, false if temporary
     */
    public boolean isPermanent() {
        return endTime == -1;
    }

    /**
     * Check if the punishment has expired
     * 
     * @return true if expired, false otherwise
     */
    public boolean hasExpired() {
        return !isPermanent() && System.currentTimeMillis() > endTime;
    }

    /**
     * Get the duration of the punishment in milliseconds
     * 
     * @return The duration, or -1 if permanent
     */
    public long getDuration() {
        return isPermanent() ? -1 : endTime - startTime;
    }

    /**
     * Get a metadata value by key
     *
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Set a metadata value
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    /**
     * Get all metadata
     *
     * @return The metadata map
     */
    public Map<String, String> getAllMetadata() {
        return metadata != null ? metadata : new HashMap<>();
    }

    /**
     * Set all metadata
     *
     * @param metadata The metadata map
     */
    public void setAllMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
} 