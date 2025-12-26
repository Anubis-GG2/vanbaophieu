package me.math3w.bazaar.edit;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.edit.EditManager;
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

import java.util.function.Consumer;

public class DefaultEditManager implements EditManager {
    private final BazaarPlugin plugin;

    public DefaultEditManager(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Consumer<ContextClickInfo> createEditableItemClickAction(Consumer<ContextClickInfo> primaryAction, Consumer<ContextClickInfo> secondaryAction, Consumer<ContextClickInfo> editAction, boolean edit) {
        return info -> {
            if (edit) {
                editAction.accept(info);
            } else {
                primaryAction.accept(info);
            }
        };
    }

    @Override
    public void openProductEdit(Player player, Product product) {
        GUI gui = Component.gui()
                .title("Chỉnh sửa: " + product.getName())
                .rows(5)
                .prepare((g, p) -> {
                    // 1. Icon (Slot 13)
                    g.setElement(13, Component.element().item(product.getIcon(g, 13, p)).build());

                    // 2. Chỉnh Giá (Slot 20)
                    g.setElement(20, Component.element()
                            .click(info -> {
                                p.closeInventory();
                                p.sendMessage("");
                                p.sendMessage("§e§lCHỈNH SỬA GIÁ:");
                                double price = (product instanceof ProductImpl) ? ((ProductImpl) product).getConfig().getPrice() : 0;
                                p.sendMessage("§7Giá hiện tại: §a" + Utils.getTextPrice(price));

                                TextComponent msg = new TextComponent("§a[BẤM ĐỂ NHẬP GIÁ MỚI]");
                                msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/vanbaophieuedit setprice " + product.getId() + " "));
                                p.spigot().sendMessage(msg);
                                p.sendMessage("");
                            })
                            .item(ItemBuilder.newBuilder(Material.GOLD_INGOT)
                                    .withName("§eChỉnh sửa Giá")
                                    .appendLore("§7Giá hiện tại: §f" + Utils.getTextPrice((product instanceof ProductImpl) ? ((ProductImpl) product).getConfig().getPrice() : 0))
                                    .appendLore("").appendLore("§eBấm để nhập giá mới!")
                                    .build())
                            .build());

                    // 3. Chỉnh Icon (Slot 21)
                    g.setElement(21, Component.element()
                            .click(info -> {
                                p.closeInventory();
                                p.sendMessage("");
                                p.sendMessage("§b§lCHỈNH SỬA ICON:");
                                p.sendMessage("§7Hãy cầm vật phẩm muốn làm Icon trên tay.");
                                TextComponent msg = new TextComponent("§b[BẤM ĐỂ XÁC NHẬN ICON TRÊN TAY]");
                                msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vanbaophieuedit seticon " + product.getId()));
                                p.spigot().sendMessage(msg);
                                p.sendMessage("");
                            })
                            .item(ItemBuilder.newBuilder(Material.NETHER_STAR)
                                    .withName("§bThay đổi Icon")
                                    .appendLore("§7Đổi icon hiển thị của cổ phiếu.").appendLore("").appendLore("§bBấm để thay đổi!")
                                    .build())
                            .build());

                    // 4. Chỉnh Tên (Slot 22)
                    g.setElement(22, Component.element()
                            .click(info -> {
                                p.closeInventory();
                                p.sendMessage("");
                                p.sendMessage("§6§lCHỈNH SỬA TÊN:");
                                p.sendMessage("§7Tên hiện tại: §f" + product.getName());
                                TextComponent msg = new TextComponent("§6[BẤM ĐỂ ĐỔI TÊN]");
                                msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/vanbaophieuedit setname " + product.getId() + " "));
                                p.spigot().sendMessage(msg);
                                p.sendMessage("");
                            })
                            .item(ItemBuilder.newBuilder(Material.NAME_TAG)
                                    .withName("§6Đổi Tên Hiển Thị")
                                    .appendLore("§7Tên hiện tại: §f" + product.getName()).appendLore("").appendLore("§6Bấm để đổi tên!")
                                    .build())
                            .build());

                    // [NEW] 5. Chỉnh Số Lượng (Slot 23)
                    g.setElement(23, Component.element()
                            .click(info -> {
                                p.closeInventory();
                                p.sendMessage("");
                                p.sendMessage("§d§lCHỈNH SỬA SỐ LƯỢNG:");
                                p.sendMessage("§7Số lượng hiện tại: §f" + Utils.formatNumber(product.getCirculatingSupply()));

                                TextComponent msg = new TextComponent("§d[BẤM ĐỂ NHẬP SỐ LƯỢNG MỚI]");
                                msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/vanbaophieuedit setsupply " + product.getId() + " "));
                                p.spigot().sendMessage(msg);
                                p.sendMessage("");
                            })
                            .item(ItemBuilder.newBuilder(Material.CHEST)
                                    .withName("§dChỉnh sửa Số Lượng")
                                    .appendLore("§7Hiện tại: §f" + Utils.formatNumber(product.getCirculatingSupply()))
                                    .appendLore("")
                                    .appendLore("§dBấm để thay đổi tổng cung!")
                                    .build())
                            .build());

                    // 6. Xóa Cổ Phiếu (Slot 24)
                    g.setElement(24, Component.element()
                            .click(info -> {
                                plugin.getBazaarConfig().removeProduct(product.getId());
                                product.getProductCategory().removeProduct(product);
                                p.closeInventory();
                                p.sendMessage("§c§lĐã xóa cổ phiếu [" + product.getName() + "] vĩnh viễn!");
                                plugin.getBazaarConfig().getProductCategoryMenuConfiguration().getMenu(product.getProductCategory(), true).open(p);
                            })
                            .item(ItemBuilder.newBuilder(Material.TNT)
                                    .withName("§c§lXÓA CỔ PHIẾU")
                                    .appendLore("§7Hành động này không thể hoàn tác.").appendLore("").appendLore("§4Bấm để XÓA NGAY LẬP TỨC!")
                                    .build())
                            .build());

                    // Nút Quay lại (Slot 36)
                    g.setElement(36, Component.element()
                            .click(c -> plugin.getBazaarConfig().getProductCategoryMenuConfiguration().getMenu(product.getProductCategory(), true).open(p))
                            .item(ItemBuilder.newBuilder(Material.ARROW).withName("§aQuay lại").build())
                            .build());

                    // Nút Đóng (Slot 40)
                    g.setElement(40, Component.element()
                            .click(c -> p.closeInventory())
                            .item(ItemBuilder.newBuilder(Material.BARRIER).withName("§cĐóng").build())
                            .build());

                    ItemStack glass = ItemBuilder.newBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(" ").build();
                    g.fillElement(Component.element(glass).build());
                }).build();

        gui.open(player);
    }
}