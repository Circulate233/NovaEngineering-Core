package github.kasuminova.novaeng.client.hitokoto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class HitokotoAPI {
    public static final String API_URL = "https://v1.hitokoto.cn/";
    private static final Gson DESERIALIZER = new GsonBuilder()
        .registerTypeHierarchyAdapter(HitokotoResult.class, new HitokotoDeserializer())
        .create();
    private static final AtomicBoolean loading = new AtomicBoolean(false);
    private static volatile String hitokotoCache = null;

    public static String getHitokotoCache() {
        return hitokotoCache;
    }

    public static String getRandomHitokoto() {
        if (hitokotoCache != null) {
            return hitokotoCache;
        }
        if (!loading.compareAndSet(false, true)) {
            return "";
        }

        try {
            String jsonStr;
            try {
                jsonStr = getStringFromURL(API_URL);
            } catch (IOException e) {
                return "";
            }

            if (jsonStr == null || jsonStr.isEmpty()) {
                return "";
            }

            HitokotoResult hitokoto;
            try {
                hitokoto = JsonUtils.fromJson(DESERIALIZER, jsonStr, HitokotoResult.class, true);
            } catch (Exception e) {
                return "";
            }

            if (hitokoto == null) {
                return "";
            }

            String assembled = assembleHitokoto(hitokoto);
            if (!assembled.isEmpty()) {
                hitokotoCache = assembled;
            }
            return assembled;
        } finally {
            loading.set(false);
        }
    }

    public static String assembleHitokoto(HitokotoResult result) {
        String hitokoto = result.hitokoto();
        String fromWho = result.fromWho();
        if (fromWho.isEmpty()) {
            fromWho = result.from();
            if (fromWho.isEmpty()) {
                fromWho = result.creator();
            }
        }

        if (hitokoto != null && fromWho != null) {
            return hitokoto + " —— " + fromWho;
        }

        return "";
    }

    public static String getStringFromURL(String urlStr) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.connect();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return stringBuilder.toString();
            }
        } catch (MalformedURLException e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
