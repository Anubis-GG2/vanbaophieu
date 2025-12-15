package me.math3w.bazaar.menu.configurations;

import com.cryptomorin.xseries.XMaterial;
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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PendingOrdersMenuConfiguration extends MenuConfiguration implements ConfigurationSerializable {

    public PendingOrdersMenuConfiguration(String name, int rows, List<DefaultConfigurableMenuItem> items) {
        super(name, rows, items);
    }

    public static PendingOrdersMenuConfiguration createDefaultConfiguration() {
        List<DefaultConfigurableMenuItem> items = new ArrayList<>();
        MenuConfiguration.fillWithGlass(6, items);

        items.add(new DefaultConfigurableMenuItem(49,
                ItemBuilder.newBuilder(Material.BARRIER).withName(ChatColor.RED + "Đóng").build(), "close"));
        items.add(new DefaultConfigurableMenuItem(48,
                ItemBuilder.newBuilder(Material.ARROW).withName(ChatColor.GREEN + "Quay lại").build(), "back"));

        return new PendingOrdersMenuConfiguration("Lệnh Đang Xử Lý", 6, items);
    }

    public static PendingOrdersMenuConfiguration deserialize(Map<String, Object> args) {
        return new PendingOrdersMenuConfiguration((String) args.get("name"), (Integer) args.get("rows"), (List<DefaultConfigurableMenuItem>) args.get("items"));
    }

    // [FIX] Xóa @Override
    public GUI getMenu(BazaarAPI bazaarApi, boolean edit) {
        return getMenuBuilder().prepare((gui, player) -> {
            super.loadItems(gui, bazaarApi, player, null, edit);
            BazaarPlugin plugin = (BazaarPlugin) bazaarApi;

            PagedContainer container = Component.pagedContainer().size(6, 4).init(c -> {
                List<PortfolioTransaction> txs = plugin.getPortfolioManager().getTransactions(player.getUniqueId());

                if (txs.isEmpty()) {
                    c.appendElement(Component.element(ItemBuilder.newBuilder(Material.PAPER).withName("§cKhông có lệnh nào").build()).build());
                    return;
                }

                for (PortfolioTransaction tx : txs) {
                    Product product = plugin.getBazaar().getProduct(tx.getProductId());
                    if (product == null) continue;

                    if (tx.getType() == PortfolioTransaction.TransactionType.SELL_PENDING) {
                        boolean canClaim = tx.isSettled();
                        ItemStack icon = ItemBuilder.newBuilder(XMaterial.GOLD_NUGGET.parseItem())
                                .withName("§6Lệnh Bán: " + product.getName())
                                .appendLore("§7Giá trị: §6" + Utils.getTextPrice(tx.getValue()))
                                .appendLore("§7Trạng thái: " + (canClaim ? "§aĐÃ THANH KHOẢN" : "§eĐANG CHỜ"))
                                .appendLore("")
                                .appendLore(canClaim ? "§eBấm để RÚT TIỀN ngay!" : "§7Mở khóa trong: §f" + formatTime(tx.getTimeRemaining()))
                                .build();

                        c.appendElement(Component.element().click(info -> {
                            if (canClaim) {
                                plugin.getPortfolioManager().claimMoney(player, tx);
                                gui.close(player);
                            }
                        }).item(icon).build());
                    } else if (tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING && !tx.isSettled()) {
                        ItemStack icon = ItemBuilder.newBuilder(XMaterial.CLOCK.parseItem())
                                .withName("§bLệnh Mua: " + product.getName())
                                .appendLore("§7Số lượng: §f" + tx.getAmount())
                                .appendLore("§7Trạng thái: §cĐANG KHÓA")
                                .appendLore("")
                                .appendLore("§7Mở khóa trong: §f" + formatTime(tx.getTimeRemaining()))
                                .build();

                        c.appendElement(Component.element().item(icon).build());
                    }
                }
            }).build();

            gui.setContainer(10, container);
        }).build();
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        return String.format("%02d:%02d", minutes, seconds % 60);
    }
}