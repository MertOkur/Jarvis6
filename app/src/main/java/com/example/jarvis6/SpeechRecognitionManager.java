package com.example.jarvis6;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.jarvis6.MainActivity.messages;

class SpeechRecognitionManager {
    private final Context context;
    private final AudioManager audioManager;
    private final TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private final MainActivity mainActivity;
    private final MemorySaver memorySaver;
    private GPSLocationHelper gpsLocationHelper;

    public SpeechRecognitionManager(Context context, MainActivity mainActivity, TextToSpeech tts, MemorySaver memorySaver) {
        this.context = context;
        this.mainActivity = mainActivity;
        this.tts = tts;
        this.memorySaver = memorySaver;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.gpsLocationHelper = new GPSLocationHelper(context);
    }

    public void startSpeechRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                System.out.println("Es ist ein Fehler aufgetreten: " + i);

                if (i == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || i == SpeechRecognizer.ERROR_SERVER) {
                    stop();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> startSpeechRecognition(), 500);
                } else if (i == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || i == SpeechRecognizer.ERROR_NO_MATCH) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> startSpeechRecognition(), 100);
                } else {
                    tts.speak("Ich konnte Sie nicht verstehen", TextToSpeech.QUEUE_FLUSH, null);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> startSpeechRecognition(), 2000);
                }
            }

            @Override
            public void onResults(Bundle bundle) {
                var result = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                // var confidenceScores = bundle.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES); // TODO later implement a method, which also uses other results based on confidence scores

                if (result != null && !result.isEmpty()) {
                    var recognizedText = result.get(0);

                    messages.add(recognizedText);
                    if (messages.size() >= 8)
                        messages.remove(0);

                    memorySaver.saveRequests(recognizedText);
                    mainActivity.runOnUiThread(mainActivity::handleUI);

                    if (recognizedText.toLowerCase().contains("auf wiedersehen") || recognizedText.toLowerCase().contains("tschüss") || recognizedText.toLowerCase().contains("kapat") || recognizedText.toLowerCase().contains("görüşürüz") || recognizedText.toLowerCase().contains("hoşça kal")) {
                        stop();
                        SaveConversation.appendToLongTermMemory(context, "conversation.jsonl","conversation_long_term_memory.jsonl");
                        SaveConversation.saveConversation(context, new Conversation(new ArrayList<>()), false);   // use to delete memory after closing app to aviod too huge requests!

                        System.exit(0);
                    }

                    stop();
                    //audioManager.setMode(AudioManager.MODE_NORMAL);

                    if (ActivityCompat.checkSelfPermission(mainActivity.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(mainActivity.getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        System.out.println("GPS permission not granted");
                        return;
                    }

                    gpsLocationHelper.getCurrentLocation(new GPSLocationHelper.GPSCallback() {
                        @Override
                        public void onLocationReceived(GPSLocation location) {
                            var message = new Message(
                                    DateTime.getCurrentDateTime(),
                                    location,
                                    recognizedText
                            );

                            new GPT(context, mainActivity, SpeechRecognitionManager.this, tts).execute(message);
                        }

                        @Override
                        public void onLocationError(String errorMessage) {
                            var message = new Message(
                                    DateTime.getCurrentDateTime(),
                                    new GPSLocation(48.9177, 8.7865),   // my home
                                    recognizedText);
                            new GPT(context, mainActivity, SpeechRecognitionManager.this, tts).execute(message);
                        }
                    });

                    //new GPT().respond(context, mainActivity, SpeechRecognitionManager.this, tts);

                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        var intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        try {
            speechRecognizer.startListening(intent);
        } catch (ActivityNotFoundException e) {
            System.err.println("Activity not found exception: " + e);
        }
    }

   /*  public static String getCurrentDateTime() {
        var now = LocalDateTime.now();
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return now.format(formatter);
    } */

    public void stop() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
        }
    }
}
