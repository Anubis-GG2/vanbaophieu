package me.math3w.bazaar.config;

import com.cryptomorin.xseries.XMaterial;
import me.math3w.bazaar.api.bazaar.orders.OrderType;
import me.math3w.bazaar.bazaar.category.CategoryConfiguration;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import me.math3w.bazaar.bazaar.productcategory.ProductCategoryConfiguration;
import me.math3w.bazaar.menu.configurations.*;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.internal.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BazaarConfig extends CustomConfig {
    public BazaarConfig(JavaPlugin plugin) {
        super(plugin, "bazaar");
    }

    @Override
    protected void addDefaults() {
        addDefaultCategories();

        addDefault("portfolio-menu", PortfolioMenuConfiguration.createDefaultConfiguration());
        addDefault("pending-orders-menu", PendingOrdersMenuConfiguration.createDefaultConfiguration());
    }

    private void addDefaultCategories() {
        ArrayList<CategoryConfiguration> categories = new ArrayList<>();

        categories.add(createDefaultCategory(
                Material.PAPER,
                "&eTrại Trẻ Nhà Họ Lạc",
                XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem(),
                getTraiTreNhaHoLacCategories()
        ));

        categories.add(createDefaultCategory(
                Material.PAPER,
                "&bNhà Sách Hảo Tâm",
                XMaterial.CYAN_STAINED_GLASS_PANE.parseItem(),
                getNhaSachHaoTamCategories()
        ));

        categories.add(createDefaultCategory(
                Material.PAPER,
                "&cThần Vệ Giới",
                XMaterial.RED_STAINED_GLASS_PANE.parseItem(),
                getNoiThanVeGheChoiCategories()
        ));

        categories.add(createDefaultCategory(
                Material.PAPER,
                "&6Tam Thập Lục Giới",
                XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem(),
                getVungDatTamThapLucCategories()
        ));

        categories.add(createDefaultCategory(
                Material.PAPER,
                "&dHạ Đường Huyết Giới",
                XMaterial.PINK_STAINED_GLASS_PANE.parseItem(),
                getTheGioiHaDuongHuyetCategories()
        ));

        addDefault("categories", categories);
        addDefault("search-menu", SearchMenuConfiguration.createDefaultConfiguration(ChatColor.GREEN + "Vạn Bảo Phiếu ➜ Tìm kiếm", XMaterial.LIME_STAINED_GLASS_PANE.parseItem()));
        addDefault("product-menu", ProductMenuConfiguration.createDefaultProductConfiguration());
        for (OrderType orderType : OrderType.values()) {
            addDefault("confirm-" + orderType.name().toLowerCase() + "-menu", ConfirmationMenuConfiguration.createDefaultConfirmationConfiguration(orderType));
        }
        addDefault("orders", OrdersMenuConfiguration.createDefaultConfiguration());
    }

    private List<ProductCategoryConfiguration> getTraiTreNhaHoLacCategories() {
        return Collections.singletonList(
                getProductCategoryConfiguration(Material.PAPER, "Vật Phẩm", new ProductConfiguration[]{
                        getProductConfiguration(Material.PAPER, "Sushi", 100.0),
                        getProductConfiguration(Material.PAPER, "Sushi Thôn Hải", 100.0),
                        getProductConfiguration(Material.PAPER, "Hồng Sắc Bảo Thạch", 100.0),
                        getProductConfiguration(Material.PAPER, "Lạc Hoàng Tệ", 100.0),
                        getProductConfiguration(Material.PAPER, "Thần Lôi Tinh Tủy", 100.0),
                        getProductConfiguration(Material.PAPER, "Ám Chu Tri", 100.0),
                        getProductConfiguration(Material.PAPER, "Chu Tri Độc Nang", 100.0)
                }, ChatColor.YELLOW)
        );
    }

    private List<ProductCategoryConfiguration> getNhaSachHaoTamCategories() {
        return Collections.singletonList(
                getProductCategoryConfiguration(Material.PAPER, "Vật Phẩm", new ProductConfiguration[]{
                        getProductConfiguration(Material.PAPER, "Minh Lam Tử Sa", 100.0),
                        getProductConfiguration(Material.PAPER, "Linh Quy Hạch", 100.0),
                        getProductConfiguration(Material.PAPER, "Nội Công", 100.0),
                        getProductConfiguration(Material.PAPER, "Vạn Niên Mộc Đằng", 100.0),
                        getProductConfiguration(Material.PAPER, "Cổ Hỏa Hồn Thạch", 100.0),
                        getProductConfiguration(Material.PAPER, "Vạn Niên Huyền Thiết", 100.0),
                        getProductConfiguration(Material.PAPER, "Thần Tinh Kim Thiết", 100.0),
                        getProductConfiguration(Material.PAPER, "Đại Luyện Khí Lực Đan", 100.0),
                        getProductConfiguration(Material.PAPER, "Dẫn Linh Đan", 100.0),
                        getProductConfiguration(Material.PAPER, "Linh Khí Thạch", 100.0)
                }, ChatColor.AQUA)
        );
    }

    private List<ProductCategoryConfiguration> getNoiThanVeGheChoiCategories() {
        return Collections.singletonList(
                getProductCategoryConfiguration(Material.PAPER, "Vật Phẩm", new ProductConfiguration[]{
                        getProductConfiguration(Material.PAPER, "Hư Không Tàn Phiến", 100.0),
                        getProductConfiguration(Material.PAPER, "Hư Không Thạch", 100.0),
                        getProductConfiguration(Material.PAPER, "Hư Không Tâm Hoả", 100.0)
                }, ChatColor.RED)
        );
    }

    private List<ProductCategoryConfiguration> getVungDatTamThapLucCategories() {
        return Collections.singletonList(
                getProductCategoryConfiguration(Material.PAPER, "Vật Phẩm", new ProductConfiguration[]{
                        getProductConfiguration(Material.PAPER, "Vạn Binh Thạch", 100.0),
                        getProductConfiguration(Material.PAPER, "Cửu Thiên Kiếm Châu", 100.0),
                        getProductConfiguration(Material.PAPER, "Cửu Thiên Kiếm Tinh", 100.0),
                        getProductConfiguration(Material.PAPER, "Thất Dục Cô Thạch", 100.0)
                }, ChatColor.GOLD)
        );
    }

    private List<ProductCategoryConfiguration> getTheGioiHaDuongHuyetCategories() {
        return Collections.singletonList(
                getProductCategoryConfiguration(Material.PAPER, "Vật Phẩm", new ProductConfiguration[]{
                        getProductConfiguration(Material.PAPER, "Hàn Băng Tích Thuỷ", 100.0),
                        getProductConfiguration(Material.PAPER, "Sushi Thôn Hải Băng Kình", 100.0),
                        getProductConfiguration(Material.PAPER, "Thanh Vân Kiếm Thiết", 100.0)
                }, ChatColor.LIGHT_PURPLE)
        );
    }

    public void addCategory(String name, ItemStack icon) {
        List<CategoryConfiguration> categories = getCategories();
        ProductCategoryConfiguration defaultSubCat = getProductCategoryConfiguration(
                new ItemStack(Material.CHEST),
                "General",
                new ProductConfiguration[]{},
                ChatColor.WHITE
        );
        categories.add(createDefaultCategory(
                icon,
                name,
                XMaterial.BLACK_STAINED_GLASS_PANE.parseItem(),
                new ArrayList<>(Collections.singletonList(defaultSubCat))
        ));
        set("categories", categories);
        save();
    }

    public void removeCategory(String name) {
        List<CategoryConfiguration> categories = getCategories();
        categories.removeIf(cat -> ChatColor.stripColor(Utils.colorize(cat.getName())).equalsIgnoreCase(name));
        set("categories", categories);
        save();
    }

    public void addProductToCategory(String categoryName, ProductConfiguration product) {
        List<CategoryConfiguration> categories = getCategories();
        boolean found = false;

        for (CategoryConfiguration cat : categories) {
            if (ChatColor.stripColor(Utils.colorize(cat.getName())).equalsIgnoreCase(categoryName)) {
                if (!cat.getProductCategories().isEmpty()) {
                    ProductCategoryConfiguration subCat = cat.getProductCategories().get(0);
                    subCat.getProducts().add(product);
                    subCat.getMenuConfig().addSlotForNewProduct();
                } else {
                    ProductCategoryConfiguration newSubCat = getProductCategoryConfiguration(
                            new ItemStack(Material.CHEST),
                            "General",
                            new ProductConfiguration[]{product},
                            ChatColor.WHITE
                    );
                    newSubCat.getMenuConfig().addSlotForNewProduct();
                    cat.getProductCategories().add(newSubCat);
                }
                found = true;
                break;
            }
        }

        if (found) {
            set("categories", categories);
            save();
        }
    }

    public void removeProduct(String productId) {
        List<CategoryConfiguration> categories = getCategories();
        boolean changed = false;

        for (CategoryConfiguration cat : categories) {
            for (ProductCategoryConfiguration subCat : cat.getProductCategories()) {
                boolean removed = subCat.getProducts().removeIf(p ->
                        ChatColor.stripColor(Utils.colorize(p.getName()))
                                .replace(" ", "_")
                                .toLowerCase()
                                .equals(productId)
                );

                if (removed) {
                    List<Integer> slots = subCat.getMenuConfig().getProductSlots();
                    if (!slots.isEmpty()) {
                        slots.remove(slots.size() - 1);
                    }
                    changed = true;
                }
            }
        }

        if (changed) {
            set("categories", categories);
            save();
        }
    }

    public PortfolioMenuConfiguration getPortfolioMenuConfiguration() {
        return (PortfolioMenuConfiguration) getConfig().get("portfolio-menu");
    }

    public PendingOrdersMenuConfiguration getPendingOrdersMenuConfiguration() {
        return (PendingOrdersMenuConfiguration) getConfig().get("pending-orders-menu");
    }


    private CategoryConfiguration createDefaultCategory(ItemStack icon, String name, ItemStack glass, List<ProductCategoryConfiguration> productCategories) {
        String colorizedName = Utils.colorize(name);
        return new CategoryConfiguration(getDefaultMenuConfiguration(colorizedName, glass),
                ItemBuilder.newBuilder(icon)
                        .withName(colorizedName)
                        .appendLore(ChatColor.DARK_GRAY + "Danh mục", "", ChatColor.YELLOW + "Bấm để xem danh mục!")
                        .build(),
                name,
                productCategories);
    }

    private CategoryConfiguration createDefaultCategory(Material icon, String name, ItemStack glass, List<ProductCategoryConfiguration> productCategories) {
        return createDefaultCategory(new ItemStack(icon), name, glass, productCategories);
    }

    private CategoryMenuConfiguration getDefaultMenuConfiguration(String categoryName, ItemStack glass) {
        return CategoryMenuConfiguration.createDefaultConfiguration(ChatColor.getLastColors(categoryName) + "Vạn Bảo Phiếu ➜ " + categoryName, glass);
    }

    public ProductCategoryConfiguration getProductCategoryConfiguration(Material icon, String name, ProductConfiguration[] products, ChatColor color) {
        return getProductCategoryConfiguration(new ItemStack(icon), name, products, color);
    }

    public ProductCategoryConfiguration getProductCategoryConfiguration(ItemStack icon, String name, ProductConfiguration[] products, ChatColor color) {
        return new ProductCategoryConfiguration(ProductCategoryMenuConfiguration.createDefaultProductCategoryConfiguration(name, products.length),
                getDefaultProductCategoryIcon(icon, color + name),
                name,
                Arrays.asList(products));
    }

    public ProductConfiguration getProductConfiguration(Material material, String name, double price) {
        return new ProductConfiguration(
                new ItemStack(material),
                ItemBuilder.newBuilder(material)
                        .withName(name)
                        .appendLore("")
                        .appendLore("%product-lore%")
                        .appendLore("")
                        .appendLore(ChatColor.YELLOW + "Bấm để xem chi tiết!")
                        .build(),
                name,
                price
        );
    }

    public ProductConfiguration getProductConfiguration(ItemStack item, String name) {
        return new ProductConfiguration(item,
                ItemBuilder.newBuilder(item)
                        .appendLore("")
                        .appendLore("%product-lore%")
                        .appendLore("")
                        .appendLore(ChatColor.YELLOW + "Bấm để xem chi tiết!")
                        .build(),
                name, 10.0);
    }

    private ItemStack getDefaultProductCategoryIcon(ItemStack icon, String name) {
        return ItemBuilder.newBuilder(icon)
                .withName(name)
                .appendLore("%productcategory-lore%")
                .appendLore("")
                .appendLore(ChatColor.YELLOW + "Bấm để xem chi tiết")
                .build();
    }

    public SearchMenuConfiguration getSearchMenuConfiguration() { return (SearchMenuConfiguration) getConfig().get("search-menu"); }
    public ProductMenuConfiguration getProductMenuConfiguration() { return (ProductMenuConfiguration) getConfig().get("product-menu"); }
    public ConfirmationMenuConfiguration getConfirmationMenuConfiguration(OrderType orderType) { return (ConfirmationMenuConfiguration) getConfig().get("confirm-" + orderType.name().toLowerCase() + "-menu"); }
    public OrdersMenuConfiguration getOrdersMenuConfiguration() { return (OrdersMenuConfiguration) getConfig().get("orders"); }
    public List<CategoryConfiguration> getCategories() { return (List<CategoryConfiguration>) getConfig().getList("categories", new ArrayList<>()); }
}