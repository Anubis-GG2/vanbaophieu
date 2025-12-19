package me.math3w.bazaar.utils;

import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordService {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1450480673939914824/9hrph42Z-x7OgVALzm9KAM_0YGZqGSTU35txoczUMEpOWjtY29425ZnoRjnkyncSP720";

    public static void sendManipulationAlert(String targetName, boolean isCategory, boolean isIncrease, double percent, int hours) {
        Bukkit.getScheduler().runTaskAsynchronously(me.math3w.bazaar.BazaarPlugin.getPlugin(me.math3w.bazaar.BazaarPlugin.class), () -> {
            try {
                String trend = isIncrease ? "TĂNG !!!" : "GIẢM !!!";
                String targetType = isCategory ? "Danh Mục" : "Cổ phiếu";
                int color = isIncrease ? 65280 : 16711680;

                String jsonPayload = String.format(
                        "{" +
                                "  \"content\": \"@everyone\"," +
                                "  \"username\": \"Thiên Đạo StockBot\"," +
                                "  \"embeds\": [{" +
                                "    \"title\": \"⚠ WARNING: Thiên Đạo Xoay Chuyển Vạn Bảo Phiếu !!!\"," +
                                "    \"color\": %d," +
                                "    \"fields\": [" +
                                "      {\"name\": \"Xu hướng\", \"value\": \"**%s**\", \"inline\": true}," +
                                "      {\"name\": \"%s\", \"value\": \"**%s**\", \"inline\": true}," +
                                "      {\"name\": \"Giá trị\", \"value\": \"**%.0f%%**\", \"inline\": true}," +
                                "      {\"name\": \"Thời gian\", \"value\": \"**%dh**\", \"inline\": true}" +
                                "    ]" +
                                "  }]" +
                                "}",
                        color, trend, targetType, targetName, percent, hours
                );

                sendRawWebhook(jsonPayload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void sendRawWebhook(String jsonPayload) {
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            connection.getInputStream().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}