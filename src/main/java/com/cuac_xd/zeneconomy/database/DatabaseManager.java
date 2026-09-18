package com.cuac_xd.zeneconomy.database;

import com.cuac_xd.zeneconomy.database.dao.TopEntry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private StorageType storageType;
    private String tableAccounts;
    private String tableBalances;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        FileConfiguration config = plugin.getConfig();
        String typeStr = config.getString("database.type", "SQLITE");
        this.storageType = StorageType.fromString(typeStr);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ZenEconomyPool");

        int maxPool = config.getInt("database.pool.maximum-pool-size", 10);
        int minIdle = config.getInt("database.pool.minimum-idle", 5);
        long maxLifetime = config.getLong("database.pool.max-lifetime", 1800000);
        long connectionTimeout = config.getLong("database.pool.connection-timeout", 10000);

        hikariConfig.setMaximumPoolSize(maxPool);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setConnectionTimeout(connectionTimeout);

        String prefix = config.getString("database.mysql.table-prefix", "ze_");
        this.tableAccounts = prefix + "accounts";
        this.tableBalances = prefix + "balances";

        if (storageType == StorageType.SQLITE) {
            String dbFileName = config.getString("database.sqlite.file", "zeneconomy.db");
            File dbFile = new File(plugin.getDataFolder(), dbFileName);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setMaximumPoolSize(1); // SQLite is best suited for 1 write-safe connection
        } else {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "minecraft");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");
            boolean ssl = config.getBoolean("database.mysql.ssl", false);

            hikariConfig.setDriverClassName(storageType == StorageType.MARIADB ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver");
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&characterEncoding=utf8");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);

            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        }

        this.dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("El DataSource de base de datos no está disponible.");
        }
        return dataSource.getConnection();
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            String createAccounts = (storageType == StorageType.SQLITE) ?
                    "CREATE TABLE IF NOT EXISTS " + tableAccounts + " (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "username VARCHAR(16) NOT NULL, " +
                            "last_seen BIGINT NOT NULL);" :
                    "CREATE TABLE IF NOT EXISTS " + tableAccounts + " (" +
                            "uuid VARCHAR(36) NOT NULL, " +
                            "username VARCHAR(16) NOT NULL, " +
                            "last_seen BIGINT NOT NULL, " +
                            "PRIMARY KEY (uuid));";

            String createBalances = (storageType == StorageType.SQLITE) ?
                    "CREATE TABLE IF NOT EXISTS " + tableBalances + " (" +
                            "uuid VARCHAR(36) NOT NULL, " +
                            "currency_id VARCHAR(32) NOT NULL, " +
                            "balance DOUBLE NOT NULL, " +
                            "PRIMARY KEY (uuid, currency_id));" :
                    "CREATE TABLE IF NOT EXISTS " + tableBalances + " (" +
                            "uuid VARCHAR(36) NOT NULL, " +
                            "currency_id VARCHAR(32) NOT NULL, " +
                            "balance DOUBLE NOT NULL, " +
                            "PRIMARY KEY (uuid, currency_id), " +
                            "INDEX idx_curr_bal (currency_id, balance DESC));";

            try (PreparedStatement stmt1 = conn.prepareStatement(createAccounts)) {
                stmt1.executeUpdate();
            }
            try (PreparedStatement stmt2 = conn.prepareStatement(createBalances)) {
                stmt2.executeUpdate();
            }

            if (storageType == StorageType.SQLITE) {
                try (PreparedStatement stmtIndex = conn.prepareStatement(
                        "CREATE INDEX IF NOT EXISTS idx_curr_bal ON " + tableBalances + " (currency_id, balance DESC);")) {
                    stmtIndex.executeUpdate();
                }
            }

            plugin.getLogger().info("Tablas de base de datos listas (" + storageType + ").");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fallo al inicializar tablas en base de datos", e);
        }
    }

    public CompletableFuture<Void> saveAccount(UUID uuid, String username, Map<String, Double> balances) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                long now = System.currentTimeMillis();

                String upsertAccount = (storageType == StorageType.SQLITE) ?
                        "INSERT INTO " + tableAccounts + " (uuid, username, last_seen) VALUES (?, ?, ?) " +
                                "ON CONFLICT(uuid) DO UPDATE SET username = excluded.username, last_seen = excluded.last_seen;" :
                        "INSERT INTO " + tableAccounts + " (uuid, username, last_seen) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE username = VALUES(username), last_seen = VALUES(last_seen);";

                try (PreparedStatement stmt = conn.prepareStatement(upsertAccount)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, username);
                    stmt.setLong(3, now);
                    stmt.executeUpdate();
                }

                String upsertBalance = (storageType == StorageType.SQLITE) ?
                        "INSERT INTO " + tableBalances + " (uuid, currency_id, balance) VALUES (?, ?, ?) " +
                                "ON CONFLICT(uuid, currency_id) DO UPDATE SET balance = excluded.balance;" :
                        "INSERT INTO " + tableBalances + " (uuid, currency_id, balance) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE balance = VALUES(balance);";

                try (PreparedStatement stmt = conn.prepareStatement(upsertBalance)) {
                    for (Map.Entry<String, Double> entry : balances.entrySet()) {
                        stmt.setString(1, uuid.toString());
                        stmt.setString(2, entry.getKey().toLowerCase());
                        stmt.setDouble(3, entry.getValue());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al guardar cuenta para " + uuid, e);
            }
        });
    }

    public CompletableFuture<Map<String, Double>> loadBalances(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Double> result = new HashMap<>();
            String sql = "SELECT currency_id, balance FROM " + tableBalances + " WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getString("currency_id").toLowerCase(), rs.getDouble("balance"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al cargar balances para " + uuid, e);
            }
            return result;
        });
    }

    public CompletableFuture<Optional<UUID>> lookupUuidByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid FROM " + tableAccounts + " WHERE LOWER(username) = LOWER(?) LIMIT 1;";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(UUID.fromString(rs.getString("uuid")));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error en lookup de UUID para nombre: " + name, e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<String>> lookupNameByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT username FROM " + tableAccounts + " WHERE uuid = ? LIMIT 1;";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getString("username"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error en lookup de nombre para UUID: " + uuid, e);
            }
            return Optional.empty();
        });
    }

    public CompletableFuture<List<TopEntry>> getTopBalances(String currencyId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<TopEntry> entries = new ArrayList<>();
            String sql = "SELECT a.uuid, a.username, b.balance FROM " + tableBalances + " b " +
                    "INNER JOIN " + tableAccounts + " a ON b.uuid = a.uuid " +
                    "WHERE b.currency_id = ? ORDER BY b.balance DESC LIMIT ?;";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, currencyId.toLowerCase());
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        entries.add(new TopEntry(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("username"),
                                rs.getDouble("balance"),
                                rank++
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al consultar top balances para: " + currencyId, e);
            }
            return entries;
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Conexión de base de datos cerrada.");
        }
    }
}
