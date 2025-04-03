package com.ultimateban.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a player in the UltimateBan system
 */
public class Player {
    private final UUID uuid;
    private final String name;
    private boolean banned;
    private boolean muted;
    
    /**
     * Constructor
     *
     * @param uuid The player's UUID
     * @param name The player's name
     */
    public Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.banned = false;
        this.muted = false;
    }
    
    /**
     * Get the player's UUID
     *
     * @return The UUID
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * Get the player's name
     *
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Check if the player is banned
     *
     * @return true if banned, false otherwise
     */
    public boolean isBanned() {
        return banned;
    }
    
    /**
     * Set the banned status
     *
     * @param banned The banned status
     */
    public void setBanned(boolean banned) {
        this.banned = banned;
    }
    
    /**
     * Check if the player is muted
     *
     * @return true if muted, false otherwise
     */
    public boolean isMuted() {
        return muted;
    }
    
    /**
     * Set the muted status
     *
     * @param muted The muted status
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(uuid, player.uuid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
    
    @Override
    public String toString() {
        return name + " (" + uuid + ")" + (banned ? " [BANNED]" : "") + (muted ? " [MUTED]" : "");
    }
} 