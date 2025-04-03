package com.ultimateban.models;

import java.util.UUID;

/**
 * Represents an appeal for a punishment
 */
public class Appeal {
    
    /**
     * Status of an appeal
     */
    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }
    
    private int id;
    private int punishmentId;
    private UUID playerUUID;
    private String playerName;
    private String reason;
    private long submissionTime;
    private Status status;
    private UUID responderUUID;
    private String responderName;
    private String response;
    private long responseTime;
    
    /**
     * Constructor for an appeal
     * 
     * @param id The appeal ID
     * @param punishmentId The ID of the punishment being appealed
     * @param playerUUID The UUID of the player making the appeal
     * @param playerName The name of the player making the appeal
     * @param reason The reason for the appeal
     * @param submissionTime The time when the appeal was submitted
     * @param status The status of the appeal
     * @param responderUUID The UUID of the staff member who responded to the appeal
     * @param responderName The name of the staff member who responded to the appeal
     * @param response The response message from the staff member
     * @param responseTime The time when the response was given
     */
    public Appeal(int id, int punishmentId, UUID playerUUID, String playerName, String reason,
                 long submissionTime, Status status, UUID responderUUID, String responderName,
                 String response, long responseTime) {
        this.id = id;
        this.punishmentId = punishmentId;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.reason = reason;
        this.submissionTime = submissionTime;
        this.status = status;
        this.responderUUID = responderUUID;
        this.responderName = responderName;
        this.response = response;
        this.responseTime = responseTime;
    }
    
    /**
     * Constructor for a new appeal
     * 
     * @param punishmentId The ID of the punishment being appealed
     * @param playerUUID The UUID of the player making the appeal
     * @param playerName The name of the player making the appeal
     * @param reason The reason for the appeal
     */
    public Appeal(int punishmentId, UUID playerUUID, String playerName, String reason) {
        this.punishmentId = punishmentId;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.reason = reason;
        this.submissionTime = System.currentTimeMillis();
        this.status = Status.PENDING;
    }
    
    /**
     * Get the ID of the appeal
     * 
     * @return The appeal ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Set the ID of the appeal
     * 
     * @param id The appeal ID
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Get the ID of the punishment being appealed
     * 
     * @return The punishment ID
     */
    public int getPunishmentId() {
        return punishmentId;
    }
    
    /**
     * Get the UUID of the player making the appeal
     * 
     * @return The player's UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Get the name of the player making the appeal
     * 
     * @return The player's name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Get the reason for the appeal
     * 
     * @return The appeal reason
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Get the time when the appeal was submitted
     * 
     * @return The submission time
     */
    public long getSubmissionTime() {
        return submissionTime;
    }
    
    /**
     * Get the status of the appeal
     * 
     * @return The appeal status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Set the status of the appeal
     * 
     * @param status The appeal status
     */
    public void setStatus(Status status) {
        this.status = status;
    }
    
    /**
     * Get the UUID of the staff member who responded to the appeal
     * 
     * @return The responder's UUID
     */
    public UUID getResponderUUID() {
        return responderUUID;
    }
    
    /**
     * Set the UUID of the staff member who responded to the appeal
     * 
     * @param responderUUID The responder's UUID
     */
    public void setResponderUUID(UUID responderUUID) {
        this.responderUUID = responderUUID;
    }
    
    /**
     * Get the name of the staff member who responded to the appeal
     * 
     * @return The responder's name
     */
    public String getResponderName() {
        return responderName;
    }
    
    /**
     * Set the name of the staff member who responded to the appeal
     * 
     * @param responderName The responder's name
     */
    public void setResponderName(String responderName) {
        this.responderName = responderName;
    }
    
    /**
     * Get the response message from the staff member
     * 
     * @return The response message
     */
    public String getResponse() {
        return response;
    }
    
    /**
     * Set the response message from the staff member
     * 
     * @param response The response message
     */
    public void setResponse(String response) {
        this.response = response;
    }
    
    /**
     * Get the time when the response was given
     * 
     * @return The response time
     */
    public long getResponseTime() {
        return responseTime;
    }
    
    /**
     * Set the time when the response was given
     * 
     * @param responseTime The response time
     */
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    /**
     * Check if the appeal is pending
     * 
     * @return True if the status is PENDING
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }
    
    /**
     * Check if the appeal has been accepted
     * 
     * @return True if the status is ACCEPTED
     */
    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }
    
    /**
     * Check if the appeal has been rejected
     * 
     * @return True if the status is REJECTED
     */
    public boolean isRejected() {
        return status == Status.REJECTED;
    }
    
    /**
     * Accept the appeal with a response
     * 
     * @param responderUUID The UUID of the staff member accepting the appeal
     * @param responderName The name of the staff member accepting the appeal
     * @param response The response message
     */
    public void accept(UUID responderUUID, String responderName, String response) {
        this.status = Status.ACCEPTED;
        this.responderUUID = responderUUID;
        this.responderName = responderName;
        this.response = response;
        this.responseTime = System.currentTimeMillis();
    }
    
    /**
     * Reject the appeal with a response
     * 
     * @param responderUUID The UUID of the staff member rejecting the appeal
     * @param responderName The name of the staff member rejecting the appeal
     * @param response The response message
     */
    public void reject(UUID responderUUID, String responderName, String response) {
        this.status = Status.REJECTED;
        this.responderUUID = responderUUID;
        this.responderName = responderName;
        this.response = response;
        this.responseTime = System.currentTimeMillis();
    }
} 