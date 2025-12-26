package me.math3w.bazaar.edit;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Category;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.ProductCategory;
import me.math3w.bazaar.api.edit.EditManager;
import me.math3w.bazaar.api.menu.ConfigurableMenuItem;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import me.math3w.bazaar.utils.Utils;
import me.zort.containr.Component;
import me.zort.containr.ContextClickInfo;
import me.zort.containr.internal.util.ItemBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class DefaultEditManager implements EditManager {
    private final BazaarPlugin plugin;

    public DefaultEditManager(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public void openItemEdit(Player player, ConfigurableMenuItem configurableMenuItem) {}

    @Override
    public void openCategoryEdit(Player player, Category category) {
        new EditMenuBuilder()
                .title("§0Sửa Category: " + category.getName())
                .updateMenuPlayerConsumer(p -> openCategoryEdit(p, category))
                .addPreviewElement(13, category.getIcon(), newItem -> {
                    category.setIcon(newItem);
                    plugin.getBazaar().saveConfig();
                })
                .addNameEditElement(plugin.getMessageInputManager(), 20, category.getName(), newName -> {
                    category.setName(newName);
                    plugin.getBazaar().saveConfig();
                })
                .addBackElementElement(36, plugin.getMenuHistory())
                .build().open(player);
    }

    @Override public void openProductCategoryEdit(Player player, ProductCategory productCategory) {}

    @Override
    public void openProductEdit(Player player, Product product) {
        EditMenuBuilder builder = new EditMenuBuilder()
                .title("§0Sửa Sản Phẩm: " + product.getName())
                .updateMenuPlayerConsumer(p -> openProductEdit(p, product));

        // Slot 13: Chỉnh sửa Icon (Xóa lore, giữ tên - Hết lỗi)
        builder.addPreviewElement(13, product.getRawIcon(), newItem -> {
            product.setIcon(newItem);
            plugin.getBazaar().saveConfig();
        });

        // Slot 19: Chỉnh sửa Tên (Hết lỗi)
        builder.addNameEditElement(plugin.getMessageInputManager(), 19, product.getName(), newName -> {
            product.setName(newName);
            plugin.getBazaar().saveConfig();
        });

        // Slot 20: Chỉnh sửa Lore (Hết lỗi nhờ thêm getLore/setLore vào Product)
        builder.addLoreEditElement(plugin.getMessageInputManager(), 20, product.getLore(), newLore -> {
            product.setLore(newLore);
            plugin.getBazaar().saveConfig();
        });

        // Slot 21: Chỉnh sửa Giá
        builder.addElement(21, Component.element()
                .click(info -> {
                    player.closeInventory();
                    player.sendMessage("§e§lVẠN BẢO PHIẾU ➜ §fNhập giá mới:");
                    double price = (product instanceof ProductImpl) ? ((ProductImpl) product).getConfig().getPrice() : 0;
                    player.sendMessage("§7Giá hiện tại: §a" + Utils.getTextPrice(price));
                    TextComponent msg = new TextComponent("§a[BẤM ĐỂ NHẬP GIÁ MỚI]");
                    msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/vanbaophieuedit setprice " + product.getId() + " "));
                    player.spigot().sendMessage(msg);
                })
                .item(ItemBuilder.newBuilder(Material.GOLD_INGOT).withName("§eChỉnh sửa Giá").build()).build());

        // Slot 24: Xóa Stock
        builder.addElement(24, Component.element()
                .click(info -> {
                    plugin.getBazaarConfig().removeProduct(product.getId());
                    product.getProductCategory().removeProduct(product);
                    player.closeInventory();
                    player.sendMessage("§c§l[!] §fĐã xóa Stock thành công!");
                    plugin.getBazaarConfig().getProductCategoryMenuConfiguration().getMenu(product.getProductCategory(), true).open(player);
                })
                .item(ItemBuilder.newBuilder(Material.TNT).withName("§c§lXÓA STOCK").build()).build());

        builder.addBackElementElement(36, plugin.getMenuHistory());
        builder.build().open(player);
    }

    @Override
    public Consumer<ContextClickInfo> createEditableItemClickAction(Consumer<ContextClickInfo> defaultClickAction, Consumer<ContextClickInfo> defaultEditClickAction, Consumer<ContextClickInfo> editClickAction, boolean editing) {
        return info -> {
            if (editing) {
                if (info.getClickType().isLeftClick()) defaultEditClickAction.accept(info);
                else if (info.getClickType().isRightClick()) editClickAction.accept(info);
            } else defaultClickAction.accept(info);
        };
    }

    @Override
    public Consumer<ContextClickInfo> createEditableItemClickAction(Consumer<ContextClickInfo> defaultClickAction, Consumer<ContextClickInfo> defaultEditClickAction, Consumer<ContextClickInfo> editClickAction, Consumer<ContextClickInfo> removeClickAction, Consumer<ContextClickInfo> updateMenu, boolean editing) {
        return info -> {
            if (editing) {
                if (info.getClickType().isLeftClick()) defaultEditClickAction.accept(info);
                else if (info.getClickType().isRightClick()) editClickAction.accept(info);
                else if (info.getClickType().isCreativeAction()) removeClickAction.accept(info);
            } else defaultClickAction.accept(info);
        };
    }
}