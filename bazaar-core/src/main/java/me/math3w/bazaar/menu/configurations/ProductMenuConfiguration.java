package me.math3w.bazaar.menu.configurations;

import com.cryptomorin.xseries.XMaterial;
import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.config.MessagePlaceholder;
import me.math3w.bazaar.menu.DefaultConfigurableMenuItem;
import me.math3w.bazaar.menu.MenuConfiguration;
import me.math3w.bazaar.utils.DiscordService;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.Component;
import me.zort.containr.ContextClickInfo;
import me.zort.containr.GUI;
import me.zort.containr.internal.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SerializableAs("ProductMenuConfiguration")
public class ProductMenuConfiguration extends MenuConfiguration implements ConfigurationSerializable {
    private final int productSlot;

    public ProductMenuConfiguration(String name, int rows, List<DefaultConfigurableMenuItem> items, int productSlot) {
        super(name, rows, items);
        this.productSlot = productSlot;
    }

    public static ProductMenuConfiguration createDefaultProductConfiguration() {
        List<DefaultConfigurableMenuItem> items = new ArrayList<>();

        items.add(new DefaultConfigurableMenuItem(10,
                ItemBuilder.newBuilder(XMaterial.GOLD_INGOT.parseItem())
                        .withName(ChatColor.GREEN + "Mua Cổ Phiếu")
                        .appendLore(ChatColor.GRAY + "Giá mua: " + ChatColor.GOLD + "%buy-price%")
                        .appendLore("")
                        .appendLore(ChatColor.YELLOW + "Bấm Để Mua!")
                        .build(),
                "buy-instantly"));

        items.add(new DefaultConfigurableMenuItem(16,
                ItemBuilder.newBuilder(XMaterial.IRON_INGOT.parseItem())
                        .withName(ChatColor.RED + "Bán Cổ Phiếu")
                        .appendLore(ChatColor.GRAY + "Tổng Cổ Phiếu đang sở hữu: " + ChatColor.GREEN + "%item-amount%")
                        .appendLore(ChatColor.GRAY + "Tổng giá trị: " + ChatColor.GOLD + "%coins%")
                        .appendLore("")
                        .appendLore(ChatColor.YELLOW + "Bấm Chuột Trái: Bán Tất Cả")
                        .appendLore(ChatColor.AQUA + "Bấm Chuột Phải: Bán Theo Số lượng")
                        .build(),
                "sell-instantly"));

        items.add(new DefaultConfigurableMenuItem(32,
                ItemBuilder.newBuilder(Material.CHEST)
                        .withName(ChatColor.GREEN + "Danh mục đầu tư của bạn")
                        .appendLore(ChatColor.GRAY + "Xem các cổ phiếu của bạn")
                        .appendLore("")
                        .appendLore(ChatColor.YELLOW + "Bấm để quản lí các cổ phiếu!")
                        .build(),
                "manage-orders"));

        items.add(new DefaultConfigurableMenuItem(30, ItemBuilder.newBuilder(Material.ARROW).withName(ChatColor.GREEN + "Quay lại").build(), "back"));
        items.add(new DefaultConfigurableMenuItem(31, ItemBuilder.newBuilder(Material.BARRIER).withName(ChatColor.RED + "Đóng Sàn Giao Dịch").build(), "close"));

        int rows = 4;
        fillWithGlass(rows, items);

        return new ProductMenuConfiguration("%product%", rows, items, 13);
    }

    public static ProductMenuConfiguration deserialize(Map<String, Object> args) {
        return new ProductMenuConfiguration((String) args.get("name"), (Integer) args.get("rows"), (List<DefaultConfigurableMenuItem>) args.get("items"), (int) args.get("slot"));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> args = super.serialize();
        args.put("slot", productSlot);
        return args;
    }

    public GUI getMenu(Product product, boolean edit) {
        BazaarAPI bazaarApi = product.getProductCategory().getCategory().getBazaar().getBazaarApi();
        BazaarPlugin plugin = (BazaarPlugin) bazaarApi;

        return getMenuBuilder().title(name.replace("%product%", product.getName())).prepare((gui, player) -> {
            super.loadItems(gui, bazaarApi, player, product, edit);

            overrideItem(gui, plugin, player, product, 10, item -> {
                double buyPrice = plugin.getMarketTicker().getCurrentPrice(product);
                return replacePlaceholders(item,
                        new MessagePlaceholder("buy-price", Utils.getTextPrice(buyPrice))
                );
            });

            overrideItem(gui, plugin, player, product, 16, item -> {
                int amount = plugin.getPortfolioManager().getAvailableStockAmount(player.getUniqueId(), product);
                double sellPrice = plugin.getMarketTicker().getSellPrice(product);
                double totalValue = sellPrice * amount;

                return replacePlaceholders(item,
                        new MessagePlaceholder("item-amount", String.valueOf(amount)),
                        new MessagePlaceholder("coins", Utils.getTextPrice(totalValue))
                );
            });

            // [FIX] Sử dụng phương thức an toàn để bảo toàn Model Data
            ItemStack icon = product.getIcon(gui, productSlot, player);
            ItemStack clickableIcon = addClickLore(icon);

            gui.setElement(productSlot, Component.element()
                    .click(clickInfo -> {
                    })
                    .item(clickableIcon)
                    .build());

        }).build();
    }

    // [FIX] Sử dụng Bukkit API thuần để không làm hỏng CustomModelData
    private ItemStack addClickLore(ItemStack item) {
        if (item == null) return null;
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.AQUA + "▶ Bấm để chọn khung thời gian biểu đồ");
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private void overrideItem(GUI gui, BazaarPlugin plugin, org.bukkit.entity.Player player, Product product, int slot, java.util.function.Function<ItemStack, ItemStack> replacer) {
        DefaultConfigurableMenuItem menuItem = items.stream()
                .filter(i -> i.getSlot() == slot)
                .findFirst()
                .orElse(null);

        if (menuItem != null) {
            ItemStack original = menuItem.getItem().clone();
            ItemStack newItem = replacer.apply(original);

            Consumer<ContextClickInfo> action = plugin.getClickActionManager().getClickAction(menuItem, product, false);

            if (action == null) {
                action = (info) -> {};
            }

            gui.setElement(slot, Component.element()
                    .click(action)
                    .item(newItem)
                    .build());
        }
    }

    private ItemStack replacePlaceholders(ItemStack item, MessagePlaceholder... placeholders) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            List<String> newLore = new ArrayList<>();
            for (String line : lore) {
                String newLine = line;
                for (MessagePlaceholder p : placeholders) {
                    newLine = p.replace(newLine);
                }
                newLore.add(newLine);
            }
            meta.setLore(newLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}