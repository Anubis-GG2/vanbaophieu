package me.math3w.bazaar.bazaar.portfolio;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.databases.StockDatabase; // [ĐÃ SỬA] database số ít để khớp với file StockDatabase
import me.math3w.bazaar.utils.Utils;
import org.bukkit.entity.Player;

import java.util.*;

public class PortfolioManager {
    private final BazaarPlugin plugin;
    private final Map<UUID, List<PortfolioTransaction>> portfolios = new HashMap<>();
    private final StockDatabase database; // Database Instance

    public PortfolioManager(BazaarPlugin plugin) {
        this.plugin = plugin;
        this.database = new StockDatabase(plugin);

        loadDataFromDatabase(); // Load dữ liệu ngay khi khởi động
    }

    private void loadDataFromDatabase() {
        List<StockDatabase.SavedTransaction> savedData = database.loadAllTransactions();
        for (StockDatabase.SavedTransaction entry : savedData) {
            addTransactionToMemory(entry.playerUuid, entry.transaction);
        }
        plugin.getLogger().info("Loaded " + savedData.size() + " stock transactions from Database.");
    }

    public void stop() {
        database.close();
    }

    // --- LOGIC MUA ---
    public void buyStock(Player player, Product product, int amount) {
        double currentPrice = plugin.getMarketTicker().getCurrentPrice(product);
        double totalCost = currentPrice * amount;

        if (!plugin.getEconomy().has(player, totalCost)) {
            player.sendMessage("§cKhông đủ tiền! Cần: " + Utils.getTextPrice(totalCost));
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, totalCost);
        PortfolioTransaction tx = new PortfolioTransaction(
                product.getId(), amount, totalCost, PortfolioTransaction.TransactionType.BUY_HOLDING
        );
        // 1. Lưu vào RAM
        addTransactionToMemory(player.getUniqueId(), tx);
        // 2. Lưu vào Database
        database.saveTransaction(player.getUniqueId(), tx);
        player.sendMessage("§aĐã đặt lệnh Mua " + amount + "x " + product.getName());
        player.sendMessage("§7Cổ phiếu sẽ bị khóa trong 24h (T+1).");
        plugin.getLiquidityService().recordTransaction(totalCost);
    }

    // --- LOGIC BÁN ---
    public void sellStock(Player player, Product product, int sellAmount) {
        UUID uuid = player.getUniqueId();
        List<PortfolioTransaction> txList = portfolios.getOrDefault(uuid, new ArrayList<>());

        int available = getAvailableStockAmount(uuid, product);
        if (available < sellAmount) {
            player.sendMessage("§cKhông đủ cổ phiếu khả dụng để bán!");
            return;
        }

        double sellPrice = plugin.getMarketTicker().getSellPrice(product);
        double totalPayout = sellPrice * sellAmount;
        int remainingToSell = sellAmount;
        Iterator<PortfolioTransaction> iterator = txList.iterator();

        while (iterator.hasNext() && remainingToSell > 0) {
            PortfolioTransaction tx = iterator.next();
            if (tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING
                    && tx.getProductId().equals(product.getId())
                    && tx.isSettled()) {

                if (tx.getAmount() <= remainingToSell) {
                    remainingToSell -= tx.getAmount();
                    iterator.remove(); // Xóa khỏi RAM
                    database.deleteTransaction(tx);
                    // Xóa khỏi DB
                } else {
                    tx.setAmount(tx.getAmount() - remainingToSell);
                    remainingToSell = 0;
                    database.updateTransactionAmount(tx); // Cập nhật số lượng mới vào DB
                }
            }
        }

        // Tạo Transaction Pending Money
        PortfolioTransaction pendingTx = new PortfolioTransaction(
                product.getId(), sellAmount, totalPayout, PortfolioTransaction.TransactionType.SELL_PENDING
        );
        addTransactionToMemory(uuid, pendingTx);
        database.saveTransaction(uuid, pendingTx);

        player.sendMessage("§aĐã khớp lệnh Bán " + sellAmount + "x " + product.getName());
        player.sendMessage("§7Tiền (" + Utils.getTextPrice(totalPayout) + ") sẽ về sau 24h.");
    }

    // --- CLAIM TIỀN ---
    public void claimMoney(Player player, PortfolioTransaction tx) {
        if (tx.getType() == PortfolioTransaction.TransactionType.SELL_PENDING && tx.isSettled()) {
            if (!plugin.getLiquidityService().canAdminPay(tx.getValue())) {
                player.sendMessage("§cSàn giao dịch tạm thời hết thanh khoản!");
                return;
            }

            plugin.getEconomy().depositPlayer(player, tx.getValue());
            plugin.getLiquidityService().recordPayout(tx.getValue());
            // Xóa khỏi RAM và DB
            portfolios.get(player.getUniqueId()).remove(tx);
            database.deleteTransaction(tx);
            player.sendMessage("§aĐã rút thành công " + Utils.getTextPrice(tx.getValue()) + " coins!");
        } else {
            player.sendMessage("§cLệnh này chưa đến hạn thanh toán!");
        }
    }

    private void addTransactionToMemory(UUID uuid, PortfolioTransaction tx) {
        portfolios.computeIfAbsent(uuid, k -> new ArrayList<>()).add(tx);
    }

    public List<PortfolioTransaction> getTransactions(UUID uuid) {
        return portfolios.getOrDefault(uuid, new ArrayList<>());
    }

    public Map<UUID, List<PortfolioTransaction>> getAllPortfolios() {
        return Collections.unmodifiableMap(portfolios);
    }

    public int getAvailableStockAmount(UUID uuid, Product product) {
        return getTransactions(uuid).stream()
                .filter(tx -> tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING
                        && tx.getProductId().equals(product.getId())
                        && tx.isSettled())
                .mapToInt(PortfolioTransaction::getAmount)
                .sum();
    }

    // [QUAN TRỌNG] Hàm Getter để MarketTicker có thể lấy Database
    public StockDatabase getDatabase() {
        return database;
    }
}