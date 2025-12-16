package me.math3w.bazaar.menu.configurations;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.ProductCategory;
import me.math3w.bazaar.api.edit.EditManager;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import me.math3w.bazaar.config.BazaarConfig;
import me.math3w.bazaar.menu.DefaultConfigurableMenuItem;
import me.math3w.bazaar.menu.MenuConfiguration;
import me.math3w.bazaar.utils.MenuUtils;
import me.zort.containr.Component;
import me.zort.containr.Element;
import me.zort.containr.GUI;
import me.zort.containr.internal.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ProductCategoryMenuConfiguration extends MenuConfiguration {
    private final List<Integer> productSlots;

    public ProductCategoryMenuConfiguration(String name, int rows, List<DefaultConfigurableMenuItem> items, List<Integer> productSlots) {
        super(name, rows, items);
        this.productSlots = productSlots;
    }

    public static ProductCategoryMenuConfiguration createDefaultProductCategoryConfiguration(String name, int products) {
        List<DefaultConfigurableMenuItem> items = new ArrayList<>();

        items.add(new DefaultConfigurableMenuItem(30,
                ItemBuilder.newBuilder(Material.ARROW)
                        .withName(ChatColor.GREEN + "Quay Lại")
                        .appendLore(ChatColor.GRAY + "đến Vạn Bảo Phiếu")
                        .build(),
                "back"));
        items.add(new DefaultConfigurableMenuItem(31,
                ItemBuilder.newBuilder(Material.BARRIER)
                        .withName(ChatColor.RED + "Đóng")
                        .build(),
                "close"));
        items.add(new DefaultConfigurableMenuItem(32,
                ItemBuilder.newBuilder(Material.BOOK)
                        .withName(ChatColor.GREEN + "Quản lí Đơn hàng")
                        .appendLore(ChatColor.GRAY + "Bạn không có đơn hàng nào!")
                        .appendLore(ChatColor.GRAY + "đơn hàng.")
                        .appendLore("")
                        .appendLore(ChatColor.YELLOW + "Bấm để quản lí!")
                        .build(),
                "manage-orders"));

        int rows = 4;
        List<Integer> productSlots = new ArrayList<>();

        switch (products) {
            case 1: productSlots = Collections.singletonList(13); break;
            case 2: productSlots = Arrays.asList(12, 14); break;
            case 3: productSlots = Arrays.asList(11, 13, 15); break;
            case 4: productSlots = Arrays.asList(10, 12, 14, 16); break;
            default:
                break;
        }

        fillWithGlass(rows, items);

        return new ProductCategoryMenuConfiguration(name, rows, items, productSlots);
    }

    public void addSlotForNewProduct() {
        int[] possibleSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int slot : possibleSlots) {
            if (!productSlots.contains(slot)) {
                productSlots.add(slot);
                return;
            }
        }
    }

    public List<Integer> getProductSlots() {
        return productSlots;
    }

    public static ProductCategoryMenuConfiguration deserialize(Map<String, Object> args) {
        return new ProductCategoryMenuConfiguration((String) args.get("name"),
                (Integer) args.get("rows"),
                (List<DefaultConfigurableMenuItem>) args.get("items"),
                (List<Integer>) args.get("slots"));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> args = super.serialize();
        args.put("slots", productSlots);
        return args;
    }

    public GUI getMenu(ProductCategory selectedCategory, boolean edit) {
        BazaarAPI bazaarApi = selectedCategory.getCategory().getBazaar().getBazaarApi();
        EditManager editManager = bazaarApi.getEditManager();

        return getMenuBuilder().prepare((gui, player) -> {
            super.loadItems(gui, bazaarApi, player, selectedCategory, edit);


            if (edit) {
                for (Map.Entry<Integer, Element> slotElementEntry : gui.content(null).entrySet()) {
                    int slot = slotElementEntry.getKey();
                    Element element = slotElementEntry.getValue();
                    ItemStack originalItem = element.item(player);
                    if (originalItem == null) continue;

                    ItemStack item = ItemBuilder.newBuilder(originalItem)
                            .appendLore(ChatColor.DARK_AQUA + "Bấm chuột giữa để chỉnh sửa")
                            .build();

                    gui.setElement(slot, Component.element(item).click(clickInfo -> {
                        if (clickInfo.getClickType() == ClickType.MIDDLE) {
                            BazaarConfig bazaarConfig = ((BazaarPlugin) bazaarApi).getBazaarConfig();
                            ProductConfiguration productConfiguration = bazaarConfig.getProductConfiguration(ItemBuilder.newBuilder(Material.COAL).withName(ChatColor.RED + "Not set!").build(), ChatColor.RED + "Not set!");
                            ProductImpl product = new ProductImpl(selectedCategory, productConfiguration);

                            productSlots.add(slot);
                            selectedCategory.addProduct(product);
                            editManager.openProductEdit(player, product);
                            return;
                        }
                        element.click(clickInfo);
                    }).build());
                }
            }

            List<Product> products = selectedCategory.getProducts();


            for (int i = 0; i < productSlots.size(); i++) {
                if (i >= products.size()) break;

                int slot = productSlots.get(i);
                Product product = products.get(i);

                int finalI = i;
                gui.setElement(slot, Component.element()
                        .click(editManager.createEditableItemClickAction(
                                clickInfo -> bazaarApi.getBazaar().openProduct(player, product),
                                clickInfo -> bazaarApi.getBazaar().openProductEdit(player, product),
                                clickInfo -> editManager.openProductEdit(player, product),
                                clickInfo -> {
                                    productSlots.remove(finalI);
                                    selectedCategory.removeProduct(product);
                                },
                                clickInfo -> getMenu(selectedCategory, true).open(player), edit))
                        .item(MenuUtils.appendEditLore(product.getIcon(gui, slot, player), edit, true))
                        .build());
            }
        }).build();
    }
}