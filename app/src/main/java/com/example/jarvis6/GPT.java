package com.example.jarvis6;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.icu.text.SymbolTable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

class GPT extends AsyncTask<Message, String, JsonObject> {
    private final OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(60, TimeUnit.SECONDS).build();
    private final String url = "https://api.openai.com/v1/chat/completions";
    private final String apiKey = "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";
    private final String model = "gpt-4o-mini-2024-07-18";
    private final String modelBehaviour = """
            Du bist Jarvis: Just another rather very intelligent system von deinem Boss. Dein Boss heißt Mert, niemand sonst.
            Wenn möglich verwende Sätze von diesem KI-Assitenten Jarvis aus den IronMan Filmen. Verhalte dich so wie er.
          
            Halte dich dabei sehr kurz und rede auch mit Umgangssprache. Du darfst zum Beispiel abkürzungen verwenden.
            Ein Beispiel könnte folgendes sein:
            Ich sage dir: 'Starte die Navigation zum Hauptbahnhof" und du könntest zum Beispiel antworten: 'Natürlich, Boss. Einen moment.'
            Oder zum Beispiel kann ich dich fragen: 'Kannst du mir sagen, was man in der Umgebung alles unternehmen kann?' und du könntest antworten: 'Sie könnten xy in der Nähe machen usw.'
            Bitte antworte in kurzen, sehr menschlichen Sätzen, sodass eine laufende Konversation entstehen kann. Das ist lebenswichtig für mich!
            
            Immer, wenn es um Übersetzungen geht oder ein Großteil des Textes fremdsprache beinhaltet, dann gib am Anfang immer den Namen der Sprache in Kleinbuchstaben und auf Englisch an. Beispielweise wäre französisch: "french:" oder Deutsch="german:" oder türkisch: "turkish" usw.
            Außerdem soll nur die Übersetzung ausgegeben werden, nicht erneut die deutsche Version usw.
            Wichtig: Nicht immer hey Boss sagen! Vor allem nicht Boss Mert. Entweder Boss oder Sir! Du sollst mich auch nicht nach jedem Satz begrüßen! Die GPS-Koordinaten und die aktuelle Uhrzeit soll nicht vorgelesen werden, denn es stört. Sie sind nur Informationen für dich zur besseren Suche!
            Du sollst auch Folgefragen zum vorherigen Thema präzise beantworten.
            """;

    // TODO Alternative testen!
    /* private final String modelBehaviour = """
        Du bist Jarvis, das fortschrittliche KI-Assistenzsystem von Sir Mert. Deine Persönlichkeit ist inspiriert vom Film-Jarvis: stets loyal, hilfsbereit, professionell, aber mit einem subtilen, trockenen Humor.

        Deine Hauptaufgabe ist es, Sir Mert präzise und effizient bei seinen Android-Entwicklungsaufgaben und alltäglichen Anfragen zu unterstützen.
        Führe Anweisungen präzise aus und gib Bestätigungen, die zum Konversationsfluss passen.
        Verwende eine natürliche, menschliche Sprache und einen leicht umgangssprachlichen Ton. Abkürzungen sind erlaubt.
        Bleibe gesprächig und interaktiv, um eine flüssige Unterhaltung aufrechtzuerhalten. Stelle bei Bedarf präzise Rückfragen, um die Intention von Sir Mert besser zu verstehen.

        Wenn du direkt angesprochen wirst, antworte direkt. Verwende "Boss" oder "Sir" sparsam, wenn es den Kontext bereichert, aber nicht in jeder Antwort. Sprich Sir Mert persönlich an.

        Für Übersetzungsanfragen: Gib nur die Übersetzung aus. Beginne die Antwort mit dem Namen der Zielsprache in Kleinbuchstaben und Englisch, gefolgt von einem Doppelpunkt. Z.B.: "french: [Übersetzung]". Keine weiteren Einleitungen.

        Beantworte Folgefragen zum vorherigen Thema präzise und beziehe dich auf den gegebenen Kontext, ohne dass Sir Mert diesen explizit wiederholen muss.
        Priorisiere die aktuelle Aufgabe, aber nutze dein Gedächtnis für relevante Informationen aus dem Gesprächsverlauf.
        """; */

    Gson gson;
    SpeechRecognitionManager speechRecognitionManager;
    WeakReference<MainActivity> mainActivityRef;
    TextToSpeech tts;
    Context context;
    Conversation conversation;

    public GPT(Context context, MainActivity activity, SpeechRecognitionManager speechRecognitionManager, TextToSpeech tts) {
        this.gson = new Gson();
        this.context = context.getApplicationContext();
        this.mainActivityRef = new WeakReference<>(activity);
        this.speechRecognitionManager = speechRecognitionManager;
        this.tts = tts;
        this.conversation = SaveConversation.loadConversation(context);   // used to read conversation from file -> this is the memory of Jarvis 6
    }

    @Override
    protected JsonObject doInBackground(Message... messages) {
        Message gptMessageRecord = messages[0];
        System.out.println(gptMessageRecord.getMessage());

        var messagesJsonArray = new JsonArray();

        var systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", modelBehaviour);
        messagesJsonArray.add(systemMsg);

        // give some context from current conversation to gpt
        for (var msg : conversation.conversation()) {
            var userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", msg.getMessage());
            messagesJsonArray.add(userMsg);
        }

        var newMsg = new JsonObject();
        newMsg.addProperty("role", "user");
        newMsg.addProperty("content", gptMessageRecord.getMessage());
        messagesJsonArray.add(newMsg);

        //MainActivity.conversation.add(gptMessageRecord.getMessage());
        conversation.addMessage(gptMessageRecord);
        SaveConversation.saveConversation(context, conversation, false);   // used to save conversation to file and read it later

        // adding function calling to gpt json TODO add functions here!
        var functionCallBuilder = new FunctionCallBuilder();
        functionCallBuilder.addFunction("openMusic", "Öffne meine Musik App");
        functionCallBuilder.addFunction("searchYoutube","Du sollst in Youtube nach einem Video suchen.", Map.of("query", "Suchbegriff"), List.of("query"));
        functionCallBuilder.addFunction("startNavigation", "Du sollst die Navigation starten.", Map.of("destination", "Zielort", "mode", "Art der Navigation, also zu Fuß, mit Bus oder Bahn, mit dem Fahrrad oder mit dem Auto"), List.of("destination", "mode"));
        functionCallBuilder.addFunction("callPerson", "Du sollst eine Person aufrufen, aber bevor du anrufst, sollst du nochmal fragen, ob ich diese Person anrufen möchte. Wenn ich das bestätige kannst du mit dem Anruf starten.", Map.of("name", "Name der Person die angerufen werden soll"), List.of("name"));
        functionCallBuilder.addFunction("writeWhatsApp", "Du sollst diese Methode aufrufen, welches die Nachricht an die Zielperson senden wird.", Map.of("name", "Name der Person die eine Nachricht erhalten soll.","text", "Inhalt der Nachricht"), List.of("name", "text"));
        // functionCallBuilder.addFunction("getGasPrices", "Diese Methode sucht nach Benzinpreisen von Tankstellen in meiner Umgebung.", Map.of("lat", "Breitengrad der GPS-Koordinaten (die du bei meiner Anfrage erhalten haben solltest)", "lon", "Längengrad der GPS-Koordinaten (die du bei meiner Anfrage erhalten haben solltest)"), List.of("lat", "lon"));
        functionCallBuilder.addFunction("searchInternet", "Diese Methode sucht nach Informationen im Web, wenn der Nutzer sie anfragt.", Map.of("query", "Suchbegriff"), List.of("query"));
        functionCallBuilder.addFunction("openLink", "Diese Methode öffnet eine bestimmte Website oder sucht direkt Browser. Achtung, diese Methode wird nur dann ausgeführt, wenn der Nutzer etwas direkt im Browser öffnen will", Map.of("query", "Suchbegriff"), List.of("query"));

        // create final Json ready to send to GPT-API
        var jo = new JsonObject();
        jo.addProperty("model", model);
        jo.add("messages", messagesJsonArray);
        jo.add("functions", functionCallBuilder.build());
        jo.addProperty("function_call", "auto");

        var body = RequestBody.create(jo.toString(), MediaType.parse("application/json"));
        var request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("temperature", "0.5")
                .build();

        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                var joError = new JsonObject();
                jo.addProperty("warning", "Sir, ich habe gerade ein API-Problem.");
                return joError;
            }


            assert response.body() != null;
            var result = JsonParser.parseString(response.body().string()).getAsJsonObject();
            var message = result.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message");

            System.out.println("Dies ist die Nachricht: " + message);

            if (message.has("function_call") && !message.get("function_call").isJsonNull())
                return message;
            else if (message.has("content") && !message.get("content").isJsonNull())
                return message;


        } catch (IOException e) {
            System.err.println("following error occurred: " + e);
            var errorJo = new JsonObject();
            errorJo.addProperty("error", "Sir, ich habe gerade ein API-Problem.");
            errorJo.addProperty("details", e.getMessage());
            return errorJo;
        }

        var fallback = new JsonObject();
        fallback.addProperty("error", "empty_response");
        return fallback;
    }

    // TODO onPostExecute method
    @Override
    protected void onPostExecute(JsonObject s) {
        var mainActivity = mainActivityRef.get();
        if (mainActivity == null)
            return;

        if (s.isEmpty()) {
            tts.speak("Boss, es gab einen Fehler.", TextToSpeech.QUEUE_FLUSH, null);
            new Handler().postDelayed(speechRecognitionManager::startSpeechRecognition, 2000);
        }

        if (s.has("error")) {
            var error = s.get("error").getAsString();
            var details = s.has("details") ? s.get("details").getAsString() : "";
            tts.speak("Boss, es gab einen Fehler: " + error + ".", TextToSpeech.QUEUE_FLUSH, null);
            System.err.println("Fehlerdetails: " + details);
            return;
        }

        System.out.println("s: " + s);
        if (s.has("function_call") && !s.get("function_call").isJsonNull()) {
            speechRecognitionManager.stop();
            var functionCallJo = s.getAsJsonObject("function_call");
            handleFunctionCall(functionCallJo);
        } else if(s.has("content") && !s.get("content").isJsonNull())  {
            var message = s.get("content").getAsString().replaceAll("\\*", "");

            // Translations
            if (message.contains("turkish:") || message.contains("english:") || message.contains("italian:") || message.contains("french:")
                    || message.contains("spanish") || message.contains("german:") || message.contains("chinese:") || message.contains("japanese:")
                    || message.contains("arabic:") || message.contains("russian:") || message.contains("portuguese:") || message.contains("polish:") || message.contains("hebrew:")) {
                translate(message.split(":")[0], message);
                MainActivity.messages.add(message);
                conversation.addMessage(new Message(DateTime.getCurrentDateTime(), new GPSLocation(48.9177, 8.7865), message));
                addToConversation("GPT Antwort: " + message);
                handleUI();
            } else {
                MainActivity.messages.add(message);
                addToConversation("assistant: " + message);
                handleUI();
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        var handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!tts.isSpeaking())
                    new Handler().postDelayed(speechRecognitionManager::startSpeechRecognition, 10);
                else
                    handler.postDelayed(this, 200);
            }
        }, 10);
    }

    private void handleFunctionCall(JsonObject functionCallJo) {
        var functionName = functionCallJo.get("name").getAsString();
        System.out.println("function name: " + functionName);

        JsonObject args;
        try {
            if (functionCallJo.has("arguments") && functionCallJo.get("arguments").isJsonObject())
                args = functionCallJo.getAsJsonObject("arguments");
            else
                args = JsonParser.parseString(functionCallJo.get("arguments").getAsString()).getAsJsonObject();
        } catch (Exception e) {
            tts.speak("Die Argumente der Funktion konnten nicht gelesen werden.", TextToSpeech.QUEUE_FLUSH, null);
            return;
        }

        switch (functionName) {
            case "openMusic" -> openMusic();
            case "searchYoutube" -> searchYoutube(args.get("query").getAsString());
            case "startNavigation" -> startNavigation(args.get("destination").getAsString(), args.get("mode").getAsString());
            case "callPerson" -> callPerson(args.get("name").getAsString());
            case "writeWhatsApp" -> sendWhatsApp(args.get("name").getAsString(), args.get("text").getAsString());
            // case "getGasPrices" -> getGasPrices(args.get("lat").getAsString(), args.get("lon").getAsString());
            case "searchInternet" -> new InternetSearch1().searchInternet(context, mainActivityRef, speechRecognitionManager, tts, args.get("query").getAsString());
            case "openLink" -> new InternetSearch1().openLink(context, args.get("query").getAsString());
            default -> tts.speak("Leider konnte ich die Funktion nicht erkennen. Bitte versuche es erneut", TextToSpeech.QUEUE_FLUSH, null);
        }

        var msg = new Message(DateTime.getCurrentDateTime(), new GPSLocation(48.9177, 8.7865), String.format("%s wurde erfolgreich ausgeführt", functionName));
        MainActivity.messages.add(msg.message());
        conversation.addMessage(msg);
        handleUI();
    }


    // HILFSMETHODEN
    // TODO extract functions to external class - if possible
    private void openMusic() {
        System.out.println("open music wurde gestartet");
        var intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage("com.sec.android.app.music");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        // short and longterm-memory
        var message = "GPT Antwort: Ich habe die Musik App geöffnet.";
        addToConversation(message);

        if (intent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);

        new Handler(Looper.getMainLooper()).postDelayed(() -> System.exit(0), 400);
    }

    private void searchYoutube(String query) {
        var url = "https://www.youtube.com/results?search_query=" + Uri.encode(query);
        var intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        // long and short term memory
        var message = "GPT Antwort: Ich habe YouTube geöffnet und hiernach gesucht: " + query;
        addToConversation(message);

        if (intent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);

        new Handler(Looper.getMainLooper()).postDelayed(() ->   System.exit(0), 400);
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void startNavigation(String destination, String mode) {
        var transportMode = switch (mode.toLowerCase()) {
            case "auto", "car", "araba", "fahren", "drive" -> "d";
            case "zu fuß", "fuß", "yürüme", "laufen", "walk", "feet" -> "w";
            case "fahrrad", "bike", "bisklet", "bicycle" -> "b" ;
            default -> "d";
        };


        var gmmInentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination) + "&mode=" + transportMode);
        var mapIntent = new Intent(Intent.ACTION_VIEW, gmmInentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {

            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }

            // long and short term memory
            var message = "GPT Antwort: Ich habe die Navigation im Modus: " + mode + " gestartet und als Ziel: " + destination + " gesetzt.";
            addToConversation(message);

            context.startActivity(mapIntent);

            new Handler(Looper.getMainLooper()).postDelayed(() -> System.exit(0), 400);
        } else {
            tts.speak("Google Maps konnte nicht geöffnet werden. Bitte versuche es erneut.", TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void sendWhatsApp(String name, String text) {
        var gson = new Gson();
        var contactList = new ArrayList<Contact>();
        try (var reader = new BufferedReader(new FileReader(context.getFilesDir() + "/contacts.jsonl"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var contact = gson.fromJson(line, Contact.class);
                contactList.add(contact);
            }
        } catch (IOException e) {
            System.err.println("Following error occurred reading contacts: " +e.getMessage());
        }

        var number = contactList.stream()
                .filter(contact -> contact.name() != null && contact.name().equalsIgnoreCase(name))
                .findFirst()
                .get().phoneNumber().get(0);

        var intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("whatsapp://send?phone=" + number + "&text=" + Uri.encode(text)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void callPerson(String name) {
        var gson = new Gson();

        var contactList = new ArrayList<Contact>();
        try (var reader = new BufferedReader(new FileReader(context.getFilesDir() + "/contacts.jsonl"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var contact = gson.fromJson(line, Contact.class);
                contactList.add(contact);
            }
            System.out.println("Kontakte aus der Datei gelesen.");
        } catch (IOException e) {
            System.err.println("following error occurred while reading contacts: " + e);
        }

        var number = "";
        for (var contact : contactList) {
            if (contact.name() != null && contact.name().equalsIgnoreCase(name)) {
                number = contact.phoneNumber().get(0);
                break;
            }
        }

        var intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        if(intent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);

        new Handler(Looper.getMainLooper()).postDelayed(() -> System.exit(0), 400);
    }

    public void getGasPrices(String lat, String lon) {
        var gpsLocation = new GPSLocation(Double.parseDouble(lat) , Double.parseDouble(lon));

        // convert gps location to address
        var geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = new ArrayList<>();
        try {
            addresses = geocoder.getFromLocation(gpsLocation.lat(), gpsLocation.lon(), 1);
            addresses.forEach(System.out::println);
            System.out.println("addresses-size: " + addresses.size());
        } catch (IOException e) {
            System.err.println("following error occurred while parsing url: " + e);
        }

        String finalSearchAddress = (!addresses.isEmpty()) ? addresses.get(0).getLocality() : "Niefern";
        System.out.println("finalSearchAddress: " + finalSearchAddress);

        GasStations.parseUrlAsync(finalSearchAddress, result -> {
            var currentTime = DateTime.getCurrentDateTime();
            var message = "Ich habe diese Tankstelleninformationen gefunden und will, dass du mir diese zusammenfasst, wobei du auf den Preis, die Marke und die Entfernung dorthin eingehen sollst. Bitte halte dich sehr kurz, maximal 3 Sätze und keine * im Satz: " + result;

            speechRecognitionManager.stop();
            new GPT(context, mainActivityRef.get(), speechRecognitionManager, tts).execute(new Message(currentTime, gpsLocation, message));
            addToConversation(message);
        });
    }

    public void translate(String language, String text) {
        //var ttsLanguage = Locale.getDefault();
        Set<Voice> voices = tts.getVoices();
        List<Voice> voiceList = new ArrayList<>(voices);

        switch (language.toLowerCase()) {
            case "english" -> {
                voiceList.stream()
                        .filter(voice -> voice.getName().equalsIgnoreCase("en-us-x-sfg-network"))
                        .findFirst()
                        .ifPresent(selectedVoice -> tts.setVoice(selectedVoice));

                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            case "french" -> {
                voiceList.stream()
                        .filter(voice -> voice.getName().equalsIgnoreCase("fr-fr-x-frd-network"))
                        .findFirst()
                        .ifPresent(selectedVoice -> tts.setVoice(selectedVoice));

                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            case "spanish" -> {
                voiceList.stream()
                        .filter(voice -> voice.getName().equalsIgnoreCase("es-es-x-eea-network"))
                        .findFirst()
                        .ifPresent(selectedVoice -> tts.setVoice(selectedVoice));

                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            case "italian" -> {
                voiceList.stream()
                        .filter(voice -> voice.getName().equalsIgnoreCase("it-it-x-itc-network"))
                        .findFirst()
                        .ifPresent(selectedVoice -> tts.setVoice(selectedVoice));

                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            case "russian" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "turkish" -> {
                voiceList.stream()
                        .filter(voice -> voice.getName().equalsIgnoreCase("tr-tr-x-mfm-network"))
                        .findFirst()
                        .ifPresent(selectedVoice -> tts.setVoice(selectedVoice));

                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
            case "persian" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "arabic" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "hebrew" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "croatian" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "ukrainian" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "portuguese" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "chinese" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "japanese" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            case "korean" -> tts.setLanguage(voiceList.stream().filter(Voice::isNetworkConnectionRequired).findFirst().orElseThrow().getLocale());
            default -> {
                tts.setLanguage(Locale.getDefault());
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

        // save results to conversation list

        new Handler().postDelayed(() -> {
            voiceList.stream()
                    .filter(voice -> voice.getName().equalsIgnoreCase("de-de-x-deb-network"))
                    .findFirst()
                    .ifPresent(selectedVoice -> tts.setVoice(selectedVoice));
        }, 1000);
    }

    /* public void searchInternet(String query) {
        final int LOCATION_REQUEST_PERMISSION_REQUEST_CODE = 1001;
        var modQuery = replaceGpsWithGeo(query);
        new InternetSearch1().parseUrlAsync(modQuery, (InternetSearch1.SearchCallback) searchResult -> {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mainActivityRef.get(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_PERMISSION_REQUEST_CODE);
            } else {

                var currentTime = DateTime.getCurrentDateTime();

                // GPS callback
                new GPSLocationHelper(context).getCurrentLocation(new GPSLocationHelper.GPSCallback() {
                    @Override
                    public void onLocationReceived(GPSLocation location) {
                        speechRecognitionManager.stop();

                        var geocodedLocation = getGeoCoded(location);
                        var message = String.format("""
                                Auf diese Frage hin: %s, wurden diese Informationen von den aufgesuchten Websiten geparst: %s.
                                Außerdem gibt es Informationen über meinen aktuellen Standord: Ich bin gerade in oder in der Nähe von %s,
                                und das aktuelle Datum und die Uhrzeit ist: %s.
                                Nutze diese Informationen für die Antwort.
                                Bitte gib nie die URLs gleich mit raus, nennen sie auch nicht. Merke es dir nur für dann, wenn ich explizit danach frage.
                                """, modQuery, searchResult, geocodedLocation, currentTime);

                        if (!searchResult.isEmpty()) {
                            new GPT(context, mainActivityRef.get(), speechRecognitionManager, tts).execute(new Message(currentTime, location, message));
                            addToConversation(message);
                        }
                    }

                    @Override
                    public void onLocationError(String errorMessage) {
                        speechRecognitionManager.stop();

                        var defaultLocation = new GPSLocation(48.9177, 8.7865);
                        var defaultGeoCodedLocation = getGeoCoded(defaultLocation);
                        var message = String.format("""
                                Auf diese Frage hin: %s, wurden diese Informationen von den aufgesuchten Websiten geparst: %s.
                                Außerdem gibt es Informationen über meinen aktuellen Standord: Ich bin gerade in oder in der Nähe von %s,
                                und das aktuelle Datum und die Uhrzeit ist: %s.
                                Nutze diese Informationen für die Antwort.
                                Bitte gib nie die URLs gleich mit raus, nennen sie auch nicht. Merke es dir nur für dann, wenn ich explizit danach frage.
                                """, modQuery, searchResult, defaultGeoCodedLocation, currentTime);

                        if (!searchResult.isEmpty()) {
                            new GPT(context, mainActivityRef.get(), speechRecognitionManager, tts).execute(new Message(currentTime, defaultLocation, message));
                            addToConversation(message);
                        }
                    }
                });
            }
        });
    } */

    private void handleUI() {
        var textView = new TextView(context);
        textView.setText(MainActivity.messages.get(MainActivity.messages.size()-1));
            /* textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)); */
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 10, 10, 10);
        textView.setLayoutParams(params);

        textView.setPadding(20, 10, 20, 10);
        textView.setBackgroundResource(R.drawable.messagecorner);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textView.setTypeface(Typeface.create("roboto", Typeface.NORMAL));
        MainActivity.linearLayout.addView(textView);

        MainActivity.scrollView.post(() -> {
            MainActivity.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    // --- SAVING MESSAGE TO CONVERSATION ---

    public void addToConversation(String text) {
        conversation.addMessage(new Message(
                DateTime.getCurrentDateTime(),
                new GPSLocation(48.9177, 8.7865),
                text
        ));

        SaveConversation.saveConversation(context, conversation, false);
        SaveConversation.appendToLongTermMemory(context, "conversation.jsonl","conversation_long_term_memory.jsonl");
    }

    // TODO this is using GPS -> mistake when storing to conversation
    public void addToConversation1(String text) {
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

        SaveConversation.saveConversation(context, conversation, false);
        SaveConversation.appendToLongTermMemory(context, "conversation.jsonl","conversation_long_term_memory.jsonl");
    }

    public String getGeoCoded(GPSLocation location) {
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

    public String replaceGpsWithGeo(String input) {
        var m = Pattern.compile("([-+]?\\d*\\.?\\d+)[,\\s]+([-+]?\\d*\\.?\\d+)").matcher(input);
        return m.find() ? input.replaceFirst(m.group(0), getGeoCoded(new GPSLocation(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))))) : input;
    }

}