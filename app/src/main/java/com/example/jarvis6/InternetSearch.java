package com.example.jarvis6;

import android.os.Build;
import android.os.StrictMode;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// TODO this uses the Google Custom Search (CSE) with limited API calls. -> Currently not used
class InternetSearch {
    private final static String GoogleCustomSearchAPI_KEY = "AIzaSyD1y2VKZg7GNpcLVSOwUHjd6tTnSc0oJU0"; // Up to 100 calls a day free, for more -> 5$ per 1000 calls
    private final static String GoogleCustomSearchAPI_CX = "a54bde9bb5d6a45a1";

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void parseUrlAsync(String inputQuery, Consumer<String> callback) {
        executor.submit(() -> {
            List<String> urls = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                urls = searchFor(inputQuery);
                System.out.println("url: " + urls.get(0));
            }

            assert urls != null;
            urls.forEach(System.out::println);

            var result = urls.stream()
                    .limit(3)
                    .map(this::parseUrl)
                    .collect(Collectors.joining("\n"));

            System.out.println("result: " + result);

            if (callback != null)
                callback.accept(result);
        });
    }


    private List<String> searchFor(String inputQuery) {
        var urls = new ArrayList<String>();

        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            var query = inputQuery.replace(" ", "+");
            var urlStr = "https://www.googleapis.com/customsearch/v1?key=" + GoogleCustomSearchAPI_KEY + "&cx=" + GoogleCustomSearchAPI_CX + "&q=" + query;

            var url = new URL(urlStr);
            var con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            var in = new BufferedReader((new InputStreamReader(con.getInputStream())));

            var jsonSb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                jsonSb.append(line);
            }

            in.close();

            var gson = new Gson();
            var response = gson.fromJson(jsonSb.toString(), JsonObject.class);
            var items = response.getAsJsonArray("items");

            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    urls.add(item.get("link").getAsString());
                }
            }
        } catch (IOException e) {
            System.err.println("following error occurred searching in the web: " +e);
        }

        return urls;
    }

    private String parseUrl(String url) {
        try {
            var doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();

            doc.select("""
                    script, style, nav, footer, header, noscript, iframe, aside, img, figure, figcaption,
                    form, input, button, select, label, ul.menu, ol.menu, li.menu, div.menu, .navbar, .navigation, .pagination,
                    [class*=ad], [id=*ad], .sidebar, .widget, svg, audio, video, canvas, object, embed, head
                    link[rel=stylesheet], meta, noscript, time, .date, .timestamp
                    """).remove();

            var mainContent = doc.select("div.content, article, main, #main-content, #content, .post-content, .entry-content, .main-content");

            String text;
            if (!mainContent.isEmpty())
                text = mainContent.text();
            else
                text = doc.body().text();

            // var body = doc.body();
            // var text = body.text();

            System.out.println("website content: \n" + text.trim() + "\n" + ("-".repeat(10)));
            return text.trim();
        } catch (IOException e) {
            System.err.println("Fehler beim Parsen der URL: " + e);
            return "Ein Fehler ist aufgetreten.";
        }
    }


    /* private String getCurrentDateTime() {
        var now = LocalDateTime.now();
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return now.format(formatter);
    }

    public void openLink(Context context, String inputQuery) {
        var inputQueryWithTime = inputQuery + " " + getCurrentDateTime();



        var url = "https://www.google.com/search?q=" + Uri.encode(inputQueryWithTime);
        var uri = Uri.parse(url);

        var intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    } */
}
