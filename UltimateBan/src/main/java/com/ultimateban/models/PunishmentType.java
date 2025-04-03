package com.ultimateban.models;

/**
 * Represents the different types of punishments in the UltimateBan system
 */
public enum PunishmentType {
    BAN("ban", true),
    TEMP_BAN("tempban", true),
    MUTE("mute", false),
    TEMP_MUTE("tempmute", false),
    KICK("kick", false),
    WARN("warn", false);
    
    private final String name;
    private final boolean preventJoin;
    
    /**
     * Constructor for PunishmentType
     * 
     * @param name The name of the punishment type
     * @param preventJoin Whether this punishment prevents the player from joining
     */
    PunishmentType(String name, boolean preventJoin) {
        this.name = name;
        this.preventJoin = preventJoin;
    }
    
    /**
     * Get the name of the punishment type
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Check if this punishment prevents the player from joining
     * 
     * @return true if joining is prevented, false otherwise
     */
    public boolean preventJoin() {
        return preventJoin;
    }
    
    /**
     * Get the punishment type from its name
     * 
     * @param name The name of the punishment type
     * @return The corresponding PunishmentType, or null if not found
     */
    public static PunishmentType fromName(String name) {
        for (PunishmentType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Check if the punishment type is temporary
     * 
     * @return true if temporary, false if permanent
     */
    public boolean isTemporary() {
        return this == TEMP_BAN || this == TEMP_MUTE;
    }
} 