package me.HitroxVN.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.HitroxVN.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private HikariDataSource dataSource;

    public DatabaseManager() {
        init();
        createTables();
    }

    private void init() {
        HikariConfig config = new HikariConfig();
        String type = Main.getInstance().getConfig().getString("database.type", "SQLITE");

        if (type.equalsIgnoreCase("MYSQL")) {
            String host = Main.getInstance().getConfig().getString("database.mysql.host");
            int port = Main.getInstance().getConfig().getInt("database.mysql.port");
            String db = Main.getInstance().getConfig().getString("database.mysql.database");
            String user = Main.getInstance().getConfig().getString("database.mysql.username");
            String pass = Main.getInstance().getConfig().getString("database.mysql.password");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db);
            config.setUsername(user);
            config.setPassword(pass);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            Main.getInstance().getLogger().info("Dang khoi tao ket noi toi SQLITE...");
            File dbFile = new File(Main.getInstance().getDataFolder(), "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setPoolName("SpeedrunPool");

        // Neu loi thi de no hien ra cho de debug, khong tu dong chuyen sang SQLITE nua
        this.dataSource = new HikariDataSource(config);
        Main.getInstance().getLogger().info("Ket noi Database [" + type + "] thanh cong!");
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps1 = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS rs_records (" +
                                "uuid VARCHAR(36), " +
                                "item VARCHAR(64), " +
                                "best_time BIGINT, " +
                                "PRIMARY KEY (uuid, item))");
                PreparedStatement ps2 = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS rs_stats (" +
                                "uuid VARCHAR(36) PRIMARY KEY, " +
                                "name VARCHAR(32), " +
                                "total_wins INT DEFAULT 0)")) {
            ps1.execute();
            ps2.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Long> getBestTime(UUID uuid, Material item) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn
                            .prepareStatement("SELECT best_time FROM rs_records WHERE uuid = ? AND item = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, item.name());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getLong("best_time");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1L;
        });
    }

    public void saveRecord(UUID uuid, String name, Material item, long time) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // Luu ky luc item
                try (PreparedStatement psSelection = conn
                        .prepareStatement("SELECT best_time FROM rs_records WHERE uuid = ? AND item = ?")) {
                    psSelection.setString(1, uuid.toString());
                    psSelection.setString(2, item.name());
                    ResultSet rs = psSelection.executeQuery();

                    if (!rs.next() || time < rs.getLong("best_time")) {
                        try (PreparedStatement psUpsert = conn.prepareStatement(
                                "REPLACE INTO rs_records (uuid, item, best_time) VALUES (?, ?, ?)")) {
                            psUpsert.setString(1, uuid.toString());
                            psUpsert.setString(2, item.name());
                            psUpsert.setLong(3, time);
                            psUpsert.executeUpdate();
                        }
                    }
                }

                // Cap nhat stats tong
                updatePlayerStats(conn, uuid, name);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void updatePlayerStats(Connection conn, UUID uuid, String name) throws SQLException {
        // Kiem tra ton tai
        boolean exists = false;
        try (PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM rs_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            if (ps.executeQuery().next())
                exists = true;
        }

        if (exists) {
            try (PreparedStatement ps = conn
                    .prepareStatement("UPDATE rs_stats SET name = ?, total_wins = total_wins + 1 WHERE uuid = ?")) {
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = conn
                    .prepareStatement("INSERT INTO rs_stats (uuid, name, total_wins) VALUES (?, ?, 1)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            }
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
