package me.math3w.bazaar.bazaar.portfolio;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.orders.OrderType;
import me.math3w.bazaar.databases.StockDatabase;
import me.math3w.bazaar.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PortfolioManager {
    private final BazaarPlugin plugin;
    private final Map<UUID, List<PortfolioTransaction>> portfolios = new HashMap<>();
    private final StockDatabase database;
    private static final double SELL_FEE_PERCENTAGE = 0.05;

    public PortfolioManager(BazaarPlugin plugin) {
        this.plugin = plugin;
        this.database = new StockDatabase(plugin);
        loadDataFromDatabase();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processSettlementQueue, 20L, 12000L);
    }

    private void loadDataFromDatabase() {
        List<StockDatabase.SavedTransaction> savedData = database.loadAllTransactions();
        for (StockDatabase.SavedTransaction entry : savedData) {
            addTransactionToMemory(entry.playerUuid, entry.transaction);
        }
    }

    public void stop() { database.close(); }
    public void buyStock(Player player, Product product, int amount) {
        double currentPrice = plugin.getMarketTicker().getCurrentPrice(product);
        double totalCost = currentPrice * amount;

        if (!plugin.getEconomy().has(player, totalCost)) {
            player.sendMessage("§cKhông đủ tiền! Cần: " + Utils.getTextPrice(totalCost));
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, totalCost);
        plugin.getMarketTicker().addVolume(product, totalCost, OrderType.BUY);
        plugin.getLiquidityService().recordVolume(totalCost);

        PortfolioTransaction tx = new PortfolioTransaction(
                UUID.randomUUID(), product.getId(), amount, totalCost,
                PortfolioTransaction.TransactionType.BUY_HOLDING, System.currentTimeMillis(), 0, 0
        );
        addTransactionToMemory(player.getUniqueId(), tx);
        database.saveTransaction(player.getUniqueId(), tx);

        player.sendMessage("§aĐã mua " + amount + "x " + product.getName());
        player.sendMessage("§7Cổ phiếu sẽ bị khóa trong 24h.");
    }

    public void sellStock(Player player, Product product, int sellAmount) {
        UUID uuid = player.getUniqueId();
        List<PortfolioTransaction> txList = portfolios.getOrDefault(uuid, new ArrayList<>());

        int available = getAvailableStockAmount(uuid, product);
        if (available < sellAmount) {
            player.sendMessage("§cKhông đủ cổ phiếu khả dụng để bán!");
            return;
        }

        int remainingToSell = sellAmount;
        Iterator<PortfolioTransaction> iterator = txList.iterator();
        while (iterator.hasNext() && remainingToSell > 0) {
            PortfolioTransaction tx = iterator.next();
            if (tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING
                    && tx.getProductId().equals(product.getId())
                    && tx.isSettled()) {

                if (tx.getAmount() <= remainingToSell) {
                    remainingToSell -= tx.getAmount();
                    iterator.remove();
                    database.deleteTransaction(tx);
                } else {
                    tx.setAmount(tx.getAmount() - remainingToSell);
                    remainingToSell = 0;
                    database.updateTransactionAmount(tx);
                }
            }
        }

        double sellPrice = plugin.getMarketTicker().getSellPrice(product);
        double grossValue = sellPrice * sellAmount;
        double originalPrice = sellPrice / (1.0 - SELL_FEE_PERCENTAGE);
        double trueGrossValue = originalPrice * sellAmount;
        double fee = trueGrossValue * SELL_FEE_PERCENTAGE;

        plugin.getMarketTicker().addVolume(product, trueGrossValue, OrderType.SELL);
        plugin.getLiquidityService().recordVolume(trueGrossValue);
        plugin.getLiquidityService().addFee(fee);

        PortfolioTransaction pendingTx = new PortfolioTransaction(
                UUID.randomUUID(), product.getId(), sellAmount, grossValue,
                PortfolioTransaction.TransactionType.SELL_PENDING, System.currentTimeMillis(), 0, 0
        );
        addTransactionToMemory(uuid, pendingTx);
        database.saveTransaction(uuid, pendingTx);

        player.sendMessage("§aĐã đặt lệnh Bán " + sellAmount + "x " + product.getName());
        player.sendMessage("§7Tổng nhận: " + Utils.getTextPrice(grossValue) + " (Đã trừ phí 5%)");
        player.sendMessage("§eĐang chờ duyệt chi từ Quỹ...");
    }

    public void processSettlementQueue() {
        List<PortfolioTransaction> allPendingSells = new ArrayList<>();
        for (List<PortfolioTransaction> list : portfolios.values()) {
            for (PortfolioTransaction tx : list) {
                if (tx.getType() == PortfolioTransaction.TransactionType.SELL_PENDING) {
                    if (System.currentTimeMillis() - tx.getTimestamp() >= PortfolioTransaction.COOLDOWN_MS
                            && tx.getRemainingValueToSettle() > 0) {
                        allPendingSells.add(tx);
                    }
                }
            }
        }

        if (allPendingSells.isEmpty()) return;

        allPendingSells.sort(Comparator.comparingLong(PortfolioTransaction::getTimestamp));

        // 3. Phân bổ Quỹ
        double availableFund = plugin.getLiquidityService().getAvailableLiquidity();

        if (availableFund <= 0) return;

        for (PortfolioTransaction tx : allPendingSells) {
            double needed = tx.getRemainingValueToSettle();
            double canPay = Math.min(availableFund, needed);

            if (canPay <= 0) break;

            tx.setSettledValue(tx.getSettledValue() + canPay);
            plugin.getLiquidityService().useLiquidity(canPay);
            availableFund -= canPay;

            database.updateTransactionSettled(tx);
        }
    }

    // --- TIME SKIP (Admin Debug) ---
    public void processTimeSkip(int hours) {
        long millisSkipped = hours * 3600000L;
        int count = 0;

        for (List<PortfolioTransaction> list : portfolios.values()) {
            for (PortfolioTransaction tx : list) {
                // Chỉ giảm thời gian nếu chưa đủ cooldown
                if (tx.getTimeRemaining() > 0) {
                    tx.setTimestamp(tx.getTimestamp() - millisSkipped);
                    count++;
                }
            }
        }
        plugin.getLogger().info("Admin Debug: Skipped " + hours + " hours for " + count + " transactions.");
    }

    // --- CLAIM TIỀN ---
    public void claimMoney(Player player, PortfolioTransaction tx) {
        if (tx.getType() != PortfolioTransaction.TransactionType.SELL_PENDING) return;

        double claimable = tx.getSettledValue() - tx.getClaimedValue();

        if (claimable <= 0) {
            player.sendMessage("§cChưa có khoản thanh khoản nào mới để rút.");
            player.sendMessage("§7Vui lòng đợi qua 00:00 hoặc chờ Quỹ được cấp thêm vốn.");
            return;
        }

        plugin.getEconomy().depositPlayer(player, claimable);
        tx.setClaimedValue(tx.getSettledValue());
        database.updateTransactionClaimed(tx);

        player.sendMessage("§aĐã rút thành công: " + Utils.getTextPrice(claimable) + " linh thạch!");

        if (tx.getClaimedValue() >= tx.getValue()) {
            portfolios.get(player.getUniqueId()).remove(tx);
            database.deleteTransaction(tx);
            player.sendMessage("§aLệnh bán đã hoàn tất 100%.");
        } else {
            player.sendMessage("§eVẫn còn " + Utils.getTextPrice(tx.getValue() - tx.getClaimedValue()) + " chưa thanh khoản.");
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
    public StockDatabase getDatabase() { return database; }
}