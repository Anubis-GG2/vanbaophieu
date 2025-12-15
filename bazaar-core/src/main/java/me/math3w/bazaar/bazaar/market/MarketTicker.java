package me.math3w.bazaar.bazaar.market;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.portfolio.PortfolioManager;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MarketTicker {
    private final BazaarPlugin plugin;
    // Map lưu giá hiện tại trong RAM
    private final Map<String, Double> currentPrices = new HashMap<>();
    private BukkitRunnable task;

    // Spread: Chênh lệch giá mua/bán (1%)
    private static final double SPREAD_PERCENTAGE = 0.01;

    public MarketTicker(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // 1. [NEW] Load giá từ Database trước khi thị trường chạy
        // Chúng ta lấy instance Database từ PortfolioManager (hoặc tạo getter riêng trong BazaarPlugin)
        // Giả sử PortfolioManager đã giữ instance của DB
        if (plugin.getPortfolioManager() != null) {
            Map<String, Double> savedPrices = plugin.getPortfolioManager().getDatabase().loadAllStockPrices();
            currentPrices.putAll(savedPrices);
            plugin.getLogger().info("Đã khôi phục giá của " + savedPrices.size() + " mã cổ phiếu từ Database.");
        }

        // 2. Chạy task biến động giá
        task = new BukkitRunnable() {
            @Override
            public void run() {
                updateMarket();
            }
        };
        // 60s update 1 lần (20 ticks * 60)
        task.runTaskTimer(plugin, 20L, 1200L);
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        // 3. [NEW] Lưu toàn bộ giá hiện tại vào Database khi tắt
        if (plugin.getPortfolioManager() != null && !currentPrices.isEmpty()) {
            plugin.getPortfolioManager().getDatabase().saveStockPrices(currentPrices);
            plugin.getLogger().info("Đã lưu giá thị trường vào Database.");
        }
    }

    /**
     * Lấy giá thị trường hiện tại (Giá MUA)
     */
    public double getCurrentPrice(Product product) {
        // Nếu đã có trong RAM (do load từ DB hoặc vừa update), trả về ngay
        if (currentPrices.containsKey(product.getId())) {
            return currentPrices.get(product.getId());
        }

        // Nếu chưa có (Stock mới thêm), lấy giá gốc từ Config
        if (product instanceof ProductImpl) {
            double basePrice = ((ProductImpl) product).getConfig().getPrice();
            currentPrices.put(product.getId(), basePrice);
            return basePrice;
        }

        return 100.0; // Fallback
    }

    /**
     * Lấy giá thanh khoản (Giá BÁN) - Đã trừ Spread
     */
    public double getSellPrice(Product product) {
        double marketPrice = getCurrentPrice(product);
        return marketPrice * (1.0 - SPREAD_PERCENTAGE);
    }

    public void setManualPrice(Product product, double newPrice) {
        currentPrices.put(product.getId(), newPrice);
        // Lưu ngay vào DB để an toàn
        // (Tùy chọn, nhưng tốt nhất cứ để stop() lưu 1 thể cho đỡ lag nếu spam lệnh)
    }

    private void updateMarket() {
        for (String productId : currentPrices.keySet()) {
            double currentPrice = currentPrices.get(productId);

            // Biến động ngẫu nhiên -5% đến +5%
            double percentChange = ThreadLocalRandom.current().nextDouble(-0.05, 0.05);
            double newPrice = currentPrice * (1 + percentChange);

            // Giới hạn giá sàn là 0.1 để không bị âm hoặc bằng 0
            if (newPrice < 0.1) newPrice = 0.1;

            currentPrices.put(productId, newPrice);
        }

        // [Optimization] Có thể autosave mỗi 5-10 phút ở đây nếu muốn
    }
}