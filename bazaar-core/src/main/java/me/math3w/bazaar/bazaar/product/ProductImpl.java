package me.math3w.bazaar.bazaar.product;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.ProductCategory;
import me.math3w.bazaar.api.config.MessagePlaceholder;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.ContainerComponent;
import me.zort.containr.internal.util.ItemBuilder;
import me.zort.containr.internal.util.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProductImpl implements Product {
    private final ProductCategory productCategory;
    private final ProductConfiguration config;

    public ProductImpl(ProductCategory productCategory, ProductConfiguration config) {
        this.productCategory = productCategory;
        this.config = config;
    }

    @Override
    public ItemStack getItem() { return config.getItem(); }
    @Override
    public void setItem(ItemStack item) { config.setItem(item); productCategory.getCategory().getBazaar().saveConfig(); }
    @Override
    public ItemStack getRawIcon() { return config.getIcon().clone(); }
    @Override
    public void setIcon(ItemStack icon) { config.setIcon(icon); productCategory.getCategory().getBazaar().saveConfig(); }
    @Override
    public String getName() { return Utils.colorize(config.getName()); }
    @Override
    public void setName(String name) { config.setName(name); productCategory.getCategory().getBazaar().saveConfig(); }
    @Override
    public String getId() { return getName().replace(" ", "_").toLowerCase(); }
    @Override
    public ProductCategory getProductCategory() { return productCategory; }

    @Override
    public double getCurrentPrice() {
        return 0;
    }

    @Override
    public void setCurrentPrice(double price) {

    }

    @Override
    public double getBasePrice() {
        return 0;
    }

    @Override
    public void setBasePrice(double basePrice) {

    }

    @Override
    public long getCirculatingSupply() {
        return 0;
    }

    @Override
    public void modifyCirculatingSupply(long delta) {

    }

    @Override
    public List<Double> getPriceHistory() {
        return List.of();
    }

    public ProductConfiguration getConfig() { return config; }
    private BazaarAPI getBazaarApi() { return productCategory.getCategory().getBazaar().getBazaarApi(); }

    // [FIX] Implement method còn thiếu từ Interface
    @Override
    public void addHistoryPoint(double price) {
        // Có thể để trống nếu không dùng hệ thống history cũ
        // Hoặc log lại nếu cần
    }

    @Override
    public ItemStack getIcon(ContainerComponent container, int itemSlot, Player player) {
        BazaarAPI bazaarApi = getBazaarApi();
        BazaarPlugin plugin = (BazaarPlugin) bazaarApi;
        ItemStack icon = config.getIcon().clone();

        double buyPrice = plugin.getMarketTicker().getCurrentPrice(this);
        double sellPrice = plugin.getMarketTicker().getSellPrice(this);

        return replaceLorePlaceholders(icon,
                new MessagePlaceholder("buy-price", Utils.getTextPrice(buyPrice)),
                new MessagePlaceholder("sell-price", Utils.getTextPrice(sellPrice))
        );
    }

    private ItemStack replaceLorePlaceholders(ItemStack item, MessagePlaceholder... placeholders) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return item;

        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>();

        for (String line : lore) {
            String newLine = line;
            for (MessagePlaceholder p : placeholders) {
                // [FIX] Sử dụng p.replace() thay vì p.getKey()
                newLine = p.replace(newLine);
            }
            newLore.add(newLine);
        }

        meta.setLore(newLore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public ItemStack getConfirmationIcon(double unitPrice, int amount) {
        return getBazaarApi().getMenuConfig().replaceLorePlaceholders(
                ItemBuilder.newBuilder(config.getItem()).appendLore("%confirm-lore%").build(),
                "confirm-lore",
                new MessagePlaceholder("unit-price", Utils.getTextPrice(unitPrice)),
                new MessagePlaceholder("total-price", Utils.getTextPrice(unitPrice * amount)),
                new MessagePlaceholder("amount", String.valueOf(amount)),
                new MessagePlaceholder("product", getName()));
    }

    @Override
    public CompletableFuture<Double> getLowestBuyPrice() {
        return CompletableFuture.supplyAsync(() -> ((BazaarPlugin)getBazaarApi()).getMarketTicker().getCurrentPrice(this));
    }

    @Override
    public CompletableFuture<Double> getHighestSellPrice() {
        return CompletableFuture.supplyAsync(() -> ((BazaarPlugin)getBazaarApi()).getMarketTicker().getSellPrice(this));
    }

    @Override
    public CompletableFuture<Pair<Double, Integer>> getBuyPriceWithOrderableAmount(int amount) {
        return getLowestBuyPrice().thenApply(price -> new Pair<>(price * amount, amount));
    }

    @Override
    public CompletableFuture<Pair<Double, Integer>> getSellPriceWithOrderableAmount(int amount) {
        return getHighestSellPrice().thenApply(price -> new Pair<>(price * amount, amount));
    }
}