package me.math3w.bazaar.bazaar.product;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ProductConfiguration implements ConfigurationSerializable {
    private ItemStack item;
    private ItemStack icon;
    private String name;
    // [NEW] Thêm trường giá gốc
    private double price;

    public ProductConfiguration(ItemStack item, ItemStack icon, String name, double price) {
        this.item = item;
        this.icon = icon;
        this.name = name;
        this.price = price;
    }

    // Constructor cũ để tương thích (mặc định giá 10.0)
    public ProductConfiguration(ItemStack item, ItemStack icon, String name) {
        this(item, icon, name, 10.0);
    }

    public static ProductConfiguration deserialize(Map<String, Object> args) {
        // [NEW] Đọc giá từ file, nếu không có thì mặc định 10.0
        double price = args.containsKey("price") ? ((Number) args.get("price")).doubleValue() : 10.0;

        return new ProductConfiguration(
                (ItemStack) args.get("item"),
                (ItemStack) args.get("icon"),
                (String) args.get("name"),
                price
        );
    }

    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // [NEW] Getter & Setter cho giá
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> args = new HashMap<>();
        args.put("item", item);
        args.put("icon", icon);
        args.put("name", name);
        // [NEW] Lưu giá vào file
        args.put("price", price);
        return args;
    }
}