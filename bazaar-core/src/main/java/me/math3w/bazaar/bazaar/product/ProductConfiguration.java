package me.math3w.bazaar.bazaar.product;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

@SerializableAs("ProductConfiguration")
public class ProductConfiguration implements ConfigurationSerializable {
    private String name;
    private ItemStack item;
    private ItemStack icon;
    private double price;
    private int supply;

    // Constructor chính
    public ProductConfiguration(String name, ItemStack item, ItemStack icon, double price, int supply) {
        this.name = name;
        this.item = item;
        this.icon = icon;
        this.price = price;
        this.supply = supply;
    }

    // [FIX] Constructor tương thích ngược cho BazaarConfig (cấu trúc 1)
    public ProductConfiguration(ItemStack item, ItemStack icon, String name, double price) {
        this(name, item, icon, price, 100000);
    }

    // [FIX] Constructor tương thích ngược cho BazaarConfig (cấu trúc 2)
    public ProductConfiguration(String name, ItemStack item, ItemStack icon, double price) {
        this(name, item, icon, price, 100000);
    }

    public ProductConfiguration(ItemStack item, String name) {
        this(name, item, item, 100.0, 100000);
    }

    public static ProductConfiguration deserialize(Map<String, Object> args) {
        String name = (String) args.get("name");
        ItemStack item = (ItemStack) args.get("item");
        ItemStack icon = (ItemStack) args.get("icon");
        double price = (Double) args.get("price");
        int supply = args.containsKey("supply") ? (Integer) args.get("supply") : 100000;

        return new ProductConfiguration(name, item, icon, price, supply);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("item", item);
        map.put("icon", icon);
        map.put("price", price);
        map.put("supply", supply);
        return map;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getSupply() { return supply; }
    public void setSupply(int supply) { this.supply = supply; }
}