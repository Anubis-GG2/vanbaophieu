package me.math3w.bazaar.commands;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Category;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import me.math3w.bazaar.utils.DiscordService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

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

            ItemStack iconItem = item.clone();
            iconItem.setAmount(1);
            ItemMeta meta = iconItem.getItemMeta();
            if (meta != null) {
                meta.setLore(new ArrayList<>());
                iconItem.setItemMeta(meta);
            }

            ProductConfiguration productConfig = plugin.getBazaarConfig().getProductConfiguration(iconItem, stockName);
            productConfig.setPrice(100.0);

            plugin.getBazaarConfig().addProductToCategory(catName, productConfig);

            player.sendMessage("§aĐã thêm Stock [" + stockName + "] vào [" + catName + "]");
            player.sendMessage("§7Dùng /stock setprice để chỉnh giá.");

        }
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
            plugin.getMarketTicker().setManualPrice(product, newPrice);
            player.sendMessage("§aĐã cập nhật giá [" + product.getName() + "] thành " + newPrice);
        }
        else if (action.equals("seticon")) {
            if (args.length < 2) {
                player.sendMessage("§cDùng: /stock seticon <tên_cổ_phiếu>");
                return true;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
            String inputName = sb.toString().trim();

            String targetId = inputName.toLowerCase().replace(" ", "_");
            Product product = plugin.getBazaar().getProduct(targetId);

            if (product == null) {
                for (Product p : plugin.getBazaar().getProducts()) {
                    if (ChatColor.stripColor(p.getName()).equalsIgnoreCase(inputName)) {
                        product = p;
                        break;
                    }
                }
            }

            if (product == null) {
                player.sendMessage("§cKhông tìm thấy Stock: " + inputName);
                return true;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                player.sendMessage("§cVui lòng cầm Custom Item trên tay!");
                return true;
            }

            ItemStack newIcon = itemInHand.clone();
            newIcon.setAmount(1);
            ItemMeta meta = newIcon.getItemMeta();
            if (meta != null) {
                meta.setLore(new ArrayList<>());
                newIcon.setItemMeta(meta);
            }

            product.setItem(newIcon);
            product.setIcon(newIcon);

            player.sendMessage("§aĐã cập nhật Icon/Item cho [" + product.getName() + "] thành công!");
        }
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

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
            String categoryTarget = sb.toString().trim();

            Category foundCategory = null;
            for (Category cat : plugin.getBazaar().getCategories()) {
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

            foundCategory.setIcon(newIcon);
            player.sendMessage("§aĐã cập nhật Icon cho danh mục [" + foundCategory.getName() + "] thành công!");
        }
        else if (action.equals("remove")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
            String inputName = sb.toString().trim();

            String targetId = inputName.toLowerCase().replace(" ", "_");
            Product product = plugin.getBazaar().getProduct(targetId);

            if (product == null) {
                for (Product p : plugin.getBazaar().getProducts()) {
                    if (ChatColor.stripColor(p.getName()).equalsIgnoreCase(inputName)) {
                        product = p;
                        targetId = p.getId();
                        break;
                    }
                }
            }

            if (product == null) {
                player.sendMessage("§cKhông tìm thấy Stock: " + inputName);
                return true;
            }

            plugin.getBazaarConfig().removeProduct(targetId);
            product.getProductCategory().removeProduct(product);

            player.sendMessage("§aĐã xóa hoàn toàn Stock: " + product.getName());
        }
        else if (action.equals("timeskip")) {
            if (args.length < 2) {
                player.sendMessage("§cDùng: /stock timeskip <giờ>");
                return true;
            }
            try {
                int hours = Integer.parseInt(args[1]);
                plugin.getPortfolioManager().processTimeSkip(hours);
                player.sendMessage("§aĐã tua nhanh thời gian " + hours + " giờ cho toàn bộ Portfolio!");
            } catch (NumberFormatException e) {
                player.sendMessage("§cSố giờ không hợp lệ.");
            }
        }
        else if (action.equals("manipulate")) {
            if (args.length < 6) {
                player.sendMessage("§cDùng: /stock manipulate <increase/decrease> <stock/category> <name> <%> <hours>");
                return true;
            }
            String mode = args[1].toLowerCase();
            String type = args[2].toLowerCase();
            String targetName = args[3].replace("_", " ");
            double percent;
            int hours;
            try {
                percent = Double.parseDouble(args[4]);
                hours = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cPhần trăm hoặc thời gian không hợp lệ.");
                return true;
            }
            String targetId = targetName;
            boolean isCategory = false;

            if (type.equals("stock")) {
                Product p = plugin.getBazaar().getProduct(targetName.toLowerCase().replace(" ", "_"));
                if (p == null) {
                    player.sendMessage("§cKhông tìm thấy Stock: " + targetName);
                    return true;
                }
                targetId = p.getId();
            } else if (type.equals("category")) {
                boolean found = false;
                for (Category cat : plugin.getBazaar().getCategories()) {
                    if (ChatColor.stripColor(cat.getName()).equalsIgnoreCase(targetName)) {
                        targetId = cat.getName();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    player.sendMessage("§cKhông tìm thấy Category: " + targetName);
                    return true;
                }
                isCategory = true;
            } else {
                player.sendMessage("§cLoại mục tiêu phải là 'stock' hoặc 'category'.");
                return true;
            }
            boolean isIncrease = true;
            if (mode.equals("decrease")) {
                percent = -percent;
                isIncrease = false;
            } else if (!mode.equals("increase")) {
                player.sendMessage("§cChế độ phải là 'increase' hoặc 'decrease'.");
                return true;
            }

            plugin.getMarketTicker().registerManipulation(targetId, isCategory, percent, hours);
            player.sendMessage("§aĐã kích hoạt thao túng thị trường: " + (percent > 0 ? "TĂNG" : "GIẢM") + " " + Math.abs(percent) + "% trong " + hours + "h.");
            player.sendMessage("§7Mục tiêu: " + targetId);

            DiscordService.sendManipulationAlert(targetName, isCategory, isIncrease, Math.abs(percent), hours);
        }
        else {
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
        player.sendMessage("§6/stock remove <name> §7(Xóa)");
        player.sendMessage("§6/stock timeskip <hours> §7(Tua thời gian T+1)");
        player.sendMessage("§6/stock manipulate <inc/dec> <stock/cat> <name> <%> <hours>");
    }
}