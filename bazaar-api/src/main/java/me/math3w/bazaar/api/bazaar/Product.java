package me.math3w.bazaar.api.bazaar;

import me.math3w.bazaar.api.menu.MenuInfo;
import me.zort.containr.ContainerComponent;
import me.zort.containr.internal.util.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Product extends MenuInfo {
    // --- Visual & Metadata ---
    ItemStack getItem();
    void setItem(ItemStack item);
    ItemStack getIcon(ContainerComponent container, int itemSlot, Player player);
    ItemStack getRawIcon();
    void setIcon(ItemStack icon);
    ItemStack getConfirmationIcon(double unitPrice, int amount);
    String getName();
    void setName(String name);
    String getId();
    ProductCategory getProductCategory();

    // --- Financial Logic ---
    double getCurrentPrice();
    void setCurrentPrice(double price);
    double getBasePrice();
    void setBasePrice(double basePrice);
    long getCirculatingSupply();
    void modifyCirculatingSupply(long delta);

    // --- NEW: Chart Data ---
    /**
     * Lấy danh sách lịch sử giá để vẽ biểu đồ.
     * Index 0 là cũ nhất, Index cuối là mới nhất.
     */
    List<Double> getPriceHistory();

    /**
     * Thêm giá mới vào lịch sử (gọi mỗi khi MarketTicker cập nhật giá).
     */
    void addHistoryPoint(double price);

    // --- Deprecated P2P Logic ---
    @Deprecated
    CompletableFuture<Double> getLowestBuyPrice();
    @Deprecated
    CompletableFuture<Double> getHighestSellPrice();
    @Deprecated
    CompletableFuture<Pair<Double, Integer>> getBuyPriceWithOrderableAmount(int amount);
    @Deprecated
    CompletableFuture<Pair<Double, Integer>> getSellPriceWithOrderableAmount(int amount);
}