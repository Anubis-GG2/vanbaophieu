package me.math3w.bazaar.bazaar.market;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.portfolio.PortfolioManager;
import me.math3w.bazaar.bazaar.portfolio.PortfolioTransaction;
import me.math3w.bazaar.utils.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class LiquidityService {
    private final BazaarPlugin plugin;
    private double dailyLiquidityCap = 0.0;
    private double usedLiquidityToday = 0.0;
    private double accumulatedFees = 0.0;

    // [NEW] Tracking Buy/Sell riêng biệt
    private double dailyBuyVolume = 0.0;
    private double dailySellVolume = 0.0;
    private double dailyVolume = 0.0; // Tổng volume chung (giữ lại để tính toán Cap)

    public LiquidityService(BazaarPlugin plugin) {
        this.plugin = plugin;
        this.accumulatedFees = plugin.getConfig().getDouble("data.accumulated-fees", 0.0);
        this.dailyVolume = plugin.getConfig().getDouble("data.daily-volume", 0.0);

        // [NEW] Load dữ liệu Buy/Sell
        this.dailyBuyVolume = plugin.getConfig().getDouble("data.daily-buy-volume", 0.0);
        this.dailySellVolume = plugin.getConfig().getDouble("data.daily-sell-volume", 0.0);

        // Đảm bảo tính nhất quán nếu file config cũ chưa có key mới
        if (this.dailyVolume > 0 && this.dailyBuyVolume == 0 && this.dailySellVolume == 0) {
            // Trường hợp migrate từ bản cũ, ta tạm chấp nhận chưa phân loại được volume cũ
        } else {
            this.dailyVolume = this.dailyBuyVolume + this.dailySellVolume;
        }

        recalculateDailyCap();

        new BukkitRunnable() {
            @Override
            public void run() {
                recalculateDailyCap();
            }
        }.runTaskTimer(plugin, 1728000L, 1728000L); // 24h in ticks
    }

    public void addFee(double amount) {
        this.accumulatedFees += amount;
        saveData();
    }

    // [DEPRECATED] Dùng recordBuy hoặc recordSell thay thế để chi tiết hơn
    public void recordVolume(double amount) {
        this.dailyVolume += amount;
        saveData();
    }

    // [NEW] Ghi nhận lệnh Mua
    public void recordBuy(double amount) {
        this.dailyBuyVolume += amount;
        this.dailyVolume += amount;
        saveData();
    }

    // [NEW] Ghi nhận lệnh Bán
    public void recordSell(double amount) {
        this.dailySellVolume += amount;
        this.dailyVolume += amount;
        saveData();
    }

    private void saveData() {
        plugin.getConfig().set("data.accumulated-fees", accumulatedFees);
        plugin.getConfig().set("data.daily-volume", dailyVolume);
        plugin.getConfig().set("data.daily-buy-volume", dailyBuyVolume);
        plugin.getConfig().set("data.daily-sell-volume", dailySellVolume);
        plugin.saveConfig();
    }

    public void recalculateDailyCap() {
        double marketCap = calculateMarketCap();
        double volumeBonus = this.dailyVolume * 0.20;
        double capBase = marketCap * 0.10;
        this.dailyLiquidityCap = capBase + volumeBonus + accumulatedFees;

        plugin.getLogger().info("DAILY RESET (Liquidity):");
        plugin.getLogger().info(" - Market Cap: " + Utils.getTextPrice(marketCap));
        plugin.getLogger().info(" - Previous 24h Volume: " + Utils.getTextPrice(this.dailyVolume));
        plugin.getLogger().info("   + Buy: " + Utils.getTextPrice(this.dailyBuyVolume));
        plugin.getLogger().info("   + Sell: " + Utils.getTextPrice(this.dailySellVolume));
        plugin.getLogger().info(" - Accumulated Fees: " + Utils.getTextPrice(this.accumulatedFees));
        plugin.getLogger().info(" -> NEW DAILY CAP: " + Utils.getTextPrice(dailyLiquidityCap));

        this.usedLiquidityToday = 0.0;
        this.accumulatedFees = 0.0;

        // Reset các biến Volume về 0 cho ngày mới
        this.dailyVolume = 0.0;
        this.dailyBuyVolume = 0.0;
        this.dailySellVolume = 0.0;

        saveData();
    }

    // [UPDATE] Public method này để Menu có thể gọi
    public double calculateMarketCap() {
        double cap = 0.0;
        PortfolioManager pm = plugin.getPortfolioManager();
        if (pm == null) return 0.0;

        for (List<PortfolioTransaction> transactions : pm.getAllPortfolios().values()) {
            for (PortfolioTransaction tx : transactions) {
                if (tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING) {
                    Product product = plugin.getBazaar().getProduct(tx.getProductId());
                    if (product != null) {
                        double currentPrice = plugin.getMarketTicker().getCurrentPrice(product);
                        cap += currentPrice * tx.getAmount();
                    }
                }
            }
        }
        return cap;
    }

    public double getAvailableLiquidity() {
        return Math.max(0, dailyLiquidityCap - usedLiquidityToday);
    }

    public void useLiquidity(double amount) {
        this.usedLiquidityToday += amount;
    }

    public double getDailyLiquidityCap() { return dailyLiquidityCap; }
    public double getAccumulatedFees() { return accumulatedFees; }
    public double getDailyVolume() { return dailyVolume; }
    public double getDailyBuyVolume() { return dailyBuyVolume; }
    public double getDailySellVolume() { return dailySellVolume; }
}