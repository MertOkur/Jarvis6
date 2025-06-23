package com.example.jarvis6;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

class SaveConversation {
    public static void saveConversation(Context context, Conversation conversation, boolean isAppending) {
        var gson = new Gson();
        var directory = context.getFilesDir();
        var file = new File(directory, "conversation.jsonl");

        try (var writer = new BufferedWriter(new FileWriter(file, isAppending))) {
            for (var msg : conversation.conversation()) {
                var jsonLine = gson.toJson(msg);
                writer.write(jsonLine);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving conversation: " + e);
        }
    }

    public static Conversation loadConversation(Context context) {
        var gson = new Gson();
        var directory = context.getFilesDir();
        var file = new File(directory, "conversation.jsonl");

        if (!file.exists())
            return new Conversation(new ArrayList<>());

        var currentConversationList = new ArrayList<Message>();
        try (var reader = new BufferedReader(new FileReader(file))) {
            var line = "";
            while ((line = reader.readLine()) != null) {
                try {
                    var message = gson.fromJson(line, Message.class);
                    currentConversationList.add(message);
                } catch (JsonSyntaxException e) {
                    System.err.println("Error parsing JSON: " + e);
                }
            }
            return new Conversation(currentConversationList);
        } catch (IOException e) {
            System.err.println("Error loading conversation: " + e);
            return new Conversation(new ArrayList<>());
        }
    }

    public static void appendToLongTermMemory(Context context, String sourceName, String targetName) {
        var source = new File(context.getFilesDir(), sourceName);
        var target = new File(context.getFilesDir(), targetName);

        if (!target.exists()) {
            try {
                target.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (var reader = new BufferedReader(new FileReader(source));
             var writer = new BufferedWriter(new FileWriter(target, true))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error appending to long-term memory: " +e);
        }
    }
 }
