package com.ultimateban.models;

import java.util.UUID;

/**
 * Represents an IP ban
 */
public class IpBan {
    private int id;
    private String ipAddress;
    private UUID punisherUUID;
    private String punisherName;
    private String reason;
    private long startTime;
    private long endTime;
    private boolean active;
    private boolean isSubnet;

    /**
     * Constructor for a new IP ban
     *
     * @param ipAddress    The IP address to ban
     * @param punisherUUID The UUID of the punisher
     * @param punisherName The name of the punisher
     * @param reason       The reason for the ban
     * @param startTime    The start time of the ban
     * @param endTime      The end time of the ban
     * @param isSubnet     Whether this is a subnet ban
     */
    public IpBan(String ipAddress, UUID punisherUUID, String punisherName, String reason, long startTime, long endTime, boolean isSubnet) {
        this.ipAddress = ipAddress;
        this.punisherUUID = punisherUUID;
        this.punisherName = punisherName;
        this.reason = reason;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = true;
        this.isSubnet = isSubnet;
    }

    /**
     * Constructor for an existing IP ban from the database
     *
     * @param id           The ban ID
     * @param ipAddress    The IP address
     * @param punisherUUID The UUID of the punisher
     * @param punisherName The name of the punisher
     * @param reason       The reason for the ban
     * @param startTime    The start time of the ban
     * @param endTime      The end time of the ban
     * @param active       Whether the ban is active
     * @param isSubnet     Whether this is a subnet ban
     */
    public IpBan(int id, String ipAddress, UUID punisherUUID, String punisherName, String reason, long startTime, long endTime, boolean active, boolean isSubnet) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.punisherUUID = punisherUUID;
        this.punisherName = punisherName;
        this.reason = reason;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
        this.isSubnet = isSubnet;
    }

    /**
     * Get the ID of the ban
     *
     * @return The ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set the ID of the ban
     *
     * @param id The ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the IP address
     *
     * @return The IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Set the IP address
     *
     * @param ipAddress The IP address
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Get the UUID of the punisher
     *
     * @return The UUID
     */
    public UUID getPunisherUUID() {
        return punisherUUID;
    }

    /**
     * Set the UUID of the punisher
     *
     * @param punisherUUID The UUID
     */
    public void setPunisherUUID(UUID punisherUUID) {
        this.punisherUUID = punisherUUID;
    }

    /**
     * Get the name of the punisher
     *
     * @return The name
     */
    public String getPunisherName() {
        return punisherName;
    }

    /**
     * Set the name of the punisher
     *
     * @param punisherName The name
     */
    public void setPunisherName(String punisherName) {
        this.punisherName = punisherName;
    }

    /**
     * Get the reason for the ban
     *
     * @return The reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Set the reason for the ban
     *
     * @param reason The reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Get the start time of the ban
     *
     * @return The start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Set the start time of the ban
     *
     * @param startTime The start time
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Get the end time of the ban
     *
     * @return The end time
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Set the end time of the ban
     *
     * @param endTime The end time
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Check if the ban is active
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set whether the ban is active
     *
     * @param active true if active, false otherwise
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Check if this is a subnet ban
     *
     * @return true if a subnet ban, false otherwise
     */
    public boolean isSubnet() {
        return isSubnet;
    }

    /**
     * Set whether this is a subnet ban
     *
     * @param subnet true if a subnet ban, false otherwise
     */
    public void setSubnet(boolean subnet) {
        isSubnet = subnet;
    }

    /**
     * Check if the ban is permanent
     *
     * @return true if permanent, false otherwise
     */
    public boolean isPermanent() {
        return endTime == Long.MAX_VALUE;
    }

    /**
     * Check if the ban has expired
     *
     * @return true if expired, false otherwise
     */
    public boolean hasExpired() {
        return !isPermanent() && System.currentTimeMillis() > endTime;
    }
} 