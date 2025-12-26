package me.math3w.bazaar.api.bazaar;

import me.math3w.bazaar.api.menu.MenuInfo;
import me.zort.containr.ContainerComponent;
import me.zort.containr.internal.util.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Product extends MenuInfo {
    ItemStack getItem();
    void setItem(ItemStack item);
    ItemStack getIcon(ContainerComponent container, int itemSlot, Player player);
    ItemStack getRawIcon();
    void setIcon(ItemStack icon);

    // [THÊM]
    List<String> getLore();
    void setLore(List<String> lore);

    ItemStack getConfirmationIcon(double unitPrice, int amount);
    String getName();
    void setName(String name);
    String getId();
    ProductCategory getProductCategory();
    double getCurrentPrice();
    void setCurrentPrice(double price);
    double getBasePrice();
    void setBasePrice(double basePrice);
    long getCirculatingSupply();
    void modifyCirculatingSupply(long delta);
    List<Double> getPriceHistory();
    void addHistoryPoint(double price);

    List<StockCandle> getCandleHistory();
    void addCandle(StockCandle candle);

    @Deprecated
    CompletableFuture<Double> getLowestBuyPrice();
    @Deprecated
    CompletableFuture<Double> getHighestSellPrice();
    @Deprecated
    CompletableFuture<Pair<Double, Integer>> getBuyPriceWithOrderableAmount(int amount);
    @Deprecated
    CompletableFuture<Pair<Double, Integer>> getSellPriceWithOrderableAmount(int amount);
}