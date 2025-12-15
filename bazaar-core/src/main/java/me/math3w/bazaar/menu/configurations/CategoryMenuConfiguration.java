package me.math3w.bazaar.menu.configurations;

import com.cryptomorin.xseries.XMaterial;
import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Bazaar;
import me.math3w.bazaar.api.bazaar.Category;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.ProductCategory;
import me.math3w.bazaar.api.edit.EditManager;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import me.math3w.bazaar.bazaar.productcategory.ProductCategoryImpl;
import me.math3w.bazaar.config.BazaarConfig;
import me.math3w.bazaar.menu.DefaultConfigurableMenuItem;
import me.math3w.bazaar.menu.MenuConfiguration;
import me.math3w.bazaar.utils.MenuUtils;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.Component;
import me.zort.containr.GUI;
import me.zort.containr.PagedContainer;
import me.zort.containr.internal.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryMenuConfiguration extends MenuConfiguration {
    public CategoryMenuConfiguration(String name, int rows, List<DefaultConfigurableMenuItem> items) {
        super(name, rows, items);
    }

    public static CategoryMenuConfiguration createDefaultConfiguration(String name, ItemStack glass) {
        return new CategoryMenuConfiguration(name, 6, getDefaultCategoryMenuItems(glass));
    }

    public static List<DefaultConfigurableMenuItem> getDefaultCategoryMenuItems(ItemStack glass) {
        int[] glassSlots = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 10, 17, 19, 26, 28, 35, 37, 44, 46, 48, 51, 52, 53};
        List<DefaultConfigurableMenuItem> items = new ArrayList<>();
        for (int glassSlot : glassSlots) {
            items.add(new DefaultConfigurableMenuItem(glassSlot, ItemBuilder.newBuilder(glass).withName(ChatColor.WHITE.toString()).build(), ""));
        }
        items.add(new DefaultConfigurableMenuItem(45, ItemBuilder.newBuilder(XMaterial.OAK_SIGN.parseItem()).withName(ChatColor.GREEN + "Search").appendLore(ChatColor.GRAY + "Tìm Cổ Phiếu theo tên!").build(), "search"));
        items.add(new DefaultConfigurableMenuItem(49, ItemBuilder.newBuilder(Material.BARRIER).withName(ChatColor.RED + "Đóng").build(), "close"));
        items.add(new DefaultConfigurableMenuItem(50, ItemBuilder.newBuilder(Material.BOOK).withName(ChatColor.GREEN + "Danh Mục Đầu Tư").build(), "manage-orders"));
        return items;
    }

    public static CategoryMenuConfiguration deserialize(Map<String, Object> args) {
        return new CategoryMenuConfiguration((String) args.get("name"), (Integer) args.get("rows"), (List<DefaultConfigurableMenuItem>) args.get("items"));
    }

    public GUI getMenu(Category selectedCategory, boolean edit) {
        BazaarAPI bazaarApi = selectedCategory.getBazaar().getBazaarApi();

        return getMenuBuilder().prepare((gui, player) -> {
            super.loadItems(gui, bazaarApi, player, selectedCategory, edit);
            Bazaar bazaar = selectedCategory.getBazaar();
            EditManager editManager = bazaarApi.getEditManager();

            // Cột bên trái: Danh sách Main Categories
            gui.setContainer(0, Component.staticContainer().size(1, 5).init(container -> {
                for (Category category : bazaar.getCategories()) {
                    ItemStack categoryIcon = category.equals(selectedCategory) ? Utils.getGlowedItem(category.getIcon()) : category.getIcon();
                    container.appendElement(Component.element()
                            .click(editManager.createEditableItemClickAction(
                                    clickInfo -> bazaar.open(player, category),
                                    clickInfo -> bazaar.openEdit(player, category),
                                    clickInfo -> editManager.openCategoryEdit(player, category),
                                    edit))
                            .item(MenuUtils.appendEditLore(categoryIcon, edit))
                            .build());
                }
            }).build());

            // Vùng chính: Hiển thị trực tiếp STOCKS (Bỏ qua Sub-Category)
            PagedContainer productsContainer = Component.pagedContainer().size(6, 4).init(container -> {

                // 1. Duyệt qua tất cả Sub-Categories để lấy Stock
                for (ProductCategory productCategory : selectedCategory.getProductCategories()) {
                    for (Product product : productCategory.getProducts()) {
                        // Hiển thị Stock
                        container.appendElement(Component.element()
                                .click(editManager.createEditableItemClickAction(
                                        clickInfo -> bazaar.openProduct(player, product),
                                        clickInfo -> bazaar.openProductEdit(player, product), // Mở edit product
                                        clickInfo -> editManager.openProductEdit(player, product),
                                        clickInfo -> {
                                            // Logic xóa: Xóa khỏi sub-category hiện tại
                                            productCategory.removeProduct(product);
                                            // Refresh menu
                                            getMenu(selectedCategory, true).open(player);
                                        },
                                        clickInfo -> getMenu(selectedCategory, true).open(player),
                                        edit))
                                .item(MenuUtils.appendEditLore(product.getIcon(container, MenuUtils.getNextFreeSlot(container), player), edit, true))
                                .build());
                    }
                }

                // 2. Nút Add Product (Chỉ hiện khi Edit)
                if (edit) {
                    container.appendElement(Component.element((MenuUtils.getPlusSkull(ChatColor.GREEN + "Thêm Cổ Phiếu")))
                            .click(clickInfo -> {
                                BazaarConfig bazaarConfig = ((BazaarPlugin) bazaar.getBazaarApi()).getBazaarConfig();
                                // Tạo sản phẩm mới mặc định
                                ProductConfiguration productConfiguration = bazaarConfig.getProductConfiguration(
                                        ItemBuilder.newBuilder(Material.PAPER).withName(ChatColor.RED + "Cổ Phiếu mới").build(),
                                        ChatColor.RED + "Cổ Phiếu mới"
                                );

                                // Tự động thêm vào Sub-Category đầu tiên (hoặc tạo mới nếu chưa có)
                                if (selectedCategory.getProductCategories().isEmpty()) {
                                    // Tạo sub-cat ẩn "General"
                                    ProductCategoryImpl newSubCat = new ProductCategoryImpl(selectedCategory,
                                            bazaarConfig.getProductCategoryConfiguration(new ItemStack(Material.CHEST), "General", new ProductConfiguration[]{}, ChatColor.WHITE));
                                    selectedCategory.addProductCategory(newSubCat);
                                }

                                ProductCategory targetSubCat = selectedCategory.getProductCategories().get(0);
                                ProductImpl product = new ProductImpl(targetSubCat, productConfiguration);
                                targetSubCat.addProduct(product);

                                // Mở menu edit ngay cho sản phẩm mới
                                editManager.openProductEdit(player, product);
                            }).build());
                }
            }).build();

            gui.setContainer(11, productsContainer);
            MenuUtils.setPagingArrows(this, gui, productsContainer, bazaarApi, player, 48, 51, edit);
        }).build();
    }
}