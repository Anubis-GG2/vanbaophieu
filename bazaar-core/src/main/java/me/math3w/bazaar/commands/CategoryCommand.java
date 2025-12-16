package me.math3w.bazaar.commands;

import com.cryptomorin.xseries.XMaterial;
import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CategoryCommand implements CommandExecutor {
    private final BazaarPlugin plugin;

    public CategoryCommand(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Lệnh này chỉ dành cho người chơi.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bazaar.admin")) {
            player.sendMessage(ChatColor.RED + "Bạn không có quyền thực hiện lệnh này.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Sử dụng: /category <create|delete> <tên_danh_mục>");
            return true;
        }

        String action = args[0].toLowerCase();
        String name = args[1];

        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            name = sb.toString().trim();
        }

        switch (action) {
            case "create":
            case "add":
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType() == Material.AIR) {
                    handItem = XMaterial.CHEST.parseItem();
                }

                plugin.getBazaarConfig().addCategory(name, handItem);
                player.sendMessage(ChatColor.GREEN + "Đã tạo danh mục chứng khoán: " + name);
                break;

            case "delete":
            case "remove":
                plugin.getBazaarConfig().removeCategory(name);
                player.sendMessage(ChatColor.GREEN + "Đã xóa danh mục chứng khoán: " + name);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Hành động không hợp lệ. Dùng 'create' hoặc 'delete'.");
                break;
        }

        return true;
    }
}