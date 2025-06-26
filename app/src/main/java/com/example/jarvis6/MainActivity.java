package com.example.jarvis6; // Passe den Paketnamen an

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.Toast;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

import static android.media.AudioManager.MODE_IN_COMMUNICATION;
import static android.media.AudioManager.MODE_NORMAL;
import static androidx.core.content.ContextCompat.registerReceiver;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2;
    private static final int REQUEST_PERMISSIONS = 123;

    public static List<String> conversation = new ArrayList<>();
    public static List<String> messages = new ArrayList<>();

    private TextView textView;
    private TextInputEditText inputEditText;
    public static ScrollView scrollView;
    public static LinearLayout linearLayout;

    private SpeechRecognitionManager speechRecognitionManager;
    private TextToSpeech tts;
    private AudioManager audioManager;
    // private BluetoothHelper bluetoothHelper;
    private BluetoothHelper1 bluetoothHelper1;

    private MemorySaver memorySaver;


    /* private final BroadcastReceiver scoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE , -1);
            if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                audioManager.startBluetoothSco();
                audioManager.setMode(MODE_IN_COMMUNICATION);
                audioManager.setBluetoothScoOn(true);
                audioManager.setSpeakerphoneOn(false);
            }
        }
    }; */

    private final BroadcastReceiver scoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);

            if(state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                bluetoothHelper1.onScoDisconnected();
                System.out.println("Bluetooth is connected");
            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                System.out.println("Bluetooth is disconnected ");
                bluetoothHelper1.onScoDisconnected();
            } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                bluetoothHelper1.onScoDisconnected();
                System.out.println("Bluetooth connecting");
            } else {
                System.out.println("Bluetooth connection error!");
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = findViewById(R.id.scrollView4);
        linearLayout = scrollView.findViewById(R.id.linearLayoutId);

        // autoscroll chat down
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        // textView = findViewById(R.id.text);
        inputEditText = findViewById(R.id.inputField);
        tts = new TextToSpeech(this, this);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        bluetoothHelper1 = new BluetoothHelper1(this, audioManager);
        // bluetoothHelper.startBluetoothConnection(audioManager);

        requestBluetoothPermissions();

        var filter = new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(scoReceiver, filter);

       // new NotificationHelper(getApplicationContext()).showNotification("Smart Assistant", "Benachrichtigung vom Smart Assistant", MainActivity.class);
        var calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 27);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        NotificationScheduler.scheduleNotification(getApplicationContext(), "Jarvis 6", "Benachrichtigung vom Smart Assistant", calendar);

        memorySaver = new MemorySaver();

        // Berechtigungen anfragen
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_CALENDAR
                    }, REQUEST_PERMISSIONS);
        }


        var helper = new ContactsHelper(this);
        helper.loadContacts(contacts -> {
            System.out.println("Fertig geladen: " + contacts.size());

            var gson = new Gson();
            try (var writer = new BufferedWriter(new FileWriter(getFilesDir() + "/contacts.jsonl", true))) {
                for (var contact : contacts) {
                    var jo = gson.toJson(contact);
                    writer.write(jo);
                    writer.newLine();
                }

                writer.flush();
                System.out.println("Contacts erfolgreich geschrieben in contacts.jsonl.");
            } catch (Exception e) {
                System.err.println("following error occurred while writing contacts: " + e);
            }
        });

        speechRecognitionManager = new SpeechRecognitionManager(this, this, tts, memorySaver);

        // TODO try to refactor -> extract out of constructor
        var startRoutine = new StartRoutine(this, MainActivity.this, tts, new GPT(getApplicationContext(), MainActivity.this, speechRecognitionManager, tts));
        startRoutine.simpleGreetUser(DateTime.getCurrentDateTime(), new GPSLocation(48.9177, 8.7865));


        inputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                var inputText = inputEditText.getText().toString().trim();
                memorySaver.saveRequests(inputText);
                messages.add(inputText);
                handleUI();
                //textView.setText(inputText);

                if (!inputText.equalsIgnoreCase("[\\s'']")) {
                    if (inputText.contains("Auf Wiedersehen") || inputText.contains("tschüss") || inputText.equalsIgnoreCase("auf Wiedersehen") || inputText.equalsIgnoreCase("wiedersehen") || inputText.equalsIgnoreCase("bye") || inputText.equalsIgnoreCase("kapat")) {
                        SaveConversation.saveConversation(this, new Conversation(new ArrayList<>()), false);   // use to delete memory after closing app to aviod too huge requests!
                        onDestroy();
                    } else {
                        inputEditText.setText("");

                        // TODO update this like in SpeechRecognitionManager!!!
                        new GPT(getApplicationContext(), MainActivity.this, speechRecognitionManager, tts).execute(new Message("no date", new GPSLocation(48.9177, 8.7865), inputText));
                    }
                }
                return true;
            }
            return false;
        });
    }

    public void handleUI() {
        var textView = new TextView(MainActivity.this);
        // var requestsList = memorySaver.readRequests();
        //textView.setText(requestsList.get(Math.max(0, requestsList.size()-1)));
        textView.setText(messages.get(messages.size()-1));

        // textView.setText("default text");
        var params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 10, 10, 10);
        textView.setLayoutParams(params);

        // textView.setGravity(Gravity.END);
        textView.setGravity(Gravity.RIGHT);
        textView.setPadding(20, 10, 20, 10);
        textView.setBackgroundResource(R.drawable.corner);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textView.setTypeface(Typeface.create("Light300", Typeface.NORMAL));
        linearLayout.addView(textView);

        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
        }
        if (audioManager != null) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
        }

        SaveConversation.appendToLongTermMemory(this, "conversation.jsonl","conversation_long_term_memory.jsonl");
        SaveConversation.saveConversation(this, new Conversation(new ArrayList<>()), false);   // use to delete memory after closing app to avoid too huge requests!

        bluetoothHelper1.stopBluetoothScoConnection();
        unregisterReceiver(scoReceiver);
    }

    public void requestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_CONNECT_PERMISSION);
            } else {
                // Berechtigung ist bereits erteilt.
                // Optional: Hier könnte man den Initialisierungsversuch starten,
                // falls die App direkt mit BT-Audio starten soll.
                // checkAndStartScoConnection();
            }
        } else {
            // Ältere Android-Versionen benötigen BLUETOOTH_CONNECT nicht zur Laufzeit
            // checkAndStartScoConnection();
        }
    }

    // Ergebnis der Berechtigungsanfrage behandeln
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("permission for audio is granted");
            } else {
                Toast.makeText(this, "Mikrofonberechtigung erforderlich", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_BLUETOOTH_CONNECT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("Bluetooth-Berechtigung erteilt.");
                //  checkAndStartScoConnection();
            } else {
                System.out.println("Bluetooth-Berechtigung abgelehnt.");
            }
        }
    }

    @Override
    public void onInit(int i) {
        //tts.setLanguage(new Locale("de_DE"));
        tts.setLanguage(Locale.GERMAN);
        tts.setEngineByPackageName("com.google.android.tts");

        Set<Voice> voices = tts.getVoices();
        List<Voice> voiceList = new ArrayList<>(voices);
        for (int j = 0; j < voiceList.size(); j++) {
            System.out.println(j +": " + voiceList.get(j));
        }
        // var selectedVoice = voiceList.get(55);  // german voice online very good
        // var selectedVoice = voiceList.get(135);  // german voice female not so good
        // var selectedVoice = voiceList.get(247);  // german voice online very good - calm
        // var selectedVoice = voiceList.get(345);  // german voice offline
        // var selectedVoice = voiceList.get(431);  // german voice offline
        voiceList.stream()
                .filter(voice -> voice.getName().equalsIgnoreCase("de-de-x-deb-network"))
                .findFirst()
                .ifPresent(selectedVoice -> tts.setVoice(selectedVoice));

        tts.setSpeechRate(1.3f);
    }

    // HelperMethod
    public String getGeoCoded(GPSLocation location) {
        var geocoder = new Geocoder(this, Locale.getDefault());

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