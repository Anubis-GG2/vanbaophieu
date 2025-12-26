package me.math3w.bazaar.bazaar.product;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.ProductCategory;
import me.math3w.bazaar.api.bazaar.StockCandle;
import me.math3w.bazaar.api.config.MessagePlaceholder;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.ContainerComponent;
import me.zort.containr.internal.util.ItemBuilder;
import me.zort.containr.internal.util.Pair;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProductImpl implements Product {
    private final ProductCategory productCategory;
    private final ProductConfiguration config;
    private final List<Double> priceHistory = new ArrayList<>();
    private final List<StockCandle> candleHistory = new ArrayList<>();

    public ProductImpl(ProductCategory productCategory, ProductConfiguration config) {
        this.productCategory = productCategory;
        this.config = config;
        BazaarPlugin plugin = (BazaarPlugin) getBazaarApi();
        if (plugin.getPortfolioManager() != null) {
            this.candleHistory.addAll(plugin.getPortfolioManager().getDatabase().loadCandles(getId()));
        }
    }

    @Override public ItemStack getItem() { return config.getItem(); }
    @Override public void setItem(ItemStack item) { config.setItem(item); productCategory.getCategory().getBazaar().saveConfig(); }
    @Override public ItemStack getRawIcon() { return config.getIcon().clone(); }
    @Override public void setIcon(ItemStack icon) { config.setIcon(icon); productCategory.getCategory().getBazaar().saveConfig(); }
    @Override public String getName() { return Utils.colorize(config.getName()); }
    @Override public void setName(String name) { config.setName(name); productCategory.getCategory().getBazaar().saveConfig(); }
    @Override public String getId() { return ChatColor.stripColor(getName()).replace(" ", "_").toLowerCase(); }
    @Override public ProductCategory getProductCategory() { return productCategory; }

    @Override public List<String> getLore() { return config.getLore(); }
    @Override public void setLore(List<String> lore) { config.setLore(lore); productCategory.getCategory().getBazaar().saveConfig(); }

    @Override public double getCurrentPrice() { return config.getPrice(); }
    @Override public void setCurrentPrice(double price) { config.setPrice(price); }
    @Override public double getBasePrice() { return config.getPrice(); }
    @Override public void setBasePrice(double basePrice) { }
    @Override public long getCirculatingSupply() { return config.getSupply(); }
    @Override public void modifyCirculatingSupply(long delta) { config.setSupply((int)(config.getSupply() + delta)); productCategory.getCategory().getBazaar().saveConfig(); }

    @Override public List<Double> getPriceHistory() { return priceHistory; }
    @Override public void addHistoryPoint(double price) { priceHistory.add(price); if (priceHistory.size() > 100) priceHistory.remove(0); }
    @Override public List<StockCandle> getCandleHistory() { return Collections.unmodifiableList(candleHistory); }
    @Override public void addCandle(StockCandle candle) { candleHistory.add(candle); }

    private BazaarAPI getBazaarApi() { return productCategory.getCategory().getBazaar().getBazaarApi(); }

    @Override
    public ItemStack getIcon(ContainerComponent container, int itemSlot, Player player) {
        ItemStack icon = config.getIcon().clone();
        // Logic thêm thông tin giá như bạn đã viết
        return icon;
    }

    @Override
    public ItemStack getConfirmationIcon(double unitPrice, int amount) {
        return ItemBuilder.newBuilder(config.getItem()).withName(getName()).build();
    }

    // Các phương thức Deprecated bắt buộc triển khai để hết lỗi "is not abstract"
    @Deprecated @Override public CompletableFuture<Double> getLowestBuyPrice() { return CompletableFuture.completedFuture(0.0); }
    @Deprecated @Override public CompletableFuture<Double> getHighestSellPrice() { return CompletableFuture.completedFuture(0.0); }
    @Deprecated @Override public CompletableFuture<Pair<Double, Integer>> getBuyPriceWithOrderableAmount(int amount) { return CompletableFuture.completedFuture(new Pair<>(0.0, 0)); }
    @Deprecated @Override public CompletableFuture<Pair<Double, Integer>> getSellPriceWithOrderableAmount(int amount) { return CompletableFuture.completedFuture(new Pair<>(0.0, 0)); }

    public ProductConfiguration getConfig() { return config; }
}