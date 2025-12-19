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
import me.zort.containr.GUI;
import me.zort.containr.internal.util.ItemBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.ClickType;

import java.util.function.Consumer;

public class DefaultEditManager implements EditManager {
    private final BazaarPlugin plugin;

    public DefaultEditManager(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public void openItemEdit(Player player, ConfigurableMenuItem configurableMenuItem) {}
    @Override public void openCategoryEdit(Player player, Category category) {}
    @Override public void openProductCategoryEdit(Player player, ProductCategory productCategory) {}

    // [FIX] Sửa logic chuyển Category: Trái = Chuyển, Phải = Sửa
    @Override
    public Consumer<ContextClickInfo> createEditableItemClickAction(Consumer<ContextClickInfo> defaultClickAction, Consumer<ContextClickInfo> defaultEditClickAction, Consumer<ContextClickInfo> editClickAction, boolean editing) {
        return info -> {
            if (editing) {
                if (info.getClickType().isLeftClick()) {
                    defaultEditClickAction.accept(info); // CHUYỂN CATEGORY
                } else if (info.getClickType().isRightClick()) {
                    editClickAction.accept(info); // SỬA CHI TIẾT
                }
            } else {
                defaultClickAction.accept(info);
            }
        };
    }

    // [FIX] Sửa logic Click cho Sản phẩm
    @Override
    public Consumer<ContextClickInfo> createEditableItemClickAction(Consumer<ContextClickInfo> defaultClickAction, Consumer<ContextClickInfo> defaultEditClickAction, Consumer<ContextClickInfo> editClickAction, Consumer<ContextClickInfo> removeClickAction, Consumer<ContextClickInfo> updateMenu, boolean editing) {
        return info -> {
            if (editing) {
                if (info.getClickType().isLeftClick()) {
                    defaultEditClickAction.accept(info);
                } else if (info.getClickType().isRightClick()) {
                    editClickAction.accept(info);
                } else if (info.getClickType() == ClickType.MIDDLE) {
                    removeClickAction.accept(info);
                }
            } else {
                defaultClickAction.accept(info);
            }
        };
    }

    @Override
    public void openProductEdit(Player player, Product product) {
        GUI gui = Component.gui()
                .title("§0Chỉnh sửa: " + product.getName())
                .rows(5)
                .prepare((g, p) -> {
                    g.setElement(13, Component.element().item(product.getIcon(g, 13, p)).build());

                    // Chỉnh Giá (Slot 20)
                    g.setElement(20, Component.element()
                            .click(info -> {
                                p.closeInventory();
                                p.sendMessage("§e§lVẠN BẢO PHIẾU ➜ §fNhập giá mới:");
                                double price = (product instanceof ProductImpl) ? ((ProductImpl) product).getConfig().getPrice() : 0;
                                p.sendMessage("§7Giá hiện tại: §a" + Utils.getTextPrice(price));
                                TextComponent msg = new TextComponent("§a[BẤM ĐỂ NHẬP GIÁ MỚI]");
                                msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/vanbaophieuedit setprice " + product.getId() + " "));
                                p.spigot().sendMessage(msg);
                            })
                            .item(ItemBuilder.newBuilder(Material.GOLD_INGOT).withName("§eChỉnh sửa Giá").build()).build());

                    // NÚT XÓA STOCK (Slot 24)
                    g.setElement(24, Component.element()
                            .click(info -> {
                                plugin.getBazaarConfig().removeProduct(product.getId());
                                product.getProductCategory().removeProduct(product);
                                p.closeInventory();
                                p.sendMessage("§c§l[!] §fĐã xóa Stock thành công!");
                                plugin.getBazaarConfig().getProductCategoryMenuConfiguration().getMenu(product.getProductCategory(), true).open(p);
                            })
                            .item(ItemBuilder.newBuilder(Material.TNT).withName("§c§lXÓA STOCK").build()).build());

                    // Nút Quay lại (Slot 36)
                    g.setElement(36, Component.element()
                            .click(c -> plugin.getBazaarConfig().getProductCategoryMenuConfiguration().getMenu(product.getProductCategory(), true).open(p))
                            .item(ItemBuilder.newBuilder(Material.ARROW).withName("§aQuay lại").build()).build());

                    g.fillElement(Component.element(ItemBuilder.newBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(" ").build()).build());
                }).build();
        gui.open(player);
    }
}