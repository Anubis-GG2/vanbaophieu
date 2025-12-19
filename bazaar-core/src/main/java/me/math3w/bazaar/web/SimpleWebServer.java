package me.math3w.bazaar.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import me.math3w.bazaar.BazaarPlugin;
import me.math3w.bazaar.api.bazaar.Category;
import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.StockCandle;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SimpleWebServer {

    private final BazaarPlugin plugin;
    private HttpServer server;
    private final int port = 8081;

    public SimpleWebServer(BazaarPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // 1. Endpoint trả về HTML giao diện (TradingView Style)
            server.createContext("/", exchange -> {
                String html = getDashboardHtml();
                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });

            // 2. Endpoint API: Trả về danh sách Stock GOM NHÓM theo Category
            server.createContext("/api/stocks", exchange -> {
                JsonArray rootArray = new JsonArray();

                // Lặp qua từng danh mục có trong server
                for (Category cat : plugin.getBazaar().getCategories()) {
                    JsonObject catObj = new JsonObject();
                    catObj.addProperty("categoryName", ChatColor.stripColor(cat.getName())); // Bỏ mã màu Minecraft

                    JsonArray productsArray = new JsonArray();
                    boolean hasProduct = false;

                    // Tìm các product thuộc category này
                    for (Product p : plugin.getBazaar().getProducts()) {
                        // Logic kiểm tra category của product
                        if (p.getProductCategory() != null &&
                                p.getProductCategory().getCategory().getName().equals(cat.getName())) {

                            JsonObject pObj = new JsonObject();
                            pObj.addProperty("id", p.getId());
                            pObj.addProperty("name", ChatColor.stripColor(p.getName()));
                            pObj.addProperty("price", plugin.getMarketTicker().getCurrentPrice(p));
                            // Tính % thay đổi (Change 24h) giả lập hoặc thực tế
                            double open = p.getCandleHistory().isEmpty() ? 0 : p.getCandleHistory().get(0).getOpen();
                            double current = plugin.getMarketTicker().getCurrentPrice(p);
                            double change = open == 0 ? 0 : ((current - open) / open) * 100;
                            pObj.addProperty("change", change);

                            productsArray.add(pObj);
                            hasProduct = true;
                        }
                    }

                    // Chỉ thêm category vào danh sách nếu nó có ít nhất 1 sản phẩm
                    if (hasProduct) {
                        catObj.add("products", productsArray);
                        rootArray.add(catObj);
                    }
                }

                sendJson(exchange, rootArray.toString());
            });

            // 3. Endpoint API: Lấy dữ liệu nến (History)
            server.createContext("/api/history", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String stockId = query != null && query.contains("id=") ? query.split("id=")[1] : null;

                Product product = plugin.getBazaar().getProduct(stockId);
                if (product == null) {
                    sendJson(exchange, "[]");
                    return;
                }

                JsonArray data = new JsonArray();
                List<StockCandle> history = product.getCandleHistory();

                for (StockCandle c : history) {
                    JsonObject candle = new JsonObject();
                    candle.addProperty("time", c.getTimestamp() / 1000);
                    candle.addProperty("open", c.getOpen());
                    candle.addProperty("high", c.getHigh());
                    candle.addProperty("low", c.getLow());
                    candle.addProperty("close", c.getClose());
                    data.add(candle);
                }
                sendJson(exchange, data.toString());
            });

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            plugin.getLogger().info("Web Dashboard (TradingView Mode) đang chạy tại: http://localhost:" + port);

        } catch (IOException e) {
            plugin.getLogger().severe("Không thể khởi động Web Server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void sendJson(com.sun.net.httpserver.HttpExchange exchange, String jsonResponse) throws IOException {
        byte[] response = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
    }

    private String getDashboardHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Stock Marketplace</title>\n" +
                "    <script src=\"https://unpkg.com/lightweight-charts/dist/lightweight-charts.standalone.production.js\"></script>\n" +
                "    <style>\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; background: #131722; color: #d1d4dc; margin: 0; display: flex; height: 100vh; overflow: hidden; }\n" +
                "        /* Sidebar Styling giống TradingView */\n" +
                "        #sidebar { width: 300px; background: #1e222d; border-right: 1px solid #2a2e39; display: flex; flex-direction: column; }\n" +
                "        .sidebar-header { padding: 15px; border-bottom: 1px solid #2a2e39; font-weight: bold; font-size: 1.1em; color: #fff; display: flex; justify-content: space-between; align-items: center; }\n" +
                "        #stock-list { overflow-y: auto; flex-grow: 1; }\n" +
                "        \n" +
                "        /* Category Header */\n" +
                "        .category-header { background: #2a2e39; color: #8e95a5; padding: 8px 15px; font-size: 0.85em; font-weight: bold; text-transform: uppercase; letter-spacing: 1px; margin-top: 10px; }\n" +
                "        \n" +
                "        /* Stock Item Styling */\n" +
                "        .stock-item { padding: 10px 15px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #2a2e39; transition: 0.2s; }\n" +
                "        .stock-item:hover { background: #2a2e39; }\n" +
                "        .stock-item.active { background: #2962ff; color: white; }\n" +
                "        .stock-info { display: flex; flex-direction: column; }\n" +
                "        .stock-symbol { font-weight: bold; font-size: 0.95em; }\n" +
                "        .stock-name { font-size: 0.8em; color: #787b86; }\n" +
                "        .stock-item.active .stock-name { color: #e0e0e0; }\n" +
                "        .stock-price-box { text-align: right; }\n" +
                "        .price-val { font-weight: bold; display: block; }\n" +
                "        .price-change { font-size: 0.8em; }\n" +
                "        .up { color: #00E396; } .down { color: #FF4560; }\n" +
                "        .stock-item.active .up, .stock-item.active .down { color: white; }\n" +
                "\n" +
                "        #chart-area { flex-grow: 1; display: flex; flex-direction: column; }\n" +
                "        #chart-header { padding: 15px; background: #131722; font-size: 1.2em; font-weight: bold; border-bottom: 1px solid #2a2e39; }\n" +
                "        #chart-container { flex-grow: 1; position: relative; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"sidebar\">\n" +
                "        <div class=\"sidebar-header\">Market Watch <span style=\"font-size:0.8em; color:#5d606b\">LIVE</span></div>\n" +
                "        <div id=\"stock-list\">Loading...</div>\n" +
                "    </div>\n" +
                "    <div id=\"chart-area\">\n" +
                "        <div id=\"chart-header\">Symbol Info</div>\n" +
                "        <div id=\"chart-container\"></div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        // Setup Chart\n" +
                "        const chart = LightweightCharts.createChart(document.getElementById('chart-container'), {\n" +
                "            layout: { backgroundColor: '#131722', textColor: '#d1d4dc' },\n" +
                "            grid: { vertLines: { color: '#2B2B43' }, horzLines: { color: '#2B2B43' } },\n" +
                "            crosshair: { mode: LightweightCharts.CrosshairMode.Normal },\n" +
                "            rightPriceScale: { borderColor: '#2B2B43' },\n" +
                "            timeScale: { borderColor: '#2B2B43', timeVisible: true, secondsVisible: false },\n" +
                "        });\n" +
                "        const candleSeries = chart.addCandlestickSeries({ \n" +
                "            upColor: '#00E396', downColor: '#FF4560', borderVisible: false, wickUpColor: '#00E396', wickDownColor: '#FF4560' \n" +
                "        });\n" +
                "        let currentStockId = null;\n" +
                "\n" +
                "        // Hàm render danh sách stock theo Category\n" +
                "        async function loadStocks() {\n" +
                "            try {\n" +
                "                const res = await fetch('/api/stocks');\n" +
                "                const categories = await res.json();\n" +
                "                const listDiv = document.getElementById('stock-list');\n" +
                "                \n" +
                "                // Giữ lại vị trí cuộn chuột\n" +
                "                const scrollTop = listDiv.scrollTop;\n" +
                "                listDiv.innerHTML = '';\n" +
                "\n" +
                "                categories.forEach(cat => {\n" +
                "                    // 1. Vẽ tiêu đề Category\n" +
                "                    const catHeader = document.createElement('div');\n" +
                "                    catHeader.className = 'category-header';\n" +
                "                    catHeader.innerText = cat.categoryName;\n" +
                "                    listDiv.appendChild(catHeader);\n" +
                "\n" +
                "                    // 2. Vẽ các Stock trong Category đó\n" +
                "                    cat.products.forEach(s => {\n" +
                "                        const item = document.createElement('div');\n" +
                "                        item.className = `stock-item ${currentStockId === s.id ? 'active' : ''}`;\n" +
                "                        \n" +
                "                        // Tính màu % change\n" +
                "                        const changeClass = s.change >= 0 ? 'up' : 'down';\n" +
                "                        const changeSign = s.change >= 0 ? '+' : '';\n" +
                "\n" +
                "                        item.innerHTML = `\n" +
                "                            <div class=\"stock-info\">\n" +
                "                                <span class=\"stock-symbol\">${s.name}</span>\n" +
                "                                <span class=\"stock-name\">ID: ${s.id}</span>\n" +
                "                            </div>\n" +
                "                            <div class=\"stock-price-box\">\n" +
                "                                <span class=\"price-val\">$${s.price.toFixed(2)}</span>\n" +
                "                                <span class=\"price-change ${changeClass}\">${changeSign}${s.change.toFixed(2)}%</span>\n" +
                "                            </div>\n" +
                "                        `;\n" +
                "                        item.onclick = () => { selectStock(s.id, s.name); };\n" +
                "                        listDiv.appendChild(item);\n" +
                "                    });\n" +
                "                });\n" +
                "                listDiv.scrollTop = scrollTop;\n" +
                "\n" +
                "                // Tự động chọn stock đầu tiên nếu chưa chọn gì\n" +
                "                if (!currentStockId && categories.length > 0 && categories[0].products.length > 0) {\n" +
                "                    selectStock(categories[0].products[0].id, categories[0].products[0].name);\n" +
                "                }\n" +
                "            } catch (e) { console.error(e); }\n" +
                "        }\n" +
                "\n" +
                "        function selectStock(id, name) {\n" +
                "            currentStockId = id;\n" +
                "            document.getElementById('chart-header').innerText = name + \" - Historical Chart\";\n" +
                "            loadChart(id);\n" +
                "            loadStocks(); // Redraw sidebar to update active class\n" +
                "        }\n" +
                "\n" +
                "        async function loadChart(id) {\n" +
                "            const res = await fetch('/api/history?id=' + id);\n" +
                "            const data = await res.json();\n" +
                "            candleSeries.setData(data);\n" +
                "        }\n" +
                "\n" +
                "        // Realtime Polling (2 giây/lần)\n" +
                "        setInterval(() => {\n" +
                "            loadStocks();\n" +
                "            if (currentStockId) loadChart(currentStockId);\n" +
                "        }, 2000);\n" +
                "\n" +
                "        loadStocks();\n" +
                "\n" +
                "        // Auto Resize\n" +
                "        new ResizeObserver(entries => {\n" +
                "            if (entries.length === 0 || entries[0].target !== document.getElementById('chart-container')) return;\n" +
                "            const newRect = entries[0].contentRect;\n" +
                "            chart.applyOptions({ width: newRect.width, height: newRect.height });\n" +
                "        }).observe(document.getElementById('chart-container'));\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}