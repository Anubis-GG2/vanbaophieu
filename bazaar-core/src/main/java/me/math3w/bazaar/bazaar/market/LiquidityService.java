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
    private double dailyVolume = 0.0;

    public LiquidityService(BazaarPlugin plugin) {
        this.plugin = plugin;
        this.accumulatedFees = plugin.getConfig().getDouble("data.accumulated-fees", 0.0);
        this.dailyVolume = plugin.getConfig().getDouble("data.daily-volume", 0.0);
        recalculateDailyCap();

        new BukkitRunnable() {
            @Override
            public void run() {
                recalculateDailyCap();
            }
        }.runTaskTimer(plugin, 1728000L, 1728000L);
    }

    public void addFee(double amount) {
        this.accumulatedFees += amount;
        saveData();
    }

    public void recordVolume(double amount) {
        this.dailyVolume += amount;
        saveData();
    }

    private void saveData() {
        plugin.getConfig().set("data.accumulated-fees", accumulatedFees);
        plugin.getConfig().set("data.daily-volume", dailyVolume);
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
        plugin.getLogger().info(" - Accumulated Fees: " + Utils.getTextPrice(this.accumulatedFees));
        plugin.getLogger().info(" -> NEW DAILY CAP: " + Utils.getTextPrice(dailyLiquidityCap));
        this.usedLiquidityToday = 0.0;
        this.accumulatedFees = 0.0;
        this.dailyVolume = 0.0;
        saveData();
    }

    private double calculateMarketCap() {
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
}