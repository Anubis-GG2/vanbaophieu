package me.math3w.bazaar.map;

import me.math3w.bazaar.api.bazaar.Product;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.Collections;
import java.util.List;

public class StockMapRenderer extends MapRenderer {
    private final Product product;

    public StockMapRenderer(Product product) {
        super(false);
        this.product = product;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                canvas.setPixel(x, y, MapPalette.matchColor(20, 20, 25)); // Dark Background
            }
        }

        List<Double> history = product.getPriceHistory();
        if (history.isEmpty() || history.size() < 2) return;

        double minPrice = Collections.min(history);
        double maxPrice = Collections.max(history);
        double range = maxPrice - minPrice;
        if (range == 0) range = 1;

        minPrice -= range * 0.1;
        maxPrice += range * 0.1;
        range = maxPrice - minPrice;

        double startPrice = history.get(0);
        double endPrice = history.get(history.size() - 1);
        byte color = (endPrice >= startPrice) ? MapPalette.matchColor(50, 205, 50) : MapPalette.matchColor(220, 20, 60);
        int width = 128;
        int height = 128;

        double xStep = (double) width / (history.size() - 1);

        for (int i = 0; i < history.size() - 1; i++) {
            double price1 = history.get(i);
            double price2 = history.get(i + 1);

            int x1 = (int) (i * xStep);
            int y1 = height - (int) ((price1 - minPrice) / range * height);

            int x2 = (int) ((i + 1) * xStep);
            int y2 = height - (int) ((price2 - minPrice) / range * height);

            drawLine(canvas, x1, y1, x2, y2, color);
        }

    }

    private void drawLine(MapCanvas canvas, int x1, int y1, int x2, int y2, byte color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x1 >= 0 && x1 < 128 && y1 >= 0 && y1 < 128) {
                canvas.setPixel(x1, y1, color);
            }

            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err = err - dy;
                x1 = x1 + sx;
            }
            if (e2 < dx) {
                err = err + dx;
                y1 = y1 + sy;
            }
        }
    }
}