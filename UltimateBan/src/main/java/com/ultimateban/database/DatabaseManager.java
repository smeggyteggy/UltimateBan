package com.ultimateban.database;

import com.ultimateban.UltimateBan;
import com.ultimateban.models.Appeal;
import com.ultimateban.models.Punishment;
import com.ultimateban.models.PunishmentType;
import com.ultimateban.models.IpBan;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handles database operations for the plugin
 */
public class DatabaseManager {

    private final UltimateBan plugin;
    private Connection connection;
    private String databaseType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String sqliteFile;
    private boolean useSSL;

    /**
     * Constructor
     *
     * @param plugin The UltimateBan plugin instance
     */
    public DatabaseManager(UltimateBan plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load database configuration from config
     */
    private void loadConfig() {
        databaseType = plugin.getConfigManager().getString("database.type");
        host = plugin.getConfigManager().getString("mysql.host");
        port = plugin.getConfigManager().getInt("mysql.port");
        database = plugin.getConfigManager().getString("mysql.database");
        username = plugin.getConfigManager().getString("mysql.username");
        password = plugin.getConfigManager().getString("mysql.password");
        useSSL = plugin.getConfigManager().getBoolean("mysql.use_ssl");
        sqliteFile = plugin.getConfigManager().getString("sqlite.file");
    }

    /**
     * Initialize the database connection
     *
     * @return true if successful, false otherwise
     */
    public boolean initialize() {
        if (databaseType.equalsIgnoreCase("MySQL")) {
            return setupMySQL();
        } else if (databaseType.equalsIgnoreCase("SQLite")) {
            return setupSQLite();
        } else {
            plugin.getLogger().warning("Unknown database type: " + databaseType + ". Falling back to SQLite.");
            return setupSQLite();
        }
    }

    /**
     * Set up MySQL connection
     *
     * @return true if successful, false otherwise
     */
    private boolean setupMySQL() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            if (!useSSL) {
                url += "?useSSL=false&allowPublicKeyRetrieval=true";
            }

            connection = DriverManager.getConnection(url, username, password);
            createTables();
            plugin.getLogger().info("Connected to MySQL database!");
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Set up SQLite connection
     *
     * @return true if successful, false otherwise
     */
    private boolean setupSQLite() {
        try {
            Class.forName("org.sqlite.JDBC");

            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            File dbFile = new File(dataFolder, sqliteFile);
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            createTables();
            plugin.getLogger().info("Connected to SQLite database!");
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite database: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create necessary tables
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create punishments table
            statement.execute("CREATE TABLE IF NOT EXISTS punishments ("
                    + "id INTEGER PRIMARY KEY " + (databaseType.equalsIgnoreCase("MySQL") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "player_name VARCHAR(16) NOT NULL, "
                    + "punisher_uuid VARCHAR(36) NOT NULL, "
                    + "punisher_name VARCHAR(16) NOT NULL, "
                    + "type VARCHAR(16) NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "start_time BIGINT NOT NULL, "
                    + "end_time BIGINT NOT NULL, "
                    + "active BOOLEAN NOT NULL DEFAULT 1, "
                    + "ip_address VARCHAR(45)"
                    + ")");

            // Create appeals table
            statement.execute("CREATE TABLE IF NOT EXISTS appeals ("
                    + "id INTEGER PRIMARY KEY " + (databaseType.equalsIgnoreCase("MySQL") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", "
                    + "punishment_id INTEGER NOT NULL, "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "player_name VARCHAR(16) NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "submission_time BIGINT NOT NULL, "
                    + "status VARCHAR(16) NOT NULL DEFAULT 'PENDING', "
                    + "responder_uuid VARCHAR(36), "
                    + "responder_name VARCHAR(16), "
                    + "response TEXT, "
                    + "response_time BIGINT"
                    + ")");
                    
            // Create IP bans table
            statement.execute("CREATE TABLE IF NOT EXISTS ip_bans ("
                    + "id INTEGER PRIMARY KEY " + (databaseType.equalsIgnoreCase("MySQL") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", "
                    + "ip_address VARCHAR(45) NOT NULL, "
                    + "punisher_uuid VARCHAR(36) NOT NULL, "
                    + "punisher_name VARCHAR(16) NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "start_time BIGINT NOT NULL, "
                    + "end_time BIGINT NOT NULL, "
                    + "active BOOLEAN NOT NULL DEFAULT 1, "
                    + "is_subnet BOOLEAN NOT NULL DEFAULT 0"
                    + ")");
                    
            // Create player IPs table to track alternative accounts
            statement.execute("CREATE TABLE IF NOT EXISTS player_ips ("
                    + "id INTEGER PRIMARY KEY " + (databaseType.equalsIgnoreCase("MySQL") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "player_name VARCHAR(16) NOT NULL, "
                    + "ip_address VARCHAR(45) NOT NULL, "
                    + "last_seen BIGINT NOT NULL, "
                    + "UNIQUE(player_uuid, ip_address)"
                    + ")");

            // Create punishment templates table
            statement.execute("CREATE TABLE IF NOT EXISTS punishment_templates ("
                    + "id INTEGER PRIMARY KEY " + (databaseType.equalsIgnoreCase("MySQL") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", "
                    + "name VARCHAR(32) NOT NULL UNIQUE, "
                    + "type VARCHAR(16) NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "duration BIGINT NOT NULL"
                    + ")");

            // Create indexes for performance
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punishments_player_uuid ON punishments (player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punishments_active ON punishments (active)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punishments_ip_address ON punishments (ip_address)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_appeals_punishment_id ON appeals (punishment_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_appeals_player_uuid ON appeals (player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_appeals_status ON appeals (status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ip_bans_ip_address ON ip_bans (ip_address)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ip_bans_active ON ip_bans (active)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_ips_ip_address ON player_ips (ip_address)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_ips_player_uuid ON player_ips (player_uuid)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating tables: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Close the database connection
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error closing database connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Save a punishment to the database
     *
     * @param punishment The punishment to save
     * @return true if successful, false otherwise
     */
    public boolean savePunishment(Punishment punishment) {
        String sql = "INSERT INTO punishments (player_uuid, player_name, punisher_uuid, punisher_name, type, reason, start_time, end_time, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, punishment.getPlayerUUID().toString());
            statement.setString(2, punishment.getPlayerName());
            statement.setString(3, punishment.getPunisherUUID().toString());
            statement.setString(4, punishment.getPunisherName());
            statement.setString(5, punishment.getType().name());
            statement.setString(6, punishment.getReason());
            statement.setLong(7, punishment.getStartTime());
            statement.setLong(8, punishment.getEndTime());
            statement.setBoolean(9, punishment.isActive());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving punishment: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update a punishment in the database
     *
     * @param punishment The punishment to update
     * @return true if successful, false otherwise
     */
    public boolean updatePunishment(Punishment punishment) {
        String sql = "UPDATE punishments SET active = ?, end_time = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, punishment.isActive());
            statement.setLong(2, punishment.getEndTime());
            statement.setInt(3, punishment.getId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating punishment: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get active punishments for a player
     *
     * @param playerUUID The UUID of the player
     * @return A list of active punishments
     */
    public List<Punishment> getActivePunishments(UUID playerUUID) {
        List<Punishment> punishments = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? AND active = 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    punishments.add(extractPunishment(resultSet));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting active punishments: " + e.getMessage(), e);
        }

        return punishments;
    }

    /**
     * Get a player's UUID from their name
     * 
     * @param playerName The player's name
     * @return A CompletableFuture that resolves to the player's UUID, or null if not found
     */
    public CompletableFuture<UUID> getPlayerUuid(String playerName) {
        CompletableFuture<UUID> future = new CompletableFuture<>();
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT uuid FROM players WHERE name = ? COLLATE NOCASE")) {
                
                stmt.setString(1, playerName);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    future.complete(uuid);
                } else {
                    future.complete(null);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error getting player UUID: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    /**
     * Extract a punishment from a ResultSet
     *
     * @param resultSet The ResultSet to extract from
     * @return The extracted punishment
     * @throws SQLException If there is an error extracting data
     */
    private Punishment extractPunishment(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));
        String playerName = resultSet.getString("player_name");
        UUID punisherUUID = UUID.fromString(resultSet.getString("punisher_uuid"));
        String punisherName = resultSet.getString("punisher_name");
        PunishmentType type = PunishmentType.valueOf(resultSet.getString("type"));
        String reason = resultSet.getString("reason");
        long startTime = resultSet.getLong("start_time");
        long endTime = resultSet.getLong("end_time");
        boolean active = resultSet.getBoolean("active");

        Punishment punishment = new Punishment(playerUUID, playerName, punisherUUID, punisherName, 
                type, reason, startTime, endTime);
        punishment.setId(id);
        punishment.setActive(active);
        return punishment;
    }

    /**
     * Get a connection to the database
     *
     * @return The database connection
     * @throws SQLException if the connection is closed
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initialize();
        }
        return connection;
    }

    /**
     * Get a punishment by ID
     *
     * @param id The punishment ID
     * @return The punishment, or null if not found
     */
    public Punishment getPunishment(int id) {
        String sql = "SELECT * FROM punishments WHERE id = ?";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractPunishmentFromResultSet(resultSet);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting punishment: " + e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Get the active ban for a player
     *
     * @param playerUUID The player's UUID
     * @return The active ban, or null if the player is not banned
     */
    public Punishment getActiveBan(UUID playerUUID) {
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? AND active = 1 AND (type = ? OR type = ?)";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, PunishmentType.BAN.name());
            statement.setString(3, PunishmentType.TEMP_BAN.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Punishment punishment = extractPunishmentFromResultSet(resultSet);
                    
                    // Check if temporary ban has expired
                    if (!punishment.isPermanent() && punishment.hasExpired()) {
                        deactivatePunishment(punishment.getId());
                        return null;
                    }
                    
                    return punishment;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting active ban: " + e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Get the active mute for a player
     *
     * @param playerUUID The player's UUID
     * @return The active mute, or null if the player is not muted
     */
    public Punishment getActiveMute(UUID playerUUID) {
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? AND active = 1 AND (type = ? OR type = ?)";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, PunishmentType.MUTE.name());
            statement.setString(3, PunishmentType.TEMP_MUTE.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Punishment punishment = extractPunishmentFromResultSet(resultSet);
                    
                    // Check if temporary mute has expired
                    if (!punishment.isPermanent() && punishment.hasExpired()) {
                        deactivatePunishment(punishment.getId());
                        return null;
                    }
                    
                    return punishment;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting active mute: " + e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Deactivate a punishment
     *
     * @param id The punishment ID
     * @return true if successful, false otherwise
     */
    public boolean deactivatePunishment(int id) {
        String sql = "UPDATE punishments SET active = 0 WHERE id = ?";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deactivating punishment: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deactivate all active punishments of a specific type for a player
     *
     * @param playerUUID The UUID of the player
     * @param type The type of punishment to deactivate
     * @return true if successful, false otherwise
     */
    public boolean deactivatePunishment(UUID playerUUID, String type) {
        String sql = "UPDATE punishments SET active = 0 WHERE player_uuid = ? AND type = ? AND active = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, type);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deactivating punishment: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all punishments for a player
     *
     * @param playerUUID The UUID of the player
     * @return List of punishments
     */
    public List<Punishment> getPlayerPunishments(UUID playerUUID) {
        List<Punishment> punishments = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? ORDER BY start_time DESC";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    punishments.add(extractPunishmentFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player punishments: " + e.getMessage(), e);
        }
        
        return punishments;
    }

    /**
     * Save an appeal to the database
     *
     * @param appeal The appeal to save
     * @return The ID of the saved appeal
     */
    public int saveAppeal(Appeal appeal) {
        String sql = "INSERT INTO appeals (punishment_id, player_uuid, player_name, reason, submission_time, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, appeal.getPunishmentId());
            statement.setString(2, appeal.getPlayerUUID().toString());
            statement.setString(3, appeal.getPlayerName());
            statement.setString(4, appeal.getReason());
            statement.setLong(5, appeal.getSubmissionTime());
            statement.setString(6, appeal.getStatus().name());
            
            statement.executeUpdate();
            
            // Get the generated ID
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    appeal.setId(generatedKeys.getInt(1));
                    return appeal.getId();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving appeal: " + e.getMessage(), e);
        }
        
        return -1;
    }

    /**
     * Update an appeal in the database
     *
     * @param appeal The appeal to update
     * @return true if successful, false otherwise
     */
    public boolean updateAppeal(Appeal appeal) {
        String sql = "UPDATE appeals SET status = ?, responder_uuid = ?, responder_name = ?, response = ?, response_time = ? " +
                     "WHERE id = ?";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, appeal.getStatus().name());
            statement.setString(2, appeal.getResponderUUID() != null ? appeal.getResponderUUID().toString() : null);
            statement.setString(3, appeal.getResponderName());
            statement.setString(4, appeal.getResponse());
            statement.setLong(5, appeal.getResponseTime());
            statement.setInt(6, appeal.getId());
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating appeal: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get an appeal by ID
     *
     * @param id The appeal ID
     * @return The appeal, or null if not found
     */
    public Appeal getAppeal(int id) {
        String sql = "SELECT * FROM appeals WHERE id = ?";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractAppealFromResultSet(resultSet);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting appeal: " + e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Get all pending appeals
     *
     * @return A list of pending appeals
     */
    public List<Appeal> getPendingAppeals() {
        List<Appeal> appeals = new ArrayList<>();
        String sql = "SELECT * FROM appeals WHERE status = ?";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, Appeal.Status.PENDING.name());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    appeals.add(extractAppealFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting pending appeals: " + e.getMessage(), e);
        }
        
        return appeals;
    }

    /**
     * Get all appeals for a player
     *
     * @param playerUUID The player's UUID a
     * @return A list of appeals
     */
    public List<Appeal> getPlayerAppeals(UUID playerUUID) {
        List<Appeal> appeals = new ArrayList<>();
        String sql = "SELECT * FROM appeals WHERE player_uuid = ?";
        
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    appeals.add(extractAppealFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player appeals: " + e.getMessage(), e);
        }
        
        return appeals;
    }

    /**
     * Extract a punishment from a ResultSet
     *
     * @param resultSet The ResultSet containing punishment data
     * @return The extracted Punishment
     * @throws SQLException if extraction fails
     */
    private Punishment extractPunishmentFromResultSet(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));
        String playerName = resultSet.getString("player_name");
        UUID punisherUUID = UUID.fromString(resultSet.getString("punisher_uuid"));
        String punisherName = resultSet.getString("punisher_name");
        PunishmentType type = PunishmentType.valueOf(resultSet.getString("type"));
        String reason = resultSet.getString("reason");
        long startTime = resultSet.getLong("start_time");
        long endTime = resultSet.getLong("end_time");
        boolean active = resultSet.getBoolean("active");
        
        Punishment punishment = new Punishment(playerUUID, playerName, punisherUUID, punisherName, 
                type, reason, startTime, endTime);
        punishment.setId(id);
        punishment.setActive(active);
        return punishment;
    }

    /**
     * Extract an appeal from a ResultSet
     *
     * @param resultSet The ResultSet containing appeal data
     * @return The extracted Appeal
     * @throws SQLException if extraction fails
     */
    private Appeal extractAppealFromResultSet(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int punishmentId = resultSet.getInt("punishment_id");
        UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));
        String playerName = resultSet.getString("player_name");
        String reason = resultSet.getString("reason");
        long submissionTime = resultSet.getLong("submission_time");
        Appeal.Status status = Appeal.Status.valueOf(resultSet.getString("status"));
        
        String responderUUIDString = resultSet.getString("responder_uuid");
        UUID responderUUID = responderUUIDString != null ? UUID.fromString(responderUUIDString) : null;
        
        String responderName = resultSet.getString("responder_name");
        String response = resultSet.getString("response");
        long responseTime = resultSet.getLong("response_time");
        
        return new Appeal(id, punishmentId, playerUUID, playerName, reason, submissionTime, status, responderUUID, responderName, response, responseTime);
    }

    /**
     * Unban a player by deactivating their active ban punishment
     *
     * @param playerName The name of the player to unban
     * @return true if successful, false otherwise
     */
    public boolean unbanPlayer(String playerName) {
        // Use the new asynchronous method but join to make this synchronous 
        // for compatibility with existing code
        UUID playerUUID = getPlayerUuid(playerName).join();
        if (playerUUID == null) {
            return false;
        }

        String sql = "UPDATE punishments SET active = 0 WHERE player_uuid = ? AND type = 'BAN' AND active = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error unbanning player: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get a list of currently banned players
     *
     * @return List of banned player names
     */
    public List<String> getBannedPlayers() {
        List<String> bannedPlayers = new ArrayList<>();
        String sql = "SELECT DISTINCT player_name FROM punishments WHERE type = 'BAN' AND active = 1";
        
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            while (resultSet.next()) {
                bannedPlayers.add(resultSet.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting banned players: " + e.getMessage(), e);
        }
        
        return bannedPlayers;
    }

    /**
     * Save an IP ban to the database
     *
     * @param ipBan The IP ban to save
     * @return true if successful, false otherwise
     */
    public boolean saveIpBan(IpBan ipBan) {
        String sql = "INSERT INTO ip_bans (ip_address, punisher_uuid, punisher_name, reason, start_time, end_time, active, is_subnet) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, ipBan.getIpAddress());
            statement.setString(2, ipBan.getPunisherUUID().toString());
            statement.setString(3, ipBan.getPunisherName());
            statement.setString(4, ipBan.getReason());
            statement.setLong(5, ipBan.getStartTime());
            statement.setLong(6, ipBan.getEndTime());
            statement.setBoolean(7, ipBan.isActive());
            statement.setBoolean(8, ipBan.isSubnet());
            
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }
            
            // Get the generated ID
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ipBan.setId(generatedKeys.getInt(1));
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving IP ban: " + e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Get an active IP ban for an IP address
     *
     * @param ipAddress The IP address to check
     * @return The IP ban or null if not found
     */
    public IpBan getActiveIpBan(String ipAddress) {
        // Check for exact IP ban
        String sql = "SELECT * FROM ip_bans WHERE ip_address = ? AND active = 1";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ipAddress);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    IpBan ipBan = extractIpBanFromResultSet(resultSet);
                    
                    // Check if temporary ban has expired
                    if (ipBan.hasExpired()) {
                        deactivateIpBan(ipBan.getId());
                        return null;
                    }
                    
                    return ipBan;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting active IP ban: " + e.getMessage(), e);
        }
        
        // Check for subnet ban (if no exact match)
        sql = "SELECT * FROM ip_bans WHERE is_subnet = 1 AND active = 1";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    IpBan ipBan = extractIpBanFromResultSet(resultSet);
                    
                    // Check if temporary ban has expired
                    if (ipBan.hasExpired()) {
                        deactivateIpBan(ipBan.getId());
                        continue;
                    }
                    
                    // Check if the IP is in this subnet
                    if (isIpInSubnet(ipAddress, ipBan.getIpAddress())) {
                        return ipBan;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting active subnet ban: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Deactivate an IP ban
     *
     * @param id The IP ban ID
     * @return true if successful, false otherwise
     */
    public boolean deactivateIpBan(int id) {
        String sql = "UPDATE ip_bans SET active = 0 WHERE id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deactivating IP ban: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Save a player's IP address to the database
     *
     * @param playerUUID The player's UUID
     * @param playerName The player's name
     * @param ipAddress  The player's IP address
     * @return true if successful, false otherwise
     */
    public boolean savePlayerIp(UUID playerUUID, String playerName, String ipAddress) {
        String sql = "INSERT OR REPLACE INTO player_ips (player_uuid, player_name, ip_address, last_seen) "
                + "VALUES (?, ?, ?, ?)";
                
        // For MySQL, use different syntax
        if (databaseType.equalsIgnoreCase("MySQL")) {
            sql = "INSERT INTO player_ips (player_uuid, player_name, ip_address, last_seen) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE player_name = ?, last_seen = ?";
        }
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, playerName);
            statement.setString(3, ipAddress);
            statement.setLong(4, System.currentTimeMillis());
            
            if (databaseType.equalsIgnoreCase("MySQL")) {
                statement.setString(5, playerName);
                statement.setLong(6, System.currentTimeMillis());
            }
            
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player IP: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get all players who have used an IP address
     *
     * @param ipAddress The IP address
     * @return A list of player UUIDs
     */
    public List<UUID> getPlayersWithIp(String ipAddress) {
        List<UUID> players = new ArrayList<>();
        String sql = "SELECT player_uuid FROM player_ips WHERE ip_address = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ipAddress);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(UUID.fromString(resultSet.getString("player_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting players with IP: " + e.getMessage(), e);
        }
        
        return players;
    }
    
    /**
     * Get all IP addresses a player has used
     *
     * @param playerUUID The player's UUID
     * @return A list of IP addresses
     */
    public List<String> getPlayerIps(UUID playerUUID) {
        List<String> ips = new ArrayList<>();
        String sql = "SELECT ip_address FROM player_ips WHERE player_uuid = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ips.add(resultSet.getString("ip_address"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player IPs: " + e.getMessage(), e);
        }
        
        return ips;
    }
    
    /**
     * Check if an IP address is within a subnet
     *
     * @param ip     The IP address to check
     * @param subnet The subnet to check (in CIDR notation)
     * @return true if the IP is in the subnet, false otherwise
     */
    private boolean isIpInSubnet(String ip, String subnet) {
        if (ip == null || subnet == null) {
            return false;
        }
        
        try {
            // Very simple CIDR check - just check if the first three octets match
            // A more robust implementation would use a proper CIDR library
            String[] ipParts = ip.split("\\.");
            if (ipParts.length != 4) {
                return false;
            }
            
            // Parse the subnet parts
            String[] subnetParts = subnet.split("/");
            if (subnetParts.length != 2) {
                return false;
            }
            
            String[] subnetIpParts = subnetParts[0].split("\\.");
            if (subnetIpParts.length != 4) {
                return false;
            }
            
            // Check the network part based on the mask
            int mask = Integer.parseInt(subnetParts[1]);
            int fullOctets = mask / 8;
            
            // Check full octets
            for (int i = 0; i < fullOctets; i++) {
                if (!ipParts[i].equals(subnetIpParts[i])) {
                    return false;
                }
            }
            
            // If mask is a multiple of 8, we're done
            if (mask % 8 == 0) {
                return true;
            }
            
            // Check partial octet
            int partialOctet = fullOctets;
            int partialMask = 0xFF << (8 - (mask % 8));
            
            int ipOctet = Integer.parseInt(ipParts[partialOctet]);
            int subnetOctet = Integer.parseInt(subnetIpParts[partialOctet]);
            
            return (ipOctet & partialMask) == (subnetOctet & partialMask);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if IP is in subnet: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Extract an IP ban from a result set
     *
     * @param resultSet The result set
     * @return The IP ban
     * @throws SQLException if a database error occurs
     */
    private IpBan extractIpBanFromResultSet(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String ipAddress = resultSet.getString("ip_address");
        UUID punisherUUID = UUID.fromString(resultSet.getString("punisher_uuid"));
        String punisherName = resultSet.getString("punisher_name");
        String reason = resultSet.getString("reason");
        long startTime = resultSet.getLong("start_time");
        long endTime = resultSet.getLong("end_time");
        boolean active = resultSet.getBoolean("active");
        boolean isSubnet = resultSet.getBoolean("is_subnet");
        
        return new IpBan(id, ipAddress, punisherUUID, punisherName, reason, startTime, endTime, active, isSubnet);
    }

    /**
     * Get a player's name from their UUID
     *
     * @param playerUUID The player's UUID
     * @return The player's name or null if not found
     */
    public String getPlayerName(UUID playerUUID) {
        String sql = "SELECT player_name FROM player_ips WHERE player_uuid = ? ORDER BY last_seen DESC LIMIT 1";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("player_name");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player name: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Check if a player has a specific permission
     * This is a simple implementation that just checks if the player is an operator
     * A real implementation would check against your permission system
     *
     * @param playerUUID The player's UUID
     * @param permission The permission to check
     * @return true if the player has the permission, false otherwise
     */
    public boolean playerHasPermission(UUID playerUUID, String permission) {
        // The default implementation just returns false for offline players
        // This could be enhanced to check your permission system's database
        return false;
    }

    /**
     * Get player punishments after a specific timestamp
     *
     * @param playerUUID The player's UUID
     * @param timestamp The timestamp to filter after
     * @return A list of punishments
     */
    public List<Punishment> getPlayerPunishmentsAfter(UUID playerUUID, long timestamp) {
        List<Punishment> punishments = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? AND start_time > ? ORDER BY start_time DESC";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.setLong(2, timestamp);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Punishment punishment = extractPunishmentFromResultSet(resultSet);
                    punishments.add(punishment);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player punishments after timestamp: " + e.getMessage(), e);
        }
        
        return punishments;
    }
    
    /**
     * Get players who have used the specified IP address
     *
     * @param ip The IP address
     * @return A list of player UUIDs
     */
    public List<UUID> getPlayersByIp(String ip) {
        List<UUID> players = new ArrayList<>();
        String sql = "SELECT player_uuid FROM player_ips WHERE ip_address = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(UUID.fromString(resultSet.getString("player_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting players by IP: " + e.getMessage(), e);
        }
        
        return players;
    }
    
    /**
     * Get players who have connected in the last X days
     *
     * @param days Number of days
     * @return A list of player UUIDs
     */
    public List<UUID> getRecentPlayers(int days) {
        List<UUID> players = new ArrayList<>();
        long timestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        String sql = "SELECT DISTINCT player_uuid FROM player_ips WHERE last_seen > ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, timestamp);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(UUID.fromString(resultSet.getString("player_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting recent players: " + e.getMessage(), e);
        }
        
        return players;
    }
    
    /**
     * Get all player names from the database
     *
     * @return A list of player names
     */
    public List<String> getAllPlayerNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT DISTINCT player_name FROM player_ips ORDER BY player_name";
        
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                names.add(resultSet.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting all player names: " + e.getMessage(), e);
        }
        
        return names;
    }
    
    /**
     * Get suspicious join patterns that might indicate alt accounts
     * This is a placeholder implementation - a real implementation would be more sophisticated
     *
     * @return A list of player pairs with suspicious join patterns
     */
    public List<Map<String, Object>> getJoinPatterns() {
        List<Map<String, Object>> patterns = new ArrayList<>();
        
        // This is a simplistic implementation that finds players who have similar join timestamps
        // A real implementation would look for more complex patterns
        try {
            // In a real implementation, this would be a complex query that looks at join/leave patterns
            // For now, we just return an empty list
            return patterns;
        } catch (Exception e) {
            plugin.getLogger().severe("Error detecting join patterns: " + e.getMessage());
            return patterns;
        }
    }

    /**
     * Get the count of punishments of a specific type for a player
     * 
     * @param playerId The player's UUID
     * @param type The punishment type (ban, mute, kick, warn)
     * @return The count of punishments
     */
    public int getPunishmentCount(UUID playerId, String type) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) as count FROM punishments WHERE player_id = ? AND type LIKE ?")) {
            
            stmt.setString(1, playerId.toString());
            stmt.setString(2, "%" + type + "%");
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting punishment count: " + e.getMessage());
        }
        
        return 0;
    }

    /**
     * Get a list of previous punishments for a player of specified types
     * 
     * @param playerId The player's UUID
     * @param types The punishment types to include
     * @return A list of previous punishments
     */
    public List<Punishment> getPreviousPunishments(UUID playerId, PunishmentType... types) {
        List<Punishment> punishments = new ArrayList<>();
        
        if (types.length == 0) {
            return punishments;
        }
        
        try (Connection conn = getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * FROM punishments WHERE player_id = ? AND type IN (");
            
            for (int i = 0; i < types.length; i++) {
                sb.append("?");
                if (i < types.length - 1) {
                    sb.append(",");
                }
            }
            
            sb.append(") ORDER BY start_time DESC");
            
            try (PreparedStatement stmt = conn.prepareStatement(sb.toString())) {
                stmt.setString(1, playerId.toString());
                
                for (int i = 0; i < types.length; i++) {
                    stmt.setString(i + 2, types[i].toString());
                }
                
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Punishment punishment = extractPunishmentFromResultSet(rs);
                    punishments.add(punishment);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting previous punishments: " + e.getMessage());
        }
        
        return punishments;
    }
} 