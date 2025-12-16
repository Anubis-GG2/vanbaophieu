package me.math3w.bazaar.menu;

import de.rapha149.signgui.SignGUI;
import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.orders.BazaarOrder;
import me.math3w.bazaar.api.bazaar.orders.InstantBazaarOrder;
import me.math3w.bazaar.api.bazaar.orders.OrderType;
import me.math3w.bazaar.api.menu.ClickActionManager;
import me.math3w.bazaar.api.menu.ConfigurableMenuItem;
import me.math3w.bazaar.api.menu.MenuInfo;
import me.zort.containr.ContextClickInfo;
import me.zort.containr.GUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DefaultClickActionManager implements ClickActionManager {
    private final BazaarPlugin bazaarPlugin;
    private final Map<String, Function<MenuInfo, Consumer<ContextClickInfo>>> clickActions = new HashMap<>();
    private final Map<String, BiFunction<ConfigurableMenuItem, MenuInfo, Consumer<ContextClickInfo>>> editActions = new HashMap<>();

    public DefaultClickActionManager(BazaarPlugin bazaarPlugin) {
        this.bazaarPlugin = bazaarPlugin;
        addClickActions();
        addEditClickActions();
    }

    private void addClickActions() {
        addClickAction("close", ContextClickInfo::close);
        addClickAction("back", (Consumer<ContextClickInfo>) clickInfo -> bazaarPlugin.getMenuHistory().openPrevious(clickInfo.getPlayer()));

        addClickAction("search", clickInfo -> {
            clickInfo.getPlayer().closeInventory();
            try {
                SignGUI.builder()
                        .setLines(bazaarPlugin.getMenuConfig().getStringList("search-sign").toArray(new String[4]))
                        .setHandler((player, result) -> {
                            String filter = result.getLine(0);
                            Bukkit.getScheduler().runTask(bazaarPlugin, () ->
                                    bazaarPlugin.getBazaar().openSearch(player, filter));
                            return Collections.emptyList();
                        })
                        .build()
                        .open(clickInfo.getPlayer());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        addClickAction("buy-instantly", menuInfo -> clickInfo -> {
            if (!(menuInfo instanceof Product)) return;
            Product product = (Product) menuInfo;

            requireNumberFromPlayer(clickInfo.getPlayer(), "buy-instantly-amount-sign", amount -> {

                bazaarPlugin.getPortfolioManager().buyStock(clickInfo.getPlayer(), product, amount);
            }, Integer::parseInt);
        });


        addClickAction("sell-instantly", menuInfo -> clickInfo -> {
            if (!(menuInfo instanceof Product)) return;
            Product product = (Product) menuInfo;


            if (clickInfo.getClickType().isRightClick()) {
                requireNumberFromPlayer(clickInfo.getPlayer(), "sell-instantly-amount-sign", amount -> {
                    bazaarPlugin.getPortfolioManager().sellStock(clickInfo.getPlayer(), product, amount);
                }, Integer::parseInt);
                return;
            }


            int available = bazaarPlugin.getPortfolioManager().getAvailableStockAmount(clickInfo.getPlayer().getUniqueId(), product);
            if (available > 0) {
                bazaarPlugin.getPortfolioManager().sellStock(clickInfo.getPlayer(), product, available);
            } else {
                clickInfo.getPlayer().sendMessage("§cBạn không có cổ phiếu khả dụng để bán!");
            }
        });


        addClickAction("manage-orders", clickInfo -> {

            bazaarPlugin.getBazaarConfig().getPortfolioMenuConfiguration()
                    .getMenu(bazaarPlugin, false)
                    .open(clickInfo.getPlayer());
        });


        addClickAction("view-pending", clickInfo -> {
            bazaarPlugin.getBazaarConfig().getPendingOrdersMenuConfiguration()
                    .getMenu(bazaarPlugin, false)
                    .open(clickInfo.getPlayer());
        });


        addClickAction("buy-order", menuInfo -> clickInfo -> clickInfo.getPlayer().sendMessage("§cChức năng này đã bị tắt."));
        addClickAction("sell-offer", menuInfo -> clickInfo -> clickInfo.getPlayer().sendMessage("§cChức năng này đã bị tắt."));
        addClickAction("sell-inventory", menuInfo -> clickInfo -> {});
        addClickAction("confirm-order", menuInfo -> clickInfo -> clickInfo.close());
        addClickAction("", clickInfo -> {});
    }

    private void addEditClickActions() {

        addEditClickAction("search", (configurableMenuItem, menuInfo) -> clickInfo -> {
            clickInfo.getPlayer().closeInventory();
            try {
                SignGUI.builder()
                        .setLines(bazaarPlugin.getMenuConfig().getStringList("search-sign").toArray(new String[4]))
                        .setHandler((player, result) -> {
                            String filter = result.getLine(0);
                            Bukkit.getScheduler().runTask(bazaarPlugin, () ->
                                    bazaarPlugin.getBazaar().openEditSearch(player, filter, configurableMenuItem));
                            return Collections.emptyList();
                        })
                        .build()
                        .open(clickInfo.getPlayer());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        addEditClickAction("manage-orders", (configurableMenuItem, menuInfo) -> clickInfo -> {
            bazaarPlugin.getBazaarConfig().getPortfolioMenuConfiguration()
                    .getMenu(bazaarPlugin, true) // Edit mode = true
                    .open(clickInfo.getPlayer());
        });


        addEditClickAction("buy-instantly", (item, info) -> clickInfo -> {});
        addEditClickAction("sell-instantly", (item, info) -> clickInfo -> {});
        addEditClickAction("buy-order", (item, info) -> clickInfo -> {});
        addEditClickAction("sell-offer", (item, info) -> clickInfo -> {});
    }

    @Override
    public void addClickAction(String name, Consumer<ContextClickInfo> action) {
        addClickAction(name, menuInfo -> action);
    }

    @Override
    public void addClickAction(String name, Function<MenuInfo, Consumer<ContextClickInfo>> action) {
        clickActions.put(name, action);
    }

    @Override
    public void addEditClickAction(String name, BiFunction<ConfigurableMenuItem, MenuInfo, Consumer<ContextClickInfo>> action) {
        editActions.put(name, action);
    }

    @Override
    public Consumer<ContextClickInfo> getClickAction(ConfigurableMenuItem configurableMenuItem, MenuInfo menuInfo, boolean editing) {
        if (editing) {
            return clickInfo -> {
                if (clickInfo.getClickType().isRightClick()) {
                    bazaarPlugin.getEditManager().openItemEdit(clickInfo.getPlayer(), configurableMenuItem);
                    return;
                }
                editActions.getOrDefault(configurableMenuItem.getAction(),
                                (configurableMenuItem1, menuInfo1) -> getClickAction(configurableMenuItem, menuInfo, false))
                        .apply(configurableMenuItem, menuInfo).accept(clickInfo);
            };
        }
        return clickActions.getOrDefault(configurableMenuItem.getAction(), menuInfo1 -> clickInfo -> {}).apply(menuInfo);
    }

    @Override
    public Set<String> getActions() {
        return clickActions.keySet();
    }


    private <T extends Number> void requireNumberFromPlayer(Player player, String sign, Consumer<T> callback, Function<String, T> parser) {
        Stack<GUI> history = bazaarPlugin.getMenuHistory().getHistory(player);
        player.closeInventory();

        try {
            SignGUI.builder()
                    .setLines(bazaarPlugin.getMenuConfig().getStringList(sign).toArray(new String[4]))
                    .setHandler((p, result) -> {
                        String input = result.getLine(0);
                        try {
                            T amount = parser.apply(input);
                            if (amount.doubleValue() <= 0) {
                                openHistory(player, history);
                                return Collections.emptyList();
                            }
                            Bukkit.getScheduler().runTaskLater(bazaarPlugin, () -> {
                                bazaarPlugin.getMenuHistory().setHistory(player, history);
                                callback.accept(amount);
                            }, 1);
                        } catch (NumberFormatException exception) {
                            p.sendMessage("§cLỗi: Vui lòng nhập số hợp lệ.");
                            openHistory(player, history);
                        }
                        return Collections.emptyList();
                    })
                    .build()
                    .open(player);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage("§cLỗi khi mở giao diện nhập số.");
        }
    }

    private void openHistory(Player player, Stack<GUI> history) {
        if (history != null && !history.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(bazaarPlugin, () -> {
                GUI lastGui = history.pop();
                bazaarPlugin.getMenuHistory().setHistory(player, history);
                lastGui.open(player);
            }, 1);
        }
    }
}