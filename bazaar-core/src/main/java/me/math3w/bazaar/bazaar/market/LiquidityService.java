package me.math3w.bazaar.bazaar.market;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.portfolio.PortfolioManager;
import me.math3w.bazaar.bazaar.portfolio.PortfolioTransaction;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;

public class LiquidityService {
    private final BazaarPlugin plugin;

    // Quỹ hiện tại
    private double currentLiquidity = 0.0;

    // Tổng giá trị thị trường
    private double totalMarketValue = 0.0;

    public LiquidityService(BazaarPlugin plugin) {
        this.plugin = plugin;

        // Cập nhật quỹ mỗi 60 giây
        new BukkitRunnable() {
            @Override
            public void run() {
                recalculateFund();
            }
        }.runTaskTimer(plugin, 20L, 1200L);
    }

    public void recalculateFund() {
        double marketCap = 0.0;
        PortfolioManager pm = plugin.getPortfolioManager();

        if (pm == null) return;

        // [FIX] Duyệt qua List<PortfolioTransaction> thay vì Map
        for (List<PortfolioTransaction> transactions : pm.getAllPortfolios().values()) {
            for (PortfolioTransaction tx : transactions) {
                // Chỉ tính các giao dịch MUA đang nắm giữ (BUY_HOLDING)
                if (tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING) {
                    Product product = plugin.getBazaar().getProduct(tx.getProductId());
                    if (product != null) {
                        double currentPrice = plugin.getMarketTicker().getCurrentPrice(product);
                        marketCap += currentPrice * tx.getAmount();
                    }
                }
            }
        }

        this.totalMarketValue = marketCap;

        // Công thức: MarketCap / 30
        double newLimit = marketCap / 30.0;
        if (newLimit < 10000) newLimit = 10000; // Mức sàn tối thiểu

        this.currentLiquidity = newLimit;
    }

    public void recordTransaction(double amount) {
        this.currentLiquidity += amount;
    }

    public void recordPayout(double amount) {
        this.currentLiquidity -= amount;
    }

    public boolean canAdminPay(double amount) {
        return currentLiquidity >= amount;
    }

    public double getCurrentLiquidity() {
        return currentLiquidity;
    }

    public double getTotalMarketValue() {
        return totalMarketValue;
    }
}