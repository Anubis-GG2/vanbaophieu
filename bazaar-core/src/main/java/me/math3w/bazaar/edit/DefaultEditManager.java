package me.math3w.bazaar.edit;

import com.cryptomorin.xseries.XMaterial;
import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Bazaar;
import me.math3w.bazaar.api.bazaar.Category;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.ProductCategory;
import me.math3w.bazaar.api.edit.EditManager;
import me.math3w.bazaar.api.menu.ConfigurableMenuItem;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import me.math3w.bazaar.messageinput.MessageInputManager;
import me.zort.containr.Component;
import me.zort.containr.ContextClickInfo;
import me.zort.containr.internal.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class DefaultEditManager implements EditManager {
    private final BazaarPlugin bazaarPlugin;

    public DefaultEditManager(BazaarPlugin bazaarPlugin) {
        this.bazaarPlugin = bazaarPlugin;
    }

    @Override
    public void openItemEdit(Player player, ConfigurableMenuItem configurableMenuItem) {
        Bazaar bazaar = bazaarPlugin.getBazaar();
        MessageInputManager messageInputManager = bazaarPlugin.getMessageInputManager();
        new EditMenuBuilder().title("Edit Item")
                .updateMenuPlayerConsumer(p -> openItemEdit(p, configurableMenuItem))
                .addPreviewElement(13, configurableMenuItem.getItem(), newItem -> configurableMenuItem.setItem(bazaar, newItem))
                .addNameEditElement(messageInputManager, 29, configurableMenuItem.getItem().getItemMeta().getDisplayName(), newName -> {
                    ItemStack newItem = ItemBuilder.newBuilder(configurableMenuItem.getItem()).withName(newName).build();
                    configurableMenuItem.setItem(bazaar, newItem);
                })
                .addActionEditElement(31, configurableMenuItem, bazaar)
                .addLoreEditElement(messageInputManager, 33, configurableMenuItem.getItem().getItemMeta().getLore(), newLore -> {
                    ItemStack newItem = ItemBuilder.newBuilder(configurableMenuItem.getItem()).withLore(newLore).build();
                    configurableMenuItem.setItem(bazaar, newItem);
                }).build().open(player);
    }

    // [UPDATED] Đã thêm chức năng Drag & Drop Icon cho Category
    @Override
    public void openCategoryEdit(Player player, Category category) {
        MessageInputManager messageInputManager = bazaarPlugin.getMessageInputManager();

        new EditMenuBuilder().title("Edit Category")
                .updateMenuPlayerConsumer(p -> openCategoryEdit(p, category))

                // Element 13: Preview Icon hiện tại
                .addPreviewElement(13, category.getIcon(), category::setIcon)

                // Element 29: Sửa tên
                .addNameEditElement(messageInputManager, 29, category.getIcon().getItemMeta().getDisplayName(), newName -> {
                    ItemStack newItem = ItemBuilder.newBuilder(category.getIcon()).withName(newName).build();
                    category.setIcon(newItem);
                })

                // [NEW] Element 31: Thay thế Icon bằng Item Custom (Giống Stock)
                .addElement(31, Component.element()
                        .item(ItemBuilder.newBuilder(Material.HOPPER)
                                .withName(ChatColor.AQUA + "Replace Category Icon")
                                .appendLore("")
                                .appendLore(ChatColor.YELLOW + "1. Hold Custom Item")
                                .appendLore(ChatColor.YELLOW + "2. Click here to set as Icon!")
                                .build())
                        .click(clickInfo -> {
                            ItemStack cursor = clickInfo.getCursor();
                            if (cursor == null || cursor.getType() == Material.AIR) {
                                player.sendMessage(ChatColor.RED + "Vui lòng cầm Item Custom trên con trỏ chuột!");
                                return;
                            }

                            // Clone item đầy đủ (bao gồm NBT, ModelData)
                            ItemStack newIcon = cursor.clone();
                            newIcon.setAmount(1);

                            // Cập nhật icon
                            category.setIcon(newIcon);

                            // Lưu config ngay lập tức để đảm bảo không mất khi restart
                            bazaarPlugin.getBazaarConfig().save();

                            player.setItemOnCursor(null); // Xóa item trên tay (hoặc giữ lại tùy bạn)
                            player.sendMessage(ChatColor.GREEN + "Category Icon updated!");
                            openCategoryEdit(player, category); // Reload menu
                        }).build())

                // Element 33: Sửa Lore
                .addLoreEditElement(messageInputManager, 33, category.getIcon().getItemMeta().getLore(), newLore -> {
                    ItemStack newItem = ItemBuilder.newBuilder(category.getIcon()).withLore(newLore).build();
                    category.setIcon(newItem);
                }).build().open(player);
    }

    @Override
    public void openProductCategoryEdit(Player player, ProductCategory productCategory) {
        MessageInputManager messageInputManager = bazaarPlugin.getMessageInputManager();
        new EditMenuBuilder().title("Edit Sub-Category")
                .updateMenuPlayerConsumer(p -> openProductCategoryEdit(p, productCategory))
                .addPreviewElement(13, productCategory.getRawIcon(), productCategory::setIcon)
                .addNameEditElement(messageInputManager, 30, productCategory.getRawIcon().getItemMeta().getDisplayName(), newName -> {
                    ItemStack newItem = ItemBuilder.newBuilder(productCategory.getRawIcon()).withName(newName).build();
                    productCategory.setIcon(newItem);
                    productCategory.setName(newName);
                }).build().open(player);
    }

    @Override
    public void openProductEdit(Player player, Product product) {
        MessageInputManager messageInputManager = bazaarPlugin.getMessageInputManager();

        new EditMenuBuilder()
                .title("Edit Product (Stock)")
                .updateMenuPlayerConsumer(player1 -> openProductEdit(player1, product))

                // Element 13: Preview
                .addPreviewElement(13, product.getRawIcon(), product::setIcon)

                // Element 29: Edit Name
                .addNameEditElement(messageInputManager, 29, product.getRawIcon().getItemMeta().getDisplayName(), newName -> {
                    ItemStack newItem = ItemBuilder.newBuilder(product.getRawIcon()).withName(newName).build();
                    product.setIcon(newItem);
                    product.setName(newName);
                })

                // Element 31: Replace Item (Custom Item support)
                .addElement(31, Component.element()
                        .item(ItemBuilder.newBuilder(Material.HOPPER)
                                .withName(ChatColor.AQUA + "Replace Stock Item")
                                .appendLore("")
                                .appendLore(ChatColor.YELLOW + "1. Hold Custom Item")
                                .appendLore(ChatColor.YELLOW + "2. Click here to update!")
                                .build())
                        .click(clickInfo -> {
                            ItemStack cursor = clickInfo.getCursor();
                            if (cursor == null || cursor.getType() == Material.AIR) {
                                player.sendMessage(ChatColor.RED + "Vui lòng cầm Item Custom!");
                                return;
                            }
                            ItemStack newIcon = cursor.clone();
                            newIcon.setAmount(1);

                            product.setItem(newIcon);
                            product.setIcon(newIcon);

                            // ProductImpl thường tự lưu khi setItem/setIcon, nhưng gọi save thêm cho chắc chắn
                            bazaarPlugin.getBazaarConfig().save();

                            player.setItemOnCursor(null);
                            player.sendMessage(ChatColor.GREEN + "Stock Item updated!");
                            openProductEdit(player, product);
                        }).build())

                // Element 22: EDIT PRICE
                .addElement(22, Component.element()
                        .item(ItemBuilder.newBuilder(XMaterial.GOLD_INGOT.parseItem())
                                .withName(ChatColor.GOLD + "Edit Base Price")
                                .appendLore(ChatColor.GRAY + "Current Base: " + ChatColor.YELLOW +
                                        (product instanceof ProductImpl ? ((ProductImpl)product).getConfig().getPrice() : "N/A"))
                                .appendLore("")
                                .appendLore(ChatColor.YELLOW + "Click to set price!")
                                .build())
                        .click(clickInfo -> {
                            player.closeInventory();
                            player.sendMessage(ChatColor.GREEN + "Enter new price in chat:");

                            messageInputManager.requirePlayerMessageInput(player, input -> {
                                try {
                                    double newPrice = Double.parseDouble(input);
                                    if (product instanceof ProductImpl) {
                                        ((ProductImpl) product).getConfig().setPrice(newPrice);
                                        bazaarPlugin.getBazaarConfig().save();
                                        bazaarPlugin.getMarketTicker().setManualPrice(product, newPrice);
                                        player.sendMessage("§aPrice updated to: " + newPrice);
                                    }
                                    openProductEdit(player, product);
                                } catch (NumberFormatException e) {
                                    player.sendMessage("§cInvalid number!");
                                }
                            });
                        }).build())

                // Element 33: Edit Lore
                .addLoreEditElement(messageInputManager, 33, product.getRawIcon().getItemMeta().getLore(), newLore -> {
                    ItemStack newItem = ItemBuilder.newBuilder(product.getRawIcon()).withLore(newLore).build();
                    product.setIcon(newItem);
                })
                .build()
                .open(player);
    }

    @Override
    public Consumer<ContextClickInfo> createEditableItemClickAction(Consumer<ContextClickInfo> defaultClickAction,
                                                                    Consumer<ContextClickInfo> defaultEditClickAction,
                                                                    Consumer<ContextClickInfo> editClickAction,
                                                                    Consumer<ContextClickInfo> removeClickAction,
                                                                    Consumer<ContextClickInfo> updateMenu,
                                                                    boolean editing) {
        return clickInfo -> {
            if (editing) {
                if (clickInfo.getClickType().isRightClick()) {
                    editClickAction.accept(clickInfo);
                    return;
                }
                if (clickInfo.getClickType() == ClickType.MIDDLE) {
                    removeClickAction.accept(clickInfo);
                    updateMenu.accept(clickInfo);
                    return;
                }
                defaultEditClickAction.accept(clickInfo);
                return;
            }
            defaultClickAction.accept(clickInfo);
        };
    }

    @Override
    public Consumer<ContextClickInfo> createEditableItemClickAction(Consumer<ContextClickInfo> defaultClickAction,
                                                                    Consumer<ContextClickInfo> defaultEditClickAction,
                                                                    Consumer<ContextClickInfo> editClickAction,
                                                                    boolean editing) {
        return createEditableItemClickAction(defaultClickAction, defaultEditClickAction, editClickAction, c -> {}, c -> {}, editing);
    }
}