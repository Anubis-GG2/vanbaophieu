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
                        double totalValue = tx.getValue();
                        double settled = tx.getSettledValue();
                        double claimed = tx.getClaimedValue();
                        double claimable = settled - claimed;
                        double percentSettled = (totalValue > 0) ? (settled / totalValue * 100) : 0;
                        String progressBar = getProgressBar(settled, totalValue);

                        ItemStack icon = ItemBuilder.newBuilder(XMaterial.GOLD_NUGGET.parseItem())
                                .withName("§6Lệnh Bán: " + product.getName())
                                .appendLore("§7--------------------")
                                .appendLore("§fTổng giá trị: §6" + Utils.getTextPrice(totalValue))
                                .appendLore("§fĐã duyệt chi: §e" + Utils.getTextPrice(settled) + " §7(" + String.format("%.1f", percentSettled) + "%)")
                                .appendLore("§fĐã rút: §a" + Utils.getTextPrice(claimed))
                                .appendLore("")
                                .appendLore("§7Tiến độ Quỹ: " + progressBar)
                                .appendLore("")
                                .appendLore(claimable > 0 ? "§a§lCÓ THỂ RÚT: " + Utils.getTextPrice(claimable) : "§7Đang chờ Quỹ cấp vốn...")
                                .appendLore("§7--------------------")
                                .appendLore(claimable > 0 ? "§eBấm để RÚT TIỀN ngay!" : "§7Vui lòng chờ thêm...")
                                .build();

                        c.appendElement(Component.element().click(info -> {
                            if (claimable > 0) {
                                plugin.getPortfolioManager().claimMoney(player, tx);
                                gui.update(player);
                            }
                        }).item(icon).build());

                    }
                    else if (tx.getType() == PortfolioTransaction.TransactionType.BUY_HOLDING && !tx.isSettled()) {
                        ItemStack icon = ItemBuilder.newBuilder(XMaterial.CLOCK.parseItem())
                                .withName("§bLệnh Mua: " + product.getName())
                                .appendLore("§7Số lượng: §f" + tx.getAmount())
                                .appendLore("§7Trạng thái: §cĐANG KHÓA (T+1)")
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

    private String getProgressBar(double current, double max) {
        int totalBars = 20;
        int filledBars = (int) ((current / max) * totalBars);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == filledBars) sb.append("§7");
            sb.append("|");
        }
        return sb.toString();
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02dh %02dm", hours, minutes % 60);
    }
}