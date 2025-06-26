package com.example.jarvis6;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static androidx.core.content.ContextCompat.registerReceiver;

class StartRoutine {

    private static final Logger log = LoggerFactory.getLogger(StartRoutine.class);
    Context context;
    MainActivity mainActivity;
    TextToSpeech tts;
    GPT gpt;
    String notifications;
    Conversation conversation;

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public StartRoutine(Context context, MainActivity mainActivity, TextToSpeech tts, GPT gpt) {
        this.context = context;
        this.mainActivity = mainActivity;
        this.tts = tts;
        this.gpt = gpt;
        this.conversation = SaveConversation.loadConversation(context);

        var gson = new Gson();
        var list = new ArrayList<Notification>();
        try (var reader = new BufferedReader(new FileReader(context.getFilesDir() + "/notifications.jsonl"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(gson.fromJson(line, Notification.class));
            }
        } catch (Exception e) {
            System.err.println("following error occurred while reading notifications: " + e);
        }

        // GPS-Text
        new GPSLocationHelper(context).requestPreciseCurrentLocation(new GPSLocationHelper.GPSCallback() {
            @Override
            public void onLocationReceived(GPSLocation location) {
                System.out.println("GPS war erfolgreich!");
                var geoCodedLocation = getGeoCoded(location);
                var time = DateTime.getCurrentDateTime();
                var notifications = list.stream()
                        .map(p -> String.format("package: %s, title: %s, text: %s", p.pkg(), p.title(), p.text()))
                        .collect(Collectors.joining("\n"));
                var calendar = getCalenderInfo();

                var backgroundContext = String.format("""
                   Aktuelle Lokation: %s. Aktuelles Datum und Zeit: %s.
                   Benachrichtigungen: %s. Ereignisse und Infos im Kalender: %s.
                   """, geoCodedLocation, time, notifications, calendar);

                var message = new Message(time, location, backgroundContext);
                conversation.addMessage(message);
                MainActivity.messages.add(message.message());
                handleUI();

                SaveConversation.saveConversation(context, conversation, true);
                // SaveConversation.appendToLongTermMemory(context, "conversation.jsonl","conversation_long_term_memory.jsonl");
            }

            @Override
            public void onLocationError(String errorMessage) {
                System.out.println("GPS war nicht erfolgreich!");
                var currentDeviceLocation = new GPSLocation(48.9177, 8.7865);
                var geoCodedLocation = getGeoCoded(currentDeviceLocation);
                var time = DateTime.getCurrentDateTime();
                var notifications = list.stream()
                        .map(p -> String.format("package: %s, title: %s, text: %s", p.pkg(), p.title(), p.text()))
                        .collect(Collectors.joining("\n"));
                var calendar = getCalenderInfo();


                var backgroundContext = String.format("""
                   Aktuelle Lokation: %s. Aktuelles Datum und Zeit: %s.
                   Benachrichtigungen: %s. Ereignisse und Infos im Kalender: %s.
                   """, geoCodedLocation, time, notifications, calendar);

                var message = new Message(time, currentDeviceLocation, backgroundContext);
                conversation.addMessage(message);
                MainActivity.messages.add(message.message());
                handleUI();

                SaveConversation.saveConversation(context, conversation, true);
                // SaveConversation.appendToLongTermMemory(context, "conversation.jsonl","conversation_long_term_memory.jsonl");
            }
        });
    }

    public String getCalenderInfo() {
        var calendarHelper = new CalenderHelper(context);
        var events = calendarHelper.getEvents();

        var today = CalenderHelper.getCurrentDay();
        var todayEvents = events.stream()
                .filter(event -> event.startTime().equals(today) || event.endTime().equals(today))
                .collect(Collectors.toList());

        var followingEvents = events.stream()
                .filter(event -> !event.startTime().equals(today) && !event.endTime().equals(today))
                .collect(Collectors.toList());

        return getStringBuilder(todayEvents, today, followingEvents).toString();
    }

    /* public void greetUser() {
        var calendarHelper = new CalenderHelper(context);
        var events = calendarHelper.getEvents();

        var today = CalenderHelper.getCurrentDay();
        var todayEvents = events.stream()
                .filter(event -> event.startTime().equals(today) || event.endTime().equals(today))
                .collect(Collectors.toList());

        var followingEvents = events.stream()
                .filter(event -> !event.startTime().equals(today) && !event.endTime().equals(today))
                .collect(Collectors.toList());

        if (todayEvents.isEmpty() && followingEvents.isEmpty()) {
            //tts.speak("Hey Boss. Wie kann ich dir helfen?", TextToSpeech.QUEUE_FLUSH, null);
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;

            new GPSLocationHelper(context).getCurrentLocation(new GPSLocationHelper.GPSCallback() {
                @Override
                public void onLocationReceived(GPSLocation location) {
                    var geocodedLocation = getGeoCoded(location).toLowerCase();
                    int resId = geocodedLocation.contains("niefern") ? R.raw.jarvis_greeting : R.raw.heyboss;

                    playMP3(MediaPlayer.create(context, resId));
                    handleGPTCall(todayEvents, today, followingEvents, location);
                }

                @Override
                public void onLocationError(String errorMessage) {
                    // tts.speak("Günaydın efendim. Size nasıl yardımcı olabilirim?", TextToSpeech.QUEUE_FLUSH, null);
                    tts.speak("Hey Boss! Wie kann ich helfen?", TextToSpeech.QUEUE_FLUSH, null);
                    handleGPTCall(todayEvents, today, followingEvents, new GPSLocation(48.9177, 8.7865));
                }
            });

           return;
        }

       playMP3(MediaPlayer.create(context, R.raw.heyboss));
        handleGPTCall(todayEvents, today, followingEvents, new GPSLocation(48.9177, 8.7865));
        //gpt.execute(new Message(today, new GPSLocation(48.9177, 8.7865), "Begrüße Mert: Zum Beispiel mit: Hey Boss. Wie kann ich helfen? Oder Guten Tag Boss. Oder: Zu Ihren Diensten."));
    } */

    public void simpleGreetUser(String time, GPSLocation gpsLocation) {
        new Handler().postDelayed(() -> {
            var gptPrompt = String.format("""
                    Bitte begrüße den Boss Mert ganz kurz und höflich. Deine Begrüßung soll der aktuellen Tageszeit entsprechen,also morgen, mittag oder abend.
                    Aktuelle Uhrzeit: %s.
                    """, time);
            gpt.execute(new Message(time, gpsLocation, gptPrompt));
        }, 3000);
    }

    private void handleGPTCall(List<CalenderHelper.Event> todayEvents, String today, List<CalenderHelper.Event> followingEvents, @NonNull GPSLocation currentLocation) {
        new Handler().postDelayed(() -> {
            var backgroundInfoForLLM = getStringBuilder(todayEvents, today, followingEvents).toString();
            String gptPrompt = String.format("""
                Begrüße den Boss ganz kurz und höflich!

                Du startest gerade die App, also hat der Nutzer noch nichts gesagt. Prüfe still im Hintergrund die heutigen Kalendereinträge und die Benachrichtigungen (Notifications):
                Danach gibst du dem Nutzer ein kurzes Briefing:
                
                – Sprich nur wichtige Termine heute an (keine Feiertage oder Standardkalendereinträge).
                – Sprich nur wichtige Benachrichtigungen von WhatsApp oder Gmail heute an.
                – Wenn nichts Wichtiges ansteht, sag einfach, dass der Tag frei aussieht.
                – Mach passende Vorschläge basierend auf Uhrzeit, Wetter, Terminen oder bekannten Interessen.
                – Antworte direkt wie ein persönlicher Assistent, ohne zu erwähnen, dass du Daten analysierst oder dir etwas merkst.

                Hier sind die aktuellen Benachrichtigunen: %s.
            
                %s

                Sei dabei maximal kurz und direkt!
            """, notifications, backgroundInfoForLLM);

            // Türkisch: Still in the old version
            /* var gptPrompt = String.format("""
                    Lütfen patronu kısaca selamla.
                    
                    Bunlar sana sadece bir komut olarak verdiğim şeylerdir, bu görevleri kullanıcıya vermemelisin. Ayrıca not aldığını da söylememelisin!
                    Çünkü bu yöntem uygulama başlatıldığında çağrılır. Yani daha önce kullanıcı hiçbir şey söylememiştir. Buna göre, ilk selamlamayı sen yapıyorsun.
                    
                    Kullanıcının takvim girişlerini alacaksın.
                    Takvimdeki girişleri sadece bugünün gerçekten önemliyse okuyacaksın. Özellikle sadece tatil günleri ise okuma.
                    Ayrıca bu metni de bahsetmemelisin ve not aldığını vb. de belirtmemelisin. Bunların hepsi arka planda olmalı.
                    
                    Bir örnek şöyle görünebilir:
                    Hey Patron. Mevcut takvimin boş görünüyor. Sana nasıl yardımcı olabilirim?
                    Sonra ben şöyle bir soru sorabilirim: Bugün toplu taşıma ile Stuttgart'a gitmek istiyorum.
                    Şimdi, tatil günlerini okuduğun için şöyle cevap verebilirsin: Elbette. Ancak unutma ki bugün/yarın bir tatil ve ulaşım hizmetleri kısıtlı olabilir.
                    
                    Ya da başka bir örnek:
                    Hey Patron. Bugün bir doktor randevun olduğunu gördüm. Oraya navigasyonu başlatmamı ister misin, yoksa internette mi arama yapayım?
                    Sonra ben şöyle cevap verebilirim: Evet, lütfen daha fazla bilgi almak istiyorum. Çalışma saatleri nedir?
                    O zaman sen örneğin bir internet araması başlatabilirsin.
                    
                    Yani pratik olarak, randevu takvimine bakan ve planları bilen bir asistan gibi, ancak her cevaptan sonra takvimde ne olduğunu söylemeyen. Sadece çakışmalar varsa veya önemli olabilecek durumlarda bunu belirtiyorsun.
                    
                    %s
                    
                    Lütfen çok kısa tut!
                    """, backgroundInfoForLLM); */

           // new GPT(context, mainActivity, new SpeechRecognitionManager(context, mainActivity, tts, new MemorySaver()), tts).execute(new Message(today, currentLocation, gptPrompt));
            gpt.execute(new Message(today, currentLocation, gptPrompt));
        }, 3000);

        // events.forEach(System.out::println);
    }

    private static StringBuilder getStringBuilder(List<CalenderHelper.Event> todayEvents, String today, List<CalenderHelper.Event> followingEvents) {
        var sb = new StringBuilder();
        if (!todayEvents.isEmpty()) {
            sb.append(String.format("""
                These is information retrieved from calendar:
                Today is: %s. The title for the event today is: %s. The description for the event today is: %s.
                The location for the event today is: %s. The start time for the event today is: %s. The end time for the event today is: %s.
                """, today, todayEvents.get(0).title(), todayEvents.get(0).description(), todayEvents.get(0).location(), todayEvents.get(0).startTime(), todayEvents.get(0).endTime())
            );
        } else {
            sb.append("There are no events today.");
        }

        if (!followingEvents.isEmpty()) {
            sb.append(String.format("""
                There is also information on the next two days:
                These events have this title: %s. These events have this description: %s.
                These events have this location: %s. These events have this start time: %s. These events have this end time: %s.
            """, followingEvents.get(0).title(), followingEvents.get(0).description(), followingEvents.get(0).location(), followingEvents.get(0).startTime(), followingEvents.get(0).endTime()));
        } else {
            sb.append("There are no events on the next two days.");
        }
        return sb;
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

    public void playMP3(MediaPlayer mediaPlayer) {
        // var jarvisGreetingPlayer = MediaPlayer.create(context, R.raw.jarvis_greeting);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mPlayer) {
                if (mPlayer != null) {
                    mPlayer.release();
                    mPlayer = null;
                }
            }
        });

        mediaPlayer.start();
    }

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


}