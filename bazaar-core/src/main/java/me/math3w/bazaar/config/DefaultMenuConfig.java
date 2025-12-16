package me.math3w.bazaar.config;

import me.math3w.bazaar.api.config.MenuConfig;
import me.math3w.bazaar.api.config.MessagePlaceholder;
import me.math3w.bazaar.api.config.Placeholder;
import me.math3w.bazaar.utils.Utils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultMenuConfig extends CustomConfig implements MenuConfig {
    public DefaultMenuConfig(JavaPlugin plugin) {
        super(plugin, "menu");
    }

    public static List<String> formatStringList(List<String> strings, Placeholder... placeholders) {
        for (Placeholder placeholder : placeholders) { strings.replaceAll(placeholder::replace); }
        strings.replaceAll(Utils::colorize);
        return strings;
    }
    private static String formatString(String string, Placeholder... placeholders) {
        for (Placeholder placeholder : placeholders) { string = placeholder.replace(string); }
        return Utils.colorize(string);
    }

    @Override
    protected void addDefaults() {
        addDefault("productcategory-lore", Collections.singletonList("&8%products% products"));
        addDefault("product-lore", Arrays.asList(
                "&7Giá Mua: &6%buy-price% &6linh thạch",
                "&7Giá Bán: &6%sell-price% &6linh thạch"
        ));

        addDefault("product", Collections.singletonList("&8%product%"));
        addDefault("sell-inventory", Arrays.asList("%items%", "", "&7Bạn nhận: &6%earned-coins% &6linh thạch", "", "&eBấm để Bán!"));
        addDefault("sell-inventory-none", Arrays.asList("&cBạn không có gì để", "&cbán!"));
        addDefault("item", Collections.singletonList(" &a%item-amount%&7x &f%item-name% &7với &6%item-coins% linh thạch"));
        addDefault("buy-instantly", Arrays.asList(
                "&7Price per unit: &6%buy-price% &6coins",
                "&7Approx Stack price: &6%stack-buy-price% &6coins"
        ));
        addDefault("buy-instantly-amount-sign", Arrays.asList("", "^^^^^^^^^^^^^^^", "Nhập số lượng", "để mua"));
        addDefault("sell-instantly", Arrays.asList(
                "&7Inventory: &a%item-amount% items",
                "",
                "&7Amount: &a%item-amount%&7x",
                "&7Total: &6%coins% &6coins"
        ));
        addDefault("sell-instantly-amount-sign", Arrays.asList("", "^^^^^^^^^^^^^^^", "Nhập số lượng", "để bán"));
        addDefault("buy-order", Collections.singletonList("&8- &6%coins% coins&7 each | &a%amount%&7x"));
        addDefault("buy-order-none", Collections.singletonList("&cGiao dịch đã tạm ngưng"));
        addDefault("buy-order-amount-sign", Arrays.asList("", "^^^^^^^^^^^^^^^", "Enter amount", "to order"));
        addDefault("buy-order-price-sign", Arrays.asList("", "^^^^^^^^^^^^^^^", "Enter price", "per unit"));
        addDefault("sell-offer", Collections.singletonList("&8- &6%coins% coins&7 each | &a%amount%&7x"));
        addDefault("sell-offer-none", Collections.singletonList("&cTrading Disabled"));
        addDefault("sell-offer-amount-sign", Arrays.asList("", "^^^^^^^^^^^^^^^", "Enter amount", "to sell"));
        addDefault("sell-offer-price-sign", Arrays.asList("", "^^^^^^^^^^^^^^^", "Enter price", "per unit"));
        addDefault("confirm-lore", Arrays.asList("", "&7Price per unit: &6%unit-price% coins", "", "&7Order: &a%amount%&7x &f%product%", "&7Total price: &6%total-price% coins"));
        addDefault("confirm-instant-lore", Arrays.asList("", "&7Amount: &a%amount%&7x", "&7Price: &6%price% coins"));
        addDefault("order.buy.lore", Arrays.asList("&8Worth %total-coins% coins", "", "&7Order amount: &a%amount%&7x", "&7Filled: &a%filled%&7/%amount% &a&l%percent%%!", "", "&7Price per unit: &6%coins% coins", "", "&aYou have &2%available% items &ato claim", "", "&eClick to claim"));
        addDefault("order.buy.prefix", "&a&lBUY ");
        addDefault("order.sell.lore", Arrays.asList("&8Worth %total-coins% coins", "", "&7Offer amount: &a%amount%&7x", "&7Filled: &6%filled%&7/%amount% &6&l%percent%%!", "", "&7Price per unit: &6%coins% coins", "", "&eYou have &6%available-coins% coins &eto claim", "", "&eClick to claim"));
        addDefault("order.sell.prefix", "&6&lSELL ");
        addDefault("search-sign", Arrays.asList("", "^^^^^^^^^^^^^^^", "Enter Query", ""));
        addDefault("loading", "&7Loading...");
    }

    @Override
    public List<String> getStringList(String path, Placeholder... placeholders) { return formatStringList(getConfig().getStringList(path), placeholders); }
    @Override
    public String getString(String path, MessagePlaceholder... placeholders) { return formatString(getConfig().getString(path, ""), placeholders); }
    @Override
    public ItemStack replaceLorePlaceholders(ItemStack icon, String placeholder, Placeholder... innerPlaceholders) {
        ItemStack newIcon = icon.clone();
        ItemMeta itemMeta = icon.getItemMeta();
        if (itemMeta == null || !itemMeta.hasLore()) return icon;
        List<String> lore = new ArrayList<>(itemMeta.getLore());
        List<String> placeholderLore = this.getStringList(placeholder, innerPlaceholders);
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (!line.equals("%" + placeholder + "%")) continue;
            lore.remove(i);
            for (int j = 0; j < placeholderLore.size(); j++) {
                String placeholderLine = placeholderLore.get(j);
                lore.add(i + j, placeholderLine);
            }
        }
        itemMeta.setLore(lore);
        newIcon.setItemMeta(itemMeta);
        return newIcon;
    }
}