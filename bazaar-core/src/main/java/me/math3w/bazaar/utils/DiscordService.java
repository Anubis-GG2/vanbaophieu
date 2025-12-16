package me.math3w.bazaar.utils;

import me.math3w.bazaar.api.bazaar.Product;
import me.math3w.bazaar.api.bazaar.StockCandle;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class DiscordService {

    private static final String WEBHOOK_URL = "webhook";

    public enum TimeFrame {
        M1(1, "1 Phút", 30 * 60 * 1000),
        M5(5, "5 Phút", 2 * 60 * 60 * 1000),
        M15(15, "15 Phút", 6 * 60 * 60 * 1000),
        M30(30, "30 Phút", 24 * 60 * 60 * 1000),
        H1(60, "1 Giờ", 48 * 60 * 60 * 1000),
        D1(1440, "1 Ngày", 30L * 24 * 60 * 60 * 1000);

        final int minutes;
        final String name;
        final long durationMillis;

        TimeFrame(int minutes, String name, long durationMillis) {
            this.minutes = minutes;
            this.name = name;
            this.durationMillis = durationMillis;
        }
    }

    public static void sendCandlestickChart(Product product, TimeFrame timeFrame) {
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Bazaar"), () -> {
            try {
                List<StockCandle> rawHistory = product.getCandleHistory();
                if (rawHistory == null || rawHistory.isEmpty()) return;

                List<StockCandle> candles = processCandlesForTimeFrame(rawHistory, timeFrame);

                if (candles.isEmpty()) return;

                StringBuilder labelsBuilder = new StringBuilder("[");
                StringBuilder dataBuilder = new StringBuilder("[");

                SimpleDateFormat sdf = new SimpleDateFormat(timeFrame == TimeFrame.D1 ? "dd/MM" : "HH:mm");
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

                for (int i = 0; i < candles.size(); i++) {
                    StockCandle c = candles.get(i);
                    String timeLabel = sdf.format(new Date(c.getTimestamp()));

                    labelsBuilder.append("\"").append(timeLabel).append("\"");

                    dataBuilder.append(String.format("{\"o\":%.2f, \"h\":%.2f, \"l\":%.2f, \"c\":%.2f}",
                            c.getOpen(), c.getHigh(), c.getLow(), c.getClose()));

                    if (i < candles.size() - 1) {
                        labelsBuilder.append(",");
                        dataBuilder.append(",");
                    }
                }
                labelsBuilder.append("]");
                dataBuilder.append("]");

                String chartConfig = "{"
                        + "  \"type\": \"candlestick\","
                        + "  \"data\": {"
                        + "    \"labels\": " + labelsBuilder.toString() + ","
                        + "    \"datasets\": [{"
                        + "      \"label\": \"" + product.getName() + "\","
                        + "      \"data\": " + dataBuilder.toString() + ","
                        + "      \"color\": {"
                        + "        \"up\": \"#00E396\","
                        + "        \"down\": \"#FF4560\","
                        + "        \"unchanged\": \"#775DD0\""
                        + "      },"
                        + "      \"borderColor\": \"#ffffff\","
                        + "      \"borderWidth\": 1"
                        + "    }]"
                        + "  },"
                        + "  \"options\": {"
                        + "    \"legend\": { \"display\": false },"
                        + "    \"scales\": {"
                        + "      \"xAxes\": [{"
                        + "        \"ticks\": { \"fontColor\": \"#ccc\", \"maxTicksLimit\": 10 },"
                        + "        \"gridLines\": { \"display\": false }"
                        + "      }],"
                        + "      \"yAxes\": [{"
                        + "        \"ticks\": { \"fontColor\": \"#ccc\" },"
                        + "        \"gridLines\": { \"color\": \"#333\" }"
                        + "      }]"
                        + "    }"
                        + "  }"
                        + "}";

                String shortChartUrl = getShortChartUrl(chartConfig);

                if (shortChartUrl != null) {
                    double currentPrice = candles.get(candles.size() - 1).getClose();
                    sendWebhook(product.getName(), shortChartUrl, currentPrice, timeFrame.name);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static List<StockCandle> processCandlesForTimeFrame(List<StockCandle> rawHistory, TimeFrame tf) {
        long now = System.currentTimeMillis();
        long startTime = now - tf.durationMillis;
        long periodMillis = tf.minutes * 60000L;

        TreeMap<Long, StockCandle> resampledMap = new TreeMap<>();

        for (StockCandle c : rawHistory) {
            if (c.getTimestamp() < startTime) continue;

            long bucketTime = c.getTimestamp() - (c.getTimestamp() % periodMillis);

            if (resampledMap.containsKey(bucketTime)) {
                StockCandle current = resampledMap.get(bucketTime);
                double newHigh = Math.max(current.getHigh(), c.getHigh());
                double newLow = Math.min(current.getLow(), c.getLow());
                double newClose = c.getClose();
                double newVol = current.getVolume() + c.getVolume();

                resampledMap.put(bucketTime, new StockCandle(bucketTime, current.getOpen(), newHigh, newLow, newClose, newVol));
            } else {
                resampledMap.put(bucketTime, new StockCandle(bucketTime, c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()));
            }
        }

        List<StockCandle> result = new ArrayList<>();
        long cursor = startTime - (startTime % periodMillis);
        double lastClose = rawHistory.isEmpty() ? 100.0 : rawHistory.get(0).getClose();

        while (cursor <= now) {
            if (resampledMap.containsKey(cursor)) {
                StockCandle candle = resampledMap.get(cursor);
                result.add(candle);
                lastClose = candle.getClose();
            } else {
                result.add(new StockCandle(cursor, lastClose, lastClose, lastClose, lastClose, 0));
            }
            cursor += periodMillis;
        }

        return result;
    }

    private static String getShortChartUrl(String chartConfig) {
        try {
            URL url = new URL("https://quickchart.io/chart/create");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"chart\": " + chartConfig + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            String respStr = response.toString();
            int urlIndex = respStr.indexOf("\"url\":\"");
            if (urlIndex != -1) {
                int start = urlIndex + 7;
                int end = respStr.indexOf("\"", start);
                return respStr.substring(start, end);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void sendWebhook(String stockName, String imageUrl, double currentPrice, String tf) {
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonPayload = String.format(
                    "{" +
                            "  \"username\": \"Thiên Đạo Vẽ Chart\"," +
                            "  \"embeds\": [{" +
                            "    \"title\": \"Biểu đồ: %s | Khung: %s\"," +
                            "    \"description\": \"Giá đóng cửa: **%s**\"," +
                            "    \"color\": 3447003," +
                            "    \"image\": { \"url\": \"%s\" }" +
                            "  }]" +
                            "}",
                    stockName, tf, me.math3w.bazaar.utils.Utils.getTextPrice(currentPrice), imageUrl
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            connection.getInputStream().close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}