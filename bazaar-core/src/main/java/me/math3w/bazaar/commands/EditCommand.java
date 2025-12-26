package me.math3w.bazaar.commands;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.product.ProductImpl;
import me.math3w.bazaar.messageinput.MessageInputManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EditCommand implements CommandExecutor {
    private final BazaarPlugin bazaarPlugin;

    public EditCommand(BazaarPlugin bazaarPlugin) {
        this.bazaarPlugin = bazaarPlugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) return false;

        Player player = (Player) commandSender;

        if (args.length == 0) {
            bazaarPlugin.getBazaar().openEdit(player, bazaarPlugin.getBazaar().getCategories().get(0));
            return true;
        }

        // [UPDATE] Xử lý các lệnh từ Menu Edit
        if (args.length >= 2) {
            String sub = args[0];
            String productId = args[1];

            // Xử lý lore riêng (logic cũ)
            if (sub.equals("lore")) {
                MessageInputManager messageInputManager = bazaarPlugin.getMessageInputManager();
                int lineIndex = args.length >= 3 ? Integer.parseInt(args[2]) : -1;
                switch (productId) { // args[1] là action của lore: remove, edit...
                    case "remove":
                        if (lineIndex < 0) break;
                        messageInputManager.removeLine(player, lineIndex);
                        break;
                    case "edit":
                        if (lineIndex < 0) break;
                        messageInputManager.editLine(player, lineIndex);
                        break;
                    case "add":
                        messageInputManager.addLine(player);
                        break;
                    case "confirm":
                        messageInputManager.confirmMultiLineInput(player);
                        break;
                }
                return true;
            }

            Product product = bazaarPlugin.getBazaar().getProduct(productId);
            if (product == null) {
                // Skip if checking lore (handled above) but good to check context
                return true;
            }

            // Xử lý Set Icon (Chỉ cần 2 args: /cmd seticon <id>)
            if (sub.equalsIgnoreCase("seticon")) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) {
                    player.sendMessage("§cVui lòng cầm vật phẩm trên tay.");
                    return true;
                }
                product.setIcon(hand);
                player.sendMessage("§aĐã cập nhật Icon thành công!");
                bazaarPlugin.getEditManager().openProductEdit(player, product);
                return true;
            }

            // Các lệnh cần tham số giá trị (args[2])
            if (args.length >= 3) {
                String value = args[2];

                // Gộp args nếu là tên có khoảng trắng
                if (sub.equalsIgnoreCase("setname") && args.length > 3) {
                    StringBuilder sb = new StringBuilder();
                    for(int i = 2; i < args.length; i++) sb.append(args[i]).append(" ");
                    value = sb.toString().trim();
                }

                try {
                    if (sub.equalsIgnoreCase("setprice")) {
                        double price = Double.parseDouble(value);
                        if (product instanceof ProductImpl) {
                            ((ProductImpl) product).getConfig().setPrice(price);
                            product.getProductCategory().getCategory().getBazaar().saveConfig();
                        }
                        player.sendMessage("§aĐã cập nhật giá mới: " + price);
                        bazaarPlugin.getEditManager().openProductEdit(player, product);
                    }
                    else if (sub.equalsIgnoreCase("setsupply")) {
                        long newSupply = Long.parseLong(value);
                        long currentSupply = product.getCirculatingSupply();
                        // Hàm modify cộng thêm delta, nên muốn set cứng thì delta = new - old
                        product.modifyCirculatingSupply(newSupply - currentSupply);
                        player.sendMessage("§aĐã cập nhật tổng cung: " + newSupply);
                        bazaarPlugin.getEditManager().openProductEdit(player, product);
                    }
                    else if (sub.equalsIgnoreCase("setname")) {
                        product.setName(value);
                        player.sendMessage("§aĐã đổi tên thành: " + value);
                        bazaarPlugin.getEditManager().openProductEdit(player, product);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cGiá trị nhập vào không hợp lệ!");
                }
            }
        }

        return true;
    }
}