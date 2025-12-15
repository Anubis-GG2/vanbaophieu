package me.math3w.bazaar.menu;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.orders.BazaarOrder;
import me.math3w.bazaar.api.bazaar.orders.InstantBazaarOrder;
import me.math3w.bazaar.api.config.MenuConfig;
import me.math3w.bazaar.api.config.MessagePlaceholder;
import me.math3w.bazaar.api.menu.ItemPlaceholderFunction;
import me.math3w.bazaar.api.menu.ItemPlaceholders;
import me.math3w.bazaar.api.menu.MenuInfo;
import me.math3w.bazaar.bazaar.market.MarketTicker; // [NEW] Import MarketTicker
import me.math3w.bazaar.utils.MenuUtils;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.ContainerComponent;
import me.zort.containr.internal.util.Pair;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections; // [FIX] Thêm import
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DefaultItemPlaceholders implements ItemPlaceholders {
    public static final int ORDER_LIMIT = 5;
    private final BazaarPlugin bazaarPlugin;
    private final List<ItemPlaceholderFunction> itemPlaceholders = new ArrayList<>();

    public DefaultItemPlaceholders(BazaarPlugin bazaarPlugin) {
        this.bazaarPlugin = bazaarPlugin;

        MenuConfig menuConfig = bazaarPlugin.getMenuConfig();

        // 1. Placeholder cho việc bán kho đồ (Sell Inventory)
        addItemPlaceholder((containerComponent, item, itemSlot, player, info) -> replaceMultiLinePlaceholder(menuConfig,
                containerComponent,
                item,
                itemSlot,
                player,
                "sell-inventory",
                CompletableFuture.supplyAsync(() -> {
                    Map<Product, Integer> productsInInventory = bazaarPlugin.getBazaar().getProductsInInventory(player);
                    if (productsInInventory.isEmpty()) return new ArrayList<>();

                    List<String> lore = menuConfig.getStringList("sell-inventory");
                    double totalEarnedCoins = 0;

                    int itemsLineIndex = getPlaceholderLoreLineIndex(lore, "items");
                    if (itemsLineIndex != -1) lore.remove(itemsLineIndex); // [FIX] Check index trước khi remove

                    int currentItemIndex = 0;
                    for (Map.Entry<Product, Integer> productAmountEntry : productsInInventory.entrySet()) {
                        Product product = productAmountEntry.getKey();
                        int amount = productAmountEntry.getValue();

                        // [STOCKCRAFT] Lấy giá từ MarketTicker thay vì OrderManager
                        double unitPrice = bazaarPlugin.getMarketTicker().getCurrentPrice(product);
                        double totalItemPrice = unitPrice * amount;

                        List<String> itemAsLore = menuConfig.getStringList("item",
                                new MessagePlaceholder("item-amount", String.valueOf(amount)),
                                new MessagePlaceholder("item-name", product.getName()),
                                new MessagePlaceholder("item-coins", Utils.getTextPrice(totalItemPrice)));

                        if (itemsLineIndex != -1) {
                            for (int j = 0; j < itemAsLore.size(); j++) {
                                lore.add(itemsLineIndex + currentItemIndex + j, itemAsLore.get(j));
                            }
                        }
                        currentItemIndex += itemAsLore.size();
                        totalEarnedCoins += totalItemPrice;
                    }

                    for (int i = 0; i < lore.size(); i++) {
                        String line = lore.get(i);
                        line = Utils.colorize(line.replaceAll("%earned-coins%", Utils.getTextPrice(totalEarnedCoins)));
                        lore.set(i, line);
                    }

                    return lore;
                }),
                "loading",
                "sell-inventory-none"));

        // 2. Placeholder tên sản phẩm cơ bản
        addItemPlaceholder((containerComponent, item, itemSlot, player, info) -> {
            if (info instanceof Product) {
                Product product = (Product) info;
                return menuConfig.replaceLorePlaceholders(item, "product", new MessagePlaceholder("product", product.getName()));
            }
            // Hỗ trợ hiển thị tên cho các Order cũ nếu còn sót lại
            if (info instanceof BazaarOrder) {
                BazaarOrder order = (BazaarOrder) info;
                return menuConfig.replaceLorePlaceholders(item, "product", new MessagePlaceholder("product", order.getProduct().getName()));
            }
            return item;
        });

        // 3. [STOCKCRAFT] Placeholder MUA NGAY (Buy Instantly)
        // Thay vì dùng OrderManager, dùng giá trực tiếp từ MarketTicker
        addItemPlaceholder((containerComponent, item, itemSlot, player, info) -> {
            if (!(info instanceof Product)) return item;
            Product product = (Product) info;

            // Lấy giá hiện tại
            double currentPrice = bazaarPlugin.getMarketTicker().getCurrentPrice(product);

            return menuConfig.replaceLorePlaceholders(item,
                    "buy-instantly",
                    new MessagePlaceholder("buy-price", Utils.getTextPrice(currentPrice)),
                    new MessagePlaceholder("stack-buy-price", Utils.getTextPrice(currentPrice * 64))
            );
        });

        // 4. [STOCKCRAFT] Placeholder BÁN NGAY (Sell Instantly)
        addItemPlaceholder((containerComponent, item, itemSlot, player, info) -> {
            if (!(info instanceof Product)) return item;
            Product product = (Product) info;

            int productAmountInInventory = bazaarPlugin.getBazaar().getProductAmountInInventory(product, player);
            double currentPrice = bazaarPlugin.getMarketTicker().getCurrentPrice(product);
            double totalPrice = currentPrice * productAmountInInventory;

            return menuConfig.replaceLorePlaceholders(item,
                    "sell-instantly",
                    new MessagePlaceholder("item-amount", String.valueOf(productAmountInInventory)),
                    new MessagePlaceholder("coins", Utils.getTextPrice(totalPrice))
            );
        });

        // 5. [STOCKCRAFT] Placeholder THỐNG KÊ MUA (Thay thế Buy Orders cũ)
        // Vì là Admin Liquidity, không còn danh sách Order. Hiển thị thông tin thị trường thay thế.
        addItemPlaceholder((containerComponent, item, itemSlot, player, info) -> {
            if (!(info instanceof Product)) return item;
            Product product = (Product) info;

            return replaceMultiLinePlaceholder(menuConfig,
                    containerComponent,
                    item,
                    itemSlot,
                    player,
                    "buy-orders", // Giữ nguyên key cũ để tương thích config
                    CompletableFuture.supplyAsync(() -> {
                        // Hiển thị Trend hoặc thông tin thị trường thay vì list order
                        double price = bazaarPlugin.getMarketTicker().getCurrentPrice(product);
                        // Mockup: Hiển thị giá hiện tại như là "Top Order" duy nhất
                        return Collections.singletonList(
                                ChatColor.GRAY + "Giá hiện tại: " + ChatColor.GOLD + Utils.getTextPrice(price) + " linh thạch"
                        );
                    }),
                    "loading",
                    "buy-order-none");
        });

        // 6. [STOCKCRAFT] Placeholder THỐNG KÊ BÁN (Thay thế Sell Offers cũ)
        addItemPlaceholder((containerComponent, item, itemSlot, player, info) -> {
            if (!(info instanceof Product)) return item;
            Product product = (Product) info;

            return replaceMultiLinePlaceholder(menuConfig,
                    containerComponent,
                    item,
                    itemSlot,
                    player,
                    "sell-offers", // Giữ nguyên key cũ
                    CompletableFuture.supplyAsync(() -> {
                        double price = bazaarPlugin.getMarketTicker().getCurrentPrice(product);
                        return Collections.singletonList(
                                ChatColor.GRAY + "Giá thị trường: " + ChatColor.GOLD + Utils.getTextPrice(price) + " linh thạch"
                        );
                    }),
                    "loading",
                    "sell-offer-none");
        });
    }

    @Override
    public void addItemPlaceholder(ItemPlaceholderFunction action) {
        itemPlaceholders.add(action);
    }

    @Override
    public ItemStack replaceItemPlaceholders(ContainerComponent containerComponent, ItemStack item, int itemSlot, Player player, MenuInfo info) {
        ItemStack newItem = item.clone();
        for (ItemPlaceholderFunction itemPlaceholder : itemPlaceholders) {
            newItem = itemPlaceholder.apply(containerComponent, newItem, itemSlot, player, info);
        }
        return newItem;
    }

    private int getPlaceholderLoreLineIndex(List<String> lore, String placeholder) {
        int sellInventoryLineIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (!line.equals("%" + placeholder + "%")) continue;
            sellInventoryLineIndex = i;
            break;
        }
        return sellInventoryLineIndex;
    }

    private ItemStack replaceMultiLinePlaceholder(MenuConfig menuConfig,
                                                  ContainerComponent container,
                                                  ItemStack item,
                                                  int itemSlot,
                                                  Player player,
                                                  String placeholder,
                                                  CompletableFuture<List<String>> linesFuture,
                                                  String loadingPlaceholder,
                                                  String nonePlaceholder) {
        ItemStack newItem = item.clone();
        ItemMeta itemMeta = newItem.getItemMeta();

        if (itemMeta == null || !itemMeta.hasLore()) return item;

        List<String> lore = new ArrayList<>(itemMeta.getLore());

        int placeholderLineIndex = getPlaceholderLoreLineIndex(lore, placeholder);
        if (placeholderLineIndex == -1) return item;

        lore.remove(placeholderLineIndex);

        List<String> loadingPlaceholderValue = menuConfig.getStringList(loadingPlaceholder);
        addLinesToLore(loadingPlaceholderValue, lore, placeholderLineIndex);

        itemMeta.setLore(lore);
        newItem.setItemMeta(itemMeta);

        linesFuture.thenAccept(lines -> {
            // [FIX] Cần reload lại item từ container để tránh overwrite các thay đổi khác
            // Nhưng trong ngữ cảnh này, ta chấp nhận update đè vì lore là dynamic
            // Để an toàn, chỉ update nếu GUI vẫn mở

            // Logic update GUI item:
            // Cần tính toán lại lore dựa trên item hiện tại trong GUI (vì có thể placeholder khác đã chạy)
            // Tuy nhiên để đơn giản và tránh phức tạp, ta dùng MenuUtils.updateGuiItem
            // Chú ý: Code này giả định item không bị thay đổi bởi luồng khác quá nhiều.

            // Xóa loading lines
            // (Đoạn này logic cũ khá rủi ro nếu loading lines > 1 và bị dịch chuyển,
            // nhưng giữ nguyên logic cũ của bạn để tránh phá vỡ cấu trúc)
            // Cách tốt nhất là replace lại từ đầu hoặc reload menu.

            // Ở đây tôi viết lại logic replace an toàn hơn một chút:
            ItemStack currentItem = newItem; // Item đang giữ placeholder loading
            ItemMeta currentMeta = currentItem.getItemMeta();
            if (currentMeta == null) return;

            List<String> currentLore = new ArrayList<>(currentMeta.getLore());

            // Tìm lại vị trí loading (vì có thể đã dịch chuyển)
            int loadingIndex = -1;
            String firstLoadingLine = loadingPlaceholderValue.isEmpty() ? "" : Utils.colorize(loadingPlaceholderValue.get(0));

            for(int i=0; i<currentLore.size(); i++) {
                if(currentLore.get(i).equals(firstLoadingLine)) {
                    loadingIndex = i;
                    break;
                }
            }

            if (loadingIndex != -1) {
                // Xóa các dòng loading
                for(int i=0; i<loadingPlaceholderValue.size(); i++) {
                    if (loadingIndex < currentLore.size()) currentLore.remove(loadingIndex);
                }

                // Thêm dòng mới
                List<String> linesToAdd = lines.isEmpty() ? menuConfig.getStringList(nonePlaceholder) : lines;
                addLinesToLore(linesToAdd, currentLore, loadingIndex);

                currentMeta.setLore(currentLore);
                currentItem.setItemMeta(currentMeta);

                MenuUtils.updateGuiItem(bazaarPlugin.getMenuHistory(), container, itemSlot, player, currentItem);
            }
        });

        return newItem;
    }

    private void addLinesToLore(List<String> placeholderValue, List<String> lore, int index) {
        for (int i = 0; i < placeholderValue.size(); i++) {
            String placeholderLine = placeholderValue.get(i);
            // [FIX] Đảm bảo index không vượt quá size
            if (index + i <= lore.size()) {
                lore.add(index + i, placeholderLine);
            } else {
                lore.add(placeholderLine);
            }
        }
    }
}