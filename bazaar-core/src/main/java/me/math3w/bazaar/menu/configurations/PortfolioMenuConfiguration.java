package me.math3w.bazaar.menu.configurations;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.portfolio.PortfolioTransaction;
import me.math3w.bazaar.menu.DefaultConfigurableMenuItem;
import me.math3w.bazaar.menu.MenuConfiguration;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.Component;
import me.zort.containr.GUI;
import me.zort.containr.PagedContainer;
import me.zort.containr.internal.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SerializableAs("PortfolioMenuConfiguration")
public class PortfolioMenuConfiguration extends OrdersMenuConfiguration implements ConfigurationSerializable {

    public PortfolioMenuConfiguration(String name, int rows, List<DefaultConfigurableMenuItem> items) {
        super(name, rows, items);
    }

    public static PortfolioMenuConfiguration createDefaultConfiguration() {
        List<DefaultConfigurableMenuItem> items = new ArrayList<>();
        MenuConfiguration.fillWithGlass(6, items);

        items.add(new DefaultConfigurableMenuItem(49, ItemBuilder.newBuilder(Material.BARRIER).withName(ChatColor.RED + "Đóng").build(), "close"));

        items.add(new DefaultConfigurableMenuItem(50,
                ItemBuilder.newBuilder(Material.BOOK).withName(ChatColor.YELLOW + "Xem Lệnh Đang Xử Lý").build(),
                "view-pending"));

        return new PortfolioMenuConfiguration("Danh Mục Đầu Tư", 6, items);
    }

    public static PortfolioMenuConfiguration deserialize(Map<String, Object> args) {
        return new PortfolioMenuConfiguration((String) args.get("name"), (Integer) args.get("rows"), (List<DefaultConfigurableMenuItem>) args.get("items"));
    }

    public GUI getMenu(BazaarAPI bazaarApi, boolean edit) {
        return getMenuBuilder().prepare((gui, player) -> {
            super.loadItems(gui, bazaarApi, player, null, edit);
            BazaarPlugin plugin = (BazaarPlugin) bazaarApi;

            PagedContainer container = Component.pagedContainer().size(6, 4).init(c -> {
                List<PortfolioTransaction> txs = plugin.getPortfolioManager().getTransactions(player.getUniqueId());

                // Map lưu tổng số lượng cổ phiếu
                Map<String, Integer> totalAmount = new HashMap<>();
                // Map lưu số lượng đang bị khóa (chưa T+1)
                Map<String, Integer> lockedAmount = new HashMap<>();
                // Map lưu tổng chi phí vốn (Total Cost) để tính Lãi/Lỗ
                Map<String, Double> totalCostMap = new HashMap<>();

                for (PortfolioTransaction tx : txs) {
                    if (tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING) {
                        totalAmount.merge(tx.getProductId(), tx.getAmount(), Integer::sum);
                        totalCostMap.merge(tx.getProductId(), tx.getValue(), Double::sum);

                        if (!tx.isSettled()) {
                            lockedAmount.merge(tx.getProductId(), tx.getAmount(), Integer::sum);
                        }
                    }
                }

                if (totalAmount.isEmpty()) {
                    c.appendElement(Component.element(ItemBuilder.newBuilder(Material.PAPER).withName("§cPortfolio Trống").build()).build());
                    return;
                }

                for (String productId : totalAmount.keySet()) {
                    Product product = plugin.getBazaar().getProduct(productId);
                    if (product == null) continue;

                    int total = totalAmount.get(productId);
                    int locked = lockedAmount.getOrDefault(productId, 0);
                    int available = total - locked;

                    double currentPrice = plugin.getMarketTicker().getCurrentPrice(product);
                    double totalCurrentValue = currentPrice * total;

                    // Tính toán Lãi/Lỗ
                    double totalCost = totalCostMap.getOrDefault(productId, 0.0);
                    // Giá mua trung bình
                    double avgBuyPrice = (total > 0) ? (totalCost / total) : 0;

                    double profitValue = totalCurrentValue - totalCost;
                    double profitPercent = (totalCost > 0) ? ((profitValue / totalCost) * 100.0) : 0;

                    String profitString;
                    if (profitValue >= 0) {
                        profitString = "§aLãi: +" + Utils.getTextPrice(profitValue) + " (" + String.format("%.2f", profitPercent) + "%)";
                    } else {
                        profitString = "§cLỗ: " + Utils.getTextPrice(profitValue) + " (" + String.format("%.2f", profitPercent) + "%)";
                    }

                    ItemStack icon = ItemBuilder.newBuilder(product.getItem())
                            .withName(ChatColor.GREEN + product.getName())
                            .appendLore("§7--------------------")
                            .appendLore("§fTổng sở hữu: §a" + total)
                            .appendLore("§fKhả dụng: §e" + available)
                            .appendLore("§fĐang khóa (T+1): §c" + locked)
                            .appendLore("")
                            .appendLore("§7Giá thị trường: §6" + Utils.getTextPrice(currentPrice))
                            .appendLore("§7Giá vốn TB: §7" + Utils.getTextPrice(avgBuyPrice))
                            .appendLore("")
                            .appendLore(profitString)
                            .appendLore("§7--------------------")
                            .appendLore(edit ? "" : "§eBấm để xem chi tiết lệnh")
                            .build();

                    c.appendElement(Component.element().item(icon).build());
                }
            }).build();

            gui.setContainer(10, container);
        }).build();
    }
}