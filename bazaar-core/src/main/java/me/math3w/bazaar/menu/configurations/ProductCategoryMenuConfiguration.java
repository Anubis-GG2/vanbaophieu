package me.math3w.bazaar.menu.configurations;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.ProductCategory;
import me.math3w.bazaar.api.edit.EditManager;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import me.math3w.bazaar.menu.DefaultConfigurableMenuItem;
import me.math3w.bazaar.menu.MenuConfiguration;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import me.zort.containr.internal.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@SerializableAs("ProductCategoryMenuConfiguration")
public class ProductCategoryMenuConfiguration extends MenuConfiguration implements ConfigurationSerializable {
    private final List<Integer> productSlots;

    public ProductCategoryMenuConfiguration(String name, int rows, List<DefaultConfigurableMenuItem> items, List<Integer> productSlots) {
        super(name, rows, items);
        this.productSlots = productSlots;
    }

    public static ProductCategoryMenuConfiguration createDefaultConfiguration() {
        return createDefaultProductCategoryConfiguration("%category%", 7);
    }

    public static ProductCategoryMenuConfiguration createDefaultProductCategoryConfiguration(String name, int productsCount) {
        List<DefaultConfigurableMenuItem> items = new ArrayList<>();
        items.add(new DefaultConfigurableMenuItem(48, ItemBuilder.newBuilder(Material.ARROW).withName(ChatColor.GREEN + "Quay Lại").appendLore(ChatColor.GRAY + "đến Vạn Bảo Phiếu").build(), "back"));
        items.add(new DefaultConfigurableMenuItem(49, ItemBuilder.newBuilder(Material.BARRIER).withName(ChatColor.RED + "Đóng").build(), "close"));
        items.add(new DefaultConfigurableMenuItem(50, ItemBuilder.newBuilder(Material.BOOK).withName(ChatColor.GREEN + "Quản lí Đơn hàng").appendLore(ChatColor.YELLOW + "Bấm để quản lí!").build(), "manage-orders"));

        int rows = 6;
        List<Integer> productSlots = new ArrayList<>();
        int[] defaultSlots = {10, 11, 12, 13, 14, 15, 16};
        for (int slot : defaultSlots) productSlots.add(slot);
        fillWithGlass(rows, items);
        return new ProductCategoryMenuConfiguration(name, rows, items, productSlots);
    }

    public void addSlotForNewProduct() {
        int[] possibleSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int slot : possibleSlots) {
            if (!productSlots.contains(slot)) {
                productSlots.add(slot);
                return;
            }
        }
    }

    public List<Integer> getProductSlots() { return productSlots; }

    public static ProductCategoryMenuConfiguration deserialize(Map<String, Object> args) {
        return new ProductCategoryMenuConfiguration((String) args.get("name"), (Integer) args.get("rows"), (List<DefaultConfigurableMenuItem>) args.get("items"), (List<Integer>) args.get("slots"));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> args = super.serialize();
        args.put("slots", productSlots);
        return args;
    }

    public GUI getMenu(ProductCategory selectedCategory, boolean edit) {
        BazaarAPI bazaarApi = selectedCategory.getCategory().getBazaar().getBazaarApi();
        BazaarPlugin plugin = (BazaarPlugin) bazaarApi;
        EditManager editManager = bazaarApi.getEditManager();

        return getMenuBuilder().title(name.replace("%category%", selectedCategory.getCategory().getName())).prepare((gui, player) -> {
            super.loadItems(gui, bazaarApi, player, selectedCategory, edit);

            if (edit) {
                for (Map.Entry<Integer, Element> slotElementEntry : gui.content(null).entrySet()) {
                    int slot = slotElementEntry.getKey();
                    Element element = slotElementEntry.getValue();
                    ItemStack originalItem = element.item(player);
                    if (originalItem == null) continue;

                    if (originalItem.getType().name().contains("GLASS")) {
                        ItemStack item = ItemBuilder.newBuilder(originalItem)
                                .appendLore(ChatColor.DARK_AQUA + "Bấm chuột giữa để THÊM MỚI")
                                .build();

                        gui.setElement(slot, Component.element(item).click(clickInfo -> {
                            if (clickInfo.getClickType() == ClickType.MIDDLE) {
                                ItemStack cursor = player.getItemOnCursor();
                                ItemStack iconToUse;
                                String nameToUse;

                                if (cursor != null && cursor.getType() != Material.AIR) {
                                    iconToUse = cursor.clone();
                                    iconToUse.setAmount(1);
                                    // [FIX LORE] Xóa sạch lore rác khi tạo mới
                                    ItemMeta meta = iconToUse.getItemMeta();
                                    if (meta != null) {
                                        meta.setLore(new ArrayList<>());
                                        iconToUse.setItemMeta(meta);
                                    }
                                    nameToUse = Utils.colorize(cursor.getItemMeta() != null && cursor.getItemMeta().hasDisplayName() ? cursor.getItemMeta().getDisplayName() : cursor.getType().name());
                                } else {
                                    iconToUse = ItemBuilder.newBuilder(Material.COAL).withName(ChatColor.RED + "Not set!").build();
                                    nameToUse = ChatColor.RED + "Not set!";
                                }

                                ProductConfiguration productConfiguration = new ProductConfiguration(iconToUse, nameToUse);
                                ProductImpl product = new ProductImpl(selectedCategory, productConfiguration);
                                if (!productSlots.contains(slot)) productSlots.add(slot);
                                selectedCategory.addProduct(product);
                                editManager.openProductEdit(player, product);
                                return;
                            }
                            element.click(clickInfo);
                        }).build());
                    }
                }
            }

            List<Product> products = selectedCategory.getProducts();
            for (int i = 0; i < productSlots.size(); i++) {
                if (i >= products.size()) break;
                int slot = productSlots.get(i);
                Product product = products.get(i);
                int finalI = i;

                ItemStack icon;
                if (edit) {
                    icon = product.getIcon(gui, slot, player).clone();
                    ItemMeta meta = icon.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add("");
                        lore.add("§c§l[EDIT MODE]");
                        lore.add("§eShift + Chuột Phải: §cXÓA Ngay");
                        lore.add("§eClick Trái: §aChỉnh sửa chi tiết");
                        meta.setLore(lore);
                        icon.setItemMeta(meta);
                    }
                } else {
                    icon = product.getIcon(gui, slot, player);
                }

                gui.setElement(slot, Component.element()
                        .click(clickInfo -> {
                            if (edit) {
                                // [FIX XÓA] Tự xử lý xóa thay vì dùng EditManager cũ
                                if (clickInfo.getClickType().isShiftClick() && clickInfo.getClickType().isRightClick()) {
                                    plugin.getBazaarConfig().removeProduct(product.getId());
                                    selectedCategory.removeProduct(product);
                                    if (finalI < productSlots.size()) productSlots.remove(finalI);
                                    player.sendMessage("§aĐã xóa cổ phiếu [" + product.getName() + "] thành công!");
                                    getMenu(selectedCategory, true).open(player);
                                    return;
                                }
                                editManager.openProductEdit(player, product);
                            } else {
                                plugin.getBazaarConfig().getProductMenuConfiguration().getMenu(product, false).open(player);
                            }
                        })
                        .item(icon)
                        .build());
            }
        }).build();
    }
}