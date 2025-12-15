package me.math3w.bazaar.commands;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Category;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StockCommand implements CommandExecutor {
    private final BazaarPlugin plugin;

    public StockCommand(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Lệnh chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bazaar.admin")) {
            player.sendMessage(ChatColor.RED + "Không có quyền.");
            return true;
        }

        if (args.length < 2) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();
        // 1. ADD STOCK
        if (action.equals("add")) {
            if (args.length < 3) {
                player.sendMessage("§cThiếu thông tin. /stock add <category> <name>");
                return true;
            }
            String catName = args[1];
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) sb.append(args[i]).append(" ");
            String stockName = sb.toString().trim();
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage("§cHãy cầm vật phẩm trên tay!");
                return true;
            }

            ProductConfiguration productConfig = plugin.getBazaarConfig().getProductConfiguration(item, stockName);
            plugin.getBazaarConfig().addProductToCategory(catName, productConfig);

            player.sendMessage("§aĐã thêm Stock [" + stockName + "] vào [" + catName + "]");
            player.sendMessage("§7Dùng /stock setprice để chỉnh giá.");

        }
        // 2. SET PRICE
        else if (action.equals("setprice")) {
            if (args.length < 3) {
                player.sendMessage("§cDùng: /stock setprice <tên_cổ_phiếu> <giá>");
                return true;
            }
            String stockId = args[1].toLowerCase().replace(" ", "_");
            double newPrice;
            try {
                newPrice = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cGiá không hợp lệ.");
                return true;
            }

            Product product = plugin.getBazaar().getProduct(stockId);
            if (product == null) {
                player.sendMessage("§cKhông tìm thấy Stock ID: " + stockId);
                return true;
            }

            if (product instanceof ProductImpl) {
                ProductImpl impl = (ProductImpl) product;
                impl.getConfig().setPrice(newPrice);
                plugin.getBazaarConfig().save();
            }
            // Giả định phương thức này tồn tại trong version custom của bạn
            // plugin.getMarketTicker().setManualPrice(product, newPrice);
            player.sendMessage("§aĐã cập nhật giá [" + product.getName() + "] thành " + newPrice);
        }
        // 3. SET ICON (Product)
        else if (action.equals("seticon")) {
            if (args.length < 2) {
                player.sendMessage("§cDùng: /stock seticon <tên_cổ_phiếu>");
                return true;
            }
            String stockId = args[1].toLowerCase().replace(" ", "_");
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                player.sendMessage("§cVui lòng cầm Custom Item trên tay!");
                return true;
            }

            Product product = plugin.getBazaar().getProduct(stockId);
            if (product == null) {
                player.sendMessage("§cKhông tìm thấy Stock ID: " + stockId);
                return true;
            }

            ItemStack newIcon = itemInHand.clone();
            newIcon.setAmount(1);

            product.setItem(newIcon);
            product.setIcon(newIcon);

            player.sendMessage("§aĐã cập nhật Icon/Item cho [" + product.getName() + "] thành công!");
        }
        // 4. [NEW] SET CATEGORY ICON
        else if (action.equals("setcategoryicon")) {
            if (args.length < 2) {
                player.sendMessage("§cDùng: /stock setcategoryicon <tên_danh_mục>");
                return true;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                player.sendMessage("§cVui lòng cầm Custom Item trên tay!");
                return true;
            }

            // Lấy tên category từ arguments (hỗ trợ tên có dấu cách)
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
            String categoryTarget = sb.toString().trim();

            Category foundCategory = null;
            // Duyệt qua danh sách category để tìm
            for (Category cat : plugin.getBazaar().getCategories()) {
                // So sánh tên (bỏ mã màu để chính xác hơn)
                if (ChatColor.stripColor(cat.getName()).equalsIgnoreCase(categoryTarget) || cat.getName().equalsIgnoreCase(categoryTarget)) {
                    foundCategory = cat;
                    break;
                }
            }

            if (foundCategory == null) {
                player.sendMessage("§cKhông tìm thấy danh mục: " + categoryTarget);
                return true;
            }

            ItemStack newIcon = itemInHand.clone();
            newIcon.setAmount(1);

            foundCategory.setIcon(newIcon); // Tự động lưu config trong impl
            player.sendMessage("§aĐã cập nhật Icon cho danh mục [" + foundCategory.getName() + "] thành công!");
        }
        // 5. REMOVE STOCK
        else if (action.equals("remove")) {
            String stockId = args[1].toLowerCase().replace(" ", "_");
            plugin.getBazaarConfig().removeProduct(stockId);
            player.sendMessage("§aĐã xóa Stock ID: " + stockId);
        } else {
            sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e--- Stock Admin Commands ---");
        player.sendMessage("§6/stock add <category> <name> §7(Tạo mới)");
        player.sendMessage("§6/stock setprice <id> <price> §7(Chỉnh giá)");
        player.sendMessage("§6/stock seticon <id> §7(Icon Cổ phiếu)");
        player.sendMessage("§6/stock setcategoryicon <category> §7(Icon Danh mục)");
        player.sendMessage("§6/stock remove <id> §7(Xóa)");
    }
}