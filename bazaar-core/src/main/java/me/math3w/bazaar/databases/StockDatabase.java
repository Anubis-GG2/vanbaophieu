package me.math3w.bazaar.databases;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.StockCandle;
import me.math3w.bazaar.bazaar.portfolio.PortfolioTransaction;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StockDatabase {
    private final BazaarPlugin plugin;
    private Connection connection;

    public StockDatabase(BazaarPlugin plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        File dataFolder = new File(plugin.getDataFolder(), "stocks.db");
        if (!dataFolder.getParentFile().exists()) {
            dataFolder.getParentFile().mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Không thể kết nối Database SQLite!", e);
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            String sqlTrans = "CREATE TABLE IF NOT EXISTS player_transactions (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "product_id VARCHAR(64) NOT NULL, " +
                    "amount INT NOT NULL, " +
                    "value DOUBLE NOT NULL, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "timestamp BIGINT NOT NULL, " +
                    "settled_value DOUBLE DEFAULT 0, " +
                    "claimed_value DOUBLE DEFAULT 0" +
                    ");";
            stmt.execute(sqlTrans);

            String sqlPrices = "CREATE TABLE IF NOT EXISTS stock_prices (" +
                    "product_id VARCHAR(64) PRIMARY KEY, " +
                    "price DOUBLE NOT NULL" +
                    ");";
            stmt.execute(sqlPrices);

            String sqlCandles = "CREATE TABLE IF NOT EXISTS stock_candles (" +
                    "product_id VARCHAR(64) NOT NULL, " +
                    "timestamp BIGINT NOT NULL, " +
                    "open DOUBLE NOT NULL, " +
                    "high DOUBLE NOT NULL, " +
                    "low DOUBLE NOT NULL, " +
                    "close DOUBLE NOT NULL, " +
                    "volume DOUBLE NOT NULL, " +
                    "PRIMARY KEY (product_id, timestamp)" +
                    ");";
            stmt.execute(sqlCandles);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<SavedTransaction> loadAllTransactions() {
        List<SavedTransaction> results = new ArrayList<>();
        String sql = "SELECT * FROM player_transactions";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID txId = UUID.fromString(rs.getString("id"));
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                String productId = rs.getString("product_id");
                int amount = rs.getInt("amount");
                double value = rs.getDouble("value");
                PortfolioTransaction.TransactionType type = PortfolioTransaction.TransactionType.valueOf(rs.getString("type"));
                long timestamp = rs.getLong("timestamp");
                double settledValue = rs.getDouble("settled_value");
                double claimedValue = rs.getDouble("claimed_value");

                PortfolioTransaction tx = new PortfolioTransaction(txId, productId, amount, value, type, timestamp, settledValue, claimedValue);
                results.add(new SavedTransaction(playerUuid, tx));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public void saveTransaction(UUID playerUuid, PortfolioTransaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_transactions(id, player_uuid, product_id, amount, value, type, timestamp, settled_value, claimed_value) VALUES(?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tx.getId().toString());
                pstmt.setString(2, playerUuid.toString());
                pstmt.setString(3, tx.getProductId());
                pstmt.setInt(4, tx.getAmount());
                pstmt.setDouble(5, tx.getValue());
                pstmt.setString(6, tx.getType().name());
                pstmt.setLong(7, tx.getTimestamp());
                pstmt.setDouble(8, tx.getSettledValue());
                pstmt.setDouble(9, tx.getClaimedValue());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateTransactionAmount(PortfolioTransaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE player_transactions SET amount = ?, value = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, tx.getAmount());
                pstmt.setDouble(2, tx.getValue());
                pstmt.setString(3, tx.getId().toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateTransactionSettled(PortfolioTransaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE player_transactions SET settled_value = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, tx.getSettledValue());
                pstmt.setString(2, tx.getId().toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateTransactionClaimed(PortfolioTransaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE player_transactions SET claimed_value = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, tx.getClaimedValue());
                pstmt.setString(2, tx.getId().toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void deleteTransaction(PortfolioTransaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_transactions WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tx.getId().toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public Map<String, Double> loadAllStockPrices() {
        Map<String, Double> prices = new HashMap<>();
        String sql = "SELECT * FROM stock_prices";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String productId = rs.getString("product_id");
                double price = rs.getDouble("price");
                prices.put(productId, price);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prices;
    }

    public void saveStockPrices(Map<String, Double> priceMap) {
        String sql = "REPLACE INTO stock_prices(product_id, price) VALUES(?,?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);

            for (Map.Entry<String, Double> entry : priceMap.entrySet()) {
                pstmt.setString(1, entry.getKey());
                pstmt.setDouble(2, entry.getValue());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveCandle(String productId, StockCandle candle) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO stock_candles(product_id, timestamp, open, high, low, close, volume) VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, productId);
                pstmt.setLong(2, candle.getTimestamp());
                pstmt.setDouble(3, candle.getOpen());
                pstmt.setDouble(4, candle.getHigh());
                pstmt.setDouble(5, candle.getLow());
                pstmt.setDouble(6, candle.getClose());
                pstmt.setDouble(7, candle.getVolume());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public List<StockCandle> loadCandles(String productId) {
        List<StockCandle> candles = new ArrayList<>();
        String sql = "SELECT * FROM stock_candles WHERE product_id = ? ORDER BY timestamp ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long timestamp = rs.getLong("timestamp");
                    double open = rs.getDouble("open");
                    double high = rs.getDouble("high");
                    double low = rs.getDouble("low");
                    double close = rs.getDouble("close");
                    double volume = rs.getDouble("volume");
                    candles.add(new StockCandle(timestamp, open, high, low, close, volume));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return candles;
    }

    public static class SavedTransaction {
        public final UUID playerUuid;
        public final PortfolioTransaction transaction;

        public SavedTransaction(UUID playerUuid, PortfolioTransaction transaction) {
            this.playerUuid = playerUuid;
            this.transaction = transaction;
        }
    }
}