package me.math3w.bazaar.bazaar.market;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.StockCandle;
import me.math3w.bazaar.api.bazaar.orders.OrderType;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MarketTicker {
    private final BazaarPlugin plugin;
    private final Map<String, Double> currentPrices = new HashMap<>();
    private final Map<String, Double> tickBuyVolume = new HashMap<>();
    private final Map<String, Double> tickSellVolume = new HashMap<>();
    private final Map<String, ManipulationTask> activeManipulations = new HashMap<>();
    private BukkitRunnable task;

    private static final double SELL_MULTIPLIER = 0.95;
    private static final double LIQUIDITY_DIVISOR = 2500000.0;

    public MarketTicker(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (plugin.getPortfolioManager() != null) {
            Map<String, Double> savedPrices = plugin.getPortfolioManager().getDatabase().loadAllStockPrices();
            currentPrices.putAll(savedPrices);
            plugin.getLogger().info("Prices loaded from database.");
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                updateMarket();
            }
        };
        task.runTaskTimer(plugin, 20L, 1200L);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        if (plugin.getPortfolioManager() != null && !currentPrices.isEmpty()) {
            plugin.getPortfolioManager().getDatabase().saveStockPrices(currentPrices);
        }
    }

    public double getCurrentPrice(Product product) {
        if (currentPrices.containsKey(product.getId())) {
            return currentPrices.get(product.getId());
        }

        if (product instanceof ProductImpl) {
            double basePrice = ((ProductImpl) product).getConfig().getPrice();
            currentPrices.put(product.getId(), basePrice);
            return basePrice;
        }

        return 100.0;
    }

    public double getSellPrice(Product product) {
        double marketPrice = getCurrentPrice(product);
        return marketPrice * SELL_MULTIPLIER;
    }

    public void setManualPrice(Product product, double newPrice) {
        currentPrices.put(product.getId(), newPrice);
    }

    public void addVolume(Product product, double amountValue, OrderType type) {
        String id = product.getId();
        if (type == OrderType.BUY) {
            tickBuyVolume.merge(id, amountValue, Double::sum);
        } else {
            tickSellVolume.merge(id, amountValue, Double::sum);
        }
    }

    public void registerManipulation(String targetId, boolean isCategory, double percentChange, int hoursDuration) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (hoursDuration * 3600000L);
        double totalMultiplier = 1.0 + (percentChange / 100.0);

        ManipulationTask task = new ManipulationTask(isCategory, totalMultiplier, startTime, endTime);
        activeManipulations.put(targetId, task);
    }

    private void updateMarket() {
        long timestamp = System.currentTimeMillis();

        for (Product product : plugin.getBazaar().getProducts()) {
            String id = product.getId();
            double oldPrice = getCurrentPrice(product);

            double buyVol = tickBuyVolume.getOrDefault(id, 0.0);
            double sellVol = tickSellVolume.getOrDefault(id, 0.0);

            double supplyDemandFactor = (buyVol - sellVol) / LIQUIDITY_DIVISOR;

            double manipulationBias = 0.0;
            String categoryName = product.getProductCategory().getCategory().getName();

            ManipulationTask task = null;
            if (activeManipulations.containsKey(id)) {
                task = activeManipulations.get(id);
            } else if (activeManipulations.containsKey(categoryName)) {
                task = activeManipulations.get(categoryName);
            }

            if (task != null) {
                long timeLeft = task.endTime - System.currentTimeMillis();
                if (timeLeft > 0) {
                    double targetPercent = task.targetMultiplier - 1.0;
                    double totalTicks = (task.endTime - task.startTime) / 60000.0;
                    manipulationBias = targetPercent / totalTicks;
                }
            }

            double noise = ThreadLocalRandom.current().nextDouble(-0.001, 0.001);
            double totalFactor = 1.0 + supplyDemandFactor + manipulationBias + noise;

            double newPrice = oldPrice * totalFactor;

            if (newPrice < 0.1) newPrice = 0.1;

            currentPrices.put(id, newPrice);
            product.addHistoryPoint(newPrice);

            double open = oldPrice;
            double close = newPrice;
            double high = Math.max(open, close);
            double low = Math.min(open, close);
            double volume = buyVol + sellVol;

            if (volume > 0) {
                double volatility = (high - low) * 0.1;
                high += volatility;
                low -= volatility;
            }

            StockCandle candle = new StockCandle(timestamp, open, high, low, close, volume);
            product.addCandle(candle);
        }

        tickBuyVolume.clear();
        tickSellVolume.clear();

        Iterator<Map.Entry<String, ManipulationTask>> it = activeManipulations.entrySet().iterator();
        while (it.hasNext()) {
            if (System.currentTimeMillis() >= it.next().getValue().endTime) {
                it.remove();
            }
        }
    }

    private static class ManipulationTask {
        boolean isCategory;
        double targetMultiplier;
        long startTime;
        long endTime;

        public ManipulationTask(boolean isCategory, double targetMultiplier, long startTime, long endTime) {
            this.isCategory = isCategory;
            this.targetMultiplier = targetMultiplier;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}