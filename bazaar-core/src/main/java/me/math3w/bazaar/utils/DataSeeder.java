package me.math3w.bazaar.utils;

import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Category;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DataSeeder {

    private final BazaarPlugin plugin;

    public DataSeeder(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    public void seedDefaults() {
        // Map dữ liệu: Category -> List<StockName>
        Map<String, List<String>> data = new LinkedHashMap<>();

        data.put("Trại Trẻ Nhà Họ Lạc", Arrays.asList(
                "Sushi", "Sushi Thôn Hải", "Hồng Sắc Bảo Thạch", "Lạc Hoàng Tệ",
                "Thần Lôi Tinh Tủy", "Ám Chu Tri", "Chu Tri Độc Nang"
        ));

        data.put("Nhà Sách Hảo Tâm", Arrays.asList(
                "Minh Lam Tử Sa", "Linh Quy Hạch", "Nội Công", "Vạn Niên Mộc Đằng",
                "Cổ Hỏa Hồn Thạch", "Vạn Niên Huyền Thiết", "Thần Kim Tinh Thiết",
                "Đại Luyện Khí Lực Đan", "Dẫn Linh Đan", "Linh Khí Thạch"
        ));

        data.put("Thần Vệ Giới", Arrays.asList(
                "Hư Không Tàn Phiến", "Hư Không Thạch", "Hư Không Tâm Hỏa"
        ));

        data.put("Tam Thập Lục Giới", Arrays.asList(
                "Vạn Binh Thạch", "Cửu Thiên Kiếm Châu", "Cửu Thiên Kiếm Tinh", "Thất Dục Cô Thạch"
        ));

        data.put("Hạ Đường Huyết Giới", Arrays.asList(
                "Hàn Băng Trích Tủy", "Sushi Thôn Hải Bằng Kình", "Thanh Vân Kiếm Thiết"
        ));

        plugin.getLogger().info("--- ĐANG KIỂM TRA DỮ LIỆU MẪU (DATA SEEDING) ---");
        int addedCount = 0;

        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            String categoryName = entry.getKey();
            List<String> stocks = entry.getValue();

            // 1. Kiểm tra và tạo Category nếu chưa có (Logic này giả định config tự tạo category khi add product)
            // Tuy nhiên, để chắc chắn, ta kiểm tra sự tồn tại trong danh sách categories
            boolean catExists = plugin.getBazaar().getCategories().stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(categoryName));

            if (!catExists) {
                plugin.getLogger().info("Phát hiện Category mới: " + categoryName);
            }

            for (String rawName : stocks) {
                // Làm sạch tên (xóa dấu gạch đầu dòng nếu có)
                String stockName = rawName.replace("-", "").trim();
                String stockId = stockName.toLowerCase().replace(" ", "_");

                Product existingProduct = plugin.getBazaar().getProduct(stockId);

                if (existingProduct == null) {
                    // Chưa có -> Tạo mới
                    createStock(categoryName, stockName);
                    addedCount++;
                }
            }
        }

        if (addedCount > 0) {
            plugin.getLogger().info("Đã thêm mới " + addedCount + " cổ phiếu vào hệ thống.");
            plugin.getBazaarConfig().save(); // Lưu config quan trọng
            // Reload lại Bazaar để nhận diện item mới
            plugin.getBazaar().reload();
        } else {
            plugin.getLogger().info("Dữ liệu đã đầy đủ. Không cần thêm mới.");
        }
    }

    private void createStock(String categoryName, String stockName) {
        // Tạo Item ảo để làm Icon (Mặc định là PAPER)
        ItemStack icon = new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a" + stockName);
            icon.setItemMeta(meta);
        }

        // Tạo Configuration cho Product
        // Lưu ý: Hàm getProductConfiguration thường sẽ tạo mới config object nếu chưa có
        ProductConfiguration productConfig = plugin.getBazaarConfig().getProductConfiguration(icon, stockName);

        // Set giá mặc định
        productConfig.setPrice(100.0);

        // Thêm vào Category (Hàm này trong BazaarConfig sẽ tự xử lý việc lưu vào file YML)
        plugin.getBazaarConfig().addProductToCategory(categoryName, productConfig);

        plugin.getLogger().info(" -> Đã tạo: [" + categoryName + "] " + stockName);
    }
}