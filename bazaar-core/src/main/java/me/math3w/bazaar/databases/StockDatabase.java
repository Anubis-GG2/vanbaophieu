package me.math3w.bazaar.databases;

import me.math3w.bazaar.BazaarPlugin;
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
            // 1. Bảng Transaction (Lưu lịch sử mua/bán)
            String sqlTrans = "CREATE TABLE IF NOT EXISTS player_transactions (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "product_id VARCHAR(64) NOT NULL, " +
                    "amount INT NOT NULL, " +
                    "value DOUBLE NOT NULL, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "timestamp BIGINT NOT NULL" +
                    ");";
            stmt.execute(sqlTrans);

            // 2. [NEW] Bảng Giá Cổ Phiếu (Lưu giá hiện tại)
            String sqlPrices = "CREATE TABLE IF NOT EXISTS stock_prices (" +
                    "product_id VARCHAR(64) PRIMARY KEY, " +
                    "price DOUBLE NOT NULL" +
                    ");";
            stmt.execute(sqlPrices);

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

    // =========================================================================
    // PHẦN QUẢN LÝ TRANSACTION (Giữ nguyên)
    // =========================================================================

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

                PortfolioTransaction tx = new PortfolioTransaction(txId, productId, amount, value, type, timestamp);
                results.add(new SavedTransaction(playerUuid, tx));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public void saveTransaction(UUID playerUuid, PortfolioTransaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_transactions(id, player_uuid, product_id, amount, value, type, timestamp) VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tx.getId().toString());
                pstmt.setString(2, playerUuid.toString());
                pstmt.setString(3, tx.getProductId());
                pstmt.setInt(4, tx.getAmount());
                pstmt.setDouble(5, tx.getValue());
                pstmt.setString(6, tx.getType().name());
                pstmt.setLong(7, tx.getTimestamp());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateTransactionAmount(PortfolioTransaction tx) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE player_transactions SET amount = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, tx.getAmount());
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

    // =========================================================================
    // [NEW] PHẦN QUẢN LÝ GIÁ CỔ PHIẾU (STOCK PRICES)
    // =========================================================================

    /**
     * Load toàn bộ giá cổ phiếu từ Database vào Map
     */
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

    /**
     * Lưu hoặc Cập nhật giá cổ phiếu
     * Dùng REPLACE INTO để nếu đã có thì ghi đè, chưa có thì thêm mới
     */
    public void saveStockPrices(Map<String, Double> priceMap) {
        // Lưu Sync hoặc Async tùy thời điểm gọi. Nếu gọi khi onDisable, bắt buộc phải chạy Sync (trên main thread)
        // để đảm bảo lưu xong trước khi server tắt hẳn.
        String sql = "REPLACE INTO stock_prices(product_id, price) VALUES(?,?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Sử dụng Batch Update cho nhanh
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

    public static class SavedTransaction {
        public final UUID playerUuid;
        public final PortfolioTransaction transaction;

        public SavedTransaction(UUID playerUuid, PortfolioTransaction transaction) {
            this.playerUuid = playerUuid;
            this.transaction = transaction;
        }
    }
}