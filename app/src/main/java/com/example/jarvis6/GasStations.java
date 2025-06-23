package com.example.jarvis6;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

class GasStations {
    // Currently GoogleCustomSearch is not used!
   private final static String GoogleCustomSearchAPI_KEY = "AIzaSyD1y2VKZg7GNpcLVSOwUHjd6tTnSc0oJU0"; // Up to 100 calls a day free, for more -> 5$ per 1000 calls
   private final static String GoogleCustomSearchAPI_CX = "92048df1d29444cd9";

   private static final ExecutorService executor = Executors.newSingleThreadExecutor();
   private static Context context;

   public GasStations(Context context) {
       GasStations.context = context;
   }

   // DATATYPE - NOT CONSTRUCTOR
   public record GasStation(String name, String address, String distance) {}


    public static void parseUrlAsync(String finalSearchAddress, Consumer<String> callback) {
        executor.submit(() -> {
            var result = parseUrl(String.format("https://www.benzinpreis-blitz.de/?land=de&suchfeld=%s", finalSearchAddress));

            if (callback != null)
                callback.accept(result);
        });
    }

   /**
    * @param inputQuery
    * @return urls: list of retrieved uids for a chosen topic
    * We use the google Programmable Search Engine to retrieve urls containing gas stations information
    */
   public static List<String> searchFor(String inputQuery) {
      var urls = new ArrayList<String>();
      System.out.println("suchmethode wurde gestartet... ");

      try {
         StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
         StrictMode.setThreadPolicy(policy);

         var query = inputQuery.replace(" ", "+");
         var urlStr = "https://www.googleapis.com/customsearch/v1?key=" + GoogleCustomSearchAPI_KEY + "&cx=" + GoogleCustomSearchAPI_CX + "&q=" + query;
         var url = new URL(urlStr);
         var connection = (HttpURLConnection) url.openConnection();
         connection.setRequestMethod("GET");

         var in = new BufferedReader((new InputStreamReader(connection.getInputStream())));
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
         System.err.println("following error occurred collecting gas stations with google custom search: " + e);
      }

      return urls;
   }

   public static String parseUrl(String url) {
       if (url == null || url.isEmpty()) return "";

       System.out.println(url);

       var sb = new StringBuilder();
       try {
           Document doc = Jsoup.connect(url)
                   .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                   .get();

           var rows = doc.select("table.spritpreis_liste tbody tr");
           for (int i = 0; i < rows.size() - 1; i++) {
               Element row1 = rows.get(i);
               Element row2 = rows.get(i + 1);

               if (!row1.select(".marke").isEmpty() && row2.hasClass("border")) {
                   String marke = row1.select(".marke").text();

                   String preis1 = row1.select(".preis_part1").text();
                   String preis2 = row1.select(".preis_part2").text();
                   String preis = preis1 + "," + preis2;

                   String adresse = row2.select(".tankstelle_adresse").html().replace("<br>", ", ").replaceAll("\\s+", " ").trim();
                   String distanz = row2.select(".distance").text();

                   sb.append(marke).append(", ").append(adresse).append(" ").append(distanz).append(" ").append(preis).append("\n");

                   i++; // skip next row, as it's already processed
               }
           }
       } catch (Exception e) {
           System.err.println("Fehler beim Parsen der URL: " + e.getMessage());
       }

       return sb.toString();
   }
}