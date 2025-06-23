package com.example.jarvis6;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.core.app.ActivityCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class InternetSearch1 {
    private final static String DUCKDUCKGO_SEARCH_URL = "https://duckduckgo.com/html/?q=";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface SearchCallback {
        void onFinished(String result);
    }

    public void parseUrlAsync(String query, SearchCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String result = getSearchResults(query);
            new Handler(Looper.getMainLooper()).post(() -> callback.onFinished(result));
        });
    }

    /* public void parseUrlAsync(String inputQuery, Consumer<String> callback) {
        executor.submit(() -> {
            var result = getSearchResults(inputQuery);
            System.out.println("res: " + result);
            if (callback != null)
                callback.accept(result);
        });
    } */

    public String getSearchResults(String query) {
        Document doc = null;
        var webContent = "";
        try {
            doc = Jsoup.connect(DUCKDUCKGO_SEARCH_URL + query).get();
            var results = doc.getElementById("links").getElementsByClass("results_links");

            if (results.size() < 1) return "No search results found";

            int randomIndex = new Random().nextInt(Math.min(3, results.size()));
            var firstTitle = results.get(randomIndex)
                    .getElementsByClass("links_main")
                    .first()
                    .getElementsByTag("a")
                    .first()
                    .attr("href");

            System.out.println("url: " + firstTitle);
            var doc1 = Jsoup.connect(firstTitle)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();

            doc1.select("""
                    script, style, nav, footer, header, noscript, iframe, aside, img, figure, figcaption,
                    form, input, button, select, label, ul.menu, ol.menu, li.menu, div.menu, .navbar, .navigation, .pagination,
                    [class*=ad], [id=*ad], .sidebar, .widget, svg, audio, video, canvas, object, embed, head
                    link[rel=stylesheet], meta, noscript, time, .date, .timestamp
                    """).remove();

            var mainContent = doc1.select("div.content, article, main, #main-content, #content, .post-content, .entry-content, .main-content");

            String text;
            if (!mainContent.isEmpty())
                text = mainContent.text();
            else
                text = doc1.body().text();

            webContent = text.trim();
            System.out.println("website content: \n" + text.trim() + "\n" + ("-".repeat(10)));
        } catch (IOException e) {
            System.err.println("following error occured, while parsing Web: " + e.getMessage());
        }

        return webContent.isEmpty() ? "No results found" : webContent;
    }

    public void openLink(Context context, String inputQuery) {
        // var inputQueryWithTime = inputQuery + " " + getCurrentDateTime();

        var url = "https://www.google.com/search?q=" + Uri.encode(inputQuery);
        var uri = Uri.parse(url);

        var intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }


    public void searchInternet(Context context, WeakReference<MainActivity> mainActivityRef, SpeechRecognitionManager speechRecognitionManager, TextToSpeech tts, String query) {
        final int LOCATION_REQUEST_PERMISSION_REQUEST_CODE = 1001;
        var modQuery = replaceGpsWithGeo(context, query);
        new InternetSearch1().parseUrlAsync(modQuery, (InternetSearch1.SearchCallback) searchResult -> {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mainActivityRef.get(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_PERMISSION_REQUEST_CODE);
            } else {

                var currentTime = DateTime.getCurrentDateTime();

                // GPS callback
                new GPSLocationHelper(context).getCurrentLocation(new GPSLocationHelper.GPSCallback() {
                    @Override
                    public void onLocationReceived(GPSLocation location) {
                        speechRecognitionManager.stop();

                        var geocodedLocation = getGeoCoded(context, location);
                        var message = String.format("""
                                Auf diese Frage hin: %s, wurden diese Informationen von den aufgesuchten Websiten geparst: %s.
                                Außerdem gibt es Informationen über meinen aktuellen Standord: Ich bin gerade in oder in der Nähe von %s,
                                und das aktuelle Datum und die Uhrzeit ist: %s.
                                Nutze diese Informationen für die Antwort.
                                Bitte gib nie die URLs gleich mit raus, nennen sie auch nicht. Merke es dir nur für dann, wenn ich explizit danach frage.
                                """, modQuery, searchResult, geocodedLocation, currentTime);

                        if (!searchResult.isEmpty()) {
                            new GPT(context, mainActivityRef.get(), speechRecognitionManager, tts).execute(new Message(currentTime, location, message));
                            addToConversation(context, message);
                        }
                    }

                    @Override
                    public void onLocationError(String errorMessage) {
                        speechRecognitionManager.stop();

                        var defaultLocation = new GPSLocation(48.9177, 8.7865);
                        var defaultGeoCodedLocation = getGeoCoded(context, defaultLocation);
                        var message = String.format("""
                                Auf diese Frage hin: %s, wurden diese Informationen von den aufgesuchten Websiten geparst: %s.
                                Außerdem gibt es Informationen über meinen aktuellen Standord: Ich bin gerade in oder in der Nähe von %s,
                                und das aktuelle Datum und die Uhrzeit ist: %s.
                                Nutze diese Informationen für die Antwort.
                                Bitte gib nie die URLs gleich mit raus, nennen sie auch nicht. Merke es dir nur für dann, wenn ich explizit danach frage.
                                """, modQuery, searchResult, defaultGeoCodedLocation, currentTime);

                        if (!searchResult.isEmpty()) {
                            new GPT(context, mainActivityRef.get(), speechRecognitionManager, tts).execute(new Message(currentTime, defaultLocation, message));
                            addToConversation(context, message);
                        }
                    }
                });
            }
        });
    }

    public void addToConversation(Context context, String text) {
        var conversation = SaveConversation.loadConversation(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        new GPSLocationHelper(context).getCurrentLocation(new GPSLocationHelper.GPSCallback() {
            @Override
            public void onLocationReceived(GPSLocation location) {
                conversation.addMessage(new Message(
                        DateTime.getCurrentDateTime(),
                        location,
                        text
                ));
            }

            @Override
            public void onLocationError(String errorMessage) {
                conversation.addMessage(new Message(
                        DateTime.getCurrentDateTime(),
                        new GPSLocation(48.9177, 8.7865),
                        text
                ));
            }
        });

        SaveConversation.saveConversation(context, conversation, true);
        SaveConversation.appendToLongTermMemory(context, "conversation.jsonl","conversation_long_term_memory.jsonl");
    }

    public String replaceGpsWithGeo(Context context, String input) {
        var m = Pattern.compile("([-+]?\\d*\\.?\\d+)[,\\s]+([-+]?\\d*\\.?\\d+)").matcher(input);
        return m.find() ? input.replaceFirst(m.group(0), getGeoCoded(context, new GPSLocation(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))))) : input;
    }

    public String getGeoCoded(Context context, GPSLocation location) {
        var geocoder = new Geocoder(context, Locale.getDefault());

        List<Address> adresses = new ArrayList<>();
        try {
            adresses = geocoder.getFromLocation(location.lat(), location.lon(), 1);
            System.out.println("Geocoded addresses: ");

            assert adresses != null;
            adresses.forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Following error occurred while geocoding: " +e);
        }

        var geocodedAddress = (!adresses.isEmpty()) ? adresses.get(0).getLocality() : "Niefern";
        System.out.println("Current resolved adress: " + geocodedAddress);

        return geocodedAddress;
    }

}
