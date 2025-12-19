package me.math3w.bazaar.menu;

import me.math3w.bazaar.BazaarPlugin;
import me.zort.containr.GUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class MenuListeners implements Listener {
    private final BazaarPlugin plugin;

    public MenuListeners(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (clickedInventory == null) return;

        // Kiểm tra xem GUI đang mở có phải là của Containr không (thư viện GUI)
        if (topInventory.getHolder() instanceof GUI) {
            // Nếu người chơi click vào túi đồ dưới (Bottom Inventory)
            if (clickedInventory.equals(event.getView().getBottomInventory())) {
                // Cho phép click để di chuyển item trong túi
                event.setCancelled(false);

                // Tuy nhiên, nếu Shift-Click để đẩy item lên trên Menu thì phải chặn
                // vì Menu thường không xử lý item bay vào
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}